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
package com.zegoggles.smssync.mail;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static com.zegoggles.smssync.App.TAG;

/**
 * A per-{@code host:port} store for user-enrolled TLS pinning certificates.
 *
 * <p>Certificates are stored as Base64-encoded DER bytes in a {@link SharedPreferences} file
 * named {@code "pinned_certs"} opened with {@link Context#MODE_PRIVATE}. The key is the
 * string {@code "host:port"} (e.g., {@code "mail.example.org:993"}).
 *
 * <p>The {@code "pinned_certs"} SharedPreferences file is excluded from Android Auto-Backup
 * in {@code app/src/main/res/xml/backup_descriptor.xml} so that enrolled certificates are
 * not copied to a new device without user re-enrollment.
 *
 * <p>This class is the only legitimate writer of the {@link TlsTrustPolicy#PINNED_CERTIFICATE}
 * state read by {@code ServiceBase.getBackupImapStore()} per CNTR-MODERNIZATION-001.
 */
public class PinnedCertStore {

    /** The SharedPreferences file name. Must match the backup exclusion in backup_descriptor.xml. */
    static final String PREFS_NAME = "pinned_certs";

    private final Context context;

    /**
     * @param context Android context used to open the shared preferences file.
     */
    public PinnedCertStore(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns the enrolled certificate for the given host and port, or {@code null} if
     * no certificate has been enrolled for that key.
     *
     * @param host the IMAP server hostname
     * @param port the IMAP server port
     * @return the enrolled {@link X509Certificate}, or {@code null} if not enrolled
     */
    public X509Certificate get(String host, int port) {
        final String key = makeKey(host, port);
        final String encoded = getPreferences().getString(key, null);
        if (encoded == null) {
            return null;
        }
        try {
            final byte[] der = Base64.decode(encoded, Base64.DEFAULT);
            final CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
        } catch (CertificateException | IllegalArgumentException e) {
            Log.w(TAG, "PinnedCertStore: failed to decode certificate for " + key, e);
            return null;
        }
    }

    /**
     * Enrolls (stores) a certificate for the given host and port.
     *
     * <p>Any previously enrolled certificate for the same key is overwritten.
     *
     * @param host the IMAP server hostname
     * @param port the IMAP server port
     * @param cert the {@link X509Certificate} to enroll
     * @throws CertificateEncodingException if the certificate cannot be DER-encoded
     */
    public void put(String host, int port, X509Certificate cert) throws CertificateEncodingException {
        final String key = makeKey(host, port);
        final byte[] der = cert.getEncoded();
        final String encoded = Base64.encodeToString(der, Base64.DEFAULT);
        getPreferences().edit().putString(key, encoded).apply();
    }

    /**
     * Removes the enrolled certificate for the given host and port.
     *
     * <p>After this call, {@link #get(String, int)} returns {@code null} for the key and
     * {@link #getTlsTrustPolicy(String, int)} returns {@link TlsTrustPolicy#SYSTEM_VALIDATED}.
     *
     * @param host the IMAP server hostname
     * @param port the IMAP server port
     */
    public void remove(String host, int port) {
        getPreferences().edit().remove(makeKey(host, port)).apply();
    }

    /**
     * Resolves the {@link TlsTrustPolicy} for the given host and port.
     *
     * <p>Returns {@link TlsTrustPolicy#PINNED_CERTIFICATE} if a certificate is enrolled for
     * the key, {@link TlsTrustPolicy#SYSTEM_VALIDATED} otherwise. This is the unconditionally
     * secure default per CNTR-MODERNIZATION-001 Invariant 5.
     *
     * @param host the IMAP server hostname
     * @param port the IMAP server port
     * @return the resolved {@link TlsTrustPolicy}; never {@code null}
     */
    public TlsTrustPolicy getTlsTrustPolicy(String host, int port) {
        return getPreferences().contains(makeKey(host, port))
                ? TlsTrustPolicy.PINNED_CERTIFICATE
                : TlsTrustPolicy.SYSTEM_VALIDATED;
    }

    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String makeKey(String host, int port) {
        return host + ":" + port;
    }
}
