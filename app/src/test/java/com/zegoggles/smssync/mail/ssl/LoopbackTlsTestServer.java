package com.zegoggles.smssync.mail.ssl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * TEST-ONLY loopback TLS server for the U-063 handshake tests. It is not production code and must not be
 * used outside tests.
 *
 * <p><b>What it does.</b> One server task runs on one daemon thread named {@code u063-loopback-test-server}.
 * It binds a plain listening socket on the loopback address (port 0, backlog 1), accepts one connection,
 * and then, in both modes, wraps the accepted plain socket in a server-side TLS socket and handshakes:
 * <ul>
 *   <li>{@link Mode#IMPLICIT_TLS}: the wrap and handshake follow the accept at once, as a server that speaks
 *       TLS from the first byte.</li>
 *   <li>{@link Mode#STARTTLS}: the server first writes {@link #GREETING} and a line end on the plain socket,
 *       then wraps and handshakes, as an IMAP server after the STARTTLS command.</li>
 * </ul>
 * After a successful handshake it writes one byte, {@link #PAYLOAD}, flushes and closes. The task never
 * throws: {@link #awaitOutcome()} returns {@code OK} or {@code FAILED:} followed by the exception class
 * name, or {@code TIMEOUT} if the task did not finish within {@link #TIMEOUT_MILLIS}.
 *
 * <p><b>Why it always wraps a plain socket.</b> The server always wraps an accepted plain socket and passes
 * the loopback address string as the peer host, because Conscrypt's server engine otherwise reflects into
 * {@code java.net.InetAddress} internals, which the JDK's module system refuses ({@code java.base} does not
 * open {@code java.net} to unnamed modules).
 *
 * <p><b>Bounds.</b> Every wait is bounded by {@link #TIMEOUT_MILLIS}: the accept, the reads on the accepted
 * socket, the client connect and read, and {@link #awaitOutcome()}. There are no sleeps and no polling.
 * Connections use the loopback address object, never a host name, so no name lookup happens.
 *
 * <p><b>The client is never configured here.</b> The client helpers only connect plain sockets and read a
 * greeting line; the protocols, ciphers and parameters of the client socket under test are never touched.
 *
 * <p><b>After the CR-009 move.</b> This class references no production class, so the move of the default
 * socket factory into app-owned code needs no retargeting here. A failed behavioural assertion in a test
 * that uses this server is a regression in the code under test; such a test is never edited to pass.
 */
public final class LoopbackTlsTestServer implements AutoCloseable {

    /** How the connection starts. */
    public enum Mode {
        /** TLS from the first byte. */
        IMPLICIT_TLS,
        /** A plain greeting line first, then TLS. */
        STARTTLS
    }

    /** Upper bound, in milliseconds, of every wait. */
    public static final int TIMEOUT_MILLIS = 10_000;

    /** The one data byte the server writes after the handshake. */
    public static final int PAYLOAD = 0x2A;

    /** The greeting line (without its line end) written in {@link Mode#STARTTLS}. */
    public static final String GREETING = "* OK U-063 loopback test server";

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final Future<String> outcome;
    private volatile Socket accepted;

    private LoopbackTlsTestServer(final Mode mode, final SSLContext serverContext) throws IOException {
        this.serverSocket = bindPlainLoopback();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "u063-loopback-test-server");
            thread.setDaemon(true);
            return thread;
        });
        this.outcome = executor.submit(() -> serve(mode, serverContext));
    }

    /**
     * Starts a server in the given mode. The server presents whatever chain {@code serverContext} holds.
     * The caller must {@link #close()} it.
     */
    public static LoopbackTlsTestServer start(Mode mode, SSLContext serverContext) throws IOException {
        return new LoopbackTlsTestServer(mode, serverContext);
    }

    private String serve(Mode mode, SSLContext serverContext) {
        try {
            Socket plain = serverSocket.accept();
            accepted = plain;
            plain.setSoTimeout(TIMEOUT_MILLIS);
            if (mode == Mode.STARTTLS) {
                OutputStream out = plain.getOutputStream();
                out.write((GREETING + "\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
            SSLSocket tls = (SSLSocket) serverContext.getSocketFactory().createSocket(
                    plain, InetAddress.getLoopbackAddress().getHostAddress(), plain.getPort(), true);
            tls.setUseClientMode(false);
            tls.startHandshake();
            OutputStream out = tls.getOutputStream();
            out.write(PAYLOAD);
            out.flush();
            tls.close();
            return "OK";
        } catch (Throwable e) {
            return "FAILED:" + e.getClass().getName();
        }
    }

    /** The port the server listens on. */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Waits at most {@link #TIMEOUT_MILLIS} for the server task. Returns {@code OK}, or
     * {@code FAILED:<exception class>}, or {@code TIMEOUT} if the task did not finish in time, or
     * {@code INTERRUPTED}. The value is meant to be asserted on, so a timeout is a visible failure.
     */
    public String awaitOutcome() {
        try {
            return outcome.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return "TIMEOUT";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            // unreachable in practice: the task catches every Throwable itself
            return "FAILED:" + (e.getCause() == null ? e : e.getCause()).getClass().getName();
        }
    }

    /** Closes the accepted and listening sockets and stops the thread. Idempotent; never throws. */
    @Override
    public void close() {
        closeQuietly(accepted);
        closeQuietly(serverSocket);
        executor.shutdownNow();
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // best effort; close() must never throw
            }
        }
    }

    /**
     * Connects a client socket to the loopback address and the given port, bounded by
     * {@link #TIMEOUT_MILLIS}, and sets the same read timeout.
     */
    public static void connect(Socket socket, int port) throws IOException {
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), TIMEOUT_MILLIS);
        socket.setSoTimeout(TIMEOUT_MILLIS);
    }

    /** Returns a new plain socket connected to the loopback address and the given port. */
    public static Socket connectPlain(int port) throws IOException {
        Socket socket = new Socket();
        try {
            connect(socket, port);
        } catch (IOException | RuntimeException e) {
            closeQuietly(socket);
            throw e;
        }
        return socket;
    }

    /**
     * Binds a plain listening socket on the loopback address: port 0, backlog 1, with the accept timeout
     * already set to {@link #TIMEOUT_MILLIS}. Tests that accept on the same thread use it, so their accept
     * is bounded too.
     */
    public static ServerSocket bindPlainLoopback() throws IOException {
        ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        socket.setSoTimeout(TIMEOUT_MILLIS);
        return socket;
    }

    /**
     * Reads one line, without its line end, from the raw stream of the socket: one byte at a time, at most
     * 256 bytes, failing on end of stream. <b>It is deliberately unbuffered.</b> A buffered reader could read
     * ahead and consume the first bytes of the server's TLS handshake, which the layered client socket must
     * see; a later edit must keep it byte by byte.
     */
    public static String readLine(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("end of stream before the end of the line");
            }
            if (b == '\n') {
                int n = line.length();
                if (n > 0 && line.charAt(n - 1) == '\r') {
                    line.setLength(n - 1);
                }
                return line.toString();
            }
            line.append((char) b);
        }
        throw new IOException("no line end within 256 bytes");
    }
}
