package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.*
import org.junit.Test

class IPv6GeneratorTest {

    @Test
    fun `generate produces valid global unicast address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.GLOBAL_UNICAST)
        val record = generator.generate()

        assertEquals(IPv6AddressType.GLOBAL_UNICAST, record.addressType)
        assertEquals(8, record.groups.size)
        assertTrue(record.groups.all { it in 0..0xFFFF })

        // Global unicast starts with 2000::/3 (0x2000-0x3FFF)
        assertTrue("First group should be 0x2000-0x3FFF", record.groups[0] in 0x2000..0x3FFF)
    }

    @Test
    fun `generate produces valid link-local address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.LINK_LOCAL)
        val record = generator.generate()

        assertEquals(IPv6AddressType.LINK_LOCAL, record.addressType)
        assertEquals(0xfe80, record.groups[0])
        // Groups 1-3 are zero by convention
        assertEquals(0, record.groups[1])
        assertEquals(0, record.groups[2])
        assertEquals(0, record.groups[3])
    }

    @Test
    fun `generate produces valid unique local address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.UNIQUE_LOCAL)
        val record = generator.generate()

        assertEquals(IPv6AddressType.UNIQUE_LOCAL, record.addressType)
        // Unique local: fd00::/8
        val firstGroup = record.groups[0]
        assertTrue("Should start with fd", firstGroup in 0xfd00..0xfdff)
    }

    @Test
    fun `generate produces loopback address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.LOOPBACK)
        val record = generator.generate()

        assertEquals(IPv6AddressType.LOOPBACK, record.addressType)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 1), record.groups)
        assertEquals("::1", record.compressed)
    }

    @Test
    fun `generate produces IPv4-mapped address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.IPV4_MAPPED)
        val record = generator.generate()

        assertEquals(IPv6AddressType.IPV4_MAPPED, record.addressType)
        // First 5 groups should be 0
        assertEquals(0, record.groups[0])
        assertEquals(0, record.groups[4])
        // Group 5 should be 0xFFFF
        assertEquals(0xffff, record.groups[5])

        // Mixed notation should look like ::ffff:x.x.x.x
        assertTrue(record.mixed.startsWith("::ffff:"))
    }

    @Test
    fun `generate produces documentation address`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.DOCUMENTATION)
        val record = generator.generate()

        assertEquals(IPv6AddressType.DOCUMENTATION, record.addressType)
        assertEquals(0x2001, record.groups[0])
        assertEquals(0x0db8, record.groups[1])
    }

    @Test
    fun `compression algorithm works correctly`() {
        // Test basic compression
        val record1 = IPv6Record(
            groups = listOf(0x2001, 0x0db8, 0, 0, 0, 0, 0, 1),
            addressType = IPv6AddressType.DOCUMENTATION
        )
        assertEquals("2001:db8::1", record1.compressed)

        // Test all zeros
        val record2 = IPv6Record(
            groups = listOf(0, 0, 0, 0, 0, 0, 0, 0),
            addressType = IPv6AddressType.LOOPBACK
        )
        assertEquals("::", record2.compressed)

        // Test loopback
        val record3 = IPv6Record(
            groups = listOf(0, 0, 0, 0, 0, 0, 0, 1),
            addressType = IPv6AddressType.LOOPBACK
        )
        assertEquals("::1", record3.compressed)

        // Test no compression needed
        val record4 = IPv6Record(
            groups = listOf(0x2001, 0x0db8, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006),
            addressType = IPv6AddressType.DOCUMENTATION
        )
        assertEquals("2001:db8:1:2:3:4:5:6", record4.compressed)
    }

    @Test
    fun `full format is always 8 groups with leading zeros`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.LOOPBACK)
        val record = generator.generate()

        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", record.full)
    }

    @Test
    fun `serialize JSON format`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.GLOBAL_UNICAST)
        val record = generator.generate()

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"type\":\"Global Unicast\""))
        assertTrue(json.contains("\"compressed\":"))
        assertTrue(json.contains("\"full\":"))
    }

    @Test
    fun `batch serialization includes header for CSV`() {
        val generator = IPv6Generator(addressType = IPv6AddressType.GLOBAL_UNICAST)
        val records = generator.generateBatch(3)

        val csv = generator.serializeBatch(records, OutputFormat.CSV)
        assertTrue(csv.startsWith("type,compressed,full"))
        assertEquals(4, csv.trim().lines().size)
    }
}
