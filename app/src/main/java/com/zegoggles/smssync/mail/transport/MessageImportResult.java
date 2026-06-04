/*
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.mail.transport;

import android.content.ContentValues;
import androidx.annotation.Nullable;
import com.zegoggles.smssync.mail.DataType;

/**
 * App-owned result of a per-message body fetch and conversion.
 *
 * <p>Returned by {@link MailTransport#importMessageBody} so that the restore engine
 * ({@code RestoreTask}) can act on message content without importing any
 * {@code com.fsck.k9.*} type.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned value types (bounded-residual converter bridge).
 */
public final class MessageImportResult {

    /** The IMAP UID of the imported message, for deduplication tracking. */
    public final String uid;

    /** The {@link DataType} determined by the message converter (SMS, MMS, CALLLOG). */
    @Nullable
    public final DataType dataType;

    /** The converted Android {@link ContentValues} for provider insertion, or {@code null}
     *  if conversion is not supported for this message. */
    @Nullable
    public final ContentValues contentValues;

    /**
     * Whether conversion failed (e.g. unknown DataType, MessagingException during fetch).
     * When {@code true}, {@link #contentValues} is {@code null} and the engine should skip this
     * message.
     */
    public final boolean failed;

    /** Constructs a successful import result. */
    public MessageImportResult(String uid, @Nullable DataType dataType,
                                @Nullable ContentValues contentValues) {
        this.uid = uid;
        this.dataType = dataType;
        this.contentValues = contentValues;
        this.failed = false;
    }

    /** Constructs a failed import result (e.g. fetch or conversion error). */
    private MessageImportResult(String uid) {
        this.uid = uid;
        this.dataType = null;
        this.contentValues = null;
        this.failed = true;
    }

    /** Factory: constructs a failure result. */
    public static MessageImportResult failure(String uid) {
        return new MessageImportResult(uid);
    }
}
