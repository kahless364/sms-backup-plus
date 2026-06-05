package com.zegoggles.smssync.preferences;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zegoggles.smssync.App.TAG;

/**
 * Application-level completion gate for the one-time plaintext-to-encrypted
 * credential migration (U-035 / BUG-003 fix).
 *
 * <h3>Problem</h3>
 * {@link EncryptedPrefsSecretStore#migrateFromPlaintext()} was called synchronously on
 * the main thread in {@code App.onCreate()}, causing StrictMode disk-write violations
 * and ANR risk (BUG-003). Moving it to a background thread introduces a race: a
 * {@link com.zegoggles.smssync.service.BackupWorker} that starts within seconds of launch
 * could read credentials before migration is complete, observing a pre-migration or
 * mid-migration state.
 *
 * <h3>Mechanism</h3>
 * A {@link CountDownLatch}(1) is used as a one-shot gate. The latch is initialized
 * <em>before</em> the background migration task is dispatched (in
 * {@code App.onCreate()}) so the gate is always in place when consumers check it:
 *
 * <pre>
 * [Main thread] App.onCreate():
 *   CredentialMigrationGate.prepare();          // latch = new CountDownLatch(1)
 *   executor.submit(() -> {
 *       preferences.migrate();                  // ALL disk/crypto I/O on background thread
 *       CredentialMigrationGate.signalComplete(); // latch.countDown()
 *   });
 *
 * [Background thread — BackupWorker, later:]
 *   AuthPreferences.getOauth2Token():
 *       CredentialMigrationGate.awaitIfNeeded(); // latch.await() — blocks until signalled
 *       return secretStore.get(OAUTH2_TOKEN);
 * </pre>
 *
 * <h3>Race-safety argument</h3>
 * <ul>
 *   <li>{@link #prepare()} is called exactly once, before the background task is submitted.
 *       Happens-before the submit, which happens-before the migration run.</li>
 *   <li>{@link #signalComplete()} calls {@code latch.countDown()}, which establishes a
 *       happens-before relationship with any subsequent {@code latch.await()} return. Thus,
 *       all writes performed by {@code migrateFromPlaintext()} are visible to the credential
 *       reader after the gate releases (JMM §17.4.5).</li>
 *   <li>On a second launch (migration already complete), {@code migrateFromPlaintext()}
 *       hits the idempotency short-circuit immediately and returns. The background task
 *       still calls {@code signalComplete()}, so the gate releases immediately without
 *       blocking any consumer thread.</li>
 *   <li>{@link #awaitIfNeeded()} checks {@link #migrationComplete} (a volatile boolean set
 *       before latch countdown) and skips the latch await if already signalled. This avoids
 *       the overhead of {@code latch.await()} on the hot path after migration is complete.</li>
 *   <li>If the main thread has not yet called {@link #prepare()} (e.g., a test environment
 *       that constructs AuthPreferences directly), {@link #awaitIfNeeded()} treats the gate
 *       as already satisfied and skips blocking, preserving backward-compatible behavior for
 *       all existing tests that do not use the gate.</li>
 * </ul>
 *
 * <h3>Deadlock safety</h3>
 * {@link #awaitIfNeeded()} must NEVER be called on the main thread. The migration
 * background task submits to a dedicated I/O executor; the gate is awaited only on
 * background worker threads. If called on the main thread by mistake, the method
 * logs a warning and skips blocking to avoid ANR (fail-open, not fail-closed — correct
 * because the migration may or may not have completed; the worst case is a no-op
 * migration re-attempt on the next launch, not a credential loss).
 *
 * <h3>Timeout</h3>
 * {@link #awaitIfNeeded()} waits at most {@link #AWAIT_TIMEOUT_SECONDS} seconds.
 * If the migration has not completed within this window (e.g., Keystore stall), the
 * method logs a warning and returns. The credential read will succeed if migration
 * was already complete on the previous launch (fast path hit), or return null if no
 * credentials were ever written (correct for a fresh install).
 *
 * Contract: U-035 / BUG-003
 */
public final class CredentialMigrationGate {

    /** Timeout for credential consumers waiting on the gate (seconds). */
    static final long AWAIT_TIMEOUT_SECONDS = 10L;

    /**
     * The one-shot latch. Null until {@link #prepare()} is called.
     * Volatile so visibility is guaranteed across threads without synchronization
     * on the fast-path null check.
     */
    private static volatile CountDownLatch latch = null;

    /**
     * Set to true (volatile write, JMM flushed) immediately before {@code latch.countDown()}
     * in {@link #signalComplete()}. Allows {@link #awaitIfNeeded()} to skip the latch
     * {@code await()} on the hot path once migration is complete (every subsequent credential
     * read avoids the latch overhead).
     */
    private static volatile boolean migrationComplete = false;

    /** Prevent instantiation — all methods are static. */
    private CredentialMigrationGate() {}

    /**
     * Initializes the gate latch. Must be called on the main thread in
     * {@code App.onCreate()} <em>before</em> dispatching the background migration task.
     *
     * Idempotent: if already prepared (latch != null), this is a no-op. This handles
     * test scenarios where {@code App.onCreate()} may be called multiple times.
     */
    public static synchronized void prepare() {
        if (latch == null) {
            latch = new CountDownLatch(1);
        }
    }

    /**
     * Signals that migration has completed. Must be called by the background migration
     * task after {@link SecretStore#migrateFromPlaintext()} returns (whether it performed
     * the full migration or hit the idempotency short-circuit).
     *
     * Safe to call multiple times — {@code CountDownLatch.countDown()} below zero is a no-op.
     */
    public static void signalComplete() {
        migrationComplete = true;   // volatile write — establishes happens-before for await returns
        CountDownLatch l = latch;
        if (l != null) {
            l.countDown();
        }
        Log.d(TAG, "CredentialMigrationGate: migration gate signalled");
    }

    /**
     * Blocks the calling (non-main) thread until migration is complete or the timeout
     * elapses. No-op if migration was already complete before this call.
     *
     * <p>This method must be called from a background thread only. If called from the
     * main thread, it logs a warning and returns immediately (fail-open) to prevent ANR.
     *
     * <p>Called by {@link AuthPreferences} credential getters (getOauth2Token,
     * getOauth2RefreshToken, getImapPassword) to guarantee that credentials are read
     * after migration has durably committed them.
     */
    public static void awaitIfNeeded() {
        // Fast path: migration already complete (or never needed / already-migrated launch).
        if (migrationComplete) {
            return;
        }

        CountDownLatch l = latch;
        if (l == null) {
            // Gate was never prepared — test environment or post-migration launch where
            // prepare() was not called. Treat as already satisfied.
            return;
        }

        // Guard: do not block the main thread (would cause ANR).
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            Log.w(TAG, "CredentialMigrationGate.awaitIfNeeded() called on main thread — " +
                    "skipping await to prevent ANR. Migration may not yet be complete.");
            return;
        }

        try {
            boolean completed = l.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                Log.w(TAG, "CredentialMigrationGate: timed out after " + AWAIT_TIMEOUT_SECONDS +
                        "s waiting for credential migration. Proceeding without gate.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "CredentialMigrationGate: interrupted while waiting for migration gate.");
        }
    }

    /**
     * Resets the gate to its uninitialized state. For use in tests only.
     * Do NOT call this in production code.
     */
    static synchronized void resetForTest() {
        latch = null;
        migrationComplete = false;
    }
}
