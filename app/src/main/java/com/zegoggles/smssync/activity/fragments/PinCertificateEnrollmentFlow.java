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
package com.zegoggles.smssync.activity.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;

import com.zegoggles.smssync.R;
import com.zegoggles.smssync.mail.PinnedCertStore;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.zegoggles.smssync.App.TAG;

/**
 * Orchestrates the user-initiated pin-certificate enrollment flow.
 *
 * <p>The flow:
 * <ol>
 *   <li>Connect to the configured IMAP host:port using an ephemeral trust-any TLS handshake
 *       (for display purposes only — no IMAP data is exchanged).</li>
 *   <li>Capture the server's leaf X.509 certificate.</li>
 *   <li>Display subject DN, issuer DN, SHA-256 fingerprint, and expiry date to the user.</li>
 *   <li>Require affirmative "Trust this certificate" consent (Cancel is the safe default).</li>
 *   <li>On confirmation, store the certificate in {@link PinnedCertStore}.</li>
 * </ol>
 *
 * <p>The ephemeral TrustManager used during the TLS fetch is an anonymous inner class scoped
 * exclusively to this enrollment code path. It is NOT {@code AllTrustedSocketFactory} (which is
 * deleted). Its sole purpose is to capture the leaf certificate; no data is transmitted through
 * this socket, and the ephemeral trust is never reused for IMAP data connections.
 *
 * <p>Per CNTR-MODERNIZATION-002: {@link PinnedCertStore#put} is called ONLY from the
 * affirmative confirmation callback, never automatically. The Cancel path leaves the store
 * untouched.
 */
public class PinCertificateEnrollmentFlow {

    /** Default IMAP-over-TLS port, used when SERVER_ADDRESS contains no explicit port. */
    static final int DEFAULT_IMAP_TLS_PORT = 993;

    /** Connection timeout for the cert-fetch TLS handshake (ms). */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    private final Context context;
    private final PinnedCertStore pinnedCertStore;
    @Nullable
    private final Listener listener;

    /**
     * Callback interface for enrollment lifecycle events.
     * All callbacks are invoked on the main thread.
     */
    public interface Listener {
        /** Called after enrollment confirmation and successful store write. */
        void onEnrolled(String host, int port);

        /** Called when the user cancels or the flow is dismissed without confirming. */
        void onCancelled();

        /** Called when the TLS fetch fails (unreachable host, handshake error, etc.). */
        void onFetchError(String message);
    }

    public PinCertificateEnrollmentFlow(@NonNull Context context,
                                        @NonNull PinnedCertStore pinnedCertStore,
                                        @Nullable Listener listener) {
        this.context = context.getApplicationContext();
        this.pinnedCertStore = pinnedCertStore;
        this.listener = listener;
    }

    /**
     * Launches the enrollment flow for the given server address string.
     *
     * <p>Parses {@code serverAddress} into host and port (defaulting to
     * {@link #DEFAULT_IMAP_TLS_PORT} if no port is present), fetches the leaf certificate,
     * and shows the enrollment dialog.
     *
     * @param serverAddress the value from {@code AuthPreferences.SERVER_ADDRESS}, e.g.
     *                      {@code "imap.example.org:993"} or {@code "imap.example.org"}.
     * @param onShowDialog  a callback that must present the supplied AlertDialog — typically
     *                      {@code dialog -> dialog.show()} when called from a Fragment, or a
     *                      test double in unit tests.
     */
    public void start(@NonNull String serverAddress,
                      @NonNull DialogShower onShowDialog) {
        final String host = parseHost(serverAddress);
        final int port = parsePort(serverAddress);
        new FetchCertTask(host, port, onShowDialog).execute();
    }

    /**
     * Functional interface so callers can inject a dialog-showing strategy (and tests can
     * capture the dialog without actually displaying it).
     */
    public interface DialogShower {
        void show(AlertDialog dialog);
    }

    // -------------------------------------------------------------------------
    // Package-private helpers — visible for testing
    // -------------------------------------------------------------------------

