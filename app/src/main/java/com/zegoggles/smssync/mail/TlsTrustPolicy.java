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
package com.zegoggles.smssync.mail;

/**
 * Two-state TLS trust policy for IMAP connections.
 *
 * <p>SYSTEM_VALIDATED is the default for every host that has no user-enrolled certificate.
 * PINNED_CERTIFICATE is only reachable through an explicit, user-initiated enrollment flow.
 * There is no trust-all / accept-any state — adding such a constant is a breaking violation
 * of CNTR-MODERNIZATION-001 and re-opens CWE-295.
 *
 * @see PinnedCertStore
 * @see PinnedCertificateSocketFactory
 */
public enum TlsTrustPolicy {
    /**
     * Full Android platform CA chain validation against the system CA store.
     * The factory produced for this policy is {@code DefaultTrustedSocketFactory}.
     * This is the unconditional default for any host with no enrolled certificate.
     */
    SYSTEM_VALIDATED,

    /**
     * Validates ONLY the user-enrolled certificate for a specific host:port pair.
     * The factory produced for this policy is {@code PinnedCertificateSocketFactory}.
     * This policy is only set by the user-initiated pinned-certificate enrollment flow.
     */
    PINNED_CERTIFICATE
}
