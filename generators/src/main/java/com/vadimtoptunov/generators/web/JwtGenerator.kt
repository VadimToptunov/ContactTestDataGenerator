package com.vadimtoptunov.generators.web

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import java.security.KeyPairGenerator
import java.security.Signature
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Generates JWT tokens for authentication testing.
 *
 * Use cases:
 *   - QA: Test JWT validation in APIs and frontends
 *   - Dev: Generate tokens for local development without auth server
 *   - Security: Test for algorithm confusion and signature bypass vulnerabilities
 *
 * IMPORTANT: Tokens generated with "none" algorithm are INSECURE.
 * Use only for security testing to verify APIs reject unsigned tokens.
 *
 * Supports:
 *   - HS256: HMAC-SHA256 (symmetric secret)
 *   - RS256: RSA-SHA256 (asymmetric key pair, ephemeral by default)
 *   - none: No signature (for security testing only)
 */
class JwtGenerator(
    private val algorithm: JwtAlgorithm = JwtAlgorithm.HS256(generateDefaultSecret()),
    private val expiryMinutes: Int = 60,
    private val issuer: String = "devdata-factory",
    private val customClaims: Map<String, String> = emptyMap()
) : DataGenerator<JwtRecord> {

    override val name = "JWT Generator"
    override val description = "Generates ${algorithm.algName} signed JWT tokens for testing"
    override val supportedFormats = listOf(OutputFormat.JSON, OutputFormat.TXT)

    // Lazy-init RSA key pair for RS256 (expensive to generate)
    private val rsaKeyPair by lazy {
        KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()
    }

    override fun generate(): JwtRecord {
        val now = System.currentTimeMillis() / 1000
        val exp = now + (expiryMinutes * 60)
        val jti = generateJti()

        val header = JwtHeader(alg = algorithm.algName)
        val payload = JwtPayload(
            sub = generateSubject(),
            iss = issuer,
            aud = "test-audience",
            exp = exp,
            iat = now,
            nbf = now,
            jti = jti,
            customClaims = customClaims
        )

        val encodedHeader = base64UrlEncode(buildHeaderJson(header))
        val encodedPayload = base64UrlEncode(buildPayloadJson(payload))
        val signingInput = "$encodedHeader.$encodedPayload"

        val signature = when (algorithm) {
            is JwtAlgorithm.HS256 -> signHs256(signingInput, algorithm.secret)
            is JwtAlgorithm.RS256 -> signRs256(signingInput)
            is JwtAlgorithm.None -> ""
        }

        return JwtRecord(
            header = header,
            payload = payload,
            signature = signature,
            encodedHeader = encodedHeader,
            encodedPayload = encodedPayload
        )
    }

    private fun buildHeaderJson(header: JwtHeader): String =
        """{"alg":"${header.alg}","typ":"${header.typ}"}"""

    private fun buildPayloadJson(payload: JwtPayload): String = buildString {
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

    private fun signHs256(input: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val signatureBytes = mac.doFinal(input.toByteArray(Charsets.UTF_8))
        return base64UrlEncode(signatureBytes)
    }

    private fun signRs256(input: String): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(rsaKeyPair.private)
        signature.update(input.toByteArray(Charsets.UTF_8))
        val signatureBytes = signature.sign()
        return base64UrlEncode(signatureBytes)
    }

    private fun base64UrlEncode(data: String): String =
        base64UrlEncode(data.toByteArray(Charsets.UTF_8))

    private fun base64UrlEncode(data: ByteArray): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    private fun generateSubject(): String {
        val prefixes = listOf("user", "admin", "service", "test", "api")
        val id = Random.nextInt(1000, 9999)
        return "${prefixes.random()}_$id"
    }

    private fun generateJti(): String {
        val chars = ('a'..'z') + ('0'..'9')
        return (1..32).map { chars.random() }.joinToString("")
    }

    override fun serialize(record: JwtRecord, format: OutputFormat): String = when (format) {
        OutputFormat.JSON -> buildString {
            append("{")
            append("\"token\":\"${record.token}\",")
            append("\"algorithm\":\"${record.header.alg}\",")
            append("\"header\":${record.headerJson},")
            append("\"payload\":${record.payloadJson},")
            append("\"signed\":${record.isSigned}")
            append("}")
        }
        OutputFormat.TXT -> record.token
        else -> throw UnsupportedOperationException("$format not supported by JwtGenerator")
    }

    override fun serializeBatch(records: List<JwtRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.JSON -> buildString {
                appendLine("[")
                records.forEachIndexed { i, r ->
                    append("  ${serialize(r, format)}")
                    if (i < records.lastIndex) appendLine(",") else appendLine()
                }
                append("]")
            }
            else -> super.serializeBatch(records, format)
        }

    companion object {
        const val ID = "jwt_generator"

        private fun generateDefaultSecret(): String {
            val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..32).map { chars.random() }.joinToString("")
        }

        fun registerAll() {
            // HS256 with random secret
            GeneratorRegistry.register(
                "${ID}_hs256",
                JwtGenerator(algorithm = JwtAlgorithm.HS256(generateDefaultSecret()))
            )

            // RS256 with ephemeral key pair
            GeneratorRegistry.register(
                "${ID}_rs256",
                JwtGenerator(algorithm = JwtAlgorithm.RS256())
            )

            // None algorithm (for security testing)
            GeneratorRegistry.register(
                "${ID}_none",
                JwtGenerator(algorithm = JwtAlgorithm.None)
            )

            // Short-lived tokens (5 minutes)
            GeneratorRegistry.register(
                "${ID}_short",
                JwtGenerator(
                    algorithm = JwtAlgorithm.HS256(generateDefaultSecret()),
                    expiryMinutes = 5
                )
            )

            // Long-lived tokens (30 days)
            GeneratorRegistry.register(
                "${ID}_long",
                JwtGenerator(
                    algorithm = JwtAlgorithm.HS256(generateDefaultSecret()),
                    expiryMinutes = 60 * 24 * 30
                )
            )
        }
    }
}
