// U-063 throwaway: print Base64 constants (stdout) and public provenance (stderr) for the fixtures.
// JDK only. Fails if a chain is not [leaf, issuer] or a key is not PKCS#8. Never committed with keys.
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class ExtractFixtures {
    static final char[] PW = "u063-test-only".toCharArray();   // test-only literal

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        for (String ca : new String[] {"trusted-ca", "untrusted-ca"}) {
            X509Certificate c = (X509Certificate) load(dir, ca).getCertificate(ca);
            emit(constName(ca) + "_CERT", c.getEncoded());
            summary(ca, c);
        }
        String[][] ids = {{"right-host", "trusted-ca"}, {"other-host", "trusted-ca"},
                          {"untrusted-host", "untrusted-ca"}, {"expired-host", "trusted-ca"}};
        for (String[] id : ids) {
            KeyStore ks = load(dir, id[0]);
            Certificate[] chain = ks.getCertificateChain(id[0]);
            if (chain == null || chain.length != 2) {
                throw new IllegalStateException(id[0] + ": chain length " + (chain == null ? 0 : chain.length));
            }
            X509Certificate leaf = (X509Certificate) chain[0];
            X509Certificate issuer = (X509Certificate) chain[1];
            X509Certificate expectedIssuer = (X509Certificate) load(dir, id[1]).getCertificate(id[1]);
            if (!issuer.equals(expectedIssuer)) {
                throw new IllegalStateException(id[0] + ": unexpected issuer");
            }
            PrivateKey key = (PrivateKey) ks.getKey(id[0], PW);
            if (!"PKCS#8".equals(key.getFormat())) {
                throw new IllegalStateException(id[0] + ": key format " + key.getFormat());
            }
            emit(constName(id[0]) + "_CERT", leaf.getEncoded());
            emit(constName(id[0]) + "_KEY_PKCS8", key.getEncoded());
            summary(id[0], leaf);
        }
    }

    static KeyStore load(File dir, String name) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(new File(dir, name + ".p12"))) {
            ks.load(in, PW);
        }
        return ks;
    }

    static String constName(String id) {
        return id.toUpperCase(java.util.Locale.ROOT).replace('-', '_');
    }

    static void emit(String name, byte[] der) {
        String b = Base64.getEncoder().encodeToString(der);
        System.out.println("    public static final String " + name + " =");
        for (int i = 0; i < b.length(); i += 64) {
            String part = b.substring(i, Math.min(b.length(), i + 64));
            System.out.println("            \"" + part + "\"" + (i + 64 < b.length() ? " +" : ";"));
        }
    }

    static void summary(String id, X509Certificate c) throws Exception {
        byte[] fp = MessageDigest.getInstance("SHA-256").digest(c.getEncoded());
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < fp.length; i++) {
            hex.append(String.format("%02X", fp[i])).append(i + 1 < fp.length ? ":" : "");
        }
        System.err.println(" * " + id + ": subject=" + c.getSubjectX500Principal().getName()
                + "; issuer=" + c.getIssuerX500Principal().getName()
                + "; SAN=" + c.getSubjectAlternativeNames()
                + "; notBefore=" + c.getNotBefore().toInstant()
                + "; notAfter=" + c.getNotAfter().toInstant()
                + "; SHA-256=" + hex);
    }
}
