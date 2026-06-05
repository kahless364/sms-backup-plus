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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapSearcher;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException;
import com.zegoggles.smssync.mail.BackupStoreConfig;
import com.zegoggles.smssync.mail.ConversionResult;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.Headers;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;
import com.zegoggles.smssync.mail.TlsTrustPolicy;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.service.exception.RequiresLoginException;

import android.content.ContentValues;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static java.util.Collections.sort;
import static java.util.Locale.ENGLISH;

/**
 * The sole k-9 adapter implementing {@link MailTransport}.
 *
 * <p>This is the ONLY class permitted to import {@code com.fsck.k9.mail.*} transport types.
 * All IMAP protocol logic is copied verbatim from the original {@code BackupImapStore}
 * (a reshape, not a rewrite). Uses composition: holds a private {@link BackupImapStoreDelegate}
 * (an {@code ImapStore} subclass) to avoid the checked-exception conflict between
 * {@code Store.checkSettings() throws MessagingException} and
 * {@code MailTransport.checkSettings() throws MailException}.
 *
 * <p>Constructor maps {@link MailTransportConfig#tlsPolicy} to the appropriate
 * {@link TrustedSocketFactory} per CNTR-MODERNIZATION-001:
 * {@code SYSTEM_VALIDATED} → {@code DefaultTrustedSocketFactory};
 * {@code PINNED_CERTIFICATE} → {@code PinnedCertificateSocketFactory}.
 *
 * <p>Per CNTR-MODERNIZATION-007 §K-9 adapter and DES-MODERNIZATION-009 §k-9 adapter.
 */
public class K9MailTransport implements MailTransport {

    private final BackupImapStoreDelegate store;
    private final TrustedSocketFactory resolvedSocketFactory;

    /**
     * Package-private constructor for testing: accepts a pre-built delegate and factory.
     * Skips the context-based factory resolution and BinaryTempFileBody initialization.
     */
    /* package, for testing */
    K9MailTransport(BackupImapStoreDelegate delegate, TrustedSocketFactory factory) {
        this.store = delegate;
        this.resolvedSocketFactory = factory;
    }

    /**
     * Constructs a {@code K9MailTransport} from an app-owned config.
     *
     * <p>Maps {@link MailTransportConfig#tlsPolicy} to a concrete {@link TrustedSocketFactory}:
     * <ul>
     *   <li>{@code SYSTEM_VALIDATED} → {@code new DefaultTrustedSocketFactory(context)}</li>
     *   <li>{@code PINNED_CERTIFICATE} → {@code new PinnedCertificateSocketFactory(context, host, pinnedCert)}</li>
     * </ul>
     *
     * <p>Moves the {@code BinaryTempFileBody.setTempDirectory()} call behind the adapter per
     * CNTR-MODERNIZATION-007 §Notes (MIME residual relocation; import removed from
     * {@code SmsRestoreService} in U-026).
     *
     * @param context Android context
     * @param config  app-owned configuration; {@code config.pinnedCert} must be non-null
     *                when {@code config.tlsPolicy == PINNED_CERTIFICATE}
     * @throws MailException if {@code tlsPolicy == PINNED_CERTIFICATE} but {@code pinnedCert} is null,
     *                       or if the k-9 ImapStore rejects the URI (cause preserved via getCause())
     */
    public K9MailTransport(Context context, MailTransportConfig config)
            throws MailException {
        this.resolvedSocketFactory = buildSocketFactory(context, config);
        try {
            this.store = new BackupImapStoreDelegate(context, config.storeUri,
                    this.resolvedSocketFactory);
        } catch (com.fsck.k9.mail.MessagingException e) {
            throw new MailException(e);
        }
        // Relocate BinaryTempFileBody temp-directory config behind the adapter
        // (CNTR-MODERNIZATION-007 §Notes — MIME residual). The import in
        // SmsRestoreService.java is removed in U-026 when the service is rewired.
        BinaryTempFileBody.setTempDirectory(context.getCacheDir());
    }

