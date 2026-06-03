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
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * A {@link TrustedSocketFactory} that validates TLS connections against a single
 * user-enrolled certificate for a specific host:port pair.
 *
 * <p>This factory is only produced when the user has explicitly enrolled a certificate
 * via the pinned-certificate enrollment flow (U-009). It performs:
 * <ol>
 *   <li>SHA-256 fingerprint equality check: the presented leaf certificate's fingerprint
 *       must match the enrolled certificate's fingerprint exactly.</li>
 *   <li>Validity-window check: the enrolled certificate must not be expired or
 *       not-yet-valid at the time of connection.</li>
 * </ol>
 *
 * <p>On any mismatch, {@link CertificateException} is thrown and the connection fails
 * closed. There is no trust-all fallback. This is the intended behavior per
 * DES-MODERNIZATION-002 and CNTR-MODERNIZATION-001.
 *
 * <p>Constructor: {@code public PinnedCertificateSocketFactory(Context context, String host,
 * X509Certificate enrolledCert)}. There is no zero-argument or no-cert constructor.
 */
public class PinnedCertificateSocketFactory implements TrustedSocketFactory {

    private final X509Certificate enrolledCert;

    /**
     * @param context     Android context (reserved for future use; kept for API consistency
     *                    with {@code DefaultTrustedSocketFactory})
     * @param host        the host this factory is scoped to (informational; pinning is
     *                    enforced via the fingerprint of the enrolled certificate)
     * @param enrolledCert the user-enrolled leaf certificate for this host:port pair.
     *                    Must not be {@code null}.
     */
    public PinnedCertificateSocketFactory(Context context, String host, X509Certificate enrolledCert) {
        if (enrolledCert == null) {
            throw new IllegalArgumentException("enrolledCert must not be null");
        }
        this.enrolledCert = enrolledCert;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {
        TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        if (socket == null) {
            return socketFactory.createSocket();
        } else {
            return socketFactory.createSocket(socket, host, port, true);
        }
    }

    /**
     * Package-private test seam: returns the {@link X509TrustManager} used by this factory.
     * Analogous to {@code BackupImapStore.getTrustedSocketFactory()} per story U-008 IC-4.
     * Only callable from within the same package (i.e., test classes in
     * {@code com.zegoggles.smssync.mail}).
     */
    /* package, for testing */ X509TrustManager getTrustManagerForTesting() {
        return new PinnedX509TrustManager();
    }

    /**
     * An {@link X509TrustManager} that validates the presented certificate chain
     * against the single enrolled certificate for this host:port.
     *
     * <p>{@code checkServerTrusted()} performs:
     * <ol>
     *   <li>Extracts the leaf certificate from {@code chain[0]}.</li>
     *   <li>Computes the SHA-256 fingerprint of both the presented leaf and the enrolled cert.</li>
     *   <li>Compares the fingerprints; throws {@link CertificateException} on mismatch.</li>
     *   <li>Calls {@code enrolledCert.checkValidity()} to verify the enrolled certificate's
     *       validity window (not-before / not-after). This throws
     *       {@link java.security.cert.CertificateExpiredException} or
     *       {@link java.security.cert.CertificateNotYetValidException} (both subclasses of
     *       {@link CertificateException}) if the enrolled cert is outside its window.</li>
     * </ol>
     *
     * <p>{@code getAcceptedIssuers()} returns a one-element array containing the enrolled
     * certificate — never {@code null} and never an empty array.
     */
    private class PinnedX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // Client trust is not relevant for IMAP connections; no-op is intentional.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException(
                        "Server presented an empty certificate chain; connection rejected.");
            }

            // Step 1: extract leaf certificate (index 0 is the server's leaf cert per RFC 5246).
            final X509Certificate presented = chain[0];

            // Step 2: compute SHA-256 fingerprints.
            final byte[] presentedFingerprint = sha256(presented);
            final byte[] enrolledFingerprint  = sha256(enrolledCert);

            // Step 3: compare fingerprints.
            if (!Arrays.equals(presentedFingerprint, enrolledFingerprint)) {
                throw new CertificateException(
                        "Pinned certificate mismatch: presented certificate SHA-256 fingerprint "
                        + "does not match the enrolled certificate. Connection rejected to prevent MITM.");
            }

            // Step 4: check the enrolled certificate's validity window.
            // throws CertificateExpiredException or CertificateNotYetValidException on failure.
            enrolledCert.checkValidity();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            // Return the enrolled certificate so the TLS handshake can advertise it.
            // Never null, never empty — required by CNTR-MODERNIZATION-001 AC-2.
            return new X509Certificate[]{ enrolledCert };
        }

        private byte[] sha256(X509Certificate cert) throws CertificateException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return digest.digest(cert.getEncoded());
            } catch (NoSuchAlgorithmException e) {
                // SHA-256 is guaranteed available on all Android API levels.
                throw new CertificateException("SHA-256 not available", e);
            }
        }
    }
}
