package com.vadimtoptunov.generators.crypto

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import com.vadimtoptunov.generators.core.SeededRandom

/**
 * Generates cryptocurrency addresses that match real format patterns.
 *
 * IMPORTANT: These are random strings that LOOK LIKE real addresses
 * but are NOT derived from actual private keys. They will NOT pass
 * on-chain validation and should ONLY be used for:
 * - UI testing (form validation, display formatting)
 * - Demo data generation
 * - QA testing of address parsing
 *
 * DO NOT use these addresses for actual blockchain operations.
 *
 * Supported networks:
 * - Bitcoin: Legacy (P2PKH), SegWit (Bech32), Taproot
 * - Ethereum: 0x addresses (ERC-20 compatible)
 * - Tron: T addresses (TRC-20 compatible)
 * - Litecoin, Dogecoin, Ripple, Solana
 *
 * Example:
 * ```
 * val generator = CryptoAddressGenerator(CryptoNetwork.ETHEREUM)
 * val record = generator.generate()
 * println(record.address)  // 0x742d35Cc6634C0532925a3b844Bc9e7595f...
 * ```
 */
class CryptoAddressGenerator(
    private val network: CryptoNetwork = CryptoNetwork.ETHEREUM,
    private val seed: Long? = null
) : DataGenerator<CryptoAddressRecord> {

    private val random = SeededRandom(seed)

    override val name = "${network.label} Address Generator"
    override val description = "Generates ${network.symbol} addresses for UI testing"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): CryptoAddressRecord {
        val address = when (network) {
            CryptoNetwork.BITCOIN_LEGACY -> generateBitcoinLegacy()
            CryptoNetwork.BITCOIN_SEGWIT -> generateBitcoinSegwit()
            CryptoNetwork.BITCOIN_TAPROOT -> generateBitcoinTaproot()
            CryptoNetwork.ETHEREUM -> generateEthereum()
            CryptoNetwork.TRON -> generateTron()
            CryptoNetwork.LITECOIN_LEGACY -> generateLitecoinLegacy()
            CryptoNetwork.LITECOIN_SEGWIT -> generateLitecoinSegwit()
            CryptoNetwork.DOGECOIN -> generateDogecoin()
            CryptoNetwork.RIPPLE -> generateRipple()
            CryptoNetwork.SOLANA -> generateSolana()
        }
        return CryptoAddressRecord(
            network = network,
            address = address,
            isChecksummed = network == CryptoNetwork.ETHEREUM
        )
    }

    override fun generate(seed: Long): CryptoAddressRecord {
        return CryptoAddressGenerator(network, seed).generate()
    }

    /**
     * Bitcoin Legacy (P2PKH): starts with '1', 25-34 chars, Base58Check.
     */
    private fun generateBitcoinLegacy(): String = with(random) {
        val length = (25..34).randomElement()
        val body = (1 until length).map { BASE58_CHARS.randomElement() }.joinToString("")
        "1$body"
    }

    /**
     * Bitcoin SegWit (Bech32): starts with 'bc1q', 42 chars.
     * Uses lowercase Bech32 alphabet.
     */
    private fun generateBitcoinSegwit(): String = with(random) {
        val body = (1..38).map { BECH32_CHARS.randomElement() }.joinToString("")
        "bc1q$body"
    }

    /**
     * Bitcoin Taproot (P2TR): starts with 'bc1p', 62 chars.
     */
    private fun generateBitcoinTaproot(): String = with(random) {
        val body = (1..58).map { BECH32_CHARS.randomElement() }.joinToString("")
        "bc1p$body"
    }

    /**
     * Ethereum: 0x + 40 hex chars, with EIP-55 mixed-case checksum.
     */
    private fun generateEthereum(): String = with(random) {
        val hexChars = "0123456789abcdef"
        val body = (1..40).map { hexChars.randomElement() }.joinToString("")
        "0x${applyEip55Checksum(body)}"
    }

    /**
     * Tron: starts with 'T', 34 chars, Base58.
     */
    private fun generateTron(): String = with(random) {
        val body = (1..33).map { BASE58_CHARS.randomElement() }.joinToString("")
        "T$body"
    }

    /**
     * Litecoin Legacy: starts with 'L' or 'M', 34 chars.
     */
    private fun generateLitecoinLegacy(): String = with(random) {
        val prefix = listOf("L", "M").randomElement()
        val body = (1..33).map { BASE58_CHARS.randomElement() }.joinToString("")
        "$prefix$body"
    }

    /**
     * Litecoin SegWit: starts with 'ltc1q', 43 chars.
     */
    private fun generateLitecoinSegwit(): String = with(random) {
        val body = (1..38).map { BECH32_CHARS.randomElement() }.joinToString("")
        "ltc1q$body"
    }

    /**
     * Dogecoin: starts with 'D', 34 chars.
     */
    private fun generateDogecoin(): String = with(random) {
        val body = (1..33).map { BASE58_CHARS.randomElement() }.joinToString("")
        "D$body"
    }

    /**
     * Ripple: starts with 'r', 25-35 chars.
     */
    private fun generateRipple(): String = with(random) {
        val length = (25..35).randomElement()
        val body = (1 until length).map { BASE58_CHARS.randomElement() }.joinToString("")
        "r$body"
    }

    /**
     * Solana: 32-44 chars, Base58, no prefix.
     */
    private fun generateSolana(): String = with(random) {
        val length = (32..44).randomElement()
        (1..length).map { BASE58_CHARS.randomElement() }.joinToString("")
    }

    /**
     * Apply EIP-55 mixed-case checksum to Ethereum address.
     *
     * This is a simplified version that creates visually similar output
     * without computing actual Keccak-256 hash. For UI testing only.
     */
    private fun applyEip55Checksum(hex: String): String = with(random) {
        // Real EIP-55 uses Keccak-256(lowercase_address) to determine case
        // This simplified version randomly applies case for demo purposes
        hex.map { c ->
            if (c.isLetter() && nextBoolean()) c.uppercaseChar() else c
        }.joinToString("")
    }

    override fun serialize(record: CryptoAddressRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.network.name},${record.network.symbol},${record.address}"
        OutputFormat.JSON -> buildString {
            append("""{"network":"${record.network.name}"""")
            append(""","symbol":"${record.network.symbol}"""")
            append(""","address":"${record.address}"""")
            append(""","uri":"${record.uri}"""")
            record.tokenCompatibility?.let { append(""","tokens":"$it"""") }
            append("}")
        }
        OutputFormat.TXT -> record.prefixed
        else -> throw UnsupportedOperationException("$format not supported by CryptoAddressGenerator")
    }

    override fun serializeBatch(records: List<CryptoAddressRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("network,symbol,address")
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
        const val ID = "crypto_address_generator"

        // Base58 alphabet (Bitcoin/Litecoin/Ripple style, excludes 0/O/I/l)
        private const val BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

        // Bech32 alphabet (lowercase only, excludes 1/b/i/o)
        private const val BECH32_CHARS = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

        fun registerAll() {
            // Register main networks
            val mainNetworks = listOf(
                CryptoNetwork.BITCOIN_LEGACY,
                CryptoNetwork.BITCOIN_SEGWIT,
                CryptoNetwork.ETHEREUM,
                CryptoNetwork.TRON
            )
            mainNetworks.forEach { network ->
                GeneratorRegistry.register(
                    "${ID}_${network.name.lowercase()}",
                    CryptoAddressGenerator(network)
                )
            }

            // Register all networks under 'all' variants
            CryptoNetwork.entries.forEach { network ->
                GeneratorRegistry.register(
                    "${ID}_all_${network.name.lowercase()}",
                    CryptoAddressGenerator(network)
                )
            }
        }
    }
}
