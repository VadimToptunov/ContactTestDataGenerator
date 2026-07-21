package com.vadimtoptunov.generators.network

import kotlinx.serialization.Serializable

/**
 * Known OUI (Organizationally Unique Identifier) prefixes for common vendors.
 * Useful for generating realistic MAC addresses that identify as specific devices.
 */
@Serializable
enum class OuiVendor(val label: String, val prefix: List<Int>) {
    VMWARE("VMware", listOf(0x00, 0x50, 0x56)),
    VMWARE_WORKSTATION("VMware Workstation", listOf(0x00, 0x0C, 0x29)),
    GOOGLE("Google", listOf(0x3C, 0x5A, 0xB4)),
    XEN("Xen", listOf(0x00, 0x16, 0x3E)),
    AMAZON_EC2("Amazon EC2", listOf(0x02, 0x00, 0x00)),
    QEMU("QEMU", listOf(0x52, 0x54, 0x00)),
    HYPER_V("Hyper-V", listOf(0x00, 0x15, 0x5D)),
    VIRTUALBOX("VirtualBox", listOf(0x08, 0x00, 0x27)),
    DOCKER("Docker", listOf(0x02, 0x42, 0xAC)),
    APPLE("Apple", listOf(0x00, 0x03, 0x93)),
    INTEL("Intel", listOf(0x00, 0x1E, 0x67)),
    CISCO("Cisco", listOf(0x00, 0x1B, 0x54)),
    DELL("Dell", listOf(0x00, 0x14, 0x22)),
    HP("HP", listOf(0x00, 0x1E, 0x0B)),
    SAMSUNG("Samsung", listOf(0x00, 0x15, 0x99)),
    MICROSOFT("Microsoft", listOf(0x00, 0x50, 0xF2)),
    HUAWEI("Huawei", listOf(0x00, 0x18, 0x82))
}

/**
 * A single generated MAC address record.
 *
 * Contains 6 octets and metadata about the address characteristics.
 * Provides multiple format representations for different use cases.
 */
@Serializable
data class MacAddressRecord(
    val octets: List<Int>,
    val oui: OuiVendor? = null
) {
    init {
        require(octets.size == 6) { "MAC address must have exactly 6 octets" }
        require(octets.all { it in 0..255 }) { "Each octet must be 0-255" }
    }

    /**
     * Unicast address: bit 0 of first octet is 0
     * Multicast address: bit 0 of first octet is 1
     */
    val isUnicast: Boolean
        get() = (octets[0] and 0x01) == 0

    val isMulticast: Boolean
        get() = !isUnicast

    /**
     * Locally Administered Address (LAA): bit 1 of first octet is 1
     * Universally Administered Address (UAA): bit 1 of first octet is 0
     */
    val isLocal: Boolean
        get() = (octets[0] and 0x02) != 0

    val isUniversal: Boolean
        get() = !isLocal

    /** Colon-separated format: "00:50:56:C0:00:08" (common on Linux) */
    val colonSeparated: String
        get() = octets.joinToString(":") { it.toString(16).padStart(2, '0').uppercase() }

    /** Hyphen-separated format: "00-50-56-C0-00-08" (common on Windows) */
    val hyphenSeparated: String
        get() = octets.joinToString("-") { it.toString(16).padStart(2, '0').uppercase() }

    /** Dot-separated format: "0050.56C0.0008" (Cisco style, pairs) */
    val dotSeparated: String
        get() = octets.chunked(2).joinToString(".") { pair ->
            pair.joinToString("") { it.toString(16).padStart(2, '0').lowercase() }
        }

    /** Raw hex without separators: "005056C00008" */
    val raw: String
        get() = octets.joinToString("") { it.toString(16).padStart(2, '0').uppercase() }

    /** Lowercase colon format: "00:50:56:c0:00:08" */
    val lowercase: String
        get() = octets.joinToString(":") { it.toString(16).padStart(2, '0') }

    /** Vendor name if using a known OUI */
    val vendorName: String?
        get() = oui?.label
}
