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

import com.zegoggles.smssync.R;

/**
 * Signals a transient IMAP failure (e.g. "Unable to get IMAP prefix").
 *
 * <p>Replaces the {@code State.java:32-34} string-match on
 * {@code "Unable to get IMAP prefix"}. The adapter throws this when it observes
 * that specific k-9 condition; {@code State.getErrorMessage()} then classifies by
 * <strong>type</strong> via the existing {@code LocalizableException} branch
 * ({@code State.java:35-36}) rather than by exception message text.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned exception hierarchy and
 * CNTR-MODERNIZATION-007 §Exception-translation mapping.
 */
public class TemporaryImapException extends MailException {

    /**
     * Constructs a {@code TemporaryImapException} wrapping the original k-9 throwable.
     *
     * @param cause the original k-9 throwable; may be {@code null} if not applicable
     */
    public TemporaryImapException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns the temporary-IMAP-error string resource ID.
     * Maps to {@code R.string.status_gmail_temp_error} ("Temporary IMAP error, try again later.").
     */
    @Override
    public int errorResourceId() {
        return R.string.status_gmail_temp_error;
    }
}
