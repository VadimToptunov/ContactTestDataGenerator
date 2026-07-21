package com.vadimtoptunov.generators.web

import kotlinx.serialization.Serializable

/**
 * JWT signing algorithms supported by the generator.
 *
 * IMPORTANT: The "none" algorithm is included ONLY for security testing.
 * Never use "none" in production - it disables signature verification entirely.
 */
@Serializable
sealed class JwtAlgorithm {
    abstract val algName: String

    /**
     * HMAC with SHA-256 (symmetric key).
     * Most common algorithm for simple JWT implementations.
     */
    @Serializable
    data class HS256(val secret: String) : JwtAlgorithm() {
        override val algName = "HS256"
    }

    /**
     * RSA with SHA-256 (asymmetric key pair).
     * Used when tokens need to be verified by third parties
     * without sharing the signing key.
     *
     * @param privateKeyPem PEM-encoded private key for signing (null = generate ephemeral)
     * @param publicKeyPem PEM-encoded public key for verification
     */
    @Serializable
    data class RS256(
        val privateKeyPem: String? = null,
        val publicKeyPem: String? = null
    ) : JwtAlgorithm() {
        override val algName = "RS256"
    }

    /**
     * No signature (INSECURE - for testing only).
     *
     * Use cases:
     *   - Security testing: Check if APIs reject unsigned tokens
     *   - Bug bounty: Test for JWT algorithm confusion vulnerabilities
     *
     * NEVER use in production. Any API that accepts "none" algorithm
     * has a critical security vulnerability.
     */
    @Serializable
    data object None : JwtAlgorithm() {
        override val algName = "none"
    }
}
