package com.vadimtoptunov.generators.web

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class JwtGeneratorTest {

    @Test
    fun `generate HS256 produces valid JWT structure`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        val parts = record.token.split(".")
        assertEquals("JWT should have 3 parts", 3, parts.size)
        assertTrue("Signature should not be empty", parts[2].isNotEmpty())
        assertTrue("Record should be signed", record.isSigned)
    }

    @Test
    fun `HS256 header is correct`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        assertEquals("HS256", record.header.alg)
        assertEquals("JWT", record.header.typ)
    }

    @Test
    fun `payload contains required claims`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678"),
            expiryMinutes = 60,
            issuer = "test-issuer"
        )
        val record = generator.generate()

        assertNotNull(record.payload.sub)
        assertEquals("test-issuer", record.payload.iss)
        assertEquals("test-audience", record.payload.aud)
        assertNotNull(record.payload.exp)
        assertNotNull(record.payload.iat)
        assertNotNull(record.payload.nbf)
        assertNotNull(record.payload.jti)
    }

    @Test
    fun `expiry is calculated correctly`() {
        val before = System.currentTimeMillis() / 1000
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678"),
            expiryMinutes = 60
        )
        val record = generator.generate()
        val after = System.currentTimeMillis() / 1000

        val iat = record.payload.iat!!
        val exp = record.payload.exp!!

        assertTrue("iat should be recent", iat in before..after)
        assertEquals("exp should be iat + 60 minutes", iat + 3600, exp)
    }

    @Test
    fun `custom claims are included`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678"),
            customClaims = mapOf("role" to "admin", "department" to "engineering")
        )
        val record = generator.generate()

        assertEquals("admin", record.payload.customClaims["role"])
        assertEquals("engineering", record.payload.customClaims["department"])
        assertTrue(record.payloadJson.contains("\"role\":\"admin\""))
    }

    @Test
    fun `none algorithm produces unsigned token`() {
        val generator = JwtGenerator(algorithm = JwtAlgorithm.None)
        val record = generator.generate()

        assertEquals("none", record.header.alg)
        assertTrue("Signature should be empty", record.signature.isEmpty())
        assertFalse("Record should not be signed", record.isSigned)

        val parts = record.token.split(".")
        assertEquals("JWT should have 3 parts", 3, parts.size)
        assertTrue("Last part should be empty", parts[2].isEmpty())
    }

    @Test
    fun `RS256 produces valid signed token`() {
        val generator = JwtGenerator(algorithm = JwtAlgorithm.RS256())
        val record = generator.generate()

        assertEquals("RS256", record.header.alg)
        assertTrue("Signature should not be empty", record.signature.isNotEmpty())
        assertTrue("Record should be signed", record.isSigned)
    }

    @Test
    fun `token parts are valid base64url`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        // Should not throw - parts are valid base64url
        val decoder = Base64.getUrlDecoder()
        assertNotNull(decoder.decode(record.encodedHeader))
        assertNotNull(decoder.decode(record.encodedPayload))
    }

    @Test
    fun `decoded header matches headerJson`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        val decoded = String(Base64.getUrlDecoder().decode(record.encodedHeader))
        assertTrue(decoded.contains("\"alg\":\"HS256\""))
        assertTrue(decoded.contains("\"typ\":\"JWT\""))
    }

    @Test
    fun `decoded payload contains claims`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678"),
            issuer = "my-issuer"
        )
        val record = generator.generate()

        val decoded = String(Base64.getUrlDecoder().decode(record.encodedPayload))
        assertTrue(decoded.contains("\"iss\":\"my-issuer\""))
        assertTrue(decoded.contains("\"sub\":"))
        assertTrue(decoded.contains("\"exp\":"))
    }

    @Test
    fun `serialize JSON format`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"token\":"))
        assertTrue(json.contains("\"algorithm\":\"HS256\""))
        assertTrue(json.contains("\"signed\":true"))
    }

    @Test
    fun `serialize TXT format is just the token`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()

        val txt = generator.serialize(record, OutputFormat.TXT)
        assertEquals(record.token, txt)
    }

    @Test
    fun `subject has expected format`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        repeat(10) {
            val record = generator.generate()
            val sub = record.payload.sub!!
            assertTrue(
                "Subject should match pattern",
                sub.matches(Regex("(user|admin|service|test|api)_\\d{4}"))
            )
        }
    }

    @Test
    fun `jti is 32 characters alphanumeric`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        repeat(10) {
            val record = generator.generate()
            val jti = record.payload.jti!!
            assertEquals(32, jti.length)
            assertTrue(jti.all { it in 'a'..'z' || it in '0'..'9' })
        }
    }

    @Test
    fun `different tokens have different JTIs`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val jtis = (1..10).map { generator.generate().payload.jti }
        assertEquals("All JTIs should be unique", 10, jtis.toSet().size)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `CSV format throws exception`() {
        val generator = JwtGenerator(
            algorithm = JwtAlgorithm.HS256("test-secret-key-12345678")
        )
        val record = generator.generate()
        generator.serialize(record, OutputFormat.CSV)
    }
}
