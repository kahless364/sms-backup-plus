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

import com.fsck.k9.mail.store.imap.ImapFolder;

/**
 * Opaque app-owned handle for an open IMAP backup folder.
 *
 * <p>The wrapped k-9 {@link ImapFolder} reference is package-private to {@code mail.transport}
 * and NEVER exposed via any public accessor (CNTR-MODERNIZATION-007 validation rule #4).
 * Engine code ({@code service.*}) treats this as an opaque token.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned value types.
 */
public final class BackupFolderHandle {

    /** Package-private: only accessible within {@code mail.transport}. */
    final ImapFolder folder;

    /**
     * Package-private constructor — only {@link K9MailTransport} creates instances.
     *
     * @param folder the k-9 ImapFolder being wrapped; must not be {@code null}
     */
    BackupFolderHandle(ImapFolder folder) {
        this.folder = folder;
    }
}
