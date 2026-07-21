package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlin.random.Random

/**
 * Generates random IPv6 addresses for network testing.
 *
 * Use cases:
 *   - QA: Test IPv6 validation in forms and APIs
 *   - Dev: Seed test databases with realistic IPv6 data
 *   - Security: Test firewall rules and network configurations
 *
 * Supports: Global Unicast (2000::/3), Link-Local (fe80::/10),
 * Unique Local (fc00::/7), Loopback (::1), IPv4-Mapped, and Documentation.
 */
class IPv6Generator(
    private val addressType: IPv6AddressType = IPv6AddressType.GLOBAL_UNICAST
) : DataGenerator<IPv6Record> {

    override val name = "IPv6 Generator"
    override val description = "Generates ${addressType.label} IPv6 addresses for testing"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): IPv6Record {
        val groups = when (addressType) {
            IPv6AddressType.GLOBAL_UNICAST -> generateGlobalUnicast()
            IPv6AddressType.LINK_LOCAL -> generateLinkLocal()
            IPv6AddressType.UNIQUE_LOCAL -> generateUniqueLocal()
            IPv6AddressType.LOOPBACK -> generateLoopback()
            IPv6AddressType.IPV4_MAPPED -> generateIPv4Mapped()
            IPv6AddressType.DOCUMENTATION -> generateDocumentation()
        }

        return IPv6Record(groups = groups, addressType = addressType)
    }

    private fun generateGlobalUnicast(): List<Int> {
        // 2000::/3 - first 3 bits are 001
        // First group: 0x2000 - 0x3FFF
        val first = Random.nextInt(0x2000, 0x4000)
        return listOf(first) + (1..7).map { randomGroup() }
    }

    private fun generateLinkLocal(): List<Int> {
        // fe80::/10 - first 10 bits are 1111111010
        // fe80::xxxx:xxxx:xxxx:xxxx (groups 1-3 are zero by convention)
        return listOf(0xfe80, 0, 0, 0) + (4..7).map { randomGroup() }
    }

    private fun generateUniqueLocal(): List<Int> {
        // fc00::/7 - first 7 bits are 1111110
        // In practice, fd00::/8 is used (L bit = 1)
        val first = 0xfd00 or Random.nextInt(0, 0x100)
        return listOf(first) + (1..7).map { randomGroup() }
    }

    private fun generateLoopback(): List<Int> {
        // ::1
        return listOf(0, 0, 0, 0, 0, 0, 0, 1)
    }

    private fun generateIPv4Mapped(): List<Int> {
        // ::ffff:x.x.x.x
        val ipv4High = Random.nextInt(1, 256) shl 8 or Random.nextInt(0, 256)
        val ipv4Low = Random.nextInt(0, 256) shl 8 or Random.nextInt(0, 256)
        return listOf(0, 0, 0, 0, 0, 0xffff, ipv4High, ipv4Low)
    }

    private fun generateDocumentation(): List<Int> {
        // 2001:db8::/32 - reserved for documentation and examples
        return listOf(0x2001, 0x0db8) + (2..7).map { randomGroup() }
    }

    private fun randomGroup(): Int = Random.nextInt(0, 0x10000)

    override fun serialize(record: IPv6Record, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> "${record.addressType.label},${record.compressed},${record.full}"
        OutputFormat.JSON -> buildString {
            append("{")
            append("\"type\":\"${record.addressType.label}\",")
            append("\"compressed\":\"${record.compressed}\",")
            append("\"full\":\"${record.full}\"")
            if (record.addressType == IPv6AddressType.IPV4_MAPPED) {
                append(",\"mixed\":\"${record.mixed}\"")
            }
            append("}")
        }
        OutputFormat.TXT -> "${record.addressType.label}: ${record.compressed}"
        else -> throw UnsupportedOperationException("$format not supported by IPv6Generator")
    }

    override fun serializeBatch(records: List<IPv6Record>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("type,compressed,full")
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
        const val ID = "ipv6_generator"

        fun registerAll() {
            IPv6AddressType.entries.forEach { type ->
                GeneratorRegistry.register(
                    "${ID}_${type.name.lowercase()}",
                    IPv6Generator(addressType = type)
                )
            }
        }
    }
}
