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

/**
 * Signals an XOAuth2 authentication failure.
 *
 * <p>Replaces k-9 {@code com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException}
 * at the ACL boundary. The HTTP status code is preserved via {@link #getStatus()} so
 * the engine can branch on {@code getStatus() == 400} for token refresh logic
 * ({@code BackupTask.java:184}, {@code RestoreTask.java:158}).
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned exception hierarchy.
 */
public class XOAuth2FailedException extends MailException {

    private final int status;

    /**
     * Constructs an {@code XOAuth2FailedException}.
     *
     * @param status the HTTP status code from the k-9
     *               {@code XOAuth2AuthenticationFailedException.getStatus()} call
     * @param cause  the original k-9 throwable; preserved as {@link #getCause()}
     */
    public XOAuth2FailedException(int status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    /**
     * Returns the HTTP status code from the failed XOAuth2 exchange.
     * The engine branches on {@code getStatus() == 400} to decide whether to
     * attempt a token refresh (load-bearing: {@code BackupTask.java:184}).
     *
     * @return the HTTP status code, e.g. 400 for an invalid/expired token
     */
    public int getStatus() {
        return status;
    }
}
