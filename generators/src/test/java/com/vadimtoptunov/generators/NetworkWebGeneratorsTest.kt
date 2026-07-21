package com.vadimtoptunov.generators

import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.network.IPv4Generator
import com.vadimtoptunov.generators.network.IPv6Generator
import com.vadimtoptunov.generators.network.MacAddressGenerator
import com.vadimtoptunov.generators.web.JwtGenerator
import com.vadimtoptunov.generators.web.UuidGenerator
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NetworkWebGeneratorsTest {

    @Before
    fun setUp() {
        GeneratorRegistry.clear()
    }

    @After
    fun tearDown() {
        GeneratorRegistry.clear()
    }

    @Test
    fun `registerAll registers all generators`() {
        NetworkWebGenerators.registerAll()

        val all = GeneratorRegistry.all
        assertTrue("Should have registered generators", all.isNotEmpty())
    }

    @Test
    fun `IPv4 generators are registered`() {
        NetworkWebGenerators.registerAll()

        // Check standard IPv4 types
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_private"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_public"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_loopback"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_link_local"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_multicast"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_reserved"))

        // Check CIDR variants
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_private_cidr"))
        assertNotNull(GeneratorRegistry.find("${IPv4Generator.ID}_public_cidr"))
    }

    @Test
    fun `IPv6 generators are registered`() {
        NetworkWebGenerators.registerAll()

        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_global_unicast"))
        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_link_local"))
        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_unique_local"))
        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_loopback"))
        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_ipv4_mapped"))
        assertNotNull(GeneratorRegistry.find("${IPv6Generator.ID}_documentation"))
    }

    @Test
    fun `MAC generators are registered`() {
        NetworkWebGenerators.registerAll()

        assertNotNull(GeneratorRegistry.find(MacAddressGenerator.ID))
        assertNotNull(GeneratorRegistry.find("${MacAddressGenerator.ID}_vendor"))
        assertNotNull(GeneratorRegistry.find("${MacAddressGenerator.ID}_local"))
        assertNotNull(GeneratorRegistry.find("${MacAddressGenerator.ID}_vmware"))
        assertNotNull(GeneratorRegistry.find("${MacAddressGenerator.ID}_docker"))
        assertNotNull(GeneratorRegistry.find("${MacAddressGenerator.ID}_qemu"))
    }

    @Test
    fun `UUID generators are registered`() {
        NetworkWebGenerators.registerAll()

        assertNotNull(GeneratorRegistry.find("${UuidGenerator.ID}_v4"))
        assertNotNull(GeneratorRegistry.find("${UuidGenerator.ID}_v7"))
    }

    @Test
    fun `JWT generators are registered`() {
        NetworkWebGenerators.registerAll()

        assertNotNull(GeneratorRegistry.find("${JwtGenerator.ID}_hs256"))
        assertNotNull(GeneratorRegistry.find("${JwtGenerator.ID}_rs256"))
        assertNotNull(GeneratorRegistry.find("${JwtGenerator.ID}_none"))
        assertNotNull(GeneratorRegistry.find("${JwtGenerator.ID}_short"))
        assertNotNull(GeneratorRegistry.find("${JwtGenerator.ID}_long"))
    }

    @Test
    fun `all generators can generate records`() {
        NetworkWebGenerators.registerAll()

        GeneratorRegistry.all.forEach { generator ->
            val record = generator.generate()
            assertNotNull("${generator.name} should generate a record", record)
        }
    }
}
