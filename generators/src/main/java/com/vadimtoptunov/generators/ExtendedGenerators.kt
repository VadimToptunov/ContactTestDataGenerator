package com.vadimtoptunov.generators

import com.vadimtoptunov.generators.barcode.BarcodeGenerator
import com.vadimtoptunov.generators.crypto.CryptoAddressGenerator
import com.vadimtoptunov.generators.location.AddressGenerator
import com.vadimtoptunov.generators.security.PasswordGenerator

/**
 * Central registration for Phase 5 extended generators.
 *
 * Provides:
 * - **Location**: Address generation for ~200 countries
 * - **Barcode**: EAN-13, UPC-A, ISBN-13 with valid check digits
 * - **Crypto**: Bitcoin, Ethereum, Tron address format generation
 * - **Security**: Configurable password generation with entropy estimation
 *
 * Usage:
 * ```
 * // Register all extended generators at app startup
 * ExtendedGenerators.registerAll()
 *
 * // Then use via GeneratorRegistry
 * val generator = GeneratorRegistry.find("password_generator_strong")
 * ```
 *
 * This complements the existing generators:
 * - Finance: CardGenerator, IbanGenerator
 * - Identity: SyntheticIdentityGenerator, TaxIdGenerator
 * - Network: IPv4Generator, IPv6Generator, MacAddressGenerator
 * - Web: UuidGenerator, JwtGenerator
 */
object ExtendedGenerators {

    /**
     * Register all Phase 5 generators with the GeneratorRegistry.
     *
     * Should be called once at application startup, typically after
     * registering other generator modules.
     */
    fun registerAll() {
        // Location — addresses for major countries
        AddressGenerator.registerAll()

        // Barcode — EAN-13, UPC-A, ISBN-13
        BarcodeGenerator.registerAll()

        // Crypto — Bitcoin, Ethereum, Tron addresses
        CryptoAddressGenerator.registerAll()

        // Security — passwords with various policies
        PasswordGenerator.registerAll()
    }

    /**
     * Get summary statistics about available generators.
     */
    fun stats(): Stats {
        return Stats(
            addressCountries = AddressGenerator.supportedCountries.size,
            barcodeTypes = 3,  // EAN-13, UPC-A, ISBN-13
            cryptoNetworks = 10, // BTC (3 types), ETH, TRX, LTC (2), DOGE, XRP, SOL
            passwordPolicies = 6 // weak, standard, strong, passphrase, pin4, pin6
        )
    }

    data class Stats(
        val addressCountries: Int,
        val barcodeTypes: Int,
        val cryptoNetworks: Int,
        val passwordPolicies: Int
    ) {
        override fun toString(): String = buildString {
            appendLine("ExtendedGenerators Statistics:")
            appendLine("  • Address Generator: $addressCountries countries")
            appendLine("  • Barcode Generator: $barcodeTypes types")
            appendLine("  • Crypto Generator: $cryptoNetworks networks")
            appendLine("  • Password Generator: $passwordPolicies policies")
        }
    }
}
