package com.vadimtoptunov.generators

import com.vadimtoptunov.generators.network.IPv4Generator
import com.vadimtoptunov.generators.network.IPv6Generator
import com.vadimtoptunov.generators.network.MacAddressGenerator
import com.vadimtoptunov.generators.web.JwtGenerator
import com.vadimtoptunov.generators.web.UuidGenerator

/**
 * Central registration for all network and web generators.
 *
 * Network generators:
 *   - IPv4Generator: Private, public, loopback, link-local, multicast, reserved addresses
 *   - IPv6Generator: Global unicast, link-local, unique local, loopback, IPv4-mapped
 *   - MacAddressGenerator: Random or vendor-specific MAC addresses
 *
 * Web generators:
 *   - UuidGenerator: v4 (random) and v7 (time-ordered) UUIDs
 *   - JwtGenerator: HS256, RS256, and "none" algorithm tokens
 *
 * Usage:
 *   NetworkWebGenerators.registerAll()
 */
object NetworkWebGenerators {

    /**
     * Register all network and web generators with the central registry.
     * Call this during app initialization.
     */
    fun registerAll() {
        // Network generators
        IPv4Generator.registerAll()
        IPv6Generator.registerAll()
        MacAddressGenerator.registerAll()

        // Web generators
        UuidGenerator.registerAll()
        JwtGenerator.registerAll()
    }
}
