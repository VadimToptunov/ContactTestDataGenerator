package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.*
import org.junit.Test

class IPv4GeneratorTest {

    @Test
    fun `generate produces valid private address`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.PRIVATE)
        val record = generator.generate()

        assertEquals(IPv4AddressType.PRIVATE, record.addressType)
        assertEquals(4, record.octets.size)
        assertTrue(record.octets.all { it in 0..255 })

        // Check it's actually a private range
        val first = record.octets[0]
        val second = record.octets[1]
        val isPrivate = (first == 10) ||
                (first == 172 && second in 16..31) ||
                (first == 192 && second == 168)
        assertTrue("Address ${record.raw} should be private", isPrivate)
    }

    @Test
    fun `generate produces valid public address`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.PUBLIC)
        repeat(10) {
            val record = generator.generate()
            assertEquals(IPv4AddressType.PUBLIC, record.addressType)

            // Verify it's not in reserved ranges
            val first = record.octets[0]
            assertNotEquals("Should not be 0", 0, first)
            assertNotEquals("Should not be 10 (private)", 10, first)
            assertNotEquals("Should not be 127 (loopback)", 127, first)
            assertFalse("Should not be 224-255 (multicast/reserved)", first >= 224)
        }
    }

    @Test
    fun `generate produces loopback address`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.LOOPBACK)
        val record = generator.generate()

        assertEquals(IPv4AddressType.LOOPBACK, record.addressType)
        assertEquals(127, record.octets[0])
    }

    @Test
    fun `generate produces link-local address`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.LINK_LOCAL)
        val record = generator.generate()

        assertEquals(IPv4AddressType.LINK_LOCAL, record.addressType)
        assertEquals(169, record.octets[0])
        assertEquals(254, record.octets[1])
    }

    @Test
    fun `generate produces multicast address`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.MULTICAST)
        val record = generator.generate()

        assertEquals(IPv4AddressType.MULTICAST, record.addressType)
        assertTrue("First octet should be 224-239", record.octets[0] in 224..239)
    }

    @Test
    fun `CIDR prefix is included when configured`() {
        val generator = IPv4Generator(
            addressType = IPv4AddressType.PRIVATE,
            includeCidr = true,
            cidrRange = 24..24
        )
        val record = generator.generate()

        assertEquals(24, record.cidrPrefix)
        assertTrue(record.cidr.endsWith("/24"))
    }

    @Test
    fun `computed properties are correct`() {
        val record = IPv4Record(
            octets = listOf(192, 168, 1, 1),
            addressType = IPv4AddressType.PRIVATE,
            cidrPrefix = 24
        )

        assertEquals("192.168.1.1", record.raw)
        assertEquals("192.168.1.1/24", record.cidr)
        assertEquals(3232235777L, record.numeric)
        assertEquals("11000000.10101000.00000001.00000001", record.binary)
        assertEquals("c0a80101", record.hex)
    }

    @Test
    fun `serialize JSON format`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.PRIVATE)
        val record = IPv4Record(
            octets = listOf(192, 168, 1, 1),
            addressType = IPv4AddressType.PRIVATE
        )

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"address\":\"192.168.1.1\""))
        assertTrue(json.contains("\"type\":\"Private\""))
    }

    @Test
    fun `batch serialization includes header for CSV`() {
        val generator = IPv4Generator(addressType = IPv4AddressType.PRIVATE)
        val records = generator.generateBatch(3)

        val csv = generator.serializeBatch(records, OutputFormat.CSV)
        assertTrue(csv.startsWith("type,address,cidr_prefix,numeric"))
        assertEquals(4, csv.trim().lines().size) // header + 3 records
    }
}
