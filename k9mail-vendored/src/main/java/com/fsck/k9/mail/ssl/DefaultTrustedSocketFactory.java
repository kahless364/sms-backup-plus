package com.fsck.k9.mail.ssl;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.MessagingException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


/**
 * Filter and reorder list of cipher suites and TLS versions.
 */
public class DefaultTrustedSocketFactory implements TrustedSocketFactory {
    protected static final String[] ENABLED_CIPHERS;
    protected static final String[] ENABLED_PROTOCOLS;

    protected static final String[] ORDERED_KNOWN_CIPHERS = {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
    };

    protected static final String[] BLACKLISTED_CIPHERS = {
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
    };

    protected static final String[] ORDERED_KNOWN_PROTOCOLS = {
            "TLSv1.2", "TLSv1.1", "TLSv1"
    };

    protected static final String[] BLACKLISTED_PROTOCOLS = {
            "SSLv3"
    };

    /**
     * The only protocol versions this factory ever enables, where the platform supports them
     * (BUG-019, BUG-020; product owner decision Record 8 of 2026-11-20: nothing older than
     * TLS 1.2). A name not in this list is never enabled, including a protocol name the factory
     * does not know, so a later TLS version needs an entry here before it is used.
     */
    protected static final String[] ALLOWED_PROTOCOLS = {
            "TLSv1.2", "TLSv1.3"
    };

