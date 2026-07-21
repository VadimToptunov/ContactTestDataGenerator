package com.vadimtoptunov.generators.network

import kotlinx.serialization.Serializable

/**
 * Classification of IPv4 address types based on RFC 5735 and IANA allocations.
 */
@Serializable
enum class IPv4AddressType(val label: String) {
    PRIVATE("Private"),
    PUBLIC("Public"),
    LOOPBACK("Loopback"),
    LINK_LOCAL("Link-Local"),
    MULTICAST("Multicast"),
    RESERVED("Reserved")
}

/**
 * A single generated IPv4 address record.
 *
 * Contains the raw octets and metadata about the address type.
 * Provides multiple computed representations for different use cases.
 */
@Serializable
data class IPv4Record(
    val octets: List<Int>,
    val addressType: IPv4AddressType,
    val cidrPrefix: Int? = null
) {
    init {
        require(octets.size == 4) { "IPv4 address must have exactly 4 octets" }
        require(octets.all { it in 0..255 }) { "Each octet must be 0-255" }
        cidrPrefix?.let {
            require(it in 0..32) { "CIDR prefix must be 0-32" }
        }
    }

    /** Dotted-decimal notation: "192.168.1.1" */
    val raw: String
        get() = octets.joinToString(".")

    /** CIDR notation: "192.168.1.1/24" or just raw if no prefix */
    val cidr: String
        get() = cidrPrefix?.let { "$raw/$it" } ?: raw

    /** 32-bit numeric representation */
    val numeric: Long
        get() = octets.foldIndexed(0L) { index, acc, octet ->
            acc or (octet.toLong() shl (24 - index * 8))
        }

    /** Binary representation with dots: "11000000.10101000.00000001.00000001" */
    val binary: String
        get() = octets.joinToString(".") {
            it.toString(2).padStart(8, '0')
        }

    /** Hex representation: "c0a80101" */
    val hex: String
        get() = octets.joinToString("") {
            it.toString(16).padStart(2, '0')
        }
}
