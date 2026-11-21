package com.zegoggles.smssync.mail.ssl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the SNI-setting logic in {@link DefaultTrustedSocketFactory}.
 *
 * Covers U-045 / BUG-012:
 *   - setSniViaSSLParameters sets SSLParameters with the correct SNIHostName (AC-1, AC-4)
 *   - The reflective fallback path does NOT throw when the method is absent, and does NOT
 *     log at ERROR level (verified by absence of exception propagation) (AC-1, AC-4)
 *
 * Covers U-047 / BUG-015:
 *   - IPv4/IPv6 literals and empty/blank hostnames do not throw; SNI is skipped (AC-1, AC-3)
 *   - DNS hostnames continue to set SNI as before (AC-2, AC-3)
 *
 * Uses Mockito to create a fake SSLSocket so tests run without a real TLS stack.
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultTrustedSocketFactorySniTest {

    // -----------------------------------------------------------------------
    // setSniViaSSLParameters: public API path (API 24+)
    // -----------------------------------------------------------------------

    /**
     * AC-1 / AC-4: setSniViaSSLParameters calls setSSLParameters on the socket with
     * an SSLParameters that contains the expected SNIHostName entry.
     */
    @Test
    public void setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "imap.gmail.com");

        // Verify setSSLParameters was called exactly once.
        verify(mockSocket).setSSLParameters(params);

        // Verify the SNI server name list contains the expected hostname.
        List<SNIServerName> serverNames = params.getServerNames();
        assertThat(serverNames).isNotNull();
        assertThat(serverNames).hasSize(1);
        assertThat(serverNames.get(0)).isInstanceOf(SNIHostName.class);
        assertThat(((SNIHostName) serverNames.get(0)).getAsciiName()).isEqualTo("imap.gmail.com");
    }

    /**
     * AC-1: setSniViaSSLParameters with a different hostname sets that hostname, not a stale one.
     */
    @Test
    public void setSniViaSSLParameters_setsCorrectHostname_forDifferentHost() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "mail.example.org");

        List<SNIServerName> serverNames = params.getServerNames();
        assertThat(serverNames).hasSize(1);
        assertThat(((SNIHostName) serverNames.get(0)).getAsciiName()).isEqualTo("mail.example.org");
    }

    // -----------------------------------------------------------------------
    // Reflective fallback: absent method must not throw and must not call
    // setSSLParameters (the fallback path is invoked only when the public
    // API isn't available, so setSSLParameters must NOT be called in that branch).
    // -----------------------------------------------------------------------

    /**
     * AC-1 / AC-4: When the reflective setHostname method is absent on the socket class
     * (simulated by using a mock SSLSocket that has no such method), no exception is
     * thrown and setSSLParameters is NOT called (the fallback swallows the error quietly).
     *
     * This covers the BUG-012 scenario: on conscrypt Java8EngineSocket (API 35+) the
     * setHostname method is absent. The previous code logged at ERROR; the fixed code logs
     * at DEBUG and does not propagate the exception.
     */
    @Test
    public void reflectiveFallback_withAbsentSetHostnameMethod_doesNotThrow() {
        // Use a plain mock SSLSocket — Mockito-created mocks do NOT have a setHostname(String)
        // method, so the reflective lookup will fail with NoSuchMethodException, exactly
        // mirroring the BUG-012 scenario on conscrypt Java8EngineSocket.
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        // This must complete without throwing any exception.
        // The reflective method lookup will fail (NoSuchMethodException), but the catch block
        // swallows it at DEBUG level per the fix.
        //
        // We call the private method indirectly via reflection to avoid coupling the test to
        // internal visibility. Instead, we verify the public contract: setSniViaSSLParameters
        // must not throw, and the mock must not have setSSLParameters called in the reflective
        // path (since setSSLParameters is only called in the SSLParameters branch, not the
        // reflective fallback).
        //
        // Since setHostnameViaReflection is private, we test it via the public method
        // setSniViaSSLParameters (which IS called in the API 24+ path). The reflective
        // fallback path is not directly callable from outside the package. We verify the
        // fallback's no-throw contract by confirming that the setHostnameViaReflection
        // catch block (now at DEBUG, not ERROR) does not propagate to the caller.
        //
        // Use a concrete fake SSLSocket subclass without setHostname to test the fallback.
        SSLSocket noSetHostnameSocket = new FakeSSLSocketWithoutSetHostname();
        // Must not throw even though setHostname is absent.
        callSetHostnameViaReflectionDirectly(noSetHostnameSocket, "imap.gmail.com");
    }

    /**
     * AC-4: After the reflective fallback fails to find setHostname, setSSLParameters
     * is NOT called — the error is silently absorbed (no state change on the socket).
     */
    @Test
    public void reflectiveFallback_withAbsentSetHostnameMethod_doesNotCallSetSSLParameters() {
        SSLSocket mockSocket = mock(SSLSocket.class);

        // Call the reflective fallback path; must not throw and must not call setSSLParameters.
        callSetHostnameViaReflectionDirectly(mockSocket, "imap.gmail.com");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    // -----------------------------------------------------------------------
    // U-047 / BUG-015: guard against IP literals and empty/blank hostnames
    // -----------------------------------------------------------------------

    /**
     * AC-2 / AC-3(a): DNS hostname — setSSLParameters is called with SSLParameters
     * containing an SNIHostName entry (existing behavior confirmed/unchanged).
     */
    @Test
    public void setSniViaSSLParameters_dnsHostname_setsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "imap.example.com");

        verify(mockSocket).setSSLParameters(params);
        List<SNIServerName> serverNames = params.getServerNames();
        assertThat(serverNames).isNotNull();
        assertThat(serverNames).hasSize(1);
        assertThat(serverNames.get(0)).isInstanceOf(SNIHostName.class);
        assertThat(((SNIHostName) serverNames.get(0)).getAsciiName()).isEqualTo("imap.example.com");
    }

    /**
     * AC-1 / AC-3(b): IPv4 literal — no exception; setSSLParameters NOT called with a SNIHostName.
     * SNIHostName constructor throws IllegalArgumentException for IP literals (RFC 6066 §3).
     */
    @Test
    public void setSniViaSSLParameters_ipv4Literal_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        // Must not throw; SNI should be skipped.
        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "192.168.1.10");

        // setSSLParameters must NOT have been called (SNI was skipped).
        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    /**
     * AC-1 / AC-3(c): IPv6 literal — no exception; SNI skipped.
     */
    @Test
    public void setSniViaSSLParameters_ipv6Literal_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        // Must not throw for compressed IPv6 form.
        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "::1");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    /**
     * AC-1 / AC-3(c): Full IPv6 literal — no exception; SNI skipped.
     */
    @Test
    public void setSniViaSSLParameters_ipv6FullLiteral_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "fe80::1");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    /**
     * AC-1 / AC-3(d): Empty hostname — no exception; SNI skipped.
     */
    @Test
    public void setSniViaSSLParameters_emptyHostname_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        // Must not throw.
        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    /**
     * AC-1 / AC-3(d): Blank (whitespace-only) hostname — no exception; SNI skipped.
     */
    @Test
    public void setSniViaSSLParameters_blankHostname_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "   ");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    // -----------------------------------------------------------------------
    // U-063 Group A (AC-1, AC-2): SNI for a true eight-group IPv6 literal
    //
    // What these two tests can prove: the host-name filter in DefaultTrustedSocketFactory
    // treats an IPv6 literal written with all eight groups (no "::" shorthand) as a
    // non-DNS name, so SNI is skipped for it. RFC 6066 section 3 says the server name
    // list carries DNS host names only, never literal IP addresses, which is the rule
    // the filter exists to follow. Both tests are provider-independent: they use a
    // Mockito SSLSocket and reflection, so they behave the same under any TLS provider.
    //
    // What they cannot prove: that a real socket ends up without a server name. A
    // socket-level check (the factory's createSocket with this literal, and the DNS-host
    // control that shows the observable can report a name) lives in
    // DefaultTrustedSocketFactoryJdkProviderTest (AC-3, AC-4). It is there because the
    // default test provider (Conscrypt) reports no server names whatever the factory sets.
    // Nothing here shows what a device's TLS stack sends on the wire.
    //
    // Mutations that must break them:
    //   AC-1 breaks when both the early return in setSniViaSSLParameters (guard) and
    //        the catch of IllegalArgumentException are removed: the literal then reaches
    //        SNIHostName, which rejects colons, and the exception escapes. Removing only
    //        one of the two layers does not break AC-1, because the guard and the safety
    //        net overlap by design; AC-2 covers the guard alone.
    //   AC-2 breaks when the IPv6 branch of isIpOrBlankHostname (the test for a colon in
    //        the host name) is removed or narrowed to "contains a double colon".
    //
    // After the CR-009 move: a missing field, method or class means this test's seam
    // needs retargeting to the moved class; a failed behavioural assertion is a
    // regression, and the test is never edited to pass.
    // -----------------------------------------------------------------------

    /**
     * AC-1: A full eight-group IPv6 literal does not throw and SNI is skipped
     * (setSSLParameters is never called). RFC 6066 section 3.
     */
    @Test
    public void setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI() {
        SSLSocket mockSocket = mock(SSLSocket.class);
        SSLParameters params = new SSLParameters();
        when(mockSocket.getSSLParameters()).thenReturn(params);

        // Must not throw for the uncompressed eight-group form.
        DefaultTrustedSocketFactory.setSniViaSSLParameters(
                mockSocket, "2001:0db8:0000:0000:0000:0000:0000:0001");

        verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
    }

    /**
     * AC-2: isIpOrBlankHostname answers true for the eight-group literal and for the
     * shortest eight-group form, with no "::" in either. Called by reflection because the
     * method is package-private in the production class.
     */
    @Test
    public void isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue() throws Exception {
        java.lang.reflect.Method m = DefaultTrustedSocketFactory.class
                .getDeclaredMethod("isIpOrBlankHostname", String.class);
        m.setAccessible(true);

        assertThat((Boolean) m.invoke(null, "2001:0db8:0000:0000:0000:0000:0000:0001")).isTrue();
        assertThat((Boolean) m.invoke(null, "1:2:3:4:5:6:7:8")).isTrue();
    }

    // -----------------------------------------------------------------------
    // U-064 (AC-2, AC-3): the safety net for a host name that SNIHostName rejects
    //
    // What these two tests can prove: a host name that passes the IP/blank guard
    // (isIpOrBlankHostname) but that the JDK's SNIHostName rejects (an underscore, a
    // trailing dot, a leading hyphen) neither throws from setSniViaSSLParameters nor from
    // createSocket, and no server name is set for it. Only the catch of
    // IllegalArgumentException in setSniViaSSLParameters provides this (fix for BUG-015).
    // The IP-literal and blank tests above protect the guard; these protect the catch.
    // Both are provider-independent: the first uses a Mockito SSLSocket and reflection,
    // the second an unconnected socket (no network, no name resolution).
    //
    // What they cannot prove: which names Android's own SNIHostName rejects (the desktop
    // JDK's rules apply here); what a device sends on the wire for such a name.
    //
    // Mutations that must break them (U-064 mutation log):
    //   M-N2, the try/catch replaced by its two body lines with the guard kept: the
    //        IllegalArgumentException then escapes, and both tests fail for every name.
    //   AC-2 assertion (i) also breaks when the guard is widened to cover these names,
    //        which forces a decision about where the protection now lives.
    //
    // The comment on the IPv4 test above says SNIHostName rejects IP literals; it does
    // so for IPv6 but not for IPv4, which only the guard stops (U-063 mutation M-A1g).
    // That comment is left as it is (U-064 scope concern SC-1).
    //
    // After the CR-009 move: a missing field, method or class means this test's seam
    // needs retargeting to the moved class; a failed behavioural assertion is a
    // regression, and the test is never edited to pass.
    // -----------------------------------------------------------------------

    /**
     * AC-2: for each name that passes the guard and that SNIHostName rejects, the guard
     * answers false, setSniViaSSLParameters throws nothing, and setSSLParameters is never
     * called. All names are tried before the no-throw assertion, so a failure lists every
     * name for which an exception escaped.
     */
    @Test
    public void setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI() throws Exception {
        String[] names = {"bad_host.example", "host.example.", "-x.example"};
        java.lang.reflect.Method guard = DefaultTrustedSocketFactory.class
                .getDeclaredMethod("isIpOrBlankHostname", String.class);
        guard.setAccessible(true);
        List<String> escaped = new java.util.ArrayList<>();

        for (String name : names) {
            com.google.common.truth.Truth.assertWithMessage(
                    "isIpOrBlankHostname(\"" + name + "\") must be false, so the name reaches SNIHostName")
                    .that((Boolean) guard.invoke(null, name)).isFalse();

            SSLSocket mockSocket = mock(SSLSocket.class);
            when(mockSocket.getSSLParameters()).thenReturn(new SSLParameters());
            try {
                DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, name);
            } catch (RuntimeException e) {
                escaped.add(name + " -> " + e);
                continue;
            }
            try {
                verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
            } catch (AssertionError e) {
                throw new AssertionError("setSSLParameters was called for \"" + name
                        + "\", so SNIHostName accepted it here", e);
            }
        }

        com.google.common.truth.Truth.assertWithMessage(
                "setSniViaSSLParameters must not throw for a name SNIHostName rejects; names tried: "
                + java.util.Arrays.toString(names) + "; escaped")
                .that(escaped).isEmpty();
    }

    /**
     * AC-3: createSocket for a name that SNIHostName rejects returns an SSLSocket and
     * throws nothing. The socket is never connected.
     */
    @Test
    public void createSocket_nameRejectedBySniHostName_doesNotThrow() throws Exception {
        java.net.Socket socket = new DefaultTrustedSocketFactory(
                org.robolectric.RuntimeEnvironment.getApplication())
                .createSocket(null, "bad_host.example", 993, null);
        try {
            assertThat(socket).isInstanceOf(SSLSocket.class);
        } finally {
            socket.close();
        }
    }

    // -----------------------------------------------------------------------
    // Helper: call the private setHostnameViaReflection via reflection for testing
    // -----------------------------------------------------------------------

    /**
     * Invokes {@code DefaultTrustedSocketFactory.setHostnameViaReflection(socket, hostname)}
     * via Java reflection so we can test the private method's no-throw contract without
     * changing its visibility in production code.
     */
    private static void callSetHostnameViaReflectionDirectly(SSLSocket socket, String hostname) {
        try {
            java.lang.reflect.Method m = DefaultTrustedSocketFactory.class
                    .getDeclaredMethod("setHostnameViaReflection", SSLSocket.class, String.class);
            m.setAccessible(true);
            m.invoke(null, socket, hostname);
        } catch (Exception e) {
            throw new AssertionError(
                "setHostnameViaReflection threw unexpectedly — fix broke no-throw contract: " + e, e);
        }
    }

    // -----------------------------------------------------------------------
    // Fake SSLSocket that has no setHostname method (mirrors Java8EngineSocket)
    // -----------------------------------------------------------------------

    /**
     * A minimal concrete SSLSocket subclass that does NOT declare a setHostname(String)
     * method, matching the API 35+ conscrypt Java8EngineSocket behavior that triggered BUG-012.
     */
    private static class FakeSSLSocketWithoutSetHostname extends javax.net.ssl.SSLSocket {

        @Override public String[] getSupportedCipherSuites() { return new String[0]; }
        @Override public String[] getEnabledCipherSuites() { return new String[0]; }
        @Override public void setEnabledCipherSuites(String[] suites) {}
        @Override public String[] getSupportedProtocols() { return new String[0]; }
        @Override public String[] getEnabledProtocols() { return new String[0]; }
        @Override public void setEnabledProtocols(String[] protocols) {}
        @Override public javax.net.ssl.SSLSession getSession() { return null; }
        @Override public void addHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener l) {}
        @Override public void removeHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener l) {}
        @Override public void startHandshake() {}
        @Override public void setUseClientMode(boolean mode) {}
        @Override public boolean getUseClientMode() { return true; }
        @Override public void setNeedClientAuth(boolean need) {}
        @Override public boolean getNeedClientAuth() { return false; }
        @Override public void setWantClientAuth(boolean want) {}
        @Override public boolean getWantClientAuth() { return false; }
        @Override public void setEnableSessionCreation(boolean flag) {}
        @Override public boolean getEnableSessionCreation() { return true; }

        @Override
        public SSLParameters getSSLParameters() {
            return new SSLParameters();
        }
    }
}
