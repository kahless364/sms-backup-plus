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
import com.zegoggles.smssync.service.exception.LocalizableException;

/**
 * Base app-owned transport error. The mandatory backstop for the k-9 ACL boundary:
 * any k-9 throwable not matched by a more specific catch maps to this exception,
 * ensuring no {@code com.fsck.k9.*} type can escape the adapter.
 *
 * <p>The original k-9 throwable is preserved as {@link #getCause()} so that
 * {@code State.getDetailedErrorMessage(Resources)} can read
 * {@code exception.getCause().toString()} for the "underlying=" diagnostic suffix
 * ({@code State.java:45}).
 *
 * <p>Pattern follows {@code RequiresLoginException.java:5}:
 * {@code extends Exception implements LocalizableException}.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned exception hierarchy.
 */
public class MailException extends Exception implements LocalizableException {

    /**
     * Constructs a {@code MailException} wrapping the original k-9 throwable.
     *
     * @param cause the original k-9 throwable; preserved as {@link #getCause()}
     *              for diagnostic output
     */
    public MailException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code MailException} with a message and no cause.
     * Used when the adapter itself detects a configuration error (e.g. null pinnedCert).
     *
     * @param message the error message
     */
    public MailException(String message) {
        super(message);
    }

    /**
     * Returns the generic IMAP error string resource ID.
     * The resource {@code R.string.status_unknown_error} is the nearest existing
     * generic-IMAP-failure string confirmed present in {@code strings.xml}.
     */
    @Override
    public int errorResourceId() {
        return R.string.status_unknown_error;
    }
}
