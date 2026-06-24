---
artifact_type: plan
story_id: U-058
verdict: PASS
---
# U-058 Plan — Refresh vendored k-9 deps + drop org.apache.http.legacy (BT-003)
## Root cause
The `:k9mail-vendored` module pinned 2015-vintage deps (commons-io 2.4, apache-mime4j 0.7.2) and `useLibrary 'org.apache.http.legacy'` (for the dead webdav package) — the largest unpatched-dependency surface (assessment BT-003).
## Approach
1. commons-io 2.4 → 2.16.1 (API-compatible).
2. apache-mime4j 0.7.2 → 0.8.11; adapt the 0.8 API breaks in the vendored sources (Address→LenientAddressParser, EncoderUtil charsets→StandardCharsets, MimeConfig immutable builder, QuotedPrintableInputStream.close signature).
3. Drop `org.apache.http.legacy`: the webdav package is dead in this IMAP-only fork (grep-confirmed no app reference) — exclude webdav from the source set and replace the one legacy hostname-verifier use (StrictHostnameVerifier) with HttpsURLConnection.getDefaultHostnameVerifier() + a MinimalSslSession shim (equivalent RFC 2818/6125 verification).
4. Update verification-metadata for the new dep checksums (no wildcard). Preserve the MailTransport ACL.
## Verdict
PASS (Tier A) — all three done; clean build green, 697 tests, ACL intact, hostname verification confirmed equivalent in security review.
