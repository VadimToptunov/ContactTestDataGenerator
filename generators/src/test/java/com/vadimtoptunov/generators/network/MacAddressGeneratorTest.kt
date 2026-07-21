package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.OutputFormat
import org.junit.Assert.*
import org.junit.Test

class MacAddressGeneratorTest {

    @Test
    fun `generate produces valid MAC address`() {
        val generator = MacAddressGenerator()
        val record = generator.generate()

        assertEquals(6, record.octets.size)
        assertTrue(record.octets.all { it in 0..255 })
    }

    @Test
    fun `random generator produces unicast addresses`() {
        val generator = MacAddressGenerator()
        repeat(10) {
            val record = generator.generate()
            assertTrue("MAC should be unicast", record.isUnicast)
            // Bit 0 of first octet should be 0
            assertEquals(0, record.octets[0] and 0x01)
        }
    }

    @Test
    fun `forceLocal produces locally administered addresses`() {
        val generator = MacAddressGenerator(forceLocal = true)
        repeat(10) {
            val record = generator.generate()
            assertTrue("MAC should be locally administered", record.isLocal)
            assertTrue("MAC should be unicast", record.isUnicast)
            // Bit 1 of first octet should be 1
            assertEquals(2, record.octets[0] and 0x02)
        }
    }

    @Test
    fun `useKnownOui generates vendor-prefixed addresses`() {
        val generator = MacAddressGenerator(useKnownOui = true)
        repeat(10) {
            val record = generator.generate()
            assertNotNull("Should have OUI vendor", record.oui)
            assertNotNull("Should have vendor name", record.vendorName)

            // First 3 octets should match the OUI prefix
            val prefix = record.oui!!.prefix
            assertEquals(prefix[0], record.octets[0])
            assertEquals(prefix[1], record.octets[1])
            assertEquals(prefix[2], record.octets[2])
        }
    }

    @Test
    fun `preferredVendor generates specific vendor addresses`() {
        val generator = MacAddressGenerator(
            useKnownOui = true,
            preferredVendor = OuiVendor.VMWARE
        )
        repeat(10) {
            val record = generator.generate()
            assertEquals(OuiVendor.VMWARE, record.oui)
            assertEquals("VMware", record.vendorName)
            assertEquals(0x00, record.octets[0])
            assertEquals(0x50, record.octets[1])
            assertEquals(0x56, record.octets[2])
        }
    }

    @Test
    fun `format colon separated`() {
        val record = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08),
            oui = OuiVendor.VMWARE
        )
        assertEquals("00:50:56:C0:00:08", record.colonSeparated)
    }

    @Test
    fun `format hyphen separated`() {
        val record = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08),
            oui = OuiVendor.VMWARE
        )
        assertEquals("00-50-56-C0-00-08", record.hyphenSeparated)
    }

    @Test
    fun `format dot separated (Cisco)`() {
        val record = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08),
            oui = OuiVendor.VMWARE
        )
        assertEquals("0050.56c0.0008", record.dotSeparated)
    }

    @Test
    fun `format raw`() {
        val record = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08),
            oui = OuiVendor.VMWARE
        )
        assertEquals("005056C00008", record.raw)
    }

    @Test
    fun `unicast vs multicast detection`() {
        // Unicast: bit 0 = 0
        val unicast = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08)
        )
        assertTrue(unicast.isUnicast)
        assertFalse(unicast.isMulticast)

        // Multicast: bit 0 = 1
        val multicast = MacAddressRecord(
            octets = listOf(0x01, 0x00, 0x5E, 0x00, 0x00, 0x01)
        )
        assertFalse(multicast.isUnicast)
        assertTrue(multicast.isMulticast)
    }

    @Test
    fun `local vs universal detection`() {
        // Universal (UAA): bit 1 = 0
        val universal = MacAddressRecord(
            octets = listOf(0x00, 0x50, 0x56, 0xC0, 0x00, 0x08)
        )
        assertTrue(universal.isUniversal)
        assertFalse(universal.isLocal)

        // Local (LAA): bit 1 = 1
        val local = MacAddressRecord(
            octets = listOf(0x02, 0x42, 0xAC, 0x11, 0x00, 0x02)
        )
        assertFalse(local.isUniversal)
        assertTrue(local.isLocal)
    }

    @Test
    fun `serialize JSON format`() {
        val generator = MacAddressGenerator(useKnownOui = true, preferredVendor = OuiVendor.DOCKER)
        val record = generator.generate()

        val json = generator.serialize(record, OutputFormat.JSON)
        assertTrue(json.contains("\"colon\":"))
        assertTrue(json.contains("\"hyphen\":"))
        assertTrue(json.contains("\"cisco\":"))
        assertTrue(json.contains("\"vendor\":\"Docker\""))
    }

    @Test
    fun `serialize TXT format shows LAA marker`() {
        val generator = MacAddressGenerator(forceLocal = true)
        val record = generator.generate()

        val txt = generator.serialize(record, OutputFormat.TXT)
        assertTrue(txt.contains("[LAA]"))
    }

    @Test
    fun `batch serialization includes header for CSV`() {
        val generator = MacAddressGenerator()
        val records = generator.generateBatch(3)

        val csv = generator.serializeBatch(records, OutputFormat.CSV)
        assertTrue(csv.startsWith("address,vendor,type,admin"))
        assertEquals(4, csv.trim().lines().size)
    }
}
