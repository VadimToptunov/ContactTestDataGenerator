package com.vadimtoptunov.contacttestdatagenerator.devtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests category resolution from generator registration ids — the mapping that
 * groups the flat registry into the labelled sections shown in the tool picker.
 */
class DevToolCategoryTest {

    @Test
    fun `known id prefixes map to the expected category`() {
        val cases = mapOf(
            "contact_generator" to DevToolCategory.CONTACTS,
            "card_generator_visa" to DevToolCategory.FINANCE,
            "iban_generator_de" to DevToolCategory.FINANCE,
            "taxid_us" to DevToolCategory.IDENTITY,
            "ipv4_generator" to DevToolCategory.NETWORK,
            "ipv6_generator" to DevToolCategory.NETWORK,
            "mac_generator" to DevToolCategory.NETWORK,
            "uuid_generator" to DevToolCategory.WEB,
            "jwt_generator_hs256" to DevToolCategory.WEB,
            "crypto_address_generator_bitcoin_legacy" to DevToolCategory.CRYPTO,
            "password_generator_strong" to DevToolCategory.SECURITY,
            "address_generator_us" to DevToolCategory.LOCATION,
            "barcode_generator_ean13" to DevToolCategory.BARCODE,
        )
        cases.forEach { (id, expected) ->
            assertEquals("id=$id", expected, DevToolCategory.forGeneratorId(id))
        }
    }

    @Test
    fun `unknown id falls back to OTHER`() {
        assertEquals(DevToolCategory.OTHER, DevToolCategory.forGeneratorId("totally_unknown_generator"))
        assertEquals(DevToolCategory.OTHER, DevToolCategory.forGeneratorId(""))
    }

    @Test
    fun `only finance and identity require premium`() {
        assertTrue(DevToolCategory.FINANCE.requiresPremium)
        assertTrue(DevToolCategory.IDENTITY.requiresPremium)

        val free = listOf(
            DevToolCategory.CONTACTS, DevToolCategory.NETWORK, DevToolCategory.WEB,
            DevToolCategory.CRYPTO, DevToolCategory.SECURITY, DevToolCategory.LOCATION,
            DevToolCategory.BARCODE, DevToolCategory.OTHER,
        )
        free.forEach { assertFalse(it.name, it.requiresPremium) }
    }

    @Test
    fun `matchesGeneratorId is prefix based`() {
        assertTrue(DevToolCategory.FINANCE.matchesGeneratorId("card_generator_amex"))
        assertFalse(DevToolCategory.FINANCE.matchesGeneratorId("uuid_generator"))
        // OTHER has no prefixes and never matches directly.
        assertFalse(DevToolCategory.OTHER.matchesGeneratorId("anything"))
    }
}
