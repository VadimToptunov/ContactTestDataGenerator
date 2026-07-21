package com.vadimtoptunov.generators.web

import kotlinx.serialization.Serializable

/**
 * UUID version identifiers per RFC 4122 and RFC 9562.
 */
@Serializable
enum class UuidVersion(val versionNumber: Int, val label: String) {
    V4(4, "Random (v4)"),
    V7(7, "Time-ordered (v7)")
}

/**
 * A single generated UUID record.
 *
 * Supports both v4 (random) and v7 (time-ordered per RFC 9562) formats.
 * Provides multiple string representations for different use cases.
 */
@Serializable
data class UuidRecord(
    val version: UuidVersion,
    val bytes: ByteArray,
    val timestamp: Long? = null
) {
    init {
        require(bytes.size == 16) { "UUID must have exactly 16 bytes" }
    }

    /**
     * Standard 8-4-4-4-12 hyphenated format: "550e8400-e29b-41d4-a716-446655440000"
     */
    val standard: String
        get() = buildString {
            append(bytesToHex(bytes, 0, 4))
            append("-")
            append(bytesToHex(bytes, 4, 6))
            append("-")
            append(bytesToHex(bytes, 6, 8))
            append("-")
            append(bytesToHex(bytes, 8, 10))
            append("-")
            append(bytesToHex(bytes, 10, 16))
        }

    /** Compact format without hyphens: "550e8400e29b41d4a716446655440000" */
    val compact: String
        get() = bytesToHex(bytes, 0, 16)

    /** URN format: "urn:uuid:550e8400-e29b-41d4-a716-446655440000" */
    val urn: String
        get() = "urn:uuid:$standard"

    /** Microsoft-style braced format: "{550e8400-e29b-41d4-a716-446655440000}" */
    val braced: String
        get() = "{$standard}"

    /** The version nibble extracted from the UUID */
    val versionNibble: Int
        get() = (bytes[6].toInt() and 0xF0) shr 4

    /** The variant bits extracted from the UUID */
    val variantBits: Int
        get() = (bytes[8].toInt() and 0xC0) shr 6

    private fun bytesToHex(data: ByteArray, start: Int, end: Int): String =
        data.slice(start until end).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UuidRecord) return false
        return version == other.version && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
