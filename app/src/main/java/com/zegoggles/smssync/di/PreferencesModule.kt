package com.zegoggles.smssync.di

import android.content.Context
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides app-wide preference singletons for the SingletonComponent.
 *
 * U-022: AC-5/AC-6 — PreferencesModule is ALWAYS ACTIVE (Preferences, AuthPreferences,
 * and DataTypePreferences are existing concrete classes; no sibling-story adapter needed).
 *
 * All three types are scoped @Singleton. This is safe because:
 * - Preferences and AuthPreferences are stateless readers of SharedPreferences; the
 *   underlying SharedPreferences instance is already a singleton via
 *   PreferenceManager.getDefaultSharedPreferences (verified DES-MODERNIZATION-008).
 * - Converting from per-call construction to @Singleton removes the ARCH-002 defect
 *   without changing the observable behaviour of any read operation.
 *
 * DES-MODERNIZATION-008 §Provider strategy and §Module layout.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    /**
     * Provides the app-wide Preferences singleton.
     *
     * U-022: AC-3 — replaces 'new Preferences(this)' in App.onCreate() (AC-3) and
     * 'new Preferences(getApplicationContext())' in ServiceBase.getPreferences() (AC-4).
     * grep for 'new Preferences(' in App.java and ServiceBase.java must return zero (AC-6).
     */
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): Preferences =
        Preferences(context)

    /**
     * Provides the app-wide AuthPreferences singleton.
     *
     * U-022: AC-4/AC-6 — replaces 'new AuthPreferences(this)' in
     * ServiceBase.getAuthPreferences() (verified source line ServiceBase.java:112).
     *
     * Note: AuthPreferences(Context) constructor uses the default SecretStore
     * (EncryptedPrefsSecretStore) via its own internal construction. The two-arg
     * constructor AuthPreferences(Context, SecretStore) is available for tests
     * (InMemorySecretStore injected via @BindValue / @HiltAndroidTest in U-023).
     */
    @Provides
    @Singleton
    fun provideAuthPreferences(@ApplicationContext context: Context): AuthPreferences =
        AuthPreferences(context)

    /**
     * Provides the app-wide DataTypePreferences singleton.
     *
     * DataTypePreferences is constructed from a SharedPreferences handle inside
     * Preferences.getDataTypePreferences(). This provider delegates to the Preferences
     * singleton so both share the same SharedPreferences handle — consistent behaviour
     * with the existing manual construction path.
     */
    @Provides
    @Singleton
    fun provideDataTypePreferences(preferences: Preferences): DataTypePreferences =
        preferences.getDataTypePreferences()
}
