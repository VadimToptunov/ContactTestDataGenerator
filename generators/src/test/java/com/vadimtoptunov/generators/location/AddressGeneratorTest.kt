package com.vadimtoptunov.generators.location

import org.junit.Assert.*
import org.junit.Test

class AddressGeneratorTest {

    // ═══════════════════════════════════════════════════════════════════
    // US Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `US address has valid ZIP format (5 digits)`() {
        val generator = AddressGenerator("US")

        repeat(50) {
            val record = generator.generate()

            assertEquals("US country code", "US", record.countryCode)
            assertTrue(
                "US ZIP should be 5 digits",
                record.postalCode.matches(Regex("\\d{5}"))
            )
            assertTrue("US should have state", record.state != null)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UK Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `UK address has valid postcode format`() {
        val generator = AddressGenerator("GB")

        repeat(50) {
            val record = generator.generate()

            assertEquals("GB country code", "GB", record.countryCode)
            // UK postcode format varies: A9 9AA, A99 9AA, A9A 9AA, AA9 9AA, AA99 9AA, AA9A 9AA
            assertTrue(
                "UK postcode should have space",
                record.postalCode.contains(" ") || record.postalCode.length <= 4
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // German Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `German address has 5-digit PLZ`() {
        val generator = AddressGenerator("DE")

        repeat(50) {
            val record = generator.generate()

            assertEquals("DE country code", "DE", record.countryCode)
            assertTrue(
                "German PLZ should be 5 digits",
                record.postalCode.matches(Regex("\\d{5}"))
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Russian Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Russian address has 6-digit postal index`() {
        val generator = AddressGenerator("RU")

        repeat(50) {
            val record = generator.generate()

            assertEquals("RU country code", "RU", record.countryCode)
            assertTrue(
                "Russian postal index should be 6 digits",
                record.postalCode.matches(Regex("\\d{6}"))
            )
            assertEquals("Russian locale", "ru-RU", record.locale)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Japanese Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Japanese address has XXX-XXXX format`() {
        val generator = AddressGenerator("JP")

        repeat(50) {
            val record = generator.generate()

            assertEquals("JP country code", "JP", record.countryCode)
            assertTrue(
                "Japanese postal code should be XXX-XXXX format",
                record.postalCode.matches(Regex("\\d{3}-\\d{4}"))
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Canadian Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Canadian address has ANA NAN format`() {
        val generator = AddressGenerator("CA")

        repeat(50) {
            val record = generator.generate()

            assertEquals("CA country code", "CA", record.countryCode)
            assertTrue(
                "Canadian postal code should be ANA NAN format",
                record.postalCode.matches(Regex("[A-Z]\\d[A-Z] \\d[A-Z]\\d"))
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Australian Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Australian address has 4-digit postcode`() {
        val generator = AddressGenerator("AU")

        repeat(50) {
            val record = generator.generate()

            assertEquals("AU country code", "AU", record.countryCode)
            assertTrue(
                "Australian postcode should be 4 digits",
                record.postalCode.matches(Regex("\\d{4}"))
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Brazilian Addresses
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Brazilian address has XXXXX-XXX format`() {
        val generator = AddressGenerator("BR")

        repeat(50) {
            val record = generator.generate()

            assertEquals("BR country code", "BR", record.countryCode)
            assertTrue(
                "Brazilian CEP should be XXXXX-XXX format",
                record.postalCode.matches(Regex("\\d{5}-\\d{3}"))
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // All Countries Coverage
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `all CountryProfile countries can generate addresses`() {
        val countryCodes = listOf(
            "RU", "UA", "US", "GB", "DE", "FR", "ES", "IT", "PL", "BR",
            "NL", "SE", "CH", "AT", "BE", "PT", "CZ", "HU", "RO", "BG",
            "GR", "TR", "IL", "AE", "SA", "IN", "CN", "JP", "KR", "SG",
            "AU", "NZ", "CA", "MX", "AR", "CO", "CL", "ZA", "NG", "EG",
            "ID", "TH", "VN", "MY", "PH", "PK", "BD", "GE", "AM", "KZ"
        )

        countryCodes.forEach { code ->
            val generator = AddressGenerator(code)
            val record = generator.generate()

            assertEquals("Country code should match: $code", code, record.countryCode)
            assertTrue("Street should not be empty: $code", record.street.isNotEmpty())
            assertTrue("City should not be empty: $code", record.city.isNotEmpty())
            assertTrue("House number should not be empty: $code", record.houseNumber.isNotEmpty())
        }
    }

    @Test
    fun `WorldPostalFormats fallback countries work`() {
        // Countries that are in WorldPostalFormats but not in CountryProfiles
        val fallbackCountries = listOf("AD", "AL", "BA", "BY", "CY", "EE", "FI", "IS", "LT", "LV")

        fallbackCountries.forEach { code ->
            val generator = AddressGenerator(code)
            val record = generator.generate()

            assertEquals("Country code should match: $code", code, record.countryCode)
            assertTrue("Street should not be empty: $code", record.street.isNotEmpty())
            assertTrue("City should not be empty: $code", record.city.isNotEmpty())
        }
    }

    @Test
    fun `countries without postal codes have empty postal code`() {
        // Only include countries that are NOT in CountryProfiles and have hasPostalCode=false
        // (AE and QA are in CountryProfiles with postal codes, so exclude them)
        val noPostalCodeCountries = listOf("HK", "MO", "JM", "BZ", "GH", "UG", "BS", "AW")

        noPostalCodeCountries.forEach { code ->
            val generator = AddressGenerator(code)
            val record = generator.generate()

            assertEquals("$code should have empty postal code", "", record.postalCode)
            assertFalse("$code hasPostalCode should be false", record.hasPostalCode)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Seed-based Reproducibility
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `seed produces reproducible addresses`() {
        val seed = 42L

        listOf("US", "DE", "JP", "BR").forEach { code ->
            val address1 = AddressGenerator(code, seed).generate()
            val address2 = AddressGenerator(code, seed).generate()

            assertEquals("$code: Same seed should produce same address", address1, address2)
        }
    }

    @Test
    fun `different seeds produce different addresses`() {
        listOf("US", "DE", "JP", "BR").forEach { code ->
            val address1 = AddressGenerator(code, 111L).generate()
            val address2 = AddressGenerator(code, 222L).generate()

            assertNotEquals("$code: Different seeds should produce different addresses", address1, address2)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Address Formatting
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `streetFirst format is correct`() {
        val generator = AddressGenerator("US")
        val record = generator.generate()

        val streetFirst = record.streetFirst

        assertTrue("Should contain house number", streetFirst.contains(record.houseNumber))
        assertTrue("Should contain street", streetFirst.contains(record.street))
        assertTrue("Should contain city", streetFirst.contains(record.city))
        assertTrue("Should contain postal code", streetFirst.contains(record.postalCode))
    }

    @Test
    fun `cityFirst format is correct`() {
        val generator = AddressGenerator("RU")
        val record = generator.generate()

        val cityFirst = record.cityFirst

        assertTrue("Should contain city", cityFirst.contains(record.city))
        assertTrue("Should contain street", cityFirst.contains(record.street))
        assertTrue("Should contain postal code", cityFirst.contains(record.postalCode))
    }

    @Test
    fun `multiLine format has multiple lines`() {
        val generator = AddressGenerator("US")
        val record = generator.generate()

        val multiLine = record.multiLine

        assertTrue("Should have multiple lines", multiLine.contains("\n"))
        assertTrue("Should end with country name", multiLine.trim().endsWith(record.countryName))
    }

    @Test
    fun `apartment is optional`() {
        val generator = AddressGenerator("US", seed = 12345L)

        val records = (1..50).map { generator.generate() }

        val withApartment = records.filter { it.hasApartment }
        val withoutApartment = records.filter { !it.hasApartment }

        assertTrue("Some should have apartments", withApartment.isNotEmpty())
        assertTrue("Some should not have apartments", withoutApartment.isNotEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    // Serialization
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `serialization formats work`() {
        val generator = AddressGenerator("DE")
        val record = generator.generate()

        val csv = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.CSV)
        val json = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.JSON)
        val txt = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.TXT)

        assertTrue("CSV should contain street", csv.contains(record.street))
        assertTrue("JSON should contain street", json.contains(record.street))
        assertTrue("JSON should be valid format", json.startsWith("{") && json.endsWith("}"))
        assertTrue("TXT should contain city", txt.contains(record.city))
    }

    @Test
    fun `batch serialization includes header`() {
        val generator = AddressGenerator("FR")
        val records = generator.generateBatch(5)

        val csv = generator.serializeBatch(records, com.vadimtoptunov.generators.core.OutputFormat.CSV)

        assertTrue("CSV batch should have header", csv.contains("street,house_number"))
        assertTrue("Should have 6 lines", csv.lines().filter { it.isNotBlank() }.size >= 6)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Error Handling
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `unknown country code throws exception`() {
        val generator = AddressGenerator("XX")

        assertThrows(IllegalArgumentException::class.java) {
            generator.generate()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CountryProfiles vs WorldPostalFormats
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `CountryProfile countries have rich data`() {
        // These should have detailed street names from CountryProfiles
        val richCountries = listOf("US", "GB", "DE", "FR", "RU", "JP", "CN")

        richCountries.forEach { code ->
            val generator = AddressGenerator(code)
            val record = generator.generate()

            // Rich data countries should have locale matching country
            assertTrue(
                "$code should have specific locale",
                record.locale.contains("-")
            )
        }
    }

    @Test
    fun `fallback countries use generic streets`() {
        // Albania is in WorldPostalFormats but not CountryProfiles
        val generator = AddressGenerator("AL")
        val record = generator.generate()

        assertEquals("AL country code", "AL", record.countryCode)
        assertEquals("Albania name", "Albania", record.countryName)
        // Fallback uses "en" locale
        assertEquals("Fallback uses en locale", "en", record.locale)
    }
}
