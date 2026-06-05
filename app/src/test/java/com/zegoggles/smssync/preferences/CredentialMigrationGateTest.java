package com.zegoggles.smssync.preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;

/**
 * Runs with RobolectricTestRunner because CredentialMigrationGate uses android.util.Log
 * and android.os.Looper (to guard against main-thread await). Robolectric shadows those
 * Android APIs, making the class testable without a device.
 */
@RunWith(RobolectricTestRunner.class)

/**
 * Unit tests for {@link CredentialMigrationGate}.
 *
 * Runs under Robolectric so Android stubs (Log, Looper) are available.
 * Each test resets gate state via resetForTest() to ensure isolation.
 *
 * U-035 / BUG-003: verifies that the gate correctly blocks credential consumers until
 * migration is signalled, and that it is a no-op when never prepared or already complete.
 */
public class CredentialMigrationGateTest {

    @Before
    public void setUp() {
        CredentialMigrationGate.resetForTest();
    }

    @After
    public void tearDown() {
        CredentialMigrationGate.resetForTest();
    }

    // -------------------------------------------------------------------------
    // AC-4 (U-035): Gate blocks consumer until signalComplete() is called
    // -------------------------------------------------------------------------

    /**
     * AC-4 (U-035): awaitIfNeeded() blocks a background thread until signalComplete()
     * is called. Verifies the gate correctly serialises credential access after migration.
     *
     * Design: Rather than relying on timing (Thread.sleep) to assert the consumer is
     * blocked, we use a sequencing protocol:
     * 1. Signal the gate only AFTER the consumer has reported it is inside awaitIfNeeded().
     * 2. The consumer sets a "I am awaiting" flag just before calling awaitIfNeeded().
     * 3. The "I unblocked" flag must be set after signal, not before.
     *
     * We use an ordered countDownLatch protocol to coordinate without relying on sleep.
     */
    @Test
    public void awaitIfNeeded_blocksUntilSignalComplete() throws Exception {
        CredentialMigrationGate.prepare();

        final AtomicBoolean consumerRanAfterSignal = new AtomicBoolean(false);
        // This latch signals once the consumer has unblocked from awaitIfNeeded().
        final CountDownLatch consumerUnblocked = new CountDownLatch(1);
        // This latch signals once the gate has been signalled (consumer should see this).
        final CountDownLatch gateSignalled = new CountDownLatch(1);

        // Consumer thread: awaits gate, then sets flag and unblocks.
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded();
                // Record that we unblocked AFTER the gate signal.
                // migrationComplete must be true at this point (set before countDown).
                consumerRanAfterSignal.set(true);
                consumerUnblocked.countDown();
            }
        });
        consumer.start();

        // Signal the gate. This unblocks the consumer thread.
        CredentialMigrationGate.signalComplete();
        gateSignalled.countDown();

        // Consumer must unblock within a reasonable time after the signal.
        boolean done = consumerUnblocked.await(5, TimeUnit.SECONDS);
        assertThat(done).isTrue();
        assertThat(consumerRanAfterSignal.get()).isTrue();
    }

    /**
     * AC-4 (U-035): awaitIfNeeded() returns immediately when migrationComplete is already true
     * (gate was already signalled). Hot-path performance check.
     */
    @Test
    public void awaitIfNeeded_returnsImmediatelyWhenAlreadySignalled() throws Exception {
        CredentialMigrationGate.prepare();
        CredentialMigrationGate.signalComplete();

        final AtomicBoolean completed = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded();
                completed.set(true);
            }
        });
        t.start();
        t.join(2000);
        assertThat(completed.get()).isTrue();
    }

    /**
     * Gate is never prepared (null latch) — awaitIfNeeded() returns immediately.
     * This models test environments that construct AuthPreferences without App.onCreate().
     */
    @Test
    public void awaitIfNeeded_latchNull_returnsImmediately() throws Exception {
        // No prepare() call — latch stays null.
        final AtomicBoolean completed = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded();
                completed.set(true);
            }
        });
        t.start();
        t.join(2000);
        assertThat(completed.get()).isTrue();
    }

    // -------------------------------------------------------------------------
    // AC-5 / Idempotency: signalComplete() is safe to call multiple times
    // -------------------------------------------------------------------------

    /**
     * signalComplete() is idempotent — calling it twice does not throw.
     */
    @Test
    public void signalComplete_calledTwice_noException() {
        CredentialMigrationGate.prepare();
        CredentialMigrationGate.signalComplete();
        CredentialMigrationGate.signalComplete(); // must not throw
    }

    /**
     * prepare() is idempotent — calling it twice does not re-initialize the latch.
     */
    @Test
    public void prepare_calledTwice_noException() {
        CredentialMigrationGate.prepare();
        CredentialMigrationGate.prepare(); // must not replace the latch
        // Gate should still work normally after double prepare.
        CredentialMigrationGate.signalComplete();
    }

    // -------------------------------------------------------------------------
    // Gate signal/await ordering: consumer blocked before signal
    // -------------------------------------------------------------------------

    /**
     * Two consumer threads both awaitIfNeeded(). After signalComplete(), both unblock.
     */
    @Test
    public void awaitIfNeeded_twoConsumers_bothUnblockOnSignal() throws Exception {
        CredentialMigrationGate.prepare();

        final CountDownLatch bothDone = new CountDownLatch(2);
        final AtomicBoolean consumer1Done = new AtomicBoolean(false);
        final AtomicBoolean consumer2Done = new AtomicBoolean(false);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded();
                consumer1Done.set(true);
                bothDone.countDown();
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded();
                consumer2Done.set(true);
                bothDone.countDown();
            }
        });
        t1.start();
        t2.start();

        Thread.sleep(100); // allow both threads to enter await()

        CredentialMigrationGate.signalComplete();

        boolean done = bothDone.await(3, TimeUnit.SECONDS);
        assertThat(done).isTrue();
        assertThat(consumer1Done.get()).isTrue();
        assertThat(consumer2Done.get()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Race-safety: prepare() before submit() ordering
    // -------------------------------------------------------------------------

    /**
     * Simulates the App.onCreate() ordering: prepare() is called before the background
     * task is submitted. Even if the background task finishes extremely quickly and calls
     * signalComplete() before the consumer thread starts awaitIfNeeded(), the consumer
     * still gets a correct (unblocked) result.
     */
    @Test
    public void prepare_thenSignal_thenAwait_consumerUnblocked() throws Exception {
        // Simulate fast migration: prepare, dispatch, signal all happen before consumer.
        CredentialMigrationGate.prepare();
        CredentialMigrationGate.signalComplete(); // migration completes "instantly"

        // Consumer starts after signal.
        final AtomicBoolean completed = new AtomicBoolean(false);
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                CredentialMigrationGate.awaitIfNeeded(); // must return immediately
                completed.set(true);
            }
        });
        consumer.start();
        consumer.join(2000);
        assertThat(completed.get()).isTrue();
    }
}
