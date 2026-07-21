package com.vadimtoptunov.generators.crypto

import kotlinx.serialization.Serializable

/**
 * A generated cryptocurrency address.
 *
 * IMPORTANT: These are random strings that match address format patterns.
 * They are NOT derived from real private keys and should NOT be used
 * for actual blockchain operations. Safe for UI testing only.
 *
 * @property network The cryptocurrency network
 * @property address The generated address string
 * @property isChecksummed Whether EIP-55 checksum was applied (Ethereum only)
 */
@Serializable
data class CryptoAddressRecord(
    val network: CryptoNetwork,
    val address: String,
    val isChecksummed: Boolean = false
) {
    init {
        require(address.isNotEmpty()) { "Address cannot be empty" }
    }

    /**
     * Network prefix for display: "BTC: 1ABC...", "ETH: 0xABC..."
     */
    val prefixed: String
        get() = "${network.symbol}: $address"

    /**
     * URI format for wallet apps and QR codes.
     * - Bitcoin: bitcoin:address
     * - Ethereum: ethereum:address
     * - Tron: tron:address
     */
    val uri: String
        get() = when (network) {
            CryptoNetwork.BITCOIN_LEGACY,
            CryptoNetwork.BITCOIN_SEGWIT,
            CryptoNetwork.BITCOIN_TAPROOT -> "bitcoin:$address"
            CryptoNetwork.ETHEREUM -> "ethereum:$address"
            CryptoNetwork.TRON -> "tron:$address"
            CryptoNetwork.LITECOIN_LEGACY,
            CryptoNetwork.LITECOIN_SEGWIT -> "litecoin:$address"
            CryptoNetwork.DOGECOIN -> "dogecoin:$address"
            CryptoNetwork.RIPPLE -> "ripple:$address"
            CryptoNetwork.SOLANA -> "solana:$address"
        }

    /**
     * Shortened address for display: "1ABC...XYZ9"
     */
    val shortened: String
        get() {
            val prefixLen = network.addressPrefix.length.coerceAtLeast(4)
            return if (address.length > prefixLen + 8) {
                "${address.take(prefixLen + 4)}...${address.takeLast(4)}"
            } else {
                address
            }
        }

    /**
     * Address type description.
     */
    val typeDescription: String
        get() = when (network) {
            CryptoNetwork.BITCOIN_LEGACY -> "Legacy (P2PKH)"
            CryptoNetwork.BITCOIN_SEGWIT -> "Native SegWit (Bech32)"
            CryptoNetwork.BITCOIN_TAPROOT -> "Taproot (P2TR)"
            CryptoNetwork.ETHEREUM -> if (isChecksummed) "ERC-20 Compatible (Checksummed)" else "ERC-20 Compatible"
            CryptoNetwork.TRON -> "TRC-20 Compatible"
            CryptoNetwork.LITECOIN_LEGACY -> "Legacy"
            CryptoNetwork.LITECOIN_SEGWIT -> "SegWit"
            CryptoNetwork.DOGECOIN -> "Standard"
            CryptoNetwork.RIPPLE -> "Classic"
            CryptoNetwork.SOLANA -> "SPL Compatible"
        }

    /**
     * Common token compatibility info.
     */
    val tokenCompatibility: String?
        get() = when (network) {
            CryptoNetwork.ETHEREUM -> "USDT (ERC-20), USDC, DAI, etc."
            CryptoNetwork.TRON -> "USDT (TRC-20), etc."
            CryptoNetwork.SOLANA -> "USDC, etc."
            else -> null
        }
}
