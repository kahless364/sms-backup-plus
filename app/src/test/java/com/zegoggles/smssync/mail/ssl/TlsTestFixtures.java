package com.zegoggles.smssync.mail.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * TEST-ONLY certificates and private keys for the U-063 TLS characterisation tests.
 *
 * <p><b>Nothing here is a secret and nothing here may be used outside tests.</b> The keys are throwaway: they
 * were generated offline, once, for story U-063, with the JDK's {@code keytool} (JDK 25), and are used
 * nowhere else. The two CA private keys were deleted after signing; only the CA certificates are kept here.
 * The four leaf (server) private keys are kept as PKCS#8 so a loopback test server can present them. The
 * store password used while generating, and the in-memory key password {@link #TEST_ONLY_KEY_PASSWORD}, is
 * a test-only literal that protects nothing.
 *
 * <p>The constants are the Base64 text of DER certificates and PKCS#8 keys, extracted from the generated
 * PKCS12 files by a throwaway JDK program, so no PKCS12 blob is parsed at test time.
 *
 * <p><b>Generation (exact commands).</b> Run once, in this order, with {@code <gen-dir>} standing for a
 * scratch folder and {@code <pw>} for the test-only password literal. Common key options:
 * {@code -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -storetype PKCS12 -storepass <pw> -keypass <pw>}.
 * <pre>
 * for each CA (alias trusted-ca, untrusted-ca):
 *   keytool -genkeypair -alias CA -dname DN [common key options] -keystore &lt;gen-dir&gt;/CA.p12
 *       -startdate "2019/12/01 00:00:00" -validity 36500 -ext bc:c=ca:true,pathlen:0 -ext ku:c=keyCertSign,cRLSign
 *   keytool -exportcert -alias CA -keystore &lt;gen-dir&gt;/CA.p12 -storetype PKCS12 -storepass &lt;pw&gt; -rfc -file &lt;gen-dir&gt;/CA.crt
 *
 * for each leaf (id, host, issuer, startdate, validity-days):
 *   keytool -genkeypair -alias ID -dname "CN=HOST, O=U-063 Test Only" [common key options]
 *       -keystore &lt;gen-dir&gt;/ID.p12 -startdate START -validity DAYS
 *   keytool -certreq -alias ID -keystore &lt;gen-dir&gt;/ID.p12 -storetype PKCS12 -storepass &lt;pw&gt; -file &lt;gen-dir&gt;/ID.csr
 *   keytool -gencert -alias ISSUER -keystore &lt;gen-dir&gt;/ISSUER.p12 -storetype PKCS12 -storepass &lt;pw&gt;
 *       -sigalg SHA256withRSA -infile &lt;gen-dir&gt;/ID.csr -outfile &lt;gen-dir&gt;/ID.crt -rfc -startdate START -validity DAYS
 *       -ext san=dns:HOST -ext ku:c=digitalSignature,keyEncipherment -ext eku=serverAuth -ext bc=ca:false
 *   keytool -importcert -alias ISSUER -file &lt;gen-dir&gt;/ISSUER.crt -keystore &lt;gen-dir&gt;/ID.p12 -storetype PKCS12 -storepass &lt;pw&gt; -noprompt
 *   keytool -importcert -alias ID -file &lt;gen-dir&gt;/ID.crt -keystore &lt;gen-dir&gt;/ID.p12 -storetype PKCS12 -storepass &lt;pw&gt; -noprompt
 *
 * CA distinguished names: "CN=U-063 Test Trusted CA, O=U-063 Test Only" and
 *                         "CN=U-063 Test Untrusted CA, O=U-063 Test Only".
 * leaves: right-host (imap.right.test, trusted-ca, 2020/01/01 00:00:00, 32000 days),
 *         other-host (imap.other.test, trusted-ca, 2020/01/01 00:00:00, 32000 days),
 *         untrusted-host (imap.right.test, untrusted-ca, 2020/01/01 00:00:00, 32000 days),
 *         expired-host (imap.right.test, trusted-ca, 2020/01/01 00:00:00, 30 days).
 * </pre>
 * The constants were then produced with a single-file JDK program that opens each PKCS12 file, checks that
 * every leaf chain is exactly [leaf, issuer] and every key is PKCS#8, and prints the Base64 constants and
 * the provenance lines below (the program is kept with the story's evidence tools).
 *
 * <p><b>What each certificate is</b> (subject, issuer, SAN as type and name, validity in UTC, SHA-256
 * fingerprint of the DER certificate):
 * <ul>
 *   <li>TRUSTED_CA: subject {@code CN=U-063 Test Trusted CA,O=U-063 Test Only}, self-signed, no SAN;
 *       notBefore 2019-12-01T06:00:00Z, notAfter 2119-11-07T06:00:00Z;
 *       SHA-256 BC:8F:B4:8B:08:07:4C:65:95:E9:62:84:48:44:41:72:50:7D:00:EB:CA:0D:37:CB:08:E3:57:01:C6:CA:D8:AF.
 *       The trust anchor of the handshake tests.</li>
 *   <li>UNTRUSTED_CA: subject {@code CN=U-063 Test Untrusted CA,O=U-063 Test Only}, self-signed, no SAN;
 *       notBefore 2019-12-01T06:00:00Z, notAfter 2119-11-07T06:00:00Z;
 *       SHA-256 44:C1:B1:40:50:5C:65:8B:B5:E7:1E:3C:98:34:05:DC:EC:DD:3F:14:70:85:1B:5B:0D:AF:3E:F8:6B:32:37:41.
 *       Never installed as an anchor.</li>
 *   <li>RIGHT_HOST: subject {@code CN=imap.right.test,O=U-063 Test Only}, issuer Trusted CA, SAN [2, imap.right.test];
 *       notBefore 2020-01-01T06:00:00Z, notAfter 2107-08-13T06:00:00Z;
 *       SHA-256 D5:75:0C:84:C8:F0:35:2A:80:D3:05:B4:77:92:79:23:F2:2D:92:B3:09:C9:4B:AF:78:86:96:AC:0B:E5:9F:E8.</li>
 *   <li>OTHER_HOST: subject {@code CN=imap.other.test,O=U-063 Test Only}, issuer Trusted CA, SAN [2, imap.other.test];
 *       notBefore 2020-01-01T06:00:00Z, notAfter 2107-08-13T06:00:00Z;
 *       SHA-256 2E:AB:56:BD:C6:76:4F:00:91:6F:6D:F9:33:FE:40:41:D2:76:34:66:7D:2E:EE:48:29:A9:95:5C:10:C2:4B:3F.
 *       Also the enrolled leaf of the pinned-factory tests.</li>
 *   <li>UNTRUSTED_HOST: subject {@code CN=imap.right.test,O=U-063 Test Only}, issuer Untrusted CA, SAN [2, imap.right.test];
 *       notBefore 2020-01-01T06:00:00Z, notAfter 2107-08-13T06:00:00Z;
 *       SHA-256 CC:E9:16:92:D7:49:91:53:A6:99:BC:B0:8B:5C:92:EB:2F:98:AE:D6:83:DF:48:03:DE:8B:D3:A3:83:61:18:C7.</li>
 *   <li>EXPIRED_HOST: subject {@code CN=imap.right.test,O=U-063 Test Only}, issuer Trusted CA, SAN [2, imap.right.test];
 *       notBefore 2020-01-01T06:00:00Z, notAfter 2020-01-31T06:00:00Z (expired on purpose; it lies inside the
 *       CA window, so expiry is its only defect);
 *       SHA-256 9C:D8:0B:E5:84:B3:70:71:1F:C7:AC:AD:67:92:5E:A7:3F:DE:81:E9:1E:F5:AA:C0:AC:2C:2A:C5:09:04:C2:79.</li>
 * </ul>
 * The valid certificates start before 2025-01-01 and end after 2100-01-01, so no test machine's clock makes
 * them expired or not yet valid. The key is RSA 2048 with SHA-256 signatures.
 *
 * <p><b>After the CR-009 move.</b> This class references no production class, so the move of the default
 * socket factory into app-owned code needs no retargeting here. A failed behavioural assertion in a test
 * that uses these fixtures is a regression in the code under test; such a test is never edited to pass.
 */
public final class TlsTestFixtures {
    private TlsTestFixtures() {
    }

    public static final String TRUSTED_CA_CERT =
            "MIIDPTCCAiWgAwIBAgIILoQGv9v05hQwDQYJKoZIhvcNAQELBQAwOjEYMBYGA1UE" +
            "ChMPVS0wNjMgVGVzdCBPbmx5MR4wHAYDVQQDExVVLTA2MyBUZXN0IFRydXN0ZWQg" +
            "Q0EwIBcNMTkxMjAxMDYwMDAwWhgPMjExOTExMDcwNjAwMDBaMDoxGDAWBgNVBAoT" +
            "D1UtMDYzIFRlc3QgT25seTEeMBwGA1UEAxMVVS0wNjMgVGVzdCBUcnVzdGVkIENB" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqvhQi3kIWxbOpImu5ZM7" +
            "4O0pWUDFFhwgM1S7wHVLQNQdY5rpfX5quLDQjD2AKgBlVKTBM0v0ngACYd5wsCml" +
            "wRtgehKH1niyoptSSFm/hpxzKElcAsBgDsM0MKHJtwRwTPPmalMOY4eLtukyTMgu" +
            "McZ4ySq2fV+c8VKBq+I7hpdmmxMBCPCvDYhloCkmC4AgObZNgCGwJc/m58nw8qlx" +
            "6Zvh80tLn+KhCtY4wi7WQ+Ssp40JngcmbHWenOaC9BOZktCseN8ZIhp8pGENdi1P" +
            "20nDHVrh/99UyG7KYc3tKNUuzicGTVMjLXuw51y0/6tW44MbTEq4XqY1Vj7Ob5FA" +
            "WwIDAQABo0UwQzAdBgNVHQ4EFgQUlC4dL1IaaiID/APqYVnqnCAYcLMwDgYDVR0P" +
            "AQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAwDQYJKoZIhvcNAQELBQADggEB" +
            "ADYt97vTb+JW8cGEPKE6T66h9WtagINpROWRm7RWOFJJJSlxiAB5Nl7VaopuI4+I" +
            "CNhpRdCPoRtpbOR6MJmsGj/pUqj0wQaJTJSIi0ze7rShZREi8qkGpOu9GhHIlzqP" +
            "cLYJ73NTheEABB7IFEjlDqVNCTBzIqeUjQWTI5x+nRrfD715/+G9Ar30X8Ct0tcT" +
            "F8lK0bHoK3i6kXAH1zfEcI7zdbcUOygTZDCeXNxjt28B67l0fakonX2dZFf1Eeei" +
            "eONSxzyK+MwRo3uzDFEHWgg7siXgFwviQMmI63pscUJTc4LqmSOo6lUU6yRsTlOx" +
            "b2Hy8HbrKJwlJECwcBgBK5k=";
    public static final String UNTRUSTED_CA_CERT =
            "MIIDQTCCAimgAwIBAgIIWrSMRvREPkIwDQYJKoZIhvcNAQELBQAwPDEYMBYGA1UE" +
            "ChMPVS0wNjMgVGVzdCBPbmx5MSAwHgYDVQQDExdVLTA2MyBUZXN0IFVudHJ1c3Rl" +
            "ZCBDQTAgFw0xOTEyMDEwNjAwMDBaGA8yMTE5MTEwNzA2MDAwMFowPDEYMBYGA1UE" +
            "ChMPVS0wNjMgVGVzdCBPbmx5MSAwHgYDVQQDExdVLTA2MyBUZXN0IFVudHJ1c3Rl" +
            "ZCBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJ3Ph5sRW5d7hvJL" +
            "LrX7HeWOpuvX+vbZIqLl2qHsuoaiX8Fqnx9/zv8GuDpSiws/7j+T03BEIIW5UBgk" +
            "pkjZugw1QLG8Gu5B5DGMoCyPmJwL4Py3gxsL8JTEqaWz5yRMlVc7By2ptutlnKMg" +
            "teU/zfqv5wMQGziMJvUbtolcr/+/KA912uqD/KQiNmN+cdLqaFuZsq3LCoLWa3Zk" +
            "P9FfFPn0o8veGC8EdCsYhXk+VjP5Gb0dqvc4dL7r9U81M7GZy0i4Z8e0y/4Hx1Sb" +
            "YJ+xq8+GfUKV3ZRif4aswUm1S7n9CLEZygIhRJO9dfLHAzJFg0jjSNmwdrW8ZBrY" +
            "0Gk+lKsCAwEAAaNFMEMwHQYDVR0OBBYEFI7OuT/SV+xLpPlVoYlZtEPFiTA4MA4G" +
            "A1UdDwEB/wQEAwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMA0GCSqGSIb3DQEBCwUA" +
            "A4IBAQB4IbAGaXsTlpcWw7ANHtFz2n7GfCtvp8Ac4IHTehKqxgiyV6cdhgkRh8u4" +
            "s0b5kW2SGkp0LV8OATloX7qN5vRnPipT6bZoTElTk8SZ30X5wBDt4zqaay0WIHNw" +
            "Z8MZ1sf0LXMJgqiKDMAj6+C8sbcN9EvhFUTmCKCAm2n+OMhw1eE78Qdfai2Ry5rX" +
            "huk0Qm6txb0JYVPfBcM5YGREq0ZFLEi6J8dFGqe2GGKATtgmajwFTvyXqbkM+4c/" +
            "pbfosDLsYx3BBRoji7pE90CdbuTKsF/zgWZiNIvKm8d+JjJwa0rSo/NtKpSEAwv2" +
            "wHAwSRxejjepTMtF8WZVKiQpl3Jq";
    public static final String RIGHT_HOST_CERT =
            "MIIDgzCCAmugAwIBAgIJANhgAuy/MA0KMA0GCSqGSIb3DQEBCwUAMDoxGDAWBgNV" +
            "BAoTD1UtMDYzIFRlc3QgT25seTEeMBwGA1UEAxMVVS0wNjMgVGVzdCBUcnVzdGVk" +
            "IENBMCAXDTIwMDEwMTA2MDAwMFoYDzIxMDcwODEzMDYwMDAwWjA0MRgwFgYDVQQK" +
            "Ew9VLTA2MyBUZXN0IE9ubHkxGDAWBgNVBAMTD2ltYXAucmlnaHQudGVzdDCCASIw" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJvgNXGLBdGVrxwNhW6JwN2Hn99/" +
            "g9G9VtP+FaMwHrSAP5UpTnQm95yGvmppggJn+96OkOnMKGJsBuTR6cSgpMXE9gwl" +
            "OSnZp3msJAFqk4ceEyIOvtfkrzidBvRHwB0VMZevma+pyoMQ1fFGFy2flAjC0Wnm" +
            "/o3gBDQ3T3rlUOAGgBcVp5HG7lcQoWxBGutEeDSu8AaBJCav19k46LfE+I2cjcnC" +
            "3XSdMd2+ZmBo3lX/BfiMTzgjGzmPM9COV8fZrEezp0XWYSkczzNuNilnXTO+RC+9" +
            "6RdlzLtGz9wiwLTiXtWW7efW3FvWBsJ4ZPKq4m7j11MXQZPUeI4uUSG/F5kCAwEA" +
            "AaOBjzCBjDAdBgNVHQ4EFgQUkOVGjoPl/hYPPGdYJfALOvc7luQwDgYDVR0PAQH/" +
            "BAQDAgWgMBoGA1UdEQQTMBGCD2ltYXAucmlnaHQudGVzdDAJBgNVHRMEAjAAMB8G" +
            "A1UdIwQYMBaAFJQuHS9SGmoiA/wD6mFZ6pwgGHCzMBMGA1UdJQQMMAoGCCsGAQUF" +
            "BwMBMA0GCSqGSIb3DQEBCwUAA4IBAQBjhdk+rq2AU2EHWRg6sbQcSjnQ7s4/pg0I" +
            "BbL9Hr/e1Lpdknfv8U2ul16RatdJxb75Xc2IlD1G1b05Heg4y9oEbhWEsMw6zhH0" +
            "s++BoDNB1y6iCekefzU7+vONfI8nXNRm7GgcGyV6KYXLrdfCuStu3z5eAdvbZCPR" +
            "f3qdqIrnFbYdOFYGBUe6C+1QpkHe7XqrKTfr5F///N1jp7aaRata7FEYK3p5SU8e" +
            "OIq75PhtWigP+zuS7eDM6Eg+xRkWF0lFqYjmWcLaHQ1FrugLgPaSp040JnIDDXjV" +
            "p37E75F4x/8hVYNhcvlZ9BLcr82l+4mPjdYQvp9wm3qs0C8IbTjF";
    public static final String RIGHT_HOST_KEY_PKCS8 =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCb4DVxiwXRla8c" +
            "DYVuicDdh5/ff4PRvVbT/hWjMB60gD+VKU50Jvechr5qaYICZ/vejpDpzChibAbk" +
            "0enEoKTFxPYMJTkp2ad5rCQBapOHHhMiDr7X5K84nQb0R8AdFTGXr5mvqcqDENXx" +
            "Rhctn5QIwtFp5v6N4AQ0N0965VDgBoAXFaeRxu5XEKFsQRrrRHg0rvAGgSQmr9fZ" +
            "OOi3xPiNnI3Jwt10nTHdvmZgaN5V/wX4jE84Ixs5jzPQjlfH2axHs6dF1mEpHM8z" +
            "bjYpZ10zvkQvvekXZcy7Rs/cIsC04l7Vlu3n1txb1gbCeGTyquJu49dTF0GT1HiO" +
            "LlEhvxeZAgMBAAECggEAAIHePtGvQnkrNUGYNYvG9xTONMj0U2w2LYpYvmGCOI2+" +
            "+m3EhCrMLc2px8Ooj6F2tHRud1M0nzOKLs3NP4kQlkw6LFSlXMplR/LhzXn6NYR5" +
            "2YZlVyXfxhLnjjgwo/EwucdDebtzZs4OxaGonY13X0ztVbqW0Tx85WorG6Q7/wmw" +
            "RvhiHBWaBDxQnDWTyVnZg3afwUOXA4C7FCnaXm1hkj90d0OrVnxL/6fyVjvcJ05P" +
            "uQlqhPRtL1bpI7uNOoCX8bbq4iUP5x6MO64nSxhpsQodv61kEwWBOHwaCo2zqZEh" +
            "ZHBn/aCEVEhECWeUG1aHkRY8hJ/hkXbSzDypE0S8JQKBgQDCJv11BeYW1ou5uV4o" +
            "K5/s/kPsrweEt0wXDf7CPcD/6AuQIucRVvCrnlt30n4PHvaur9kxeOaysHUrwQKh" +
            "UtM/ICg4uD0/Rbq5SRJZsjxM5IQ/h2A/0wtP43MuXCTzOkXR/wKJNTEw2Ob4Cey1" +
            "3uM/OboCWoF+L1lhqQo/MjnEtQKBgQDNh8rzJwIZdXBvF+DH49WtJt2ja6hKgKfj" +
            "lXRAVF0msHUFSUT1yFfLTl8D/XvTeAIBVjEWH9k7h/2fcYg7CxO9w+Loe68WdczY" +
            "JzotkT34o0hKiy0lOawjPtQp7OXc2wkmK3+nQm1b6Yli0k78Y5zIesMDW/18Kegt" +
            "YAxHT83Z1QKBgH+ZoHqIpe/ckXg+m+NKyJC5i2rCne5biWadjBGdBHsrxm20gqii" +
            "uwoPyY22iaLmdrnYWKZrDzPhRSC8eT4NcUjLmaOtcpNjAJVFsQnCWl8u0Lvv/RH1" +
            "YjnfApQ1exNf+QNK1h/XFb1E0Iez/h6JnlCvX9CsKu8Dr3q/3jwqhk0BAoGBAI9O" +
            "n7Pq/wvBVyQfAhxeQB77eo74Ad/w1R9aljREnq7bWrzU2cCGcLDRdG7T0AMjKkLE" +
            "VnWWv2FC8RS7lMGWd61ZaF2x3+NJQAt3Tpl3LD6xmzZe1JugGuZ4aHbTN3NHRpnW" +
            "uqtlJdtwetrsySYyTp96+X7eJWDt13XMAsO2YVIBAoGANf5ljG1F0TeXO0ELXYVR" +
            "sNTSagq5FKoLQzxsoGAjsOr2SPNjhnxUebZtfqzfBMQ7kyRoP9K9O/4VxK5RqoWK" +
            "Q78FhNzyKlaDKcxrmc3+IDs5S8qd1k50O1WEO5s9RgVuJ5YCcEr4I4z/uQQxMdNx" +
            "qu1fZipFdOB2vOB0FuktzsY=";
    public static final String OTHER_HOST_CERT =
            "MIIDgzCCAmugAwIBAgIJAK3oeEQSLnMIMA0GCSqGSIb3DQEBCwUAMDoxGDAWBgNV" +
            "BAoTD1UtMDYzIFRlc3QgT25seTEeMBwGA1UEAxMVVS0wNjMgVGVzdCBUcnVzdGVk" +
            "IENBMCAXDTIwMDEwMTA2MDAwMFoYDzIxMDcwODEzMDYwMDAwWjA0MRgwFgYDVQQK" +
            "Ew9VLTA2MyBUZXN0IE9ubHkxGDAWBgNVBAMTD2ltYXAub3RoZXIudGVzdDCCASIw" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI+xFml7C97gm4sgjn5dezovIAvq" +
            "3+BsT+Q8vw9QrST/Y0xI5tCxpDebj9sJILmcnGIuEysoHCXlNW8xZIqcZluE9kmG" +
            "Mcjql9jpjtMJNR8K+g93IRci8NoogoNDU5+Z+s20rj9NnkLUs2ikBX5zDb2vB8wW" +
            "ZbQ9ZDQA+PNVmhINaQV3DtbxMh+GlXz69Q6TAT5mcGe1lr6LmfkEmmTOdV8aC7Ar" +
            "voihk+gw57MYa5h9/ATgM0O4uxghqKTuQzup1G8Y94MiczqVvHFs7uBMqmOun3wy" +
            "xw9JXrMSWSRyxYXSTJiALokjHtWKeIYzthx8yEtllNm0yr//yAanpO3mLUkCAwEA" +
            "AaOBjzCBjDAdBgNVHQ4EFgQU6zgbhrRNsE1gwjxBPiXOVxE1rLEwDgYDVR0PAQH/" +
            "BAQDAgWgMBoGA1UdEQQTMBGCD2ltYXAub3RoZXIudGVzdDAJBgNVHRMEAjAAMB8G" +
            "A1UdIwQYMBaAFJQuHS9SGmoiA/wD6mFZ6pwgGHCzMBMGA1UdJQQMMAoGCCsGAQUF" +
            "BwMBMA0GCSqGSIb3DQEBCwUAA4IBAQCeuoRJMdszrEFmXvwWcJYfq3xr2qa1937B" +
            "rHnUmSfpNHJUciz8Bq2x9bLSr2SIPoimCokj2DnhUvB3bZtx15MOYo9YYrnSYqLf" +
            "4/9cTn/S+NSr1d1989uSVdSTr5Kbi2atI6tttbj44tCZ5BuFvemY9BBPS9nSzrJE" +
            "Hq8LXLsEmpeWRdvVoMzdpcR0TS6V3RCrvJjeECRz4RW0+POZlVvjjKuuB6PMYYb6" +
            "a6bVsovm5UYBYXIVld0VCvYf4RNsQA+4WeQcv0gTypwDcP/Omkk97RQ4E7CoMla2" +
            "4taFXJnXf2AXtbnNA7F1O1RpIoDkEM66P7jvA9rpVFsUQnoSJxww";
    public static final String OTHER_HOST_KEY_PKCS8 =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCPsRZpewve4JuL" +
            "II5+XXs6LyAL6t/gbE/kPL8PUK0k/2NMSObQsaQ3m4/bCSC5nJxiLhMrKBwl5TVv" +
            "MWSKnGZbhPZJhjHI6pfY6Y7TCTUfCvoPdyEXIvDaKIKDQ1OfmfrNtK4/TZ5C1LNo" +
            "pAV+cw29rwfMFmW0PWQ0APjzVZoSDWkFdw7W8TIfhpV8+vUOkwE+ZnBntZa+i5n5" +
            "BJpkznVfGguwK76IoZPoMOezGGuYffwE4DNDuLsYIaik7kM7qdRvGPeDInM6lbxx" +
            "bO7gTKpjrp98MscPSV6zElkkcsWF0kyYgC6JIx7ViniGM7YcfMhLZZTZtMq//8gG" +
            "p6Tt5i1JAgMBAAECggEAFrr1kn0M/SunCgSold51fQLxvQDhe7pr5EJ5xNNHIy2l" +
            "MxmV2FiUONm1EXn/ARhVorBVhu5YOY8Ue1ZNvcw3jZhZxNAOIGbVMNKNgzPB1qXM" +
            "CytAraIbhfSCOip/LXr/B5BilLqouOYLIBf7kyu3IOpHFPv2sw2XxCQ36CJX9i0y" +
            "iKzeUsOzEAd929Lj5o8/aAbQlgsjsy+o3kPIa46haQzwDA17DPFr4n4Nr7XcHsYj" +
            "AyGThQeZ0kT1YVZ/C7laJMz/hmnsSQ8MyJ/vHKbCT81I8ygd5xxz2sNITdx6FkN1" +
            "yr6BcEqCTcp4VyQt3cG4bPx9iqTWuI+XmNGLZwgPWwKBgQC7vF0tbtojJMAzyRV4" +
            "r5Vz4GKfiwUMUd5DFAVCnKGPdp4p77L4ZtGti2J1/i3M4o0AlQwPRNbYA5h3K/Ak" +
            "V0GbhlkDWoxBdM9QBIRFgUnfsKdM4hwz6s+1MbpP2us7yboPaF8EV9qhOzmUYW8t" +
            "1K+PpgTpcV74dkqxmtFQTZHkXwKBgQDD8NMmi3AiwvRMLcXC/btnWWRMVyvXRkYx" +
            "qbaVYRqnlPAPxbGYS321TIoF0a/siFfE9fgNqGt2IM9QhXQYktGXkTy6uVgvPOIl" +
            "TNfgazEomCGMi9jB9JcW1oMQEfqcO3t7+3SWNdgw8n9T8KKtdkW6f8N0YAhyvEKi" +
            "WBqPoRAPVwKBgQChbi2pPaLxiU6EuSpvZnxa8d2cVwx2NYeronGLf+gk1tjWMrlw" +
            "g+lVE4AM+g4kGcMpdwCxuH4NrbnS78NFXIqb04CDM79bIbiKk/exVKVTZjHWGLAr" +
            "rIQCyMnulM6l0KbfTN43e9wRvD2c69OQU0zLdvTOT6BcgktkQT4vkSQcfQKBgHhk" +
            "ZJjuXDw/pXOZPEYPOTLLTkFJ3norh73EFZe7TnFhpg9K3VBCMj9PhA8HRB0FuUgg" +
            "CgX6Ue0HQ9qp5SekLnv+lV31vCVqN1nL2Wje9BmJQDPnSYwDCULTEzywj1enhLwt" +
            "SPlypB+PIIUm+g2y+i0Zi7LZvtcgraCqmcdeo0uJAoGBAIwJnUNf9jCK4YkR7wHU" +
            "afRSxuUN7fa/K32OmJnGIjxuhZ4/uLuQX8JGqKwSuZ6xGSfvQNeMIw8H43GbZIvu" +
            "GgyBUbZJMQGS356qh+nbQKFBy8J1/gm2D9udN0dsOaRd7XICLt08xyQeso/Blvwj" +
            "6wOI9S8+l7Rn2FkBtnFSQDsI";
    public static final String UNTRUSTED_HOST_CERT =
            "MIIDhDCCAmygAwIBAgIILHe/qK/q9RgwDQYJKoZIhvcNAQELBQAwPDEYMBYGA1UE" +
            "ChMPVS0wNjMgVGVzdCBPbmx5MSAwHgYDVQQDExdVLTA2MyBUZXN0IFVudHJ1c3Rl" +
            "ZCBDQTAgFw0yMDAxMDEwNjAwMDBaGA8yMTA3MDgxMzA2MDAwMFowNDEYMBYGA1UE" +
            "ChMPVS0wNjMgVGVzdCBPbmx5MRgwFgYDVQQDEw9pbWFwLnJpZ2h0LnRlc3QwggEi" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDmhJGh3SsM849bekDBvFtgRTUL" +
            "iLsb71kylVb34ppUbBNzHt3w/1m6rLm2m6XSANc15c+BM57Sp8axu7ZLWtVMFtLz" +
            "L7Y1xxkwvOXnxoDcZRisRuZql0Ovx6Nn0lS3mQsVl/EQHy2zi/l/SUKg5UFqiWEg" +
            "K8VS4twbRP/yTHSSeIvMEw9XUnf56IPz2w4DlsBnWe8Dhsb5TaBRYNKDRyr66Urb" +
            "6IxA+lU0JA5EvRXXctHTrBQgeizatmObfod7jAWh2j0a5FbkV7UGNwmHX5gTB6PE" +
            "gIShAwn/3sv9nzFj5E+v1P66TpEsg51ORqZhL+Z8hF+0gV67SbEDNL8MiBF3AgMB" +
            "AAGjgY8wgYwwHQYDVR0OBBYEFNLOvpVTVoLdOllgATg4cyyk59DaMA4GA1UdDwEB" +
            "/wQEAwIFoDAaBgNVHREEEzARgg9pbWFwLnJpZ2h0LnRlc3QwCQYDVR0TBAIwADAf" +
            "BgNVHSMEGDAWgBSOzrk/0lfsS6T5VaGJWbRDxYkwODATBgNVHSUEDDAKBggrBgEF" +
            "BQcDATANBgkqhkiG9w0BAQsFAAOCAQEAQWtwwNx4epoLc1PaDnwK4bWH8jPA2Sz7" +
            "1aT/QUorBTLdsy+2711i6/8wnoWlcdLmLl0km0oCedMPO1k9ZXc9Pot3wgid+zEn" +
            "tT+PQVEl+9GNd+B6NXTOcuVGWlwdRANQRVKu/o8SXiX12FRuNEOSWTY2+J8TTfBW" +
            "EUm6SYFeucckJLK4BpF4bwROzITPFPwLUte3Ctnb4Vrz8Opo8V8H2i4oGHsolkaJ" +
            "OGpydD8nNgXr7UurvnbYYJ/lXTCuyAGnTxhBNtQ2VIToS8PzNk51tLn1oEjGPRoJ" +
            "wlBcfkFtkGm/N/zharjuaPLTLFdr2zXeNsoB6Ha9GJoVnEy5TtuTig==";
    public static final String UNTRUSTED_HOST_KEY_PKCS8 =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDmhJGh3SsM849b" +
            "ekDBvFtgRTULiLsb71kylVb34ppUbBNzHt3w/1m6rLm2m6XSANc15c+BM57Sp8ax" +
            "u7ZLWtVMFtLzL7Y1xxkwvOXnxoDcZRisRuZql0Ovx6Nn0lS3mQsVl/EQHy2zi/l/" +
            "SUKg5UFqiWEgK8VS4twbRP/yTHSSeIvMEw9XUnf56IPz2w4DlsBnWe8Dhsb5TaBR" +
            "YNKDRyr66Urb6IxA+lU0JA5EvRXXctHTrBQgeizatmObfod7jAWh2j0a5FbkV7UG" +
            "NwmHX5gTB6PEgIShAwn/3sv9nzFj5E+v1P66TpEsg51ORqZhL+Z8hF+0gV67SbED" +
            "NL8MiBF3AgMBAAECggEAWWDMYKCy+P4UtwjTQ+rm2RBsbVDyD3ZBtPmvc0SSxyny" +
            "bp/ouPEhFt6y0SlPZXn698RnrPZZXd88jJw2JciNzEdLOoxkzzwnubx8Pd47B3lR" +
            "VDbXTzvgPC8hXfdw7pDx+cjkaHZWT3f14JMEuQK61vZ2SLV/+E/KvO4HeSiIyGMl" +
            "lw7OnziZKQYv00kS97cXp2LJ0MmUTfrxdwC3qN6ctC+rG/s+DlUQSMUpY9sZtsuG" +
            "ZLhg5VwD8kDCUSCZha5IqenOzppUgCkZ0ZL3f14IRkZx4gxZkmRK+yENwMcOm8xC" +
            "98PgXZbmVyI3IucsvmLrcHcUNYSXw5xOauen+pAgdQKBgQD2VABb8RfE5rTcVTOB" +
            "T4xEplXP8V4s6E0BNe+89dXPiuoc+64d5rGOyp43ehvhU+eJBqVwKJHAlpoFsQcY" +
            "MmgCt2LNsyZ0tk5253xbfp9PDMwcIhBFJjOZpDeluoCxQXW/IwIoFT9tL2dO7MQO" +
            "jDBXCcsUT02JxiUDZ9egel91+wKBgQDvkaX9QC97kBfBNuhTm5WVmwU6QFoBsWHT" +
            "nIPunQfM0ZxTEdjWmT01UJa6OZIu6IyjM96Xx+Ifi6Nit7JRsFYk4BMtAniBACU4" +
            "T26PhmainUxwOzolcxrIa7D5Lh7f2sCqK2JE1Ks95nJ/GbC8+z1en3Rpstv6jcmZ" +
            "QlgUCSdFtQKBgDxnlvr04TZIuNs7DXlNT4FaJLrsXoK2qIN+fX6h7j+JuiVHVohA" +
            "lRQF/cxu5j1YPPPuLE2XtT478CMrKSk22yinXdRBdVGzjwQ7edRZ9akF41uWwtSi" +
            "LI8lpPvAA2T0P3KSnCnGJBqbQEMrblx+5i/B2Anv9HyvmU3/ffH9cEFXAoGAcCV3" +
            "VakxUR9v71PbyB/CE2mhNdagC8DUsbHIFPBPkXLiQgTNxA+yFn4mQQDoTKsyAsgq" +
            "QFUipm/AyVqC0DjC6ro2X9CTRpgh1E1izS82ZpNKEBO5sYRDmmTHUK5lZ5dIr6Bl" +
            "fSEtFGoHWTZWhPD7losAfOZskcrD/8mN37s1Ni0CgYBHRmIqQpwJQJVE2FUIx/yv" +
            "A4qEd+nkyNCoxZjDasCwmWwHzra6ELFoRyyCmNIco3tfBmk/SCdjd/CMSHSIjhdA" +
            "F5OYJsqyDI6rm8LSxFj/u4u/iD1skEPMJwXBOiMXN8v20esrPiFeBGmbBhkQwajH" +
            "EhhkKU74LsSadJfH1ftgqw==";
    public static final String EXPIRED_HOST_CERT =
            "MIIDgTCCAmmgAwIBAgIJAOv3/QrZrkxKMA0GCSqGSIb3DQEBCwUAMDoxGDAWBgNV" +
            "BAoTD1UtMDYzIFRlc3QgT25seTEeMBwGA1UEAxMVVS0wNjMgVGVzdCBUcnVzdGVk" +
            "IENBMB4XDTIwMDEwMTA2MDAwMFoXDTIwMDEzMTA2MDAwMFowNDEYMBYGA1UEChMP" +
            "VS0wNjMgVGVzdCBPbmx5MRgwFgYDVQQDEw9pbWFwLnJpZ2h0LnRlc3QwggEiMA0G" +
            "CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDPHhsRHkrEcDxqgTMcxf274KSmN8Rb" +
            "DsUyPEtifgLSSyNXyscSXLcvFXEgZsSZbmJZmmwqVoz3ZiNEwkiMOE5s8oT08KPV" +
            "D6SVLygRUU8G8hHpFJl5SkF9PT3Lc7GwHZ6YSRqoHGzM0xoHQ2zRdVhYSgg8iH/e" +
            "oCulIQcu50RCtUHB3lCxD3GWJfus5oWKjiqqlSFFAlLDhs0xKmrFB2WJTfIXPlRC" +
            "0ZEwujHYx9N4YUj+LtATSJxuItn1bSPBX/AttLtab7UmKujRTQXhH6kbotk6P204" +
            "2eItiet/zA8Fr2SwJAHpq4kKF+c2p5GRmaxZSJFiVnZe8Hin8tnH3ycLAgMBAAGj" +
            "gY8wgYwwHQYDVR0OBBYEFHskwoptC9yV6RztoRc+VGAWqVEmMA4GA1UdDwEB/wQE" +
            "AwIFoDAaBgNVHREEEzARgg9pbWFwLnJpZ2h0LnRlc3QwCQYDVR0TBAIwADAfBgNV" +
            "HSMEGDAWgBSULh0vUhpqIgP8A+phWeqcIBhwszATBgNVHSUEDDAKBggrBgEFBQcD" +
            "ATANBgkqhkiG9w0BAQsFAAOCAQEAiFnv4BjoKQ/XzPq1aYVfyOA1Opxhg8vElH+R" +
            "b2wYexd29ulxoWmuJzXmph7mrF/7U6lERbQ4c9DiWecCSJJDd57/ZOMHFmh0be2v" +
            "dOSdQ36cxEY26IzAnII8uTnfPquIAXiBLb5kbenF9emfOeRzeBDulPP++P8S8wxI" +
            "/mj7v+JZFEoo67tmdFBYvVzi+jQ+VwS9oTazsaKNTdt13cYASNHehFmpEmu2AwiT" +
            "6nf5GCYb6xuE0BfhUPM0/mB+H9QWedhcmh0u1vhmAi88FPy7CU7VvrJJS+fCmzPC" +
            "4FfUhRVl568tLrLXvT9gmZLqUitpPWIJNaYXLyup3JKEp8VVCQ==";
    public static final String EXPIRED_HOST_KEY_PKCS8 =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDPHhsRHkrEcDxq" +
            "gTMcxf274KSmN8RbDsUyPEtifgLSSyNXyscSXLcvFXEgZsSZbmJZmmwqVoz3ZiNE" +
            "wkiMOE5s8oT08KPVD6SVLygRUU8G8hHpFJl5SkF9PT3Lc7GwHZ6YSRqoHGzM0xoH" +
            "Q2zRdVhYSgg8iH/eoCulIQcu50RCtUHB3lCxD3GWJfus5oWKjiqqlSFFAlLDhs0x" +
            "KmrFB2WJTfIXPlRC0ZEwujHYx9N4YUj+LtATSJxuItn1bSPBX/AttLtab7UmKujR" +
            "TQXhH6kbotk6P2042eItiet/zA8Fr2SwJAHpq4kKF+c2p5GRmaxZSJFiVnZe8Hin" +
            "8tnH3ycLAgMBAAECggEANL7kAb4HKHO0zyLME33QmnIhPDtjhBUE30A/qnuQ2GFO" +
            "RONdQy3dcN4zCx9g3fogjdUnyrhz+eVHAhHATAVi45f3iDFoZ7AuJ9f0ikR8rbYk" +
            "HYmjdCIW/4ADJrb1/3Xrl7i2Uy25/4ebT1I6ejU6JHHmGxRgDCAaPVll7On0llAB" +
            "X6cBkUm3PwMyFRPGkhp43D++vMPKgl+i9cK+o3nzF8QznoyEGOGD4ZbcEEXcBkYF" +
            "W4Y1ShWq9Agqj5V0VmX0pcYVIapUhY7UUlZ47OVM4D1kosEuq3RAdwCFEp7b+5c4" +
            "dlMJ8p69DYy020cMmLTNoPtY4tvZ9gdZudpfirOQcQKBgQDbuTKH2JiCy9nZD34g" +
            "zYSyqaojKoSFH1q//R95TBsG/lO3B3J+N2tVhCiuKL5i76x/QH4YDeko0XsYgJky" +
            "YxwNaqjJnmub59Szk9vS9BHDnSanyHvlHtCXgBkdN7fweY2d3Bmjcsp+CxUaKWus" +
            "Wkp+Ykffx1+x2QzgOA76qiv05QKBgQDxUBywJWIIlBWTGnr1YfDZ2jG3/6QRlqQp" +
            "lrxAH/tFBiA/vmhuu8SFzbgM0864Gf31XuemIbwE31R9NCkSp7OYd1rsfhZZPNAW" +
            "+Xr3LUIFMMap4VbKUt3iilS8Mf397kexVKV3zxCopB5Xvi4++xl1efK1Ad/HKb7o" +
            "0rrlcx5dLwKBgHQsUE5JNCH73FwK9/oqD95THLTpNxYXxOi70ddVaPjioaqlUmDv" +
            "tl2JueSTgE5ZQNArbkATotLFJ5uF6vocMunZGhM4ItdU1T55kAHSsE3IGncd5nv3" +
            "NfzUavLq0eNgsYLkq92ixPOtsmOZwOdo+fgmyK/F/4/OHiOzICBit0kNAoGAGpD2" +
            "R5ImA8nBwkslBES/DkzOMnKLboBGHGoC2ZBrykKNZFcjCiXT0eM/AvKCjJQX+F51" +
            "ZD3MhEDN/WazquEZjYx+TxDmhvaB9jWLCxFJdmbTSDqNooKS+PC/uWRLkRwdenFW" +
            "MiVysQ44ZPKEBt0ABYXiaXkGWNfIs7sXZlFRI3MCgYB5FSaoxynUHO8/qD55lCuk" +
            "xSRinS/B0Fby1ldDeDvBe1JQw5i4y98M11JOQSifyl5tP9a02k4AKwg/h713mEuI" +
            "hC5vw1T5BZoQ27SWR812EprdUi3htu7AwFqgnG1ozTyxUzk0pzNB9ICe+yjDTHgh" +
            "zhHIR5HBf28LeS6p+CLrpQ==";

    /** Host name on the {@code right-host}, {@code untrusted-host} and {@code expired-host} certificates. */
    public static final String RIGHT_HOST_NAME = "imap.right.test";

    /** Host name on the {@code other-host} certificate. */
    public static final String OTHER_HOST_NAME = "imap.other.test";

    /** Test-only literal. Protects nothing; the keys above are throwaway and published in this file. */
    public static final char[] TEST_ONLY_KEY_PASSWORD = "u063-test-only".toCharArray();

    /** Decodes a Base64 DER certificate constant. */
    public static X509Certificate certificate(String b64) throws GeneralSecurityException, IOException {
        try (InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(b64))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    /** Decodes a Base64 PKCS#8 RSA private key constant. */
    public static PrivateKey privateKey(String b64) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64)));
    }

    /**
     * Builds a server-side TLS context that presents the chain [leaf, issuer] with the leaf's private key.
     * The key store is in memory and of the default type; the key manager uses the default algorithm.
     */
    public static SSLContext serverContext(String leafCertB64, String keyB64, String issuerCertB64)
            throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setKeyEntry("server", privateKey(keyB64), TEST_ONLY_KEY_PASSWORD,
                new Certificate[] {certificate(leafCertB64), certificate(issuerCertB64)});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, TEST_ONLY_KEY_PASSWORD);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * Returns a validating trust manager whose only anchors are the given certificates. The algorithm name
     * {@code X509} is the one the production trust-manager class asks for.
     */
    public static X509TrustManager trustManagerFor(String... anchorCertB64)
            throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        int i = 0;
        for (String anchor : anchorCertB64) {
            ks.setCertificateEntry("anchor" + (i++), certificate(anchor));
        }
        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        throw new IllegalStateException("no X509TrustManager for the test anchors");
    }

    /** Decodes several certificate constants into a chain array, in the order given. */
    public static X509Certificate[] chain(String... certB64) throws GeneralSecurityException, IOException {
        X509Certificate[] out = new X509Certificate[certB64.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = certificate(certB64[i]);
        }
        return out;
    }
}
