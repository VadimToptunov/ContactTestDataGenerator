package com.vadimtoptunov.generators.security

import org.junit.Assert.*
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun `standard policy generates valid passwords`() {
        val generator = PasswordGenerator(PasswordPolicy.STANDARD)

        repeat(100) {
            val record = generator.generate()

            assertTrue("Length should be at least 12", record.length >= 12)
            assertTrue("Length should be at most 16", record.length <= 16)
            assertTrue("Should have uppercase", record.hasUppercase)
            assertTrue("Should have lowercase", record.hasLowercase)
            assertTrue("Should have digits", record.hasDigits)
            assertTrue("Should have special chars", record.hasSpecial)
        }
    }

    @Test
    fun `weak policy allows shorter passwords`() {
        val generator = PasswordGenerator(PasswordPolicy.WEAK)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Length should be at least 6", record.length >= 6)
            assertTrue("Length should be at most 8", record.length <= 8)
            // Weak policy doesn't require special chars
        }
    }

    @Test
    fun `strong policy generates longer passwords`() {
        val generator = PasswordGenerator(PasswordPolicy.STRONG)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Length should be at least 16", record.length >= 16)
            assertTrue("Length should be at most 24", record.length <= 24)
        }
    }

    @Test
    fun `passphrase policy no special chars`() {
        val generator = PasswordGenerator(PasswordPolicy.PASSPHRASE)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Length should be at least 20", record.length >= 20)
            assertFalse("Should NOT have special chars", record.hasSpecial)
        }
    }

    @Test
    fun `PIN-4 generates 4-digit codes`() {
        val generator = PasswordGenerator(PasswordPolicy.PIN_4)

        repeat(50) {
            val record = generator.generate()

            assertEquals("Length should be 4", 4, record.length)
            assertTrue("Should be all digits", record.password.all { it.isDigit() })
        }
    }

    @Test
    fun `PIN-6 generates 6-digit codes`() {
        val generator = PasswordGenerator(PasswordPolicy.PIN_6)

        repeat(50) {
            val record = generator.generate()

            assertEquals("Length should be 6", 6, record.length)
            assertTrue("Should be all digits", record.password.all { it.isDigit() })
        }
    }

    @Test
    fun `entropy calculation is reasonable`() {
        val standardRecord = PasswordGenerator(PasswordPolicy.STANDARD).generate()
        val weakRecord = PasswordGenerator(PasswordPolicy.WEAK).generate()
        val pinRecord = PasswordGenerator(PasswordPolicy.PIN_4).generate()

        // Standard should have higher entropy than weak
        assertTrue(
            "Standard should have higher entropy than weak",
            standardRecord.entropy > weakRecord.entropy
        )

        // PIN should have low entropy
        assertTrue("PIN-4 should have low entropy", pinRecord.entropy < 20)
    }

    @Test
    fun `password strength categorization`() {
        val pinRecord = PasswordGenerator(PasswordPolicy.PIN_4).generate()
        val weakRecord = PasswordGenerator(PasswordPolicy.WEAK).generate()
        val strongRecord = PasswordGenerator(PasswordPolicy.STRONG).generate()

        // PIN should be very weak or weak
        assertTrue(
            "PIN should be weak category",
            pinRecord.strength in listOf(PasswordStrength.VERY_WEAK, PasswordStrength.WEAK)
        )

        // Strong should be at least REASONABLE
        assertTrue(
            "Strong should be at least REASONABLE",
            strongRecord.strength >= PasswordStrength.REASONABLE
        )
    }

    @Test
    fun `seed produces reproducible passwords`() {
        val seed = 12345L
        val policy = PasswordPolicy.STANDARD

        val password1 = PasswordGenerator(policy, seed).generate().password
        val password2 = PasswordGenerator(policy, seed).generate().password

        assertEquals("Same seed should produce same password", password1, password2)
    }

    @Test
    fun `different seeds produce different passwords`() {
        val policy = PasswordPolicy.STANDARD

        val password1 = PasswordGenerator(policy, 11111L).generate().password
        val password2 = PasswordGenerator(policy, 22222L).generate().password

        assertNotEquals("Different seeds should produce different passwords", password1, password2)
    }

    @Test
    fun `generate with seed parameter works`() {
        val generator = PasswordGenerator(PasswordPolicy.STANDARD)

        val password1 = generator.generate(42L).password
        val password2 = generator.generate(42L).password

        assertEquals("Same seed via parameter should produce same password", password1, password2)
    }

    @Test
    fun `excludeAmbiguous removes confusing characters`() {
        val policy = PasswordPolicy(
            minLength = 50,  // Long enough to likely include all chars
            maxLength = 50,
            excludeAmbiguous = true,
            requireUppercase = true,
            requireLowercase = true,
            requireDigits = true,
            requireSpecial = false
        )
        val generator = PasswordGenerator(policy)

        repeat(20) {
            val password = generator.generate().password

            assertFalse("Should not contain 'O'", password.contains('O'))
            assertFalse("Should not contain '0'", password.contains('0'))
            assertFalse("Should not contain 'l'", password.contains('l'))
            assertFalse("Should not contain '1'", password.contains('1'))
            assertFalse("Should not contain 'I'", password.contains('I'))
        }
    }

    @Test
    fun `masked format hides middle characters`() {
        val generator = PasswordGenerator(PasswordPolicy.STANDARD)
        val record = generator.generate()

        val masked = record.masked

        // Should start with first 2 chars
        assertTrue("Masked should start with first 2 chars", masked.startsWith(record.password.take(2)))
        // Should end with last 2 chars
        assertTrue("Masked should end with last 2 chars", masked.endsWith(record.password.takeLast(2)))
        // Should contain asterisks in the middle
        assertTrue("Masked should contain asterisks", masked.contains('*'))
    }

    @Test
    fun `entropy formatted shows bits`() {
        val record = PasswordGenerator(PasswordPolicy.STANDARD).generate()

        assertTrue("Entropy formatted should contain 'bits'", record.entropyFormatted.contains("bits"))
    }

    @Test
    fun `custom policy respected`() {
        val customPolicy = PasswordPolicy(
            minLength = 20,
            maxLength = 25,
            requireUppercase = true,
            requireLowercase = false,
            requireDigits = true,
            requireSpecial = false
        )
        val generator = PasswordGenerator(customPolicy)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Length >= 20", record.length >= 20)
            assertTrue("Length <= 25", record.length <= 25)
            assertTrue("Has uppercase", record.hasUppercase)
            assertTrue("Has digits", record.hasDigits)
        }
    }

    @Test
    fun `serialization formats work`() {
        val generator = PasswordGenerator(PasswordPolicy.STANDARD)
        val record = generator.generate()

        val csv = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.CSV)
        val json = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.JSON)
        val txt = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.TXT)

        assertTrue("CSV should contain password", csv.contains(record.password))
        assertTrue("JSON should contain password", json.contains(record.password))
        assertTrue("TXT should contain password", txt.contains(record.password))
        assertTrue("JSON should be valid format", json.startsWith("{") && json.endsWith("}"))
    }
}
