package com.vadimtoptunov.generators.contacts

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactGeneratorTest {

    private val generator = ContactGenerator()

    @Test
    fun `generates all fields by default`() {
        val record = generator.generate()
        assertTrue(record.fullName!!.isNotBlank())
        assertTrue(record.phone!!.startsWith("+"))
        assertTrue(record.email!!.contains("@"))
        assertTrue(record.company!!.isNotBlank())
        assertTrue(record.jobTitle!!.isNotBlank())
    }

    @Test
    fun `excluded fields are null`() {
        val nameOnly = ContactGenerator(
            ContactFields(
                includeName = true,
                includePhone = false,
                includeEmail = false,
                includeCompany = false,
                includeJobTitle = false,
            )
        ).generate()

        assertTrue(nameOnly.fullName!!.isNotBlank())
        assertNull(nameOnly.phone)
        assertNull(nameOnly.email)
        assertNull(nameOnly.company)
        assertNull(nameOnly.jobTitle)
    }

    @Test
    fun `same seed produces identical record`() {
        assertEquals(generator.generate(seed = 42L), generator.generate(seed = 42L))
    }

    @Test
    fun `different seeds usually differ`() {
        assertTrue(generator.generate(seed = 1L) != generator.generate(seed = 2L))
    }

    @Test
    fun `email is derived from name`() {
        val record = generator.generate(seed = 7L)
        val expectedLocalPart = record.fullName!!.replace(" ", ".").lowercase()
        assertTrue(record.email!!.startsWith("$expectedLocalPart@"))
    }

    @Test
    fun `vcard serialization is well formed`() {
        val record = generator.generate(seed = 3L)
        val vcard = generator.serialize(record, OutputFormat.VCF)
        assertTrue(vcard.startsWith("BEGIN:VCARD"))
        assertTrue(vcard.contains("VERSION:3.0"))
        assertTrue(vcard.contains("FN:${record.fullName}"))
        assertTrue(vcard.trimEnd().endsWith("END:VCARD"))
    }

    @Test
    fun `csv batch has header and one row per record`() {
        val records = generator.generateBatch(count = 3, seed = 10L)
        val csv = generator.serializeBatch(records, OutputFormat.CSV)
        val lines = csv.lines()
        assertEquals("full_name,phone,email,company,job_title", lines.first())
        assertEquals(4, lines.size) // header + 3 rows
    }

    @Test
    fun `json batch is an array of objects`() {
        val records = generator.generateBatch(count = 2, seed = 11L)
        val json = generator.serializeBatch(records, OutputFormat.JSON)
        assertTrue(json.trim().startsWith("["))
        assertTrue(json.trim().endsWith("]"))
        assertEquals(2, Regex("\"fullName\"").findAll(json).count())
    }

    @Test
    fun `sql batch produces insert statements`() {
        val records = generator.generateBatch(count = 2, seed = 12L)
        val sql = generator.serializeBatch(records, OutputFormat.SQL)
        assertEquals(2, Regex("INSERT INTO contacts").findAll(sql).count())
        assertTrue(sql.trimEnd().endsWith(";"))
    }

    @Test
    fun `sql escapes single quotes`() {
        // A company name containing an apostrophe must be doubled for SQL safety.
        val record = ContactRecord(
            fullName = "Test O'Brien",
            phone = null,
            email = null,
            company = "O'Reilly Media",
            jobTitle = null,
        )
        val sql = generator.serialize(record, OutputFormat.SQL)
        assertTrue(sql.contains("'Test O''Brien'"))
        assertTrue(sql.contains("'O''Reilly Media'"))
    }

    @Test
    fun `csv quotes fields containing commas`() {
        val record = ContactRecord(
            fullName = "Doe, John",
            phone = null,
            email = null,
            company = null,
            jobTitle = null,
        )
        val csv = generator.serialize(record, OutputFormat.CSV)
        assertTrue(csv.startsWith("\"Doe, John\""))
    }
}
