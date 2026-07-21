package com.vadimtoptunov.generators.web

import kotlinx.serialization.Serializable

/**
 * JWT header as per RFC 7519.
 */
@Serializable
data class JwtHeader(
    val alg: String,
    val typ: String = "JWT"
)

/**
 * JWT payload (claims) as per RFC 7519.
 *
 * Standard registered claims:
 *   - sub: Subject (who the token is about)
 *   - iss: Issuer (who created the token)
 *   - aud: Audience (who the token is intended for)
 *   - exp: Expiration time (Unix timestamp)
 *   - iat: Issued at (Unix timestamp)
 *   - nbf: Not before (Unix timestamp)
 *   - jti: JWT ID (unique identifier)
 */
@Serializable
data class JwtPayload(
    val sub: String? = null,
    val iss: String? = null,
    val aud: String? = null,
    val exp: Long? = null,
    val iat: Long? = null,
    val nbf: Long? = null,
    val jti: String? = null,
    val customClaims: Map<String, String> = emptyMap()
)

/**
 * A single generated JWT record.
 *
 * Contains the header, payload, signature, and the complete encoded token.
 * Use for testing JWT validation, authentication flows, and security testing.
 */
@Serializable
data class JwtRecord(
    val header: JwtHeader,
    val payload: JwtPayload,
    val signature: String,
    val encodedHeader: String,
    val encodedPayload: String
) {
    /**
     * The complete JWT token in standard format: header.payload.signature
     */
    val token: String
        get() = "$encodedHeader.$encodedPayload.$signature"

    /**
     * Decoded header as JSON string (for display/debugging)
     */
    val headerJson: String
        get() = buildString {
            append("{\"alg\":\"${header.alg}\",\"typ\":\"${header.typ}\"}")
        }

    /**
     * Decoded payload as JSON string (for display/debugging)
     */
    val payloadJson: String
        get() = buildString {
            append("{")
            val parts = mutableListOf<String>()
            payload.sub?.let { parts.add("\"sub\":\"$it\"") }
            payload.iss?.let { parts.add("\"iss\":\"$it\"") }
            payload.aud?.let { parts.add("\"aud\":\"$it\"") }
            payload.exp?.let { parts.add("\"exp\":$it") }
            payload.iat?.let { parts.add("\"iat\":$it") }
            payload.nbf?.let { parts.add("\"nbf\":$it") }
            payload.jti?.let { parts.add("\"jti\":\"$it\"") }
            payload.customClaims.forEach { (k, v) -> parts.add("\"$k\":\"$v\"") }
            append(parts.joinToString(","))
            append("}")
        }

    /**
     * Whether this token has a valid signature (false for "none" algorithm)
     */
    val isSigned: Boolean
        get() = header.alg != "none" && signature.isNotEmpty()
}
