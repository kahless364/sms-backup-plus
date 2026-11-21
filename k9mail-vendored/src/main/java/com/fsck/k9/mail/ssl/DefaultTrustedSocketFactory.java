package com.fsck.k9.mail.ssl;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * The only cipher suites this factory ever enables, in this order, and only those of them that the
     * platform enables by default (story U-066: finding F-1 of the U-065 security review; product owner
     * decision Record 11, answer 2; the orchestrator's rulings of 2026-11-21 under Record 12). Each gives
     * forward secrecy and authenticated encryption: first the three TLS 1.3 suites, in the order in which
     * Conscrypt, the Android provider, lists them; then the four TLS 1.2 ECDHE suites with AES-GCM,
     * strongest first and RSA before ECDSA, the order used before U-066; then the two TLS 1.2 ECDHE suites
     * with ChaCha20-Poly1305; last the renegotiation signalling value, which is not a cipher and is kept
     * as the platform lists it. A suite not in this list is never enabled, including a suite the factory
     * does not know, so a later suite needs an entry here before it is used, as a later protocol version
     * needs one in {@link #ALLOWED_PROTOCOLS}. Until U-066 the factory removed a blocked list, placed a
     * preferred list first and kept every other suite the platform enabled. Sources: RFC 8446 Appendix B.4
     * and section 9.1 (the TLS 1.3 suites); RFC 9325 sections 4.1 and 4.2 (the four AES-GCM suites, which
     * it recommends); RFC 7905 section 2 (the two ChaCha20-Poly1305 suites, beyond RFC 9325's four);
     * RFC 5746 section 3.3 (the signalling value). The TLS 1.2 order is inherited, not an RFC's.
     */
    protected static final String[] ALLOWED_CIPHERS = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
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
            // Only the allowed suites that the platform enables by default, in the allowed
            // order (U-066): see ciphersToEnable.
            enabledCiphers = ciphersToEnable(sock);

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

        ENABLED_CIPHERS = enabledCiphers;

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

        // Add unknown items at the end, in their original order. Since U-066 only
        // protocolsToEnable calls this, and it then keeps only ALLOWED_PROTOCOLS.
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

    /**
     * The cipher suites this factory enables on {@code socket}: those of {@link #ALLOWED_CIPHERS} that the
     * platform enables by default on it ({@code getEnabledCipherSuites()}, never the supported suites, so
     * the factory never turns on a suite the platform leaves off), in the order of
     * {@link #ALLOWED_CIPHERS}. Every other suite stays off, whatever its name. If the platform enables
     * none of the allowed suites, the result is empty and no other suite is used instead (fail closed);
     * what a platform then does with the empty list, and whether a handshake can complete, is the
     * platform's (CR-014, "Not stated"). The two emulators measured for U-066 (Android 11 and 16) enable
     * all of the allowed suites by default.
     */
    protected static String[] ciphersToEnable(SSLSocket socket) {
        List<String> offered = Arrays.asList(socket.getEnabledCipherSuites());
        List<String> suites = new ArrayList<String>();
        for (String allowed : ALLOWED_CIPHERS) {
            if (offered.contains(allowed)) {
                suites.add(allowed);
            }
        }
        return suites.toArray(new String[suites.size()]);
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
     * Applies the given lists. A null list (the lists could not be computed at class initialisation)
     * does not leave the platform's suites or protocols: each is computed from {@code sock} itself, so
     * the allowed suites (U-066) and the TLS 1.2 floor (Record 8) still apply. If that computation
     * throws, the exception leaves the calling {@code createSocket} and no socket is returned.
     */
    protected static void hardenSocket(SSLSocket sock, String[] ciphers, String[] protocols) {
        sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));
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
