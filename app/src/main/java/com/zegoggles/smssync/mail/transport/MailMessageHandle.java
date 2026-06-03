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

import com.fsck.k9.mail.store.imap.ImapMessage;

/**
 * Opaque app-owned handle for a server-side IMAP message.
 *
 * <p>The wrapped k-9 {@link ImapMessage} reference is package-private to {@code mail.transport}
 * and NEVER exposed via any public accessor (CNTR-MODERNIZATION-007 validation rule #4).
 * Engine code ({@code service.*}) treats this as an opaque token.
 *
 * <p>The {@link #uid} field is readable for logging/deduplication; the k-9 message
 * reference is accessible only within {@code mail.transport} for the adapter's
 * internal handle ⇄ k-9 Message resolution.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned value types.
 */
public final class MailMessageHandle {

    /** The IMAP UID for this message. Readable by callers for logging/deduplication. */
    public final String uid;

    /** Package-private: only accessible within {@code mail.transport}. */
    final ImapMessage message;

    /**
     * Package-private constructor — only {@link K9MailTransport} creates instances.
     *
     * @param uid     the IMAP UID of the message
     * @param message the k-9 ImapMessage being wrapped; must not be {@code null}
     */
    MailMessageHandle(String uid, ImapMessage message) {
        this.uid = uid;
        this.message = message;
    }
}
