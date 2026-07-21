package com.vadimtoptunov.generators.network

import kotlinx.serialization.Serializable

/**
 * Classification of IPv6 address types based on IANA allocations.
 */
@Serializable
enum class IPv6AddressType(val label: String, val prefix: String) {
    GLOBAL_UNICAST("Global Unicast", "2000::/3"),
    LINK_LOCAL("Link-Local", "fe80::/10"),
    UNIQUE_LOCAL("Unique Local", "fc00::/7"),
    LOOPBACK("Loopback", "::1/128"),
    IPV4_MAPPED("IPv4-Mapped", "::ffff:0:0/96"),
    DOCUMENTATION("Documentation", "2001:db8::/32")
}

/**
 * A single generated IPv6 address record.
 *
 * Contains 8 groups of 16-bit values and provides multiple format representations.
 * Implements RFC 5952 compression rules for canonical format.
 */
@Serializable
data class IPv6Record(
    val groups: List<Int>,
    val addressType: IPv6AddressType
) {
    init {
        require(groups.size == 8) { "IPv6 address must have exactly 8 groups" }
        require(groups.all { it in 0..0xFFFF }) { "Each group must be 0x0000-0xFFFF" }
    }

    /** Full expanded format: "2001:0db8:0000:0000:0000:0000:0000:0001" */
    val full: String
        get() = groups.joinToString(":") { it.toString(16).padStart(4, '0') }

    /** Compressed format with :: notation per RFC 5952 */
    val compressed: String
        get() = compressIPv6(groups)

    /** Mixed notation for IPv4-mapped addresses: "::ffff:192.168.1.1" */
    val mixed: String
        get() = if (addressType == IPv6AddressType.IPV4_MAPPED) {
            val ipv4Part = "${groups[6] shr 8}.${groups[6] and 0xFF}.${groups[7] shr 8}.${groups[7] and 0xFF}"
            "::ffff:$ipv4Part"
        } else {
            compressed
        }

    /** Hex string without colons */
    val hex: String
        get() = groups.joinToString("") { it.toString(16).padStart(4, '0') }

    companion object {
        /**
         * Compress IPv6 address following RFC 5952 rules:
         * - Find the longest run of consecutive zero groups
         * - Replace with :: (only once, prefer leftmost if tie)
         * - Remove leading zeros from each group
         */
        internal fun compressIPv6(groups: List<Int>): String {
            // Find longest run of consecutive zeros
            var longestStart = -1
            var longestLen = 0
            var currentStart = -1
            var currentLen = 0

            for (i in groups.indices) {
                if (groups[i] == 0) {
                    if (currentStart == -1) {
                        currentStart = i
                        currentLen = 1
                    } else {
                        currentLen++
                    }
                } else {
                    if (currentLen > longestLen) {
                        longestStart = currentStart
                        longestLen = currentLen
                    }
                    currentStart = -1
                    currentLen = 0
                }
            }
            // Check final run
            if (currentLen > longestLen) {
                longestStart = currentStart
                longestLen = currentLen
            }

            // Only compress if run is at least 2 groups
            if (longestLen < 2) {
                return groups.joinToString(":") { it.toString(16) }
            }

            val before = groups.subList(0, longestStart)
                .joinToString(":") { it.toString(16) }
            val after = groups.subList(longestStart + longestLen, 8)
                .joinToString(":") { it.toString(16) }

            return when {
                before.isEmpty() && after.isEmpty() -> "::"
                before.isEmpty() -> "::$after"
                after.isEmpty() -> "$before::"
                else -> "$before::$after"
            }
        }
    }
}
