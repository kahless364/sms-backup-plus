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

import com.zegoggles.smssync.mail.ConversionResult;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.service.exception.RequiresLoginException;

import java.util.Date;
import java.util.List;

/**
 * App-owned Anti-Corruption-Layer (ACL) port for IMAP mail operations.
 *
 * <p>All method signatures use ONLY app-owned types — no {@code com.fsck.k9.*} type
 * appears anywhere in this interface (CNTR-MODERNIZATION-007 invariant).
 *
 * <p>The sole implementation is {@link K9MailTransport}, which confines all k-9 imports
 * to the {@code mail.transport} package. Engine code ({@code service.*}) depends only
 * on this interface and the value/exception types below.
 *
 * <p>Operation set is derived from verified engine call sites per CNTR-MODERNIZATION-007
 * §Interface: {@code store.checkSettings()}, {@code getFolder()}, {@code folder.appendMessages()},
 * {@code folder.getMessages()}, per-message {@code fetch()}, and {@code store.closeFolders()}.
 */
public interface MailTransport {

    /**
     * Verifies the connection and credentials before backup/restore.
     * Replaces k-9 {@code ImapStore.checkSettings()}.
     *
     * @throws MailException on any failure (translated from k-9 throwables by the adapter)
     * @throws RequiresLoginException when credentials are rejected (authentication failure)
     */
    void checkSettings() throws MailException, RequiresLoginException;

    /**
     * Opens (or returns a cached) backup folder for the given data type.
     * Replaces k-9 {@code BackupImapStore.getFolder(type, prefs)}.
     *
     * @param type  the data type whose folder should be opened
     * @param prefs preferences supplying the IMAP folder label per data type
     * @return an opaque handle to the open folder; never {@code null}
     * @throws MailException on connection or folder-creation failure
     * @throws RequiresLoginException when credentials are rejected
     */
    BackupFolderHandle openFolder(DataType type, DataTypePreferences prefs)
            throws MailException, RequiresLoginException;

    /**
     * Appends the messages in {@code result} to the server folder identified by {@code folder}.
     * Replaces k-9 {@code ImapFolder.appendMessages(result.getMessages())}.
     * The adapter unwraps the k-9 {@code Message} list from {@link ConversionResult} inside
     * {@code mail.*}; the k-9 list never crosses this port.
     *
     * <p><b>Confirmed-append contract (BUG-010 fix):</b> Returns the maximum message date among
     * the messages that were confirmed appended (i.e. the value of
     * {@link ConversionResult#getMaxDate()} at the point the append completed successfully).
     * Callers MUST use this return value — not any separately-computed date — as the watermark
     * to advance for this batch. If the append fails, this method throws and never returns a
     * date, so the caller's watermark is never updated. This makes the
     * "watermark only advances on confirmed append" invariant compiler-enforced.
     *
     * @param folder the folder handle returned by {@link #openFolder}
     * @param result the converter result whose messages should be appended
     * @return the maximum message date (epoch ms) among the confirmed-appended messages;
     *         equals {@code result.getMaxDate()} if all messages were appended successfully.
     *         Never returns a wall-clock "now" value — always a message-derived date.
     * @throws MailException on any IMAP failure (append never confirmed; caller must not advance watermark)
     * @throws RequiresLoginException when credentials are rejected
     */
    long appendMessages(BackupFolderHandle folder, ConversionResult result)
            throws MailException, RequiresLoginException;

    /**
     * Retrieves message handles from the server folder matching the given criteria.
     * Replaces k-9 {@code BackupFolder.getMessages(max, flagged, since)}.
     *
     * @param folder  the folder handle returned by {@link #openFolder}
     * @param max     maximum number of messages to return (0 or negative = unlimited)
     * @param flagged if {@code true}, only return flagged (starred) messages
     * @param since   if non-null, only return messages sent on or after this date
     * @return opaque handles for matching messages; never {@code null}
     * @throws MailException on any IMAP failure
     * @throws RequiresLoginException when credentials are rejected
     */
    List<MailMessageHandle> getMessages(BackupFolderHandle folder,
                                        int max,
                                        boolean flagged,
                                        Date since) throws MailException, RequiresLoginException;

    /**
     * Fetches message content according to the given profile.
     * Replaces k-9 {@code ImapFolder.fetch(messages, fp, null)}.
     *
     * @param folder  the folder handle returned by {@link #openFolder}
     * @param handles the handles returned by {@link #getMessages}
     * @param profile the fetch profile (ENVELOPE_DATE for sorting, BODY for full content)
     * @throws MailException on any IMAP failure
     * @throws RequiresLoginException when credentials are rejected
     */
    void fetch(BackupFolderHandle folder, List<MailMessageHandle> handles, FetchSpec profile)
            throws MailException, RequiresLoginException;

    /**
     * Closes all open folders, swallowing per-folder close errors.
     * Replaces k-9 {@code BackupImapStore.closeFolders()}.
     *
     * <p>Does <strong>not</strong> throw. This is load-bearing: callers invoke
     * {@code closeFolders()} from {@code finally} blocks where a thrown exception would
     * discard the block's return value.
     */
    void closeFolders();

    /**
     * Fetches the full body for the given message handle and converts it to a
     * {@link MessageImportResult} using the supplied {@link MessageConverter}.
     *
     * <p>This operation bridges the bounded-residual {@link MessageConverter} (which uses
     * k-9 {@code Message} types internally) to the app-owned port boundary: the adapter
     * accesses the k-9 {@code Message} directly from the opaque {@link MailMessageHandle}
     * inside {@code mail.transport}, so the engine ({@code service.*}) never imports any
     * {@code com.fsck.k9.*} type (AC-10 / CNTR-MODERNIZATION-007).
     *
     * <p>Replaces the per-message {@code message.getFolder().fetch(...)} + converter-call
     * pattern in {@code RestoreTask.importMessage()} (RestoreTask.java:240-247).
     *
     * @param folder    the folder handle returned by {@link #openFolder}
     * @param handle    the message handle returned by {@link #getMessages}
     * @param converter the bounded-residual converter used to extract data type and content values
     * @return a {@link MessageImportResult} containing the converted content values and data type;
     *         {@link MessageImportResult#failed} is {@code true} if the fetch or conversion failed
     */
    MessageImportResult importMessageBody(BackupFolderHandle folder,
                                          MailMessageHandle handle,
                                          MessageConverter converter);
}