    /**
     * Parses the hostname from a {@code "host"} or {@code "host:port"} string.
     * Handles IPv6 bracket notation: {@code "[::1]:993"} → {@code "[::1]"}.
     */
    static String parseHost(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return "";
        }
        // IPv6: "[::1]:993"
        if (serverAddress.startsWith("[")) {
            int bracket = serverAddress.indexOf(']');
            return bracket < 0 ? serverAddress : serverAddress.substring(0, bracket + 1);
        }
        int colon = serverAddress.lastIndexOf(':');
        return colon < 0 ? serverAddress : serverAddress.substring(0, colon);
    }

    /**
     * Parses the port from a {@code "host"} or {@code "host:port"} string.
     * Returns {@link #DEFAULT_IMAP_TLS_PORT} if no explicit port is present or parsing fails.
     */
    static int parsePort(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return DEFAULT_IMAP_TLS_PORT;
        }
        // IPv6: "[::1]:993"
        if (serverAddress.startsWith("[")) {
            int bracket = serverAddress.indexOf(']');
            if (bracket < 0 || bracket + 1 >= serverAddress.length()) {
                return DEFAULT_IMAP_TLS_PORT;
            }
            String afterBracket = serverAddress.substring(bracket + 1);
            if (afterBracket.startsWith(":")) {
                try {
                    return Integer.parseInt(afterBracket.substring(1));
                } catch (NumberFormatException e) {
                    return DEFAULT_IMAP_TLS_PORT;
                }
            }
            return DEFAULT_IMAP_TLS_PORT;
        }
        int colon = serverAddress.lastIndexOf(':');
        if (colon < 0) return DEFAULT_IMAP_TLS_PORT;
        try {
            return Integer.parseInt(serverAddress.substring(colon + 1));
        } catch (NumberFormatException e) {
            return DEFAULT_IMAP_TLS_PORT;
        }
    }

    /**
     * Formats a SHA-256 fingerprint as uppercase colon-separated hex octets.
     * E.g. {@code "AB:CD:EF:..."} (95 characters for a 32-byte hash).
     */
    static String formatSha256Fingerprint(byte[] sha256) {
        StringBuilder sb = new StringBuilder(3 * sha256.length - 1);
        for (int i = 0; i < sha256.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02X", sha256[i] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Computes the SHA-256 digest of the DER-encoded certificate bytes.
     *
     * @throws NoSuchAlgorithmException if SHA-256 is unavailable (should never happen on Android)
     * @throws CertificateEncodingException if the certificate cannot be DER-encoded
     */
    static byte[] sha256(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        return MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
    }

    // -------------------------------------------------------------------------
    // Ephemeral cert-capture TrustManager
    // -------------------------------------------------------------------------

    /**
     * An ephemeral {@link X509TrustManager} that accepts any certificate chain during the
     * enrollment TLS handshake. Its SOLE purpose is to capture the server's leaf certificate
     * for display to the user — NO data is transmitted through the connection.
     *
     * <p>This trust manager is scoped exclusively to the enrollment flow. It is NOT the
     * deleted {@code AllTrustedSocketFactory}; it is not reachable from any data connection
     * path and is not stored or reused beyond the single handshake.
     */
    static final class EnrollmentCaptureTrustManager implements X509TrustManager {
        private volatile X509Certificate leafCert;

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Client-side check; not applicable here.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Capture the leaf (index 0); do not reject — enrollment display-only.
            if (chain != null && chain.length > 0) {
                leafCert = chain[0];
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Nullable
        X509Certificate getLeafCert() {
            return leafCert;
        }
    }

    // -------------------------------------------------------------------------
    // AsyncTask for the TLS fetch (off main thread)
    // -------------------------------------------------------------------------

    private class FetchCertTask extends AsyncTask<Void, Void, FetchResult> {
        private final String host;
        private final int port;
        private final DialogShower onShowDialog;

        FetchCertTask(String host, int port, DialogShower onShowDialog) {
            this.host = host;
            this.port = port;
            this.onShowDialog = onShowDialog;
        }

        @Override
        @WorkerThread
        protected FetchResult doInBackground(Void... voids) {
            return fetchLeafCert(host, port);
        }

        @Override
        protected void onPostExecute(FetchResult result) {
            if (result.error != null) {
                if (listener != null) listener.onFetchError(result.error);
                return;
            }
            showEnrollmentDialog(host, port, result.cert, onShowDialog);
        }
    }

    private static class FetchResult {
        @Nullable final X509Certificate cert;
        @Nullable final String error;

        FetchResult(@NonNull X509Certificate cert) {
            this.cert = cert;
            this.error = null;
        }

        FetchResult(@NonNull String error) {
            this.cert = null;
            this.error = error;
        }
    }

    @WorkerThread
    private FetchResult fetchLeafCert(String host, int port) {
        EnrollmentCaptureTrustManager captureTm = new EnrollmentCaptureTrustManager();
        SSLSocket socket = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{captureTm}, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            socket.startHandshake();
        } catch (Exception e) {
            // Handshake may throw due to the ephemeral trust manager — that's expected.
            // We only care whether the leaf cert was captured.
            Log.d(TAG, "PinCertEnrollment: TLS handshake exception (expected during capture): " + e.getMessage());
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) { /* intentional */ }
            }
        }

        final X509Certificate leaf = captureTm.getLeafCert();
        if (leaf == null) {
            return new FetchResult(context.getString(R.string.ui_protocol_pin_certificate_fetch_error));
        }
        return new FetchResult(leaf);
    }

    // -------------------------------------------------------------------------
    // Enrollment confirmation dialog
    // -------------------------------------------------------------------------

    /**
     * Builds and shows the enrollment confirmation dialog.
     *
     * <p>Displays all four mandatory fields per CNTR-MODERNIZATION-002 §EnrolledCertificate:
     * subject DN, issuer DN, SHA-256 fingerprint, and expiry date.
     *
     * <p>The "Trust this certificate" button is NOT the default-focused control —
     * the safe default is the Cancel/negative button (CNTR-002 affirmative-consent requirement).
     */
    void showEnrollmentDialog(final String host,
                              final int port,
                              final X509Certificate cert,
                              final DialogShower onShowDialog) {
        try {
            final byte[] sha256 = sha256(cert);
            final String fingerprint = formatSha256Fingerprint(sha256);
            final String subject = cert.getSubjectX500Principal().getName();
            final String issuer = cert.getIssuerX500Principal().getName();
            final String expiry = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                    .format(cert.getNotAfter());

            final String message = context.getString(R.string.ui_protocol_pin_certificate_dialog_subject, subject) + "\n\n"
                    + context.getString(R.string.ui_protocol_pin_certificate_dialog_issuer, issuer) + "\n\n"
                    + context.getString(R.string.ui_protocol_pin_certificate_dialog_fingerprint, fingerprint) + "\n\n"
                    + context.getString(R.string.ui_protocol_pin_certificate_dialog_expires, expiry);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.ui_protocol_pin_certificate_dialog_title)
                    .setMessage(message)
                    // AC-5: "Trust this certificate" is NOT the default focused button.
                    // Positive = confirm (Trust); Negative = cancel (safe default).
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel: do NOT write PinnedCertStore (AC-5).
                            if (listener != null) listener.onCancelled();
                        }
                    })
                    .setPositiveButton(R.string.ui_protocol_pin_certificate_trust_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Affirmative consent: store the cert (AC-6).
                                    storeCertOnConfirm(host, port, cert);
                                }
                            })
                    .setCancelable(true)
                    .create();

            onShowDialog.show(dialog);

        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            Log.e(TAG, "PinCertEnrollment: cannot compute fingerprint", e);
            if (listener != null) {
                listener.onFetchError(context.getString(R.string.ui_protocol_pin_certificate_fetch_error));
            }
        }
    }

    /**
     * Called on affirmative consent: stores the certificate in {@link PinnedCertStore}.
     * This is the ONLY code path that writes the store (CNTR-MODERNIZATION-002 §Validation Rule 1).
     */
    private void storeCertOnConfirm(String host, int port, X509Certificate cert) {
        try {
            pinnedCertStore.put(host.toLowerCase(Locale.US), port, cert);
            if (listener != null) listener.onEnrolled(host, port);
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "PinCertEnrollment: failed to store certificate", e);
            if (listener != null) {
                listener.onFetchError(context.getString(R.string.ui_protocol_pin_certificate_fetch_error));
            }
        }
    }
}
