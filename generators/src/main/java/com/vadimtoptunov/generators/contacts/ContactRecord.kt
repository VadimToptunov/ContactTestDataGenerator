package com.vadimtoptunov.generators.contacts

/**
 * A single generated contact.
 *
 * Every field is nullable so a record faithfully represents what was actually
 * generated: a field excluded via [ContactFields] is stored as `null` rather
 * than an empty placeholder.
 */
data class ContactRecord(
    val fullName: String?,
    val phone: String?,
    val email: String?,
    val company: String?,
    val jobTitle: String?,
)

/**
 * Which fields the [ContactGenerator] should populate. All fields are included
 * by default; set any to `false` to leave it out of the generated records.
 */
data class ContactFields(
    val includeName: Boolean = true,
    val includePhone: Boolean = true,
    val includeEmail: Boolean = true,
    val includeCompany: Boolean = true,
    val includeJobTitle: Boolean = true,
)
