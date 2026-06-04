package com.zegoggles.smssync.mail.transport;

import android.content.ContentValues;
import com.zegoggles.smssync.mail.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link MessageImportResult} value class.
 *
 * Covers the success constructor and the static failure factory per the
 * coverage gap identified in U-026 followup (MessageImportResult: 6/13 lines covered).
 */
@RunWith(RobolectricTestRunner.class)
public class MessageImportResultTest {

    // -------------------------------------------------------------------------
    // Success constructor
    // -------------------------------------------------------------------------

    @Test
    public void successConstructor_uid_isStored() {
        ContentValues cv = new ContentValues();
        MessageImportResult result = new MessageImportResult("uid-001", DataType.SMS, cv);

        assertThat(result.uid).isEqualTo("uid-001");
    }

    @Test
    public void successConstructor_dataType_isStored() {
        ContentValues cv = new ContentValues();
        MessageImportResult result = new MessageImportResult("uid-002", DataType.MMS, cv);

        assertThat(result.dataType).isEqualTo(DataType.MMS);
    }

    @Test
    public void successConstructor_contentValues_isStored() {
        ContentValues cv = new ContentValues();
        cv.put("body", "hello");
        MessageImportResult result = new MessageImportResult("uid-003", DataType.SMS, cv);

        assertThat(result.contentValues).isSameInstanceAs(cv);
    }

    @Test
    public void successConstructor_failed_isFalse() {
        ContentValues cv = new ContentValues();
        MessageImportResult result = new MessageImportResult("uid-004", DataType.CALLLOG, cv);

        assertThat(result.failed).isFalse();
    }

    @Test
    public void successConstructor_nullDataType_isAllowed() {
        // DataType may be null when the converter cannot determine type
        ContentValues cv = new ContentValues();
        MessageImportResult result = new MessageImportResult("uid-005", null, cv);

        assertThat(result.dataType).isNull();
        assertThat(result.failed).isFalse();
    }

    @Test
    public void successConstructor_nullContentValues_isAllowed() {
        // ContentValues may be null when conversion is not supported for this message
        MessageImportResult result = new MessageImportResult("uid-006", DataType.SMS, null);

        assertThat(result.contentValues).isNull();
        assertThat(result.failed).isFalse();
    }

    // -------------------------------------------------------------------------
    // failure() static factory
    // -------------------------------------------------------------------------

    @Test
    public void failure_uid_isStored() {
        MessageImportResult result = MessageImportResult.failure("uid-err-001");

        assertThat(result.uid).isEqualTo("uid-err-001");
    }

    @Test
    public void failure_failed_isTrue() {
        MessageImportResult result = MessageImportResult.failure("uid-err-002");

        assertThat(result.failed).isTrue();
    }

    @Test
    public void failure_dataType_isNull() {
        MessageImportResult result = MessageImportResult.failure("uid-err-003");

        assertThat(result.dataType).isNull();
    }

    @Test
    public void failure_contentValues_isNull() {
        MessageImportResult result = MessageImportResult.failure("uid-err-004");

        assertThat(result.contentValues).isNull();
    }

    // -------------------------------------------------------------------------
    // Distinction between success and failure
    // -------------------------------------------------------------------------

    @Test
    public void failure_isDistinctFrom_success() {
        ContentValues cv = new ContentValues();
        MessageImportResult success = new MessageImportResult("uid-x", DataType.SMS, cv);
        MessageImportResult failure = MessageImportResult.failure("uid-x");

        assertThat(success.failed).isFalse();
        assertThat(failure.failed).isTrue();
    }
}
