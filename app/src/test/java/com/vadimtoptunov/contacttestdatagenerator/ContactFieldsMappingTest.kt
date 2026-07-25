package com.vadimtoptunov.contacttestdatagenerator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the mapping from the app's persisted [ContactFieldSettings] onto the
 * library's ContactFields — the bridge that lets the contacts UI drive the
 * shared ContactGenerator.
 */
class ContactFieldsMappingTest {

    @Test
    fun `default settings map all fields on`() {
        val fields = ContactFieldSettings().toContactFields()
        assertTrue(fields.includeName)
        assertTrue(fields.includePhone)
        assertTrue(fields.includeEmail)
        assertTrue(fields.includeCompany)
        assertTrue(fields.includeJobTitle)
    }

    @Test
    fun `each flag is carried across independently`() {
        val settings = ContactFieldSettings(
            includeName = true,
            includePhone = false,
            includeEmail = true,
            includeCompany = false,
            includeJobTitle = true,
        )
        val fields = settings.toContactFields()
        assertTrue(fields.includeName)
        assertFalse(fields.includePhone)
        assertTrue(fields.includeEmail)
        assertFalse(fields.includeCompany)
        assertTrue(fields.includeJobTitle)
    }

    @Test
    fun `all fields off maps through`() {
        val settings = ContactFieldSettings(
            includeName = false,
            includePhone = false,
            includeEmail = false,
            includeCompany = false,
            includeJobTitle = false,
        )
        val fields = settings.toContactFields()
        assertFalse(fields.includeName)
        assertFalse(fields.includePhone)
        assertFalse(fields.includeEmail)
        assertFalse(fields.includeCompany)
        assertFalse(fields.includeJobTitle)
    }
}
