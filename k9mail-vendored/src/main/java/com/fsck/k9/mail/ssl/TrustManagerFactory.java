
package com.fsck.k9.mail.ssl;

import android.util.Log;

import com.fsck.k9.mail.CertificateChainException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

// U-058 (BT-003): org.apache.http.legacy removed. StrictHostnameVerifier replaced with
// HttpsURLConnection.getDefaultHostnameVerifier() + a minimal SSLSession wrapper that
// exposes the peer certificate chain. Behaviour is equivalent: RFC 2818 hostname check
// against the server certificate's CN/SAN fields.

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;

    private static LocalKeyStore keyStore;


    private static class SecureX509TrustManager implements X509TrustManager {
        private static final Map<String, SecureX509TrustManager> mTrustManager =
            new HashMap<String, SecureX509TrustManager>();

        private final String mHost;
        private final int mPort;

        private SecureX509TrustManager(String host, int port) {
            mHost = host;
            mPort = port;
        }

        public synchronized static X509TrustManager getInstance(String host, int port) {
            String key = host + ":" + port;
            SecureX509TrustManager trustManager;
            if (mTrustManager.containsKey(key)) {
                trustManager = mTrustManager.get(key);
            } else {
                trustManager = new SecureX509TrustManager(host, port);
                mTrustManager.put(key, trustManager);
            }

            return trustManager;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            String message = null;
            X509Certificate certificate = chain[0];

            Throwable cause = null;

            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                verifyHostname(mHost, chain);
                return;
            } catch (CertificateException e) {
                // cert. chain can't be validated
                message = e.getMessage();
                cause = e;
            } catch (SSLException e) {
                // host name doesn't match certificate
                message = e.getMessage();
                cause = e;
            }

            // Check the local key store if we couldn't verify the certificate using the global
            // key store or if the host name doesn't match the certificate name
            if (!keyStore.isValidCertificate(certificate, mHost, mPort)) {
                throw new CertificateChainException(message, chain, cause);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }

    }

    static {
        try {
            keyStore = LocalKeyStore.getInstance();

            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);

            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null) {
                for (TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        defaultTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        } catch (KeyStoreException e) {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }
    }

    private TrustManagerFactory() {
    }

    public static X509TrustManager get(String host, int port) {
        return SecureX509TrustManager.getInstance(host, port);
    }

    /**
     * Verify that {@code host} matches the subject of the leaf certificate in {@code chain}.
     *
     * Uses {@link HttpsURLConnection#getDefaultHostnameVerifier()} (Android's conscrypt verifier,
     * which implements RFC 2818 SAN/CN matching) via a minimal {@link SSLSession} shim that
     * exposes only the peer certificate chain. This replaces the removed
     * {@code org.apache.http.conn.ssl.StrictHostnameVerifier} (U-058 / BT-003).
     *
     * @throws SSLException if the hostname does not match the certificate.
     */
    static void verifyHostname(final String host, final X509Certificate[] chain)
            throws SSLException {
        boolean verified = HttpsURLConnection.getDefaultHostnameVerifier().verify(
                host, new MinimalSslSession(chain));
        if (!verified) {
            throw new SSLException("Hostname '" + host
                    + "' was not verified against the server certificate");
        }
    }

    /**
     * Minimal {@link SSLSession} stub that satisfies
     * {@link javax.net.ssl.HostnameVerifier#verify(String, SSLSession)} by returning
     * the server certificate chain. All other methods return safe no-op values.
     */
    private static final class MinimalSslSession implements SSLSession {
        private final X509Certificate[] chain;

        MinimalSslSession(X509Certificate[] chain) {
            this.chain = chain;
        }

        @Override public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
            return chain;
        }

        // --- All remaining SSLSession methods: safe no-ops / stubs ---
        @Override public byte[] getId() { return new byte[0]; }
        @Override public javax.net.ssl.SSLSessionContext getSessionContext() { return null; }
        @Override public long getCreationTime() { return 0; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public void invalidate() {}
        @Override public boolean isValid() { return false; }
        @Override public void putValue(String name, Object value) {}
        @Override public Object getValue(String name) { return null; }
        @Override public void removeValue(String name) {}
        @Override public String[] getValueNames() { return new String[0]; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public javax.security.cert.X509Certificate[] getPeerCertificateChain()
                throws SSLPeerUnverifiedException { return new javax.security.cert.X509Certificate[0]; }
        @Override public Principal getPeerPrincipal() throws SSLPeerUnverifiedException { return null; }
        @Override public Principal getLocalPrincipal() { return null; }
        @Override public String getCipherSuite() { return ""; }
        @Override public String getProtocol() { return ""; }
        @Override public String getPeerHost() { return null; }
        @Override public int getPeerPort() { return 0; }
        @Override public int getPacketBufferSize() { return 0; }
        @Override public int getApplicationBufferSize() { return 0; }
    }
}