    static {
        String[] enabledCiphers = null;
        String[] enabledProtocols = null;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLSocketFactory sf = sslContext.getSocketFactory();
            SSLSocket sock = (SSLSocket) sf.createSocket();
            enabledCiphers = sock.getEnabledCipherSuites();

            /*
             * Retrieve all supported protocols, not just the (default) enabled
             * ones. TLSv1.1 & TLSv1.2 are supported on API levels 16+, but are
             * only enabled by default on API levels 20+. The list is then
             * limited to TLS 1.2 and newer (U-065): see protocolsToEnable.
             */
            enabledProtocols = protocolsToEnable(sock);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting information about available SSL/TLS ciphers and " +
                    "protocols", e);
        }

        ENABLED_CIPHERS = (enabledCiphers == null) ? null :
                reorder(enabledCiphers, ORDERED_KNOWN_CIPHERS, BLACKLISTED_CIPHERS);

        ENABLED_PROTOCOLS = enabledProtocols;
    }

    public DefaultTrustedSocketFactory(Context context) {
        this.context = context;
    }

    protected static String[] reorder(String[] enabled, String[] known, String[] blacklisted) {
        List<String> unknown = new ArrayList<String>();
        Collections.addAll(unknown, enabled);

        // Remove blacklisted items
        if (blacklisted != null) {
            for (String item : blacklisted) {
                unknown.remove(item);
            }
        }

        // Order known items
        List<String> result = new ArrayList<String>();
        for (String item : known) {
            if (unknown.remove(item)) {
                result.add(item);
            }
        }

        // Add unknown items at the end. This way security won't get worse when unknown ciphers
        // start showing up in the future.
        result.addAll(unknown);

        return result.toArray(new String[result.size()]);
    }

    /**
     * The protocols this factory enables on {@code socket}: the protocols the platform supports
     * (not only those it enables by default), with the blocked protocols removed and the known
     * ones first ({@link #reorder}), and then only those in {@link #ALLOWED_PROTOCOLS}, so that
     * nothing older than TLS 1.2 is ever enabled (Record 8). TLS 1.2 and 1.3 are enabled wherever
     * the platform supports them, also where it leaves one off by default. If the platform
     * supports neither, the result is empty: the socket then cannot complete a handshake (or the
     * platform refuses the empty list), so the connection fails instead of falling back to an
     * older version. Every Android version this app supports (API 21 and later) supports TLS 1.2
     * and enables it by default (supported from API 16, enabled by default from API 20, as the
     * comment in the static initialiser and the SSLSocket reference documentation record).
     */
    protected static String[] protocolsToEnable(SSLSocket socket) {
        String[] ordered = reorder(socket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS,
                BLACKLISTED_PROTOCOLS);
        List<String> enabled = new ArrayList<String>();
        for (String protocol : ordered) {
            for (String allowed : ALLOWED_PROTOCOLS) {
                if (allowed.equals(protocol)) {
                    enabled.add(protocol);
                    break;
                }
            }
        }
        return enabled.toArray(new String[enabled.size()]);
    }

    private Context context;

    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {

        TrustManager[] trustManagers = new TrustManager[] { TrustManagerFactory.get(host, port) };
        KeyManager[] keyManagers = null;
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                keyManagers = new KeyManager[]{new KeyChainKeyManager(context, clientCertificateAlias)};
            } else {
                throw new KeyManagementException("clientCertificates are not supported with this version of Android");
            }
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        Socket trustedSocket;
        if (socket == null) {
            trustedSocket = socketFactory.createSocket();
        } else {
            trustedSocket = socketFactory.createSocket(socket, host, port, true);
        }

        SSLSocket sslSocket = (SSLSocket) trustedSocket;
        hardenSocket(sslSocket);
        setSniHost(socketFactory, sslSocket, host);

        return trustedSocket;
    }

    /**
     * Applies this factory's cipher-suite and protocol handling to {@code sock}. Public so that
     * {@code com.zegoggles.smssync.mail.PinnedCertificateSocketFactory} applies exactly the same
     * handling (BUG-020; product owner decision Record 9 of 2026-11-20, "Same as default"). It only
     * narrows and orders what the platform offers on the socket; it touches no trust manager and no
     * host-name decision.
     */
    public static void hardenSocket(SSLSocket sock) {
        hardenSocket(sock, ENABLED_CIPHERS, ENABLED_PROTOCOLS);
    }

    /**
     * Applies the given lists. A null {@code ciphers} leaves the platform's cipher suites. A null
     * {@code protocols} (the lists could not be computed at class initialisation) does not leave
     * the platform's protocols: the list is computed from {@code sock} itself, so the TLS 1.2
     * floor still applies (Record 8).
     */
    protected static void hardenSocket(SSLSocket sock, String[] ciphers, String[] protocols) {
        if (ciphers != null) {
            sock.setEnabledCipherSuites(ciphers);
        }
        sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));
    }

    public static void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // API 24+: use the public SSLParameters API to set the SNI server names list.
            // This is the supported path on all modern Android versions including API 35+
            // (conscrypt Java8EngineSocket) where the legacy reflective setHostname shim no
            // longer applies.
            setSniViaSSLParameters(socket, hostname);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                factory instanceof SSLCertificateSocketFactory) {
            SSLCertificateSocketFactory sslCertificateSocketFactory = (SSLCertificateSocketFactory) factory;
            sslCertificateSocketFactory.setHostname(socket, hostname);
        } else {
            setHostnameViaReflection(socket, hostname);
        }
    }

    /**
     * Returns {@code true} if the given hostname is an IP literal (IPv4 dotted-decimal or
     * IPv6 colon-notation) or is null/blank. Per RFC 6066 §3, SNI must not be sent for
     * IP literals; these hosts must bypass SNI setup entirely.
     *
     * <p>Detection is purely syntactic — no DNS resolution is performed.
     * IPv4: four decimal octets separated by dots (e.g. "192.168.1.10").
     * IPv6: any string containing a colon (e.g. "::1", "fe80::1", "2001:db8::1").
     */
    static boolean isIpOrBlankHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return true;
        }
        // IPv6: presence of ':' is definitive — no valid DNS label contains a colon.
        if (hostname.indexOf(':') >= 0) {
            return true;
        }
        // IPv4: four groups of 1-3 digits separated by dots, nothing else.
        return hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    /**
     * Sets the TLS SNI hostname via the public {@link SSLParameters#setServerNames} API.
     * Available on API 24+ (Android N). Extracted as a public static method to allow unit
     * testing with a mock SSLSocket without requiring Android API 24 system classes.
     *
     * <p>Per RFC 6066 §3, SNI must not be sent for IP literals or empty/blank hostnames.
     * We guard with an explicit IP/blank check followed by a try/catch safety net: if the
     * hostname is not a valid DNS name, SNI is silently skipped and TLS proceeds without
     * it (fix for BUG-015).
     */
    public static void setSniViaSSLParameters(SSLSocket socket, String hostname) {
        if (isIpOrBlankHostname(hostname)) {
            // IP literal or empty/blank — RFC 6066 §3 forbids SNI; skip silently.
            Log.d(LOG_TAG, "Skipping SNI for non-DNS hostname (IP literal or blank): " + hostname);
            return;
        }
        SSLParameters params = socket.getSSLParameters();
        try {
            params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
            socket.setSSLParameters(params);
        } catch (IllegalArgumentException e) {
            // Safety net: hostname passed the IP/blank check but SNIHostName still rejected it
            // (e.g. malformed label). Skip SNI rather than crash (fix for BUG-015).
            Log.d(LOG_TAG, "Skipping SNI — SNIHostName rejected hostname: "
                    + hostname + " — " + e.getMessage());
        }
    }

    private static void setHostnameViaReflection(SSLSocket socket, String hostname) {
        try {
            socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
        } catch (Throwable e) {
            // The reflective setHostname shim is a legacy path for pre-API 17 devices.
            // On modern Android (conscrypt Java8EngineSocket, API 35+) this method does not
            // exist — that is expected and benign. Log at DEBUG to avoid alarming ERROR noise.
            Log.d(LOG_TAG, "SSLSocket#setHostname(String) not available (expected on API 35+): "
                    + e.getMessage());
        }
    }
}
