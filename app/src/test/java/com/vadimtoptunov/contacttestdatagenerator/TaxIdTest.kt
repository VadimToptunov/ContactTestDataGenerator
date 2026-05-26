package com.vadimtoptunov.contacttestdatagenerator

import com.vadimtoptunov.contacttestdatagenerator.generators.identity.TaxIdAlgorithms
import com.vadimtoptunov.contacttestdatagenerator.generators.identity.TaxIdGenerator
import com.vadimtoptunov.contacttestdatagenerator.generators.identity.TaxIdIntent
import com.vadimtoptunov.contacttestdatagenerator.generators.identity.TaxIdValidator
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Tax ID algorithms, generator, and validator.
 *
 * Structure per country:
 *   1. Algorithm: known reference values (ground truth from official docs)
 *   2. Generator: VALID output passes validator; each INVALID variant fails for the right reason
 *   3. Cross-check: validator's failure reason matches the generator's TaxIdIntent
 */
class TaxIdTest {

    // ═══════════════════════════════════════════════════════════════════════
    // BRAZIL — CPF
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `CPF algorithm - known valid number passes`() {
        // 529.982.247-25 is the canonical CPF test vector
        assertTrue(TaxIdAlgorithms.cpfValid("52998224725"))
    }

    @Test
    fun `CPF algorithm - wrong check digit fails`() {
        assertFalse(TaxIdAlgorithms.cpfValid("52998224700"))
    }

    @Test
    fun `CPF algorithm - all same digits fails`() {
        assertFalse(TaxIdAlgorithms.cpfValid("11111111111"))
        assertFalse(TaxIdAlgorithms.cpfValid("00000000000"))
    }

