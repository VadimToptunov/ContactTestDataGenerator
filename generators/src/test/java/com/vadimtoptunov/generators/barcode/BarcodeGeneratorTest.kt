package com.vadimtoptunov.generators.barcode

import org.junit.Assert.*
import org.junit.Test

class BarcodeGeneratorTest {

    @Test
    fun `EAN-13 has valid length`() {
        val generator = BarcodeGenerator(BarcodeType.EAN_13)

        repeat(100) {
            val record = generator.generate()

            assertEquals("EAN-13 should be 13 digits", 13, record.digits.length)
            assertTrue("Should be all digits", record.digits.all { it.isDigit() })
        }
    }

    @Test
    fun `EAN-13 has valid check digit`() {
        val generator = BarcodeGenerator(BarcodeType.EAN_13)

        repeat(100) {
            val record = generator.generate()

            assertTrue(
                "Check digit should validate",
                BarcodeGenerator.validate(record.digits)
            )
        }
    }

    @Test
    fun `UPC-A is 12 digits`() {
        val generator = BarcodeGenerator(BarcodeType.UPC_A)

        repeat(100) {
            val record = generator.generate()

            assertEquals("UPC-A should be 12 digits", 12, record.digits.length)
            assertTrue("Should be all digits", record.digits.all { it.isDigit() })
        }
    }

    @Test
    fun `UPC-A has valid check digit`() {
        val generator = BarcodeGenerator(BarcodeType.UPC_A)

        repeat(100) {
            val record = generator.generate()

            assertTrue(
                "Check digit should validate",
                BarcodeGenerator.validate(record.digits)
            )
        }
    }

    @Test
    fun `ISBN-13 starts with 978 or 979`() {
        val generator = BarcodeGenerator(BarcodeType.ISBN_13)

        repeat(100) {
            val record = generator.generate()
            val prefix = record.digits.take(3)

            assertTrue(
                "ISBN should start with 978 or 979",
                prefix == "978" || prefix == "979"
            )
        }
    }

    @Test
    fun `ISBN-13 has valid check digit`() {
        val generator = BarcodeGenerator(BarcodeType.ISBN_13)

        repeat(100) {
            val record = generator.generate()

            assertTrue(
                "Check digit should validate",
                BarcodeGenerator.validate(record.digits)
            )
        }
    }

    @Test
    fun `check digit algorithm matches known values`() {
        // Known valid EAN-13 codes
        val knownValid = listOf(
            "4006381333931",  // Example EAN-13
            "9780306406157",  // Example ISBN-13
            "4012345678901"   // Another example
        )

        knownValid.forEach { code ->
            assertTrue("$code should validate", BarcodeGenerator.validate(code))
        }
    }

    @Test
    fun `invalid check digit fails validation`() {
        // Take a valid code and change the check digit
        val validCode = "4006381333931"
        val invalidCode = validCode.dropLast(1) + "0"  // Wrong check digit

        assertFalse("Modified code should fail validation", BarcodeGenerator.validate(invalidCode))
    }

    @Test
    fun `withoutCheck returns correct substring`() {
        val generator = BarcodeGenerator(BarcodeType.EAN_13)
        val record = generator.generate()

        assertEquals(
            "withoutCheck should be 12 chars",
            12,
            record.withoutCheck.length
        )
        assertEquals(
            "withoutCheck should be digits without last char",
            record.digits.dropLast(1),
            record.withoutCheck
        )
    }

    @Test
    fun `ISBN formatted has hyphens`() {
        val generator = BarcodeGenerator(BarcodeType.ISBN_13)
        val record = generator.generate()

        assertTrue("ISBN formatted should contain hyphens", record.formatted.contains("-"))
        assertEquals("ISBN formatted should have 4 hyphens", 4, record.formatted.count { it == '-' })
    }

    @Test
    fun `seed produces reproducible barcodes`() {
        val seed = 42L
        val generator = BarcodeGenerator(BarcodeType.EAN_13, seed)

        val barcode1 = BarcodeGenerator(BarcodeType.EAN_13, seed).generate().digits
        val barcode2 = BarcodeGenerator(BarcodeType.EAN_13, seed).generate().digits

        assertEquals("Same seed should produce same barcode", barcode1, barcode2)
    }

    @Test
    fun `different seeds produce different barcodes`() {
        val barcode1 = BarcodeGenerator(BarcodeType.EAN_13, 111L).generate().digits
        val barcode2 = BarcodeGenerator(BarcodeType.EAN_13, 222L).generate().digits

        assertNotEquals("Different seeds should produce different barcodes", barcode1, barcode2)
    }

    @Test
    fun `EAN country prefix is populated for EAN-13`() {
        val generator = BarcodeGenerator(BarcodeType.EAN_13)

        // Generate several and at least some should have country prefix
        val records = (1..20).map { generator.generate() }
        val withCountry = records.filter { it.countryPrefix != null }

        assertTrue("Some EAN-13 barcodes should have country prefix", withCountry.isNotEmpty())
    }

    @Test
    fun `ISBN prefix info is populated for ISBN-13`() {
        val generator = BarcodeGenerator(BarcodeType.ISBN_13)
        val record = generator.generate()

        assertNotNull("ISBN should have prefix info", record.isbnPrefix)
        assertTrue(
            "ISBN prefix info should mention Bookland or Extended",
            record.isbnPrefix!!.contains("Bookland") || record.isbnPrefix!!.contains("Extended")
        )
    }

    @Test
    fun `labeled format includes type`() {
        BarcodeType.entries.forEach { type ->
            val generator = BarcodeGenerator(type)
            val record = generator.generate()

            assertTrue(
                "Labeled should include type name",
                record.labeled.contains(type.label)
            )
        }
    }

    @Test
    fun `serialization formats work`() {
        val generator = BarcodeGenerator(BarcodeType.EAN_13)
        val record = generator.generate()

        val csv = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.CSV)
        val json = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.JSON)
        val txt = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.TXT)

        assertTrue("CSV should contain digits", csv.contains(record.digits))
        assertTrue("JSON should contain digits", json.contains(record.digits))
        assertTrue("JSON should be valid format", json.startsWith("{") && json.endsWith("}"))
        assertTrue("TXT should contain type label", txt.contains("EAN-13"))
    }

    @Test
    fun `batch serialization includes header`() {
        val generator = BarcodeGenerator(BarcodeType.UPC_A)
        val records = generator.generateBatch(5)

        val csv = generator.serializeBatch(records, com.vadimtoptunov.generators.core.OutputFormat.CSV)

        assertTrue("CSV batch should have header", csv.startsWith("type,digits,check_digit"))
        assertEquals("Should have 6 lines (header + 5 records)", 6, csv.lines().filter { it.isNotBlank() }.size)
    }

    @Test
    fun `validation rejects non-digit strings`() {
        assertFalse("Should reject non-digits", BarcodeGenerator.validate("123456789012A"))
        assertFalse("Should reject too short", BarcodeGenerator.validate("12345"))
        assertFalse("Should reject too long", BarcodeGenerator.validate("12345678901234567"))
    }
}
