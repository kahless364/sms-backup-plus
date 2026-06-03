package com.zegoggles.smssync.service.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-006 characterization tests for service.exception classes.
 * Covers SmsProviderNotWritableException and MissingPermissionException which have 0% coverage.
 * These pin the exception type hierarchy and behavior before service layer refactoring.
 */
@RunWith(RobolectricTestRunner.class)
public class ExceptionCoverageTest {

    @Test public void smsProviderNotWritableException_isLocalizableException() {
        SmsProviderNotWritableException ex = new SmsProviderNotWritableException();
        assertThat(ex).isInstanceOf(LocalizableException.class);
        assertThat(ex).isInstanceOf(Exception.class);
        // errorResourceId() must return a valid resource ID (non-zero)
        assertThat(ex.errorResourceId()).isNotEqualTo(0);
    }

    @Test public void missingPermissionException_storesPermissions() {
        HashSet<String> permissions = new HashSet<>(Arrays.asList(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS"
        ));
        MissingPermissionException ex = new MissingPermissionException(permissions);
        assertThat(ex.permissions).isEqualTo(permissions);
    }

    @Test public void missingPermissionException_isException() {
        MissingPermissionException ex = new MissingPermissionException(new HashSet<String>());
        assertThat(ex).isInstanceOf(Exception.class);
    }
}
