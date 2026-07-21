package com.vadimtoptunov.generators.network

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlin.random.Random

/**
 * Generates random MAC addresses for network testing.
 *
 * Use cases:
 *   - QA: Test MAC validation in network management tools
 *   - Dev: Seed test databases with realistic MAC data
 *   - Security: Generate test data for NAC systems
 *   - Virtualization: Create MAC addresses for VMs
 *
 * Supports known OUI prefixes for common vendors (VMware, QEMU, Docker, etc.)
 * and can generate either Locally Administered or Universal addresses.
 */
class MacAddressGenerator(
    private val useKnownOui: Boolean = false,
    private val forceLocal: Boolean = false,
    private val preferredVendor: OuiVendor? = null
) : DataGenerator<MacAddressRecord> {

    override val name = "MAC Address Generator"
    override val description = buildString {
        append("Generates MAC addresses")
        if (useKnownOui) append(" with known vendor OUIs")
        if (forceLocal) append(" (locally administered)")
    }
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): MacAddressRecord {
        val (octets, oui) = if (useKnownOui) {
            generateWithOui()
        } else {
            generateRandom()
        }

        // Apply local/universal flag modification if needed
        val finalOctets = if (forceLocal) {
            // Set bit 1 of first octet to mark as locally administered
            // Also ensure unicast (clear bit 0)
            val firstOctet = (octets[0] or 0x02) and 0xFE
            listOf(firstOctet) + octets.drop(1)
        } else {
            octets
        }

        return MacAddressRecord(octets = finalOctets, oui = oui)
    }

    private fun generateWithOui(): Pair<List<Int>, OuiVendor> {
        val vendor = preferredVendor ?: OuiVendor.entries.random()
        val prefix = vendor.prefix
        val suffix = (1..3).map { Random.nextInt(0, 256) }
        return (prefix + suffix) to vendor
    }

    private fun generateRandom(): Pair<List<Int>, OuiVendor?> {
        // Generate random first octet, ensuring unicast (bit 0 = 0)
        val firstOctet = Random.nextInt(0, 256) and 0xFE
        val rest = (1..5).map { Random.nextInt(0, 256) }
        return (listOf(firstOctet) + rest) to null
    }

    override fun serialize(record: MacAddressRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> buildString {
            append(record.colonSeparated)
            append(",")
            append(record.vendorName ?: "Unknown")
            append(",")
            append(if (record.isUnicast) "unicast" else "multicast")
            append(",")
            append(if (record.isLocal) "local" else "universal")
        }
        OutputFormat.JSON -> buildString {
            append("{")
            append("\"colon\":\"${record.colonSeparated}\",")
            append("\"hyphen\":\"${record.hyphenSeparated}\",")
            append("\"cisco\":\"${record.dotSeparated}\",")
            append("\"raw\":\"${record.raw}\",")
            record.vendorName?.let { append("\"vendor\":\"$it\",") }
            append("\"unicast\":${record.isUnicast},")
            append("\"local\":${record.isLocal}")
            append("}")
        }
        OutputFormat.TXT -> buildString {
            append(record.colonSeparated)
            record.vendorName?.let { append(" ($it)") }
            if (record.isLocal) append(" [LAA]")
        }
        else -> throw UnsupportedOperationException("$format not supported by MacAddressGenerator")
    }

    override fun serializeBatch(records: List<MacAddressRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("address,vendor,type,admin")
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
        const val ID = "mac_generator"

        fun registerAll() {
            // Basic random generator
            GeneratorRegistry.register(ID, MacAddressGenerator())

            // With known OUI prefixes
            GeneratorRegistry.register(
                "${ID}_vendor",
                MacAddressGenerator(useKnownOui = true)
            )

            // Locally administered (for VMs, containers)
            GeneratorRegistry.register(
                "${ID}_local",
                MacAddressGenerator(forceLocal = true)
            )

            // Specific vendor generators for common use cases
            GeneratorRegistry.register(
                "${ID}_vmware",
                MacAddressGenerator(useKnownOui = true, preferredVendor = OuiVendor.VMWARE)
            )
            GeneratorRegistry.register(
                "${ID}_docker",
                MacAddressGenerator(useKnownOui = true, preferredVendor = OuiVendor.DOCKER)
            )
            GeneratorRegistry.register(
                "${ID}_qemu",
                MacAddressGenerator(useKnownOui = true, preferredVendor = OuiVendor.QEMU)
            )
        }
    }
}
