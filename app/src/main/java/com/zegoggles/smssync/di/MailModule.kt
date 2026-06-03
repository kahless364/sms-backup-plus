package com.zegoggles.smssync.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds MailTransport -> K9MailTransport (or K9ImapTransport per DES-009) for the SingletonComponent.
 *
 * U-022: AC-5 — MailModule is STUBBED for bootstrap.
 *
 * Reason: K9MailTransport (the current adapter from U-025) declares a constructor that throws
 * checked exceptions (MailException, MessagingException). Dagger @Provides methods cannot
 * propagate checked exceptions — doing so results in a compile-time Dagger error. The live
 * binding therefore requires either:
 *   a) A no-throw factory wrapper (authored in U-026 when the engine is rewired), or
 *   b) A K9ImapTransport adapter class (DES-009's named class) with a no-throw constructor.
 *
 * Until U-026 / DES-009 provides the clean adapter constructor, this module is a compilable
 * stub with no active bindings.
 *
 * TODO(U-026): activate @Binds MailTransport <- K9MailTransport (or K9ImapTransport) once the
 * adapter has a no-throw constructor compatible with Dagger @Provides / @Binds requirements.
 *
 * DES-MODERNIZATION-008 §Module layout: MailModule | @Binds MailTransport <- K9ImapTransport
 * (impl from DES-009).
 */
@Module
@InstallIn(SingletonComponent::class)
object MailModule {
    // TODO(U-026): activate when K9ImapTransport / clean MailTransport adapter lands
    //
    // @Binds
    // abstract fun bindMailTransport(impl: K9MailTransport): MailTransport
    //
    // Blocked by: K9MailTransport constructor throws MailException/MessagingException —
    // incompatible with Dagger @Provides (checked exceptions are not supported).
    // DES-009 / U-026 will deliver the adapter with a no-throw constructor.
}
