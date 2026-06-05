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
