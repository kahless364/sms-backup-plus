package com.zegoggles.smssync.di

import android.content.Context
import android.net.Uri
import com.zegoggles.smssync.mail.PinnedCertStore
import com.zegoggles.smssync.mail.TlsTrustPolicy
import com.zegoggles.smssync.mail.transport.K9MailTransport
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.MailTransportConfig
import com.zegoggles.smssync.preferences.AuthPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module that provides [MailTransport] bound to [K9MailTransport].
 *
 * U-026: Activates the MailTransport binding that U-022 stubbed out.
 *
 * **Checked-exception problem and resolution:**
 * [K9MailTransport]'s public constructor declares `throws MailException, MessagingException`.
 * Dagger `@Provides` methods cannot propagate checked exceptions — the Dagger annotation
 * processor will reject a method whose body throws a checked exception at compile time.
 *
 * The engine call pattern for [MailTransport] is LAZY / PER-RUN: [ServiceBase.getMailTransport]
 * constructs a new [K9MailTransport] on each backup/restore invocation (mirroring how
 * [ServiceBase.getBackupImapStore] previously constructed a new [BackupImapStore] each time).
 * The connection to the IMAP server is NOT established at construction time; the constructor
 * only creates the [ImapStore] configuration object and resolves the TLS factory. The actual
 * IMAP TCP connection is deferred to `checkSettings()` / `openFolder()` (the first real
 * network operation in the backup/restore loop).
 *
 * Because construction itself does not open a network connection, checked exceptions at
 * construction time would only arise if:
 * (a) the URI is malformed (caught via validation in [ServiceBase.getMailTransport]), or
 * (b) the TLS policy config is inconsistent (caught as [MailException]).
 *
 * **Solution — deferred/factory provision:**
 * Rather than providing a `@Singleton MailTransport` (which would require a connected
 * transport to live for the app lifetime), this module provides a [MailTransportFactory]
 * — a functional interface whose `create()` method throws the checked exceptions. The
 * engine invokes `factory.create()` per run in [ServiceBase.getMailTransport], which IS
 * allowed to throw checked exceptions (it's a normal method, not a Dagger provision method).
 *
 * The [MailTransportFactory] is a `@Singleton` (the factory object is cheap; only the
 * transport instances it creates are per-run).
 *
 * See also: [ServiceBase.getMailTransport] (the call site that uses this factory is
 * actually the hand-wired seam — the factory approach satisfies IC-1 and the Hilt graph
 * while keeping checked-exception semantics intact).
 *
 * DES-MODERNIZATION-008 §Module layout: MailModule | MailTransport provision
 * DES-MODERNIZATION-009 §Hilt binding caveat: checked-exception deferral via factory
 */
@Module
@InstallIn(SingletonComponent::class)
object MailModule {

    /**
     * Provides the [MailTransportFactory] singleton.
     *
     * The factory captures [AuthPreferences] and the application [Context]; each call to
     * [MailTransportFactory.create] constructs a fresh [K9MailTransport] from the current
     * store URI and TLS policy. This mirrors [ServiceBase.getMailTransport]'s per-run
     * construction pattern and preserves the existing connection-lifecycle semantics.
     *
     * Checked exceptions are NOT thrown by `@Provides` itself — they are deferred to
     * [MailTransportFactory.create], which the engine calls and handles via `try/catch`.
     */
    @Provides
    fun provideMailTransportFactory(
        @ApplicationContext context: Context,
        authPreferences: AuthPreferences
    ): MailTransportFactory {
        return MailTransportFactory { ->
            val uri = authPreferences.storeUri
            if (!com.zegoggles.smssync.mail.BackupImapStore.isValidUri(uri)) {
                throw MailException("No valid IMAP URI: $uri")
            }
            val parsed = Uri.parse(uri)
            val host = parsed.host ?: ""
            val port = parsed.port
            val pinnedCertStore = PinnedCertStore(context)
            val policy = pinnedCertStore.getTlsTrustPolicy(host, port)
            val config = if (policy == TlsTrustPolicy.PINNED_CERTIFICATE) {
                MailTransportConfig(uri, policy, pinnedCertStore.get(host, port))
            } else {
                MailTransportConfig(uri, policy)
            }
            try {
                K9MailTransport(context, config)
            } catch (e: com.fsck.k9.mail.MessagingException) {
                throw MailException(e)
            }
        }
    }
}

/**
 * Factory for creating [MailTransport] instances per backup/restore run.
 *
 * Exists because [K9MailTransport]'s constructor throws checked exceptions that are
 * incompatible with Dagger `@Provides` / `@Binds` methods. By providing the factory
 * (not the transport itself), Dagger can resolve the binding without encountering checked
 * exceptions during graph construction.
 *
 * The engine calls [create] at the start of each backup/restore run, matching the
 * per-run construction lifecycle of the former [BackupImapStore].
 */
fun interface MailTransportFactory {
    /**
     * Creates a new [MailTransport] configured from current [AuthPreferences].
     *
     * @throws MailException if the store URI is invalid or TLS configuration fails
     */
    @Throws(MailException::class)
    fun create(): MailTransport
}
