package com.vadimtoptunov.generators.crypto

import kotlinx.serialization.Serializable

/**
 * Cryptocurrency networks supported by [CryptoAddressGenerator].
 *
 * @property label Human-readable network name
 * @property symbol Currency symbol (BTC, ETH, TRX)
 * @property addressPrefix Expected address prefix
 * @property addressLength Typical address length (varies by network)
 */
@Serializable
enum class CryptoNetwork(
    val label: String,
    val symbol: String,
    val addressPrefix: String,
    val addressLength: Int
) {
    /**
     * Bitcoin Legacy (P2PKH).
     * Addresses start with '1', 25-34 characters.
     * Base58Check encoding.
     */
    BITCOIN_LEGACY(
        label = "Bitcoin (P2PKH)",
        symbol = "BTC",
        addressPrefix = "1",
        addressLength = 34
    ),

    /**
     * Bitcoin Native SegWit (Bech32).
     * Addresses start with 'bc1q', 42-62 characters.
     * Bech32 encoding, lowercase only.
     */
    BITCOIN_SEGWIT(
        label = "Bitcoin (Bech32)",
        symbol = "BTC",
        addressPrefix = "bc1q",
        addressLength = 42
    ),

    /**
     * Bitcoin Taproot (P2TR).
     * Addresses start with 'bc1p', 62 characters.
     * Bech32m encoding.
     */
    BITCOIN_TAPROOT(
        label = "Bitcoin (Taproot)",
        symbol = "BTC",
        addressPrefix = "bc1p",
        addressLength = 62
    ),

    /**
     * Ethereum (and ERC-20 tokens like USDT, USDC).
     * Addresses start with '0x', 42 characters (0x + 40 hex).
     * Optional EIP-55 checksum (mixed case).
     */
    ETHEREUM(
        label = "Ethereum",
        symbol = "ETH",
        addressPrefix = "0x",
        addressLength = 42
    ),

    /**
     * Tron (and TRC-20 tokens like USDT).
     * Addresses start with 'T', 34 characters.
     * Base58Check encoding.
     */
    TRON(
        label = "Tron",
        symbol = "TRX",
        addressPrefix = "T",
        addressLength = 34
    ),

    /**
     * Litecoin Legacy (P2PKH).
     * Addresses start with 'L', 34 characters.
     */
    LITECOIN_LEGACY(
        label = "Litecoin (P2PKH)",
        symbol = "LTC",
        addressPrefix = "L",
        addressLength = 34
    ),

    /**
     * Litecoin SegWit (Bech32).
     * Addresses start with 'ltc1q'.
     */
    LITECOIN_SEGWIT(
        label = "Litecoin (Bech32)",
        symbol = "LTC",
        addressPrefix = "ltc1q",
        addressLength = 43
    ),

    /**
     * Dogecoin.
     * Addresses start with 'D', 34 characters.
     */
    DOGECOIN(
        label = "Dogecoin",
        symbol = "DOGE",
        addressPrefix = "D",
        addressLength = 34
    ),

    /**
     * Ripple (XRP).
     * Addresses start with 'r', 25-35 characters.
     */
    RIPPLE(
        label = "Ripple",
        symbol = "XRP",
        addressPrefix = "r",
        addressLength = 34
    ),

    /**
     * Solana.
     * Addresses are 32-44 characters, Base58.
     */
    SOLANA(
        label = "Solana",
        symbol = "SOL",
        addressPrefix = "",
        addressLength = 44
    )
}
