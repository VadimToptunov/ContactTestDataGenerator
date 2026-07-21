package com.vadimtoptunov.generators.web

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.*
import org.junit.Test

class UuidGeneratorTest {

    @Test
    fun `generate v4 produces valid UUID`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        assertEquals(UuidVersion.V4, record.version)
        assertEquals(16, record.bytes.size)
        assertNull(record.timestamp)
    }

    @Test
    fun `v4 UUID has correct version nibble`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        repeat(10) {
            val record = generator.generate()
            assertEquals("Version nibble should be 4", 4, record.versionNibble)
        }
    }

    @Test
    fun `v4 UUID has correct variant bits`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        repeat(10) {
            val record = generator.generate()
            // Variant should be 10xx (binary) = 2 (first two bits)
            assertEquals("Variant should be RFC 4122", 2, record.variantBits)
        }
    }

    @Test
    fun `generate v7 produces valid UUID with timestamp`() {
        val generator = UuidGenerator(version = UuidVersion.V7)
        val before = System.currentTimeMillis()
        val record = generator.generate()
        val after = System.currentTimeMillis()

        assertEquals(UuidVersion.V7, record.version)
        assertEquals(16, record.bytes.size)
        assertNotNull(record.timestamp)
        assertTrue("Timestamp should be recent", record.timestamp!! in before..after)
    }

    @Test
    fun `v7 UUID has correct version nibble`() {
        val generator = UuidGenerator(version = UuidVersion.V7)
        repeat(10) {
            val record = generator.generate()
            assertEquals("Version nibble should be 7", 7, record.versionNibble)
        }
    }

    @Test
    fun `v7 UUID has correct variant bits`() {
        val generator = UuidGenerator(version = UuidVersion.V7)
        repeat(10) {
            val record = generator.generate()
            assertEquals("Variant should be RFC 4122", 2, record.variantBits)
        }
    }

    @Test
    fun `v7 UUIDs are sortable by time`() {
        val generator = UuidGenerator(version = UuidVersion.V7)
        val uuids = (1..10).map {
            Thread.sleep(1) // Ensure different timestamps
            generator.generate()
        }

        // When sorted as strings, v7 UUIDs should maintain chronological order
        val sorted = uuids.sortedBy { it.standard }
        assertEquals(uuids.map { it.standard }, sorted.map { it.standard })
    }

    @Test
    fun `standard format is 8-4-4-4-12`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        val standard = record.standard
        val parts = standard.split("-")
        assertEquals(5, parts.size)
        assertEquals(8, parts[0].length)
        assertEquals(4, parts[1].length)
        assertEquals(4, parts[2].length)
        assertEquals(4, parts[3].length)
        assertEquals(12, parts[4].length)
        assertEquals(36, standard.length) // 32 hex + 4 hyphens
    }

    @Test
    fun `compact format has no hyphens`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        val compact = record.compact
        assertFalse(compact.contains("-"))
        assertEquals(32, compact.length)
    }

    @Test
    fun `URN format is correct`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        assertTrue(record.urn.startsWith("urn:uuid:"))
        assertEquals("urn:uuid:${record.standard}", record.urn)
    }

    @Test
    fun `braced format is correct`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        assertTrue(record.braced.startsWith("{"))
        assertTrue(record.braced.endsWith("}"))
        assertEquals("{${record.standard}}", record.braced)
    }

    @Test
    fun `serialize JSON format for v4`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"version\":4"))
        assertTrue(json.contains("\"uuid\":"))
        assertTrue(json.contains("\"compact\":"))
        assertTrue(json.contains("\"urn\":"))
        assertFalse("v4 should not have timestamp", json.contains("\"timestamp\":"))
    }

    @Test
    fun `serialize JSON format for v7`() {
        val generator = UuidGenerator(version = UuidVersion.V7)
        val record = generator.generate()

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"version\":7"))
        assertTrue("v7 should have timestamp", json.contains("\"timestamp\":"))
    }

    @Test
    fun `serialize TXT format is just the UUID`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val record = generator.generate()

        val txt = generator.serialize(record, OutputFormat.TXT)
        assertEquals(record.standard, txt)
    }

    @Test
    fun `batch serialization includes header for CSV`() {
        val generator = UuidGenerator(version = UuidVersion.V4)
        val records = generator.generateBatch(3)

        val csv = generator.serializeBatch(records, OutputFormat.CSV)
        assertTrue(csv.startsWith("version,uuid"))
        assertEquals(4, csv.trim().lines().size)
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val bytes = ByteArray(16) { it.toByte() }
        val record1 = UuidRecord(UuidVersion.V4, bytes.copyOf())
        val record2 = UuidRecord(UuidVersion.V4, bytes.copyOf())
        val record3 = UuidRecord(UuidVersion.V7, bytes.copyOf())

        assertEquals(record1, record2)
        assertEquals(record1.hashCode(), record2.hashCode())
        assertNotEquals(record1, record3)
    }
}