    /**
     * Resolves the appropriate {@link TrustedSocketFactory} for the given config.
     */
    private static TrustedSocketFactory buildSocketFactory(Context context,
                                                            MailTransportConfig config)
            throws MailException {
        if (config.tlsPolicy == TlsTrustPolicy.PINNED_CERTIFICATE) {
            if (config.pinnedCert == null) {
                throw new MailException(
                        "PINNED_CERTIFICATE policy requires a non-null pinnedCert");
            }
            Uri parsed = Uri.parse(config.storeUri);
            String host = parsed.getHost() != null ? parsed.getHost() : "";
            return new PinnedCertificateSocketFactory(context, host, config.pinnedCert);
        }
        // SYSTEM_VALIDATED (default)
        return new DefaultTrustedSocketFactory(context);
    }

    // -------------------------------------------------------------------------
    // MailTransport port implementation
    // -------------------------------------------------------------------------

    @Override
    public void checkSettings() throws MailException, RequiresLoginException {
        try {
            store.checkSettings();
        } catch (XOAuth2AuthenticationFailedException e) {
            throw new XOAuth2FailedException(e.getStatus(), e);
        } catch (AuthenticationFailedException e) {
            throw new RequiresLoginException();
        } catch (MessagingException e) {
            if ("Unable to get IMAP prefix".equals(e.getMessage())) {
                throw new TemporaryImapException(e);
            }
            throw new MailException(e);
        }
    }

    @Override
    public BackupFolderHandle openFolder(DataType type, DataTypePreferences prefs)
            throws MailException, RequiresLoginException {
        try {
            return new BackupFolderHandle(store.openFolder(type, prefs));
        } catch (XOAuth2AuthenticationFailedException e) {
            throw new XOAuth2FailedException(e.getStatus(), e);
        } catch (AuthenticationFailedException e) {
            throw new RequiresLoginException();
        } catch (MessagingException e) {
            if ("Unable to get IMAP prefix".equals(e.getMessage())) {
                throw new TemporaryImapException(e);
            }
            throw new MailException(e);
        }
    }

    @Override
    public void appendMessages(BackupFolderHandle folder, ConversionResult result)
            throws MailException, RequiresLoginException {
        try {
            List<Message> messages = result.getMessages();
            folder.folder.appendMessages(messages);
        } catch (XOAuth2AuthenticationFailedException e) {
            throw new XOAuth2FailedException(e.getStatus(), e);
        } catch (AuthenticationFailedException e) {
            throw new RequiresLoginException();
        } catch (MessagingException e) {
            if ("Unable to get IMAP prefix".equals(e.getMessage())) {
                throw new TemporaryImapException(e);
            }
            throw new MailException(e);
        }
    }

    @Override
    public List<MailMessageHandle> getMessages(BackupFolderHandle folder,
                                               int max,
                                               boolean flagged,
                                               Date since) throws MailException, RequiresLoginException {
        try {
            BackupImapStoreDelegate.BackupFolder backupFolder =
                    (BackupImapStoreDelegate.BackupFolder) folder.folder;
            List<ImapMessage> imapMessages = backupFolder.getMessagesInternal(max, flagged, since);
            List<MailMessageHandle> handles = new ArrayList<MailMessageHandle>(imapMessages.size());
            for (ImapMessage msg : imapMessages) {
                handles.add(new MailMessageHandle(msg.getUid(), msg));
            }
            return handles;
        } catch (XOAuth2AuthenticationFailedException e) {
            throw new XOAuth2FailedException(e.getStatus(), e);
        } catch (AuthenticationFailedException e) {
            throw new RequiresLoginException();
        } catch (MessagingException e) {
            if ("Unable to get IMAP prefix".equals(e.getMessage())) {
                throw new TemporaryImapException(e);
            }
            throw new MailException(e);
        }
    }

    @Override
    public void fetch(BackupFolderHandle folder,
                      List<MailMessageHandle> handles,
                      FetchSpec profile) throws MailException, RequiresLoginException {
        try {
            List<ImapMessage> messages = new ArrayList<ImapMessage>(handles.size());
            for (MailMessageHandle handle : handles) {
                messages.add(handle.message);
            }
            FetchProfile fp = new FetchProfile();
            if (profile == FetchSpec.BODY) {
                fp.add(FetchProfile.Item.BODY);
            } else {
                fp.add(FetchProfile.Item.DATE);
            }
            folder.folder.fetch(messages, fp, null);
        } catch (XOAuth2AuthenticationFailedException e) {
            throw new XOAuth2FailedException(e.getStatus(), e);
        } catch (AuthenticationFailedException e) {
            throw new RequiresLoginException();
        } catch (MessagingException e) {
            if ("Unable to get IMAP prefix".equals(e.getMessage())) {
                throw new TemporaryImapException(e);
            }
            throw new MailException(e);
        }
    }

