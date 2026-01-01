package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Data class for contact generation settings
 */
@Serializable
data class ContactFieldSettings(
    val includeName: Boolean = true,
    val includePhone: Boolean = true,
    val includeEmail: Boolean = true,
    val includeCompany: Boolean = true,
    val includeJobTitle: Boolean = true
) {
    companion object {
        const val PREFS_NAME = "contact_field_settings"
        const val KEY_INCLUDE_NAME = "include_name"
        const val KEY_INCLUDE_PHONE = "include_phone"
        const val KEY_INCLUDE_EMAIL = "include_email"
        const val KEY_INCLUDE_COMPANY = "include_company"
        const val KEY_INCLUDE_JOB_TITLE = "include_job_title"
    }
}

/**
 * Repository for managing contact field settings
 */
class SettingsRepository(context: Context) {
    
    private val prefs = context.getSharedPreferences(
        ContactFieldSettings.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ContactFieldSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): ContactFieldSettings {
        return ContactFieldSettings(
            includeName = prefs.getBoolean(ContactFieldSettings.KEY_INCLUDE_NAME, true),
            includePhone = prefs.getBoolean(ContactFieldSettings.KEY_INCLUDE_PHONE, true),
            includeEmail = prefs.getBoolean(ContactFieldSettings.KEY_INCLUDE_EMAIL, true),
            includeCompany = prefs.getBoolean(ContactFieldSettings.KEY_INCLUDE_COMPANY, true),
            includeJobTitle = prefs.getBoolean(ContactFieldSettings.KEY_INCLUDE_JOB_TITLE, true)
        )
    }
    
    fun updateSettings(newSettings: ContactFieldSettings) {
        prefs.edit().apply {
            putBoolean(ContactFieldSettings.KEY_INCLUDE_NAME, newSettings.includeName)
            putBoolean(ContactFieldSettings.KEY_INCLUDE_PHONE, newSettings.includePhone)
            putBoolean(ContactFieldSettings.KEY_INCLUDE_EMAIL, newSettings.includeEmail)
            putBoolean(ContactFieldSettings.KEY_INCLUDE_COMPANY, newSettings.includeCompany)
            putBoolean(ContactFieldSettings.KEY_INCLUDE_JOB_TITLE, newSettings.includeJobTitle)
            apply()
        }
        _settings.value = newSettings
    }
    
    fun resetToDefaults() {
        updateSettings(ContactFieldSettings())
    }
}

