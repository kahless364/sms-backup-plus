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
package com.zegoggles.smssync.mail.transport;

import androidx.annotation.Nullable;
import com.zegoggles.smssync.mail.TlsTrustPolicy;

import java.security.cert.X509Certificate;

/**
 * App-owned configuration value object passed to the {@link MailTransport} adapter.
 *
 * <p>No {@code com.fsck.k9.*} type appears in any field or constructor. The adapter
 * maps {@link #tlsPolicy} to a resolved {@code TrustedSocketFactory} internally.
 *
 * <p>Per CNTR-MODERNIZATION-007 §App-owned value types.
 */
public final class MailTransportConfig {

    /** The IMAP store URI (e.g. {@code imap+ssl+://user:token@imap.gmail.com:993}). */
    public final String storeUri;

    /**
     * TLS trust policy. The adapter maps this to a {@code TrustedSocketFactory}:
     * {@link TlsTrustPolicy#SYSTEM_VALIDATED} → {@code DefaultTrustedSocketFactory};
     * {@link TlsTrustPolicy#PINNED_CERTIFICATE} → {@code PinnedCertificateSocketFactory}.
     */
    public final TlsTrustPolicy tlsPolicy;

    /**
     * The enrolled certificate for pinned-certificate connections.
     * Must be non-null when {@link #tlsPolicy} is {@link TlsTrustPolicy#PINNED_CERTIFICATE};
     * should be {@code null} for {@link TlsTrustPolicy#SYSTEM_VALIDATED}.
     */
    @Nullable
    public final X509Certificate pinnedCert;

    /**
     * Constructs a {@code MailTransportConfig}.
     *
     * @param storeUri   the IMAP store URI
     * @param tlsPolicy  the TLS trust policy
     * @param pinnedCert the enrolled X.509 certificate, or {@code null} for {@code SYSTEM_VALIDATED}
     */
    public MailTransportConfig(String storeUri, TlsTrustPolicy tlsPolicy,
                               @Nullable X509Certificate pinnedCert) {
        this.storeUri = storeUri;
        this.tlsPolicy = tlsPolicy;
        this.pinnedCert = pinnedCert;
    }

    /**
     * Convenience constructor for {@link TlsTrustPolicy#SYSTEM_VALIDATED} (no pinned cert).
     *
     * @param storeUri  the IMAP store URI
     * @param tlsPolicy must be {@link TlsTrustPolicy#SYSTEM_VALIDATED}
     */
    public MailTransportConfig(String storeUri, TlsTrustPolicy tlsPolicy) {
        this(storeUri, tlsPolicy, null);
    }
}
