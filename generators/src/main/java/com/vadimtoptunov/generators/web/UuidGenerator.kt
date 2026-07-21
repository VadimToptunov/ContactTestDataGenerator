package com.vadimtoptunov.generators.web

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlin.random.Random

/**
 * Generates random UUIDs for web and database testing.
 *
 * Use cases:
 *   - QA: Test UUID validation in forms and APIs
 *   - Dev: Generate primary keys for test databases
 *   - Integration: Create correlation IDs for distributed systems
 *
 * Supports:
 *   - v4 (RFC 4122): Random UUIDs, most common for general use
 *   - v7 (RFC 9562): Time-ordered UUIDs, ideal for databases (sortable)
 */
class UuidGenerator(
    private val version: UuidVersion = UuidVersion.V4
) : DataGenerator<UuidRecord> {

    override val name = "UUID Generator"
    override val description = "Generates ${version.label} UUIDs for testing"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): UuidRecord {
        return when (version) {
            UuidVersion.V4 -> generateV4()
            UuidVersion.V7 -> generateV7()
        }
    }

    /**
     * Generate a v4 UUID (random).
     * - Set version nibble to 4 (bits 6-7 of byte 6)
     * - Set variant bits to 10xx (bits 0-1 of byte 8)
     */
    private fun generateV4(): UuidRecord {
        val bytes = Random.nextBytes(16)

        // Set version to 4 (0100 in high nibble of byte 6)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()

        // Set variant to 10xx (byte 8 high bits)
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        return UuidRecord(version = UuidVersion.V4, bytes = bytes)
    }

    /**
     * Generate a v7 UUID (time-ordered) per RFC 9562.
     * - Bytes 0-5: Unix timestamp in milliseconds (48 bits)
     * - Byte 6: version (4 bits) + random (4 bits)
     * - Byte 7: random (8 bits)
     * - Byte 8: variant (2 bits) + random (6 bits)
     * - Bytes 9-15: random (56 bits)
     */
    private fun generateV7(): UuidRecord {
        val timestamp = System.currentTimeMillis()
        val bytes = ByteArray(16)

        // Bytes 0-5: timestamp (48 bits, big-endian)
        bytes[0] = ((timestamp shr 40) and 0xFF).toByte()
        bytes[1] = ((timestamp shr 32) and 0xFF).toByte()
        bytes[2] = ((timestamp shr 24) and 0xFF).toByte()
        bytes[3] = ((timestamp shr 16) and 0xFF).toByte()
        bytes[4] = ((timestamp shr 8) and 0xFF).toByte()
        bytes[5] = (timestamp and 0xFF).toByte()

        // Bytes 6-15: random, then set version and variant
        val randomPart = Random.nextBytes(10)
        System.arraycopy(randomPart, 0, bytes, 6, 10)

        // Set version to 7 (0111 in high nibble of byte 6)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()

        // Set variant to 10xx (byte 8 high bits)
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        return UuidRecord(version = UuidVersion.V7, bytes = bytes, timestamp = timestamp)
    }

    override fun serialize(record: UuidRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> buildString {
            append("v${record.version.versionNumber},")
            append(record.standard)
            record.timestamp?.let { append(",$it") }
        }
        OutputFormat.JSON -> buildString {
            append("{")
            append("\"version\":${record.version.versionNumber},")
            append("\"uuid\":\"${record.standard}\",")
            append("\"compact\":\"${record.compact}\",")
            append("\"urn\":\"${record.urn}\"")
            record.timestamp?.let { append(",\"timestamp\":$it") }
            append("}")
        }
        OutputFormat.TXT -> record.standard
        else -> throw UnsupportedOperationException("$format not supported by UuidGenerator")
    }

    override fun serializeBatch(records: List<UuidRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                val hasTimestamp = records.any { it.timestamp != null }
                if (hasTimestamp) {
                    appendLine("version,uuid,timestamp")
                } else {
                    appendLine("version,uuid")
                }
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
        const val ID = "uuid_generator"

        fun registerAll() {
            UuidVersion.entries.forEach { version ->
                GeneratorRegistry.register(
                    "${ID}_${version.name.lowercase()}",
                    UuidGenerator(version = version)
                )
            }
        }
    }
}
