package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlin.random.Random

/**
 * Generates random IPv4 addresses for network testing.
 *
 * Use cases:
 *   - QA: Test IP validation in forms and APIs
 *   - Dev: Seed test databases with realistic IP data
 *   - Security: Generate test data for firewall rules and ACLs
 *
 * Supports all major address types: private (RFC 1918), public,
 * loopback, link-local, multicast, and reserved ranges.
 */
class IPv4Generator(
    private val addressType: IPv4AddressType = IPv4AddressType.PRIVATE,
    private val includeCidr: Boolean = false,
    private val cidrRange: IntRange = 8..30
) : DataGenerator<IPv4Record> {

    override val name = "IPv4 Generator"
    override val description = "Generates ${addressType.label} IPv4 addresses for testing"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): IPv4Record {
        val octets = when (addressType) {
            IPv4AddressType.PRIVATE -> generatePrivate()
            IPv4AddressType.PUBLIC -> generatePublic()
            IPv4AddressType.LOOPBACK -> generateLoopback()
            IPv4AddressType.LINK_LOCAL -> generateLinkLocal()
            IPv4AddressType.MULTICAST -> generateMulticast()
            IPv4AddressType.RESERVED -> generateReserved()
        }

        val cidrPrefix = if (includeCidr) cidrRange.random() else null

        return IPv4Record(
            octets = octets,
            addressType = addressType,
            cidrPrefix = cidrPrefix
        )
    }

    private fun generatePrivate(): List<Int> {
        // RFC 1918 private ranges:
        // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        return when (Random.nextInt(3)) {
            0 -> listOf(10, randomOctet(), randomOctet(), randomOctet())
            1 -> listOf(172, Random.nextInt(16, 32), randomOctet(), randomOctet())
            else -> listOf(192, 168, randomOctet(), randomOctet())
        }
    }

    private fun generatePublic(): List<Int> {
        // Avoid reserved first octets
        val reservedFirstOctets = setOf(
            0,      // This network
            10,     // Private
            127,    // Loopback
            169,    // Link-local (169.254.x.x)
            224, 225, 226, 227, 228, 229, 230, 231,  // Multicast (224-239)
            232, 233, 234, 235, 236, 237, 238, 239,
            240, 241, 242, 243, 244, 245, 246, 247,  // Reserved (240-255)
            248, 249, 250, 251, 252, 253, 254, 255
        )

        var first: Int
        do {
            first = randomOctet()
        } while (first in reservedFirstOctets)

        // Check for 172.16-31.x.x and 192.168.x.x private ranges
        return when {
            first == 172 -> {
                val second = Random.nextInt(0, 16).let {
                    if (Random.nextBoolean()) it else Random.nextInt(32, 256)
                }
                listOf(first, second, randomOctet(), randomOctet())
            }
            first == 192 -> {
                val second = (0..255).filter { it != 168 }.random()
                listOf(first, second, randomOctet(), randomOctet())
            }
            else -> listOf(first, randomOctet(), randomOctet(), randomOctet())
        }
    }

    private fun generateLoopback(): List<Int> {
        // 127.0.0.0/8 - entire range is loopback
        return listOf(127, randomOctet(), randomOctet(), randomOctet())
    }

    private fun generateLinkLocal(): List<Int> {
        // 169.254.0.0/16 (APIPA)
        return listOf(169, 254, randomOctet(), randomOctet())
    }

    private fun generateMulticast(): List<Int> {
        // 224.0.0.0 - 239.255.255.255
        return listOf(Random.nextInt(224, 240), randomOctet(), randomOctet(), randomOctet())
    }

    private fun generateReserved(): List<Int> {
        // 240.0.0.0 - 255.255.255.254 (future use)
        return listOf(Random.nextInt(240, 256), randomOctet(), randomOctet(), randomOctet())
    }

    private fun randomOctet(): Int = Random.nextInt(0, 256)

    override fun serialize(record: IPv4Record, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> "${record.addressType.label},${record.raw},${record.cidrPrefix ?: ""},${record.numeric}"
        OutputFormat.JSON -> buildString {
            append("{")
            append("\"type\":\"${record.addressType.label}\",")
            append("\"address\":\"${record.raw}\",")
            record.cidrPrefix?.let { append("\"cidr\":\"${record.cidr}\",") }
            append("\"numeric\":${record.numeric},")
            append("\"hex\":\"${record.hex}\"")
            append("}")
        }
        OutputFormat.TXT -> buildString {
            append("${record.addressType.label}: ${record.raw}")
            record.cidrPrefix?.let { append("/$it") }
        }
        else -> throw UnsupportedOperationException("$format not supported by IPv4Generator")
    }

    override fun serializeBatch(records: List<IPv4Record>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("type,address,cidr_prefix,numeric")
                records.forEach { appendLine(serialize(it, format)) }
            }
            OutputFormat.JSON -> buildString {
                appendLine("[")
                records.forEachIndexed { i, r ->
                    append("  ${serialize(r, format)}")
                    if (i < records.lastIndex) appendLine(",") else appendLine()
                }
                append("]")
            }
            else -> super.serializeBatch(records, format)
        }

    companion object {
        const val ID = "ipv4_generator"

        fun registerAll() {
            IPv4AddressType.entries.forEach { type ->
                GeneratorRegistry.register(
                    "${ID}_${type.name.lowercase()}",
                    IPv4Generator(addressType = type)
                )
            }
            // Also register CIDR variants for private and public
            GeneratorRegistry.register(
                "${ID}_private_cidr",
                IPv4Generator(addressType = IPv4AddressType.PRIVATE, includeCidr = true)
            )
            GeneratorRegistry.register(
                "${ID}_public_cidr",
                IPv4Generator(addressType = IPv4AddressType.PUBLIC, includeCidr = true)
            )
        }
    }
}
