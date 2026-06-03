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
 * App-owned enum specifying which parts of a message to fetch.
 *
 * <p>Replaces k-9 {@code com.fsck.k9.mail.FetchProfile.Item} in the {@link MailTransport}
 * port boundary. The adapter maps each value to the corresponding k-9 constant internally.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned value types.
 */
public enum FetchSpec {

    /**
     * Fetch the envelope (headers including date) for sort ordering.
     * Maps to k-9 {@code FetchProfile.Item.DATE} inside the adapter.
     * Used by {@code BackupFolder.getMessages()} when trimming by date.
     */
    ENVELOPE_DATE,

    /**
     * Fetch the full message body for content parsing.
     * Maps to k-9 {@code FetchProfile.Item.BODY} inside the adapter.
     * Used by {@code RestoreTask.importMessage()} for per-message body fetches.
     */
    BODY
}
