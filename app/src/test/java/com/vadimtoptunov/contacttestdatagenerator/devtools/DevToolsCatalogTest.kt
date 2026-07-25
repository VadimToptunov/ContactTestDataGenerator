package com.vadimtoptunov.contacttestdatagenerator.devtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the registry-driven catalog groups tools sensibly for the picker:
 * every tool is registered, grouped under the right category, ordered by the
 * category enum, and free of duplicate display names within a category.
 */
class DevToolsCatalogTest {

    private val byCategory = DevToolsCatalog.toolsByCategory()

    @Test
    fun `contacts generator is present under the Contacts category`() {
        val contacts = byCategory[DevToolCategory.CONTACTS].orEmpty()
        assertTrue("contacts category should not be empty", contacts.isNotEmpty())
        assertTrue(contacts.any { it.id == "contact_generator" })
    }

    @Test
    fun `several categories are populated from the registry`() {
        // The full library registers finance, network, web, crypto, etc.
        assertTrue(byCategory.containsKey(DevToolCategory.FINANCE))
        assertTrue(byCategory.containsKey(DevToolCategory.NETWORK))
        assertTrue(byCategory.containsKey(DevToolCategory.WEB))
    }

    @Test
    fun `categories are ordered by enum ordinal`() {
        val ordinals = byCategory.keys.map { it.ordinal }
        assertEquals(ordinals.sorted(), ordinals)
    }

    @Test
    fun `no duplicate display names within a category`() {
        byCategory.forEach { (category, tools) ->
            val names = tools.map { it.displayName }
            assertEquals("duplicate display name in ${category.name}", names.toSet().size, names.size)
        }
    }

    @Test
    fun `every tool reports at least one supported format`() {
        byCategory.values.flatten().forEach { tool ->
            assertTrue(tool.id, tool.supportedFormats.isNotEmpty())
        }
    }

    @Test
    fun `each tool is filed under the category its id resolves to`() {
        byCategory.forEach { (category, tools) ->
            tools.forEach { tool ->
                assertEquals(category, DevToolCategory.forGeneratorId(tool.id))
            }
        }
    }
}
