package com.vadimtoptunov.contacttestdatagenerator

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Template for contact generation
 */
@Serializable
data class ContactTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contactCount: Int,
    val fieldSettings: ContactFieldSettings,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createDefault(name: String, count: Int): ContactTemplate {
            return ContactTemplate(
                name = name,
                contactCount = count,
                fieldSettings = ContactFieldSettings()
            )
        }
    }
}