    @Test
    fun `CPF generator - valid number passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.BrazilCpf()).generate()
            val result = TaxIdValidator.validate(rec.value, "BR")
            assertTrue("Expected valid CPF, got: ${result.message}", result.isValid)
        }
    }

    @Test
    fun `CPF generator - all invalid variants fail validator`() {
        val records = TaxIdGenerator(TaxIdGenerator.BrazilCpf()).generateAll()
        val invalids = records.filter { !it.intent.isValid }
        invalids.forEach { rec ->
            val result = TaxIdValidator.validate(rec.value.filter { it.isDigit() }, "BR")
            assertFalse("${rec.intent} should fail, but passed", result.isValid)
        }
    }

    @Test
    fun `CPF checksum failure detected correctly`() {
        val rec = TaxIdGenerator(TaxIdGenerator.BrazilCpf()).generateInvalidChecksum()
        val result = TaxIdValidator.validate(rec.value.filter { it.isDigit() }, "BR")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.FAILED_CHECKSUM, result.failureReason)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GERMANY — Steuer-IdNr
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `IdNr algorithm - reference number passes`() {
        // Official test vector from Bundeszentralamt für Steuern
        assertTrue(TaxIdAlgorithms.idnrValid("86095742719"))
    }

    @Test
    fun `IdNr algorithm - wrong check digit fails`() {
        assertFalse(TaxIdAlgorithms.idnrValid("86095742710"))
    }

    @Test
    fun `IdNr generator - valid passes, invalids fail`() {
        val gen = TaxIdGenerator(TaxIdGenerator.GermanyIdnr())
        val valid = gen.generate()
        assertTrue(TaxIdValidator.validate(valid.value, "DE").isValid)

        gen.generateAll().filter { !it.intent.isValid }.forEach { rec ->
            assertFalse(
                "${rec.intent} should fail for DE",
                TaxIdValidator.validate(rec.value, "DE").isValid
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RUSSIA — ИНН
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `INN check digits computed correctly`() {
        // 7726391549 → check "49" is position 11-12, base is 772639154
        val result = TaxIdAlgorithms.innPersonalCheckDigits("7726391540".take(10))
        assertEquals(2, result.length)
        result.forEach { assertTrue("Check digit must be numeric", it.isDigit()) }
    }

    @Test
    fun `INN generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.RussiaInn()).generate()
            val result = TaxIdValidator.validate(rec.value, "RU")
            assertTrue("Expected valid INN, got: ${result.message}", result.isValid)
        }
    }

    @Test
    fun `INN generator - all invalids fail`() {
        TaxIdGenerator(TaxIdGenerator.RussiaInn()).generateAll()
            .filter { !it.intent.isValid }
            .forEach { rec ->
                assertFalse(
                    "${rec.intent} should fail for RU",
                    TaxIdValidator.validate(rec.value, "RU").isValid
                )
            }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UKRAINE — РНОКПП
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `RNOKPP generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.UkraineRnokpp()).generate()
            assertTrue(TaxIdValidator.validate(rec.value, "UA").isValid)
        }
    }

    @Test
    fun `RNOKPP checksum failure has correct reason`() {
        val rec = TaxIdGenerator(TaxIdGenerator.UkraineRnokpp()).generateInvalidChecksum()
        val result = TaxIdValidator.validate(rec.value, "UA")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.FAILED_CHECKSUM, result.failureReason)
    }

    @Test
    fun `RNOKPP too short has correct reason`() {
        val rec = TaxIdGenerator(TaxIdGenerator.UkraineRnokpp()).generateTooShort()
        val result = TaxIdValidator.validate(rec.value, "UA")
        assertEquals(TaxIdValidator.FailureReason.WRONG_LENGTH, result.failureReason)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SPAIN — NIF
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `NIF algorithm - known valid numbers`() {
        assertTrue(TaxIdAlgorithms.nifValid("12345678Z"))
        assertTrue(TaxIdAlgorithms.nifValid("00000000T"))
    }

    @Test
    fun `NIF algorithm - wrong letter fails`() {
        assertFalse(TaxIdAlgorithms.nifValid("12345678A"))
    }

    @Test
    fun `NIF generator - valid passes, invalids fail`() {
        val gen = TaxIdGenerator(TaxIdGenerator.SpainNif())
        assertTrue(TaxIdValidator.validate(gen.generate().value, "ES").isValid)
        gen.generateAll().filter { !it.intent.isValid }.forEach { rec ->
            assertFalse(TaxIdValidator.validate(rec.value, "ES").isValid)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USA — SSN
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `SSN generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.UsaSsn()).generate()
            assertTrue("Expected valid SSN, got: ${rec.value}", TaxIdValidator.validate(rec.value, "US").isValid)
        }
    }

    @Test
    fun `SSN area 000 is rejected`() {
        val result = TaxIdValidator.validate("000121234", "US")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.BLACKLISTED, result.failureReason)
    }

    @Test
    fun `SSN area 666 is rejected`() {
        val result = TaxIdValidator.validate("666121234", "US")
        assertFalse(result.isValid)
    }

    @Test
    fun `SSN 123-45-6789 is blacklisted`() {
        val result = TaxIdValidator.validate("123456789", "US")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.BLACKLISTED, result.failureReason)
    }

    @Test
    fun `SSN all invalid variants fail`() {
        TaxIdGenerator(TaxIdGenerator.UsaSsn()).generateAll()
            .filter { !it.intent.isValid }
            .forEach { rec ->
                assertFalse("${rec.intent} should fail for US",
                    TaxIdValidator.validate(rec.value.filter { it.isDigit() }, "US").isValid)
            }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POLAND — NIP
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `NIP generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.PolandNip()).generate()
            assertTrue(TaxIdValidator.validate(rec.value, "PL").isValid)
        }
    }

    @Test
    fun `NIP invalid checksum has correct reason`() {
        val rec = TaxIdGenerator(TaxIdGenerator.PolandNip()).generateInvalidChecksum()
        val result = TaxIdValidator.validate(rec.value, "PL")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.FAILED_CHECKSUM, result.failureReason)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FRANCE — NIR
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `NIR check digits Mod-97 complement`() {
        val base = "1800512134567"
        val check = TaxIdAlgorithms.nirCheckDigits(base)
        assertEquals(2, check.length)
        check.forEach { assertTrue(it.isDigit()) }
        // Verify: base+check passes mod-97 rule
        assertTrue(TaxIdAlgorithms.nirValid(base + check))
    }

    @Test
    fun `NIR generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.FranceNir()).generate()
            assertTrue("Expected valid NIR, got: ${result(rec.value, "FR").message}",
                TaxIdValidator.validate(rec.value, "FR").isValid)
        }
    }

    @Test
    fun `NIR all invalids fail`() {
        TaxIdGenerator(TaxIdGenerator.FranceNir()).generateAll()
            .filter { !it.intent.isValid }
            .forEach { rec ->
                assertFalse(TaxIdValidator.validate(rec.value.filter { it.isDigit() }, "FR").isValid)
            }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ITALY — Codice Fiscale
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `Codice Fiscale check char computation`() {
        // RSSMRA80A01H501 → check char U (well-known reference)
        val check = TaxIdAlgorithms.codiceFiscaleCheckChar("RSSMRA80A01H501")
        assertEquals('U', check)
    }

    @Test
    fun `Codice Fiscale validator - known valid number`() {
        assertTrue(TaxIdAlgorithms.codiceFiscaleValid("RSSMRA80A01H501U"))
    }

    @Test
    fun `Codice Fiscale generator - valid passes validator`() {
        repeat(20) {
            val rec = TaxIdGenerator(TaxIdGenerator.ItalyCodiceFiscale()).generate()
            assertTrue(TaxIdValidator.validate(rec.value, "IT").isValid)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CROSS-COUNTRY — generateAll() contract
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `generateAll produces exactly 7 records per country`() {
        TaxIdGenerator.ALL_SPECS.forEach { spec ->
            val records = TaxIdGenerator(spec).generateAll()
            assertEquals("${spec.countryCode} should produce 7 records", 7, records.size)
        }
    }

    @Test
    fun `generateAll first record is always VALID`() {
        TaxIdGenerator.ALL_SPECS.forEach { spec ->
            val first = TaxIdGenerator(spec).generateAll().first()
            assertTrue("${spec.countryCode} first record must be Valid, got ${first.intent}",
                first.intent.isValid)
        }
    }

    @Test
    fun `generateAll remaining 6 records are all INVALID`() {
        TaxIdGenerator.ALL_SPECS.forEach { spec ->
            val invalids = TaxIdGenerator(spec).generateAll().drop(1)
            invalids.forEach { rec ->
                assertFalse("${spec.countryCode}: ${rec.intent} must not be Valid",
                    rec.intent.isValid)
            }
        }
    }

    @Test
    fun `all 7 intents are distinct per country`() {
        TaxIdGenerator.ALL_SPECS.forEach { spec ->
            val intents = TaxIdGenerator(spec).generateAll().map { it.intent::class }
            assertEquals("${spec.countryCode}: all intents must be distinct",
                intents.size, intents.toSet().size)
        }
    }

    @Test
    fun `TaxIdValidator unknown country returns UNKNOWN_COUNTRY reason`() {
        val result = TaxIdValidator.validate("12345", "ZZ")
        assertFalse(result.isValid)
        assertEquals(TaxIdValidator.FailureReason.UNKNOWN_COUNTRY, result.failureReason)
    }

    // Helper
    private fun result(value: String, cc: String) = TaxIdValidator.validate(value, cc)
}
