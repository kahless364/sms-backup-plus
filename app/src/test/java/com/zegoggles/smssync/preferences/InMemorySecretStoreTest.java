package com.zegoggles.smssync.preferences;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for InMemorySecretStore.
 *
 * Tests the full round-trip (put -> get -> contains -> remove -> contains -> clear)
 * and the null-on-absent contract required by CNTR-MODERNIZATION-003.
 *
 * Runs on the plain JVM — no Robolectric, no Android Keystore required.
 */
public class InMemorySecretStoreTest {

    private InMemorySecretStore store;

    @Before
    public void setUp() {
        store = new InMemorySecretStore();
    }

    // AC-7 / CNTR-MODERNIZATION-003: full round-trip
    @Test
    public void roundTrip_putGetContainsRemoveContainsClear() {
        // put
        store.put("login_password", "secret123");
        store.put("oauth2_token", "accesstoken");

        // get returns the stored value
        assertThat(store.get("login_password")).isEqualTo("secret123");
        assertThat(store.get("oauth2_token")).isEqualTo("accesstoken");

        // contains returns true for stored keys
        assertThat(store.contains("login_password")).isTrue();
        assertThat(store.contains("oauth2_token")).isTrue();

        // remove deletes one key
        store.remove("login_password");
        assertThat(store.contains("login_password")).isFalse();
        assertThat(store.get("login_password")).isNull();

        // other key still present
        assertThat(store.contains("oauth2_token")).isTrue();

        // clear removes all remaining keys
        store.clear();
        assertThat(store.contains("oauth2_token")).isFalse();
        assertThat(store.get("oauth2_token")).isNull();
    }

    // AC-3 / CNTR-MODERNIZATION-003 §Error Handling: null returned for absent key
    @Test
    public void get_absentKey_returnsNull() {
        assertThat(store.get("any_absent_key")).isNull();
    }

    // AC-3: null returned even after clear
    @Test
    public void get_afterClear_returnsNull() {
        store.put("oauth2_refresh_token", "refreshtoken");
        store.clear();
        assertThat(store.get("oauth2_refresh_token")).isNull();
    }

    // contains returns false for absent key
    @Test
    public void contains_absentKey_returnsFalse() {
        assertThat(store.contains("nonexistent_key")).isFalse();
    }

    // put overwrites existing value
    @Test
    public void put_existingKey_overwritesValue() {
        store.put("oauth2_token", "first_value");
        store.put("oauth2_token", "second_value");
        assertThat(store.get("oauth2_token")).isEqualTo("second_value");
    }

    // remove on absent key is a no-op (no exception)
    @Test
    public void remove_absentKey_isNoOp() {
        store.remove("nonexistent_key");  // must not throw
        assertThat(store.contains("nonexistent_key")).isFalse();
    }

    // clear on empty store is a no-op
    @Test
    public void clear_emptyStore_isNoOp() {
        store.clear();  // must not throw
    }

    // put with null value is supported (matches SharedPreferences.getString null default)
    @Test
    public void put_nullValue_storedAndReturned() {
        store.put("oauth2_token", null);
        // contains() should return true (key was explicitly stored)
        assertThat(store.contains("oauth2_token")).isTrue();
        assertThat(store.get("oauth2_token")).isNull();
    }
}