    @Override
    public void closeFolders() {
        store.closeFolders();
    }

    /**
     * U-026: Bounded-residual converter bridge (CNTR-MODERNIZATION-007 §Converter residual).
     *
     * <p>Fetches the full message body for the given handle (via k-9 {@code folder.fetch})
     * then delegates to {@link MessageConverter} — which requires a k-9 {@code Message} — to
     * produce the {@link MessageImportResult}. Because {@code MailMessageHandle.message} is
     * package-private to {@code mail.transport}, this method is the only place in the codebase
     * where the k-9 message is unwrapped from the handle; the engine ({@code service.*}) never
     * sees the k-9 type.
     *
     * <p>Catches all conversion failures (MessagingException, IOException, IllegalArgumentException)
     * and returns {@link MessageImportResult#failure} so the engine can log and continue.
     */
    @Override
    public MessageImportResult importMessageBody(BackupFolderHandle folder,
                                                  MailMessageHandle handle,
                                                  MessageConverter converter) {
        final String uid = handle.uid;
        try {
            // Fetch the full body via k-9 (mirrors RestoreTask.java:240-245 pre-U-026)
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            List<ImapMessage> fetchList = Collections.singletonList(handle.message);
            folder.folder.fetch(fetchList, fp, null);

            // Delegate to the bounded-residual converter (k-9 Message stays inside mail.transport)
            DataType dataType = converter.getDataType(handle.message);
            ContentValues contentValues = converter.messageToContentValues(handle.message);
            return new MessageImportResult(uid, dataType, contentValues);
        } catch (MessagingException e) {
            Log.e(TAG, "importMessageBody: MessagingException for uid=" + uid, e);
            return MessageImportResult.failure(uid);
        } catch (IOException e) {
            Log.e(TAG, "importMessageBody: IOException for uid=" + uid, e);
            return MessageImportResult.failure(uid);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "importMessageBody: IllegalArgumentException for uid=" + uid, e);
            return MessageImportResult.failure(uid);
        }
    }

    // -------------------------------------------------------------------------
    // Static validators — preserved from BackupImapStore (BackupImapStore.java:210-227)
    // -------------------------------------------------------------------------

    public static boolean isValidImapFolder(String imapFolder) {
        return !(imapFolder == null || imapFolder.length() == 0) &&
               !(imapFolder.charAt(0) == '/' || imapFolder.charAt(0) == ' ' ||
                 imapFolder.charAt(imapFolder.length() - 1) == ' ');
    }

    public static boolean isValidUri(String uri) {
        if (TextUtils.isEmpty(uri)) return false;
        Uri parsed = Uri.parse(uri);
        return parsed != null &&
            !TextUtils.isEmpty(parsed.getAuthority()) &&
            !TextUtils.isEmpty(parsed.getHost()) &&
            !TextUtils.isEmpty(parsed.getScheme()) &&
            (
            "imap".equalsIgnoreCase(parsed.getScheme()) ||
            "imap+ssl+".equalsIgnoreCase(parsed.getScheme()) ||
            "imap+tls+".equalsIgnoreCase(parsed.getScheme())
            );
    }

    // -------------------------------------------------------------------------
    // Delegate accessors (testing and logging)
    // -------------------------------------------------------------------------

    /** Package-private: returns the resolved {@link TrustedSocketFactory} for test assertions. */
    /* package, for testing */ TrustedSocketFactory getTrustedSocketFactory() {
        return resolvedSocketFactory;
    }

    /** Package-private: returns the delegate store for test assertions. */
    /* package, for testing */ BackupImapStoreDelegate getStoreDelegate() {
        return store;
    }

    public String getStoreUri() {
        return store.getStoreUri();
    }

    public String getStoreUriForLogging() {
        return store.getStoreUriForLogging();
    }

    @Override
    public String toString() {
        return "K9MailTransport{uri=" + getStoreUriForLogging() + "}";
    }

    // -------------------------------------------------------------------------
    // Package-private translation helper (test seam)
    // -------------------------------------------------------------------------

    /**
     * Package-private translation helper: translates a k-9 {@link MessagingException}
     * (or subtype) to the appropriate app-owned exception.
     *
     * <p>This seam is used by {@link K9MailTransportTranslationTest} to verify each row
     * of the exception-translation table independently, without requiring a live IMAP
     * connection. The catch ordering mirrors the adapter methods exactly:
     * XOAuth2 first (subtype of AuthenticationFailed), then AuthenticationFailed,
     * then magic-string check, then generic backstop.
     *
     * @param e the k-9 MessagingException to translate
     * @return the translated app-owned exception
     */
    /* package, for testing */
    static Exception translateMessagingException(MessagingException e) {
        if (e instanceof XOAuth2AuthenticationFailedException) {
            return new XOAuth2FailedException(((XOAuth2AuthenticationFailedException) e).getStatus(), e);
        }
        if (e instanceof AuthenticationFailedException) {
            return new RequiresLoginException();
        }
        if ("Unable to get IMAP prefix".equals(e.getMessage())) {
            return new TemporaryImapException(e);
        }
        return new MailException(e);
    }

    // =========================================================================
    // BackupImapStoreDelegate — inner k-9 ImapStore subclass
    // Verbatim from BackupImapStore.java; replaces inheritance with composition.
    // =========================================================================

    /**
     * Private k-9 ImapStore subclass containing all IMAP protocol logic verbatim from
     * {@code BackupImapStore}. Exists as an inner class so {@link K9MailTransport} can hold
     * it via composition (avoiding the {@code checkSettings()} checked-exception conflict
     * between {@code Store} and {@link MailTransport}).
     *
     * <p>All IMAP logic is preserved byte-for-byte. No protocol changes.
     */
    static class BackupImapStoreDelegate extends ImapStore {
        private final Map<DataType, BackupFolder> openFolders =
                new HashMap<DataType, BackupFolder>();

        BackupImapStoreDelegate(Context context, String uri, TrustedSocketFactory socketFactory)
                throws MessagingException {
            super(new BackupStoreConfig(uri),
                  socketFactory,
                  (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE));
        }

        /**
         * Opens (or returns a cached) BackupFolder for the given data type.
         * Verbatim from {@code BackupImapStore.getFolder()}.
         */
        BackupFolder openFolder(DataType type, DataTypePreferences preferences)
                throws MessagingException {
            BackupFolder folder = openFolders.get(type);
            if (folder == null) {
                String label = preferences.getFolder(type);
                if (label == null) throw new IllegalStateException("label is null");
                folder = createAndOpenFolder(type, label);
                openFolders.put(type, folder);
            }
            return folder;
        }

        /**
         * Closes all open folders, swallowing exceptions.
         * Verbatim from {@code BackupImapStore.closeFolders()} (BackupImapStore.java:77-87).
         */
        void closeFolders() {
            Collection<BackupFolder> folders = openFolders.values();
            for (BackupFolder folder : folders) {
                try {
                    folder.close();
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
            openFolders.clear();
        }

        /**
         * Credential-masking URI for logging.
         * Verbatim from {@code BackupImapStore.getStoreUriForLogging()} (BackupImapStore.java:98-114).
         */
        String getStoreUriForLogging() {
            Uri uri = Uri.parse(mStoreConfig.getStoreUri());
            String userInfo = uri.getUserInfo();
            if (!TextUtils.isEmpty(userInfo) && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                //noinspection ReplaceAllDot
                userInfo = parts[0] + ":" + (parts[1].replaceAll(".", "X"));
                String host = uri.getHost();
                if (uri.getPort() != -1) {
                    host += ":" + uri.getPort();
                }
                return uri.buildUpon().encodedAuthority(userInfo + "@" + host).toString();
            } else {
                return uri.toString();
            }
        }

        /** Package-private test seam: returns the resolved TrustedSocketFactory. */
        TrustedSocketFactory getTrustedSocketFactory() {
            return mTrustedSocketFactory;
        }

        String getStoreUri() {
            return mStoreConfig.getStoreUri();
        }

        // -------------------------------------------------------------------------
        // Constants for create→open retry (Gmail label propagation gap — BUG-011)
        // Package-private for test access.
        // -------------------------------------------------------------------------

        /** Maximum number of attempts to open a folder immediately after CREATE (BUG-011). */
        /* package */ static final int MAX_CREATE_OPEN_RETRIES = 3;

        /**
         * Delay in milliseconds between open-after-create retries.
         * Gmail labels may not be immediately SELECTable after IMAP CREATE.
         */
        /* package */ static final long CREATE_OPEN_RETRY_DELAY_MS = 1000L;

        /**
         * Factory method that creates a {@link BackupFolder} for the given data type and label.
         * Package-private to allow test subclasses to inject mock folders (BUG-011 test seam).
         */
        /* package, for testing */ @NonNull BackupFolder createBackupFolder(DataType type,
                                                                              String label) {
            return new BackupFolder(this, label, type);
        }

        /**
         * Returns the delay in milliseconds between open-after-create retries.
         * Package-private to allow test subclasses to zero the delay for fast unit tests.
         */
        /* package, for testing */ long getRetryDelayMs() {
            return CREATE_OPEN_RETRY_DELAY_MS;
        }

        /**
         * Creates, opens, and returns a BackupFolder.
         * Based on {@code BackupImapStore.createAndOpenFolder()} (BackupImapStore.java:120-134)
         * with two BUG-011 fixes applied:
         * <ol>
         *   <li>The return value of {@link com.fsck.k9.mail.Folder#create} is now checked —
         *       if {@code false}, a {@link MessagingException} is thrown immediately rather
         *       than proceeding to {@code open()} which would always fail.</li>
         *   <li>After a successful CREATE, {@code open()} is retried up to
         *       {@link #MAX_CREATE_OPEN_RETRIES} times with a {@link #CREATE_OPEN_RETRY_DELAY_MS}
         *       delay between attempts. This handles Gmail's label-propagation gap: a freshly
         *       created Gmail label may return {@code NO [NONEXISTENT]} on the first SELECT even
         *       though the CREATE command succeeded.</li>
         * </ol>
         * The {@code IllegalArgumentException → MessagingException} re-wrap is preserved verbatim.
         */
        private @NonNull BackupFolder createAndOpenFolder(DataType type, @NonNull String label)
                throws MessagingException {
            try {
                BackupFolder folder = createBackupFolder(type, label);
                if (!folder.exists()) {
                    Log.i(TAG, "Label '" + label + "' does not exist yet. Creating.");
                    boolean created = folder.create(FolderType.HOLDS_MESSAGES);
                    if (!created) {
                        throw new MessagingException(
                                "Failed to create folder/label '" + label + "' on the server");
                    }
                    // Gmail: newly-created labels may not be immediately SELECTable.
                    // Retry open() with back-off to handle the propagation gap (BUG-011).
                    openWithRetryAfterCreate(folder);
                } else {
                    folder.open(Folder.OPEN_MODE_RW);
                }
                return folder;
            } catch (IllegalArgumentException e) {
                // thrown inside K9 — re-wrap verbatim (BackupImapStore.java:129-133)
                Log.e(TAG, "K9 error", e);
                throw new MessagingException(e.getMessage());
            }
        }

        /**
         * Attempts to open the given folder, retrying up to {@link #MAX_CREATE_OPEN_RETRIES}
         * times if the server reports NONEXISTENT (which Gmail does for a label that was just
         * created but not yet propagated).
         *
         * <p>Only retries on MessagingExceptions whose message contains "NONEXISTENT" (the IMAP
         * RFC 5530 response code returned by Gmail when a new label is not yet SELECTable). All
         * other exceptions are propagated immediately on the first occurrence.
         *
         * @param folder the folder to open
         * @throws MessagingException if the folder cannot be opened after all retries
         */
        private void openWithRetryAfterCreate(BackupFolder folder) throws MessagingException {
            MessagingException lastException = null;
            for (int attempt = 1; attempt <= MAX_CREATE_OPEN_RETRIES; attempt++) {
                try {
                    folder.open(Folder.OPEN_MODE_RW);
                    return; // success
                } catch (MessagingException e) {
                    String msg = e.getMessage();
                    boolean isNonExistent = msg != null &&
                            msg.toUpperCase(java.util.Locale.US).contains("NONEXISTENT");
                    if (!isNonExistent) {
                        // Not a NONEXISTENT error — do not retry; propagate immediately.
                        throw e;
                    }
                    lastException = e;
                    if (attempt < MAX_CREATE_OPEN_RETRIES) {
                        long delayMs = getRetryDelayMs();
                        Log.w(TAG, "Folder '" + folder.getName() +
                                "' not selectable yet after CREATE (attempt " + attempt + "/" +
                                MAX_CREATE_OPEN_RETRIES + "); retrying in " + delayMs + " ms");
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new MessagingException(
                                    "Interrupted while waiting to retry folder open after CREATE",
                                    ie);
                        }
                    }
                }
            }
            throw new MessagingException(
                    "Folder '" + folder.getName() + "' was created but is still not selectable " +
                    "after " + MAX_CREATE_OPEN_RETRIES + " attempt(s). " +
                    "Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                    lastException);
        }

        // -------------------------------------------------------------------------
        // BackupFolder inner class — verbatim from BackupImapStore.BackupFolder
        // -------------------------------------------------------------------------

        /**
         * Verbatim from {@code BackupImapStore.BackupFolder} (BackupImapStore.java:140-197).
         */
        class BackupFolder extends ImapFolder {
            private final DataType type;

            BackupFolder(ImapStore store, String name, DataType type) {
                super(store, name);
                this.type = type;
            }

            /**
             * Retrieves messages matching the given criteria.
             * Verbatim from {@code BackupImapStore.BackupFolder.getMessages(int, boolean, Date)}
             * (BackupImapStore.java:148-186).
             */
            public List<ImapMessage> getMessagesInternal(final int max, final boolean flagged,
                                                         final Date since)
                    throws MessagingException {
                if (LOCAL_LOGV)
                    Log.v(TAG, String.format(ENGLISH, "getMessages(%d, %b, %s)", max, flagged, since));

                final List<ImapMessage> messages;
                final ImapSearcher searcher = new ImapSearcher() {
                    @Override
                    public List<ImapResponse> search() throws IOException, MessagingException {
                        return executeSimpleCommand(buildSearchQuery(type, since, flagged));
                    }
                };

                final List<ImapMessage> msgs = search(searcher, null);

                Log.i(TAG, "Found " + msgs.size() + " msgs" +
                        (since == null ? "" : " (since " + since + ")"));
                if (max > 0 && msgs.size() > max) {
                    if (LOCAL_LOGV) Log.v(TAG, "Fetching envelopes");

                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.DATE);
                    fetch(msgs, fp, null);

                    if (LOCAL_LOGV) Log.v(TAG, "Sorting");
                    sort(msgs, MessageComparator.INSTANCE);
                    if (LOCAL_LOGV) Log.v(TAG, "Sorting done");

                    messages = new ArrayList<ImapMessage>(max);
                    messages.addAll(msgs.subList(0, max));
                } else {
                    messages = msgs;
                }

                Collections.reverse(messages);

                return messages;
            }

            /* package */ String buildSearchQuery(DataType dataType, Date since, boolean flagged) {
                final StringBuilder sb = new StringBuilder("UID SEARCH 1:*")
                        .append(' ')
                        .append(String.format(ENGLISH, "(HEADER %s \"%s\")",
                                Headers.DATATYPE.toUpperCase(ENGLISH), dataType))
                        .append(" UNDELETED");
                if (since != null) sb.append(" SENTSINCE ").append(RFC3501_DATE.get().format(since));
                if (flagged) sb.append(" FLAGGED");
                return sb.toString().trim();
            }
        }

        // -------------------------------------------------------------------------
        // MessageComparator — verbatim from BackupImapStore (BackupImapStore.java:199-208)
        // -------------------------------------------------------------------------

        static class MessageComparator implements Comparator<Message> {
            static final MessageComparator INSTANCE = new MessageComparator();
            static final Date EARLY = new Date(0);

            public int compare(final Message m1, final Message m2) {
                final Date d1 = m1 == null ? EARLY :
                        m1.getSentDate() != null ? m1.getSentDate() : EARLY;
                final Date d2 = m2 == null ? EARLY :
                        m2.getSentDate() != null ? m2.getSentDate() : EARLY;
                return d2.compareTo(d1);
            }
        }
    }
}
