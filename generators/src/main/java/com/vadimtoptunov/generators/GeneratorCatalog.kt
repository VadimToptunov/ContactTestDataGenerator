package com.vadimtoptunov.generators

import com.vadimtoptunov.generators.contacts.ContactGenerator
import com.vadimtoptunov.generators.finance.CardGenerator
import com.vadimtoptunov.generators.finance.IbanGenerator
import com.vadimtoptunov.generators.identity.TaxIdGenerator

/**
 * Single entry point that registers **every** generator in the library with the
 * [com.vadimtoptunov.generators.core.GeneratorRegistry].
 *
 * The DevData Factory UI calls [registerAllGenerators] once at startup and then
 * discovers all available tools purely through the registry — it never needs to
 * know the concrete generator classes.
 *
 * This ties together the module-specific registration groups:
 * - Contacts: [ContactGenerator]
 * - Finance: [CardGenerator], [IbanGenerator]
 * - Identity: [TaxIdGenerator]
 * - Network & Web: [NetworkWebGenerators] (IPv4/IPv6/MAC, UUID, JWT)
 * - Extended: [ExtendedGenerators] (address, barcode, crypto address, password)
 */
object GeneratorCatalog {

    @Volatile
    private var alreadyRegistered = false

    /**
     * Register all generators exactly once. Safe to call repeatedly (for example
     * from every Activity's `onCreate`) — subsequent calls are no-ops.
     */
    fun registerAllGenerators() {
        if (alreadyRegistered) return
        synchronized(this) {
            if (alreadyRegistered) return

            // Contacts
            ContactGenerator.registerAll()

            // Finance
            CardGenerator.registerAll()
            IbanGenerator.registerAll()

            // Identity
            TaxIdGenerator.registerAll()

            // Network & Web
            NetworkWebGenerators.registerAll()

            // Extended (location, barcode, crypto, security)
            ExtendedGenerators.registerAll()

            alreadyRegistered = true
        }
    }
}
