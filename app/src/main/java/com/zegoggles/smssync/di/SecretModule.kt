package com.zegoggles.smssync.di

import android.content.Context
import com.zegoggles.smssync.preferences.EncryptedPrefsSecretStore
import com.zegoggles.smssync.preferences.SecretStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SecretStore -> EncryptedPrefsSecretStore for the SingletonComponent.
 *
 * U-022: AC-5 — SecretModule is ACTIVE because EncryptedPrefsSecretStore was delivered
 * by U-011 (DES-004). Construction requires @ApplicationContext (the Keystore-backed
 * MasterKey is tied to the application context), so @Provides is used.
 *
 * The @Singleton scope ensures one EncryptedSharedPreferences backing store is opened
 * across the app lifetime — consistent with the singleton pattern recommended by
 * AndroidX Security (opening multiple instances over the same backing file is unsafe).
 *
 * DES-MODERNIZATION-008 §Module layout: SecretModule | @Provides SecretStore from
 * @ApplicationContext + Keystore master key (impl from DES-004).
 */
@Module
@InstallIn(SingletonComponent::class)
object SecretModule {

    @Provides
    @Singleton
    fun provideSecretStore(
        @ApplicationContext context: Context
    ): SecretStore = EncryptedPrefsSecretStore(context)
}
