package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import androidx.core.content.edit

/**
 * Repository for managing contact templates
 */
class TemplateRepository(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("contact_templates", Context.MODE_PRIVATE)
    private val json = Json { prettyPrint = true }
    
    private val _templates = MutableStateFlow<List<ContactTemplate>>(emptyList())
    val templates: StateFlow<List<ContactTemplate>> = _templates.asStateFlow()
    
    init {
        loadTemplates()
    }
    
    private fun loadTemplates() {
        val templatesJson = prefs.getString("templates_list", null)
        if (templatesJson != null) {
            try {
                _templates.value = json.decodeFromString<List<ContactTemplate>>(templatesJson)
            } catch (e: Exception) {
                e.printStackTrace()
                _templates.value = emptyList()
            }
        }
    }
    
    private fun saveTemplates() {
        val templatesJson = json.encodeToString(_templates.value)
        prefs.edit { putString("templates_list", templatesJson) }
    }
    
    fun addTemplate(template: ContactTemplate) {
        _templates.value = (_templates.value + template).sortedByDescending { it.createdAt }
        saveTemplates()
    }
    
    fun deleteTemplate(templateId: String) {
        _templates.value = _templates.value.filter { it.id != templateId }
        saveTemplates()
    }
    
    fun updateTemplate(template: ContactTemplate) {
        _templates.value = _templates.value.map { 
            if (it.id == template.id) template else it 
        }
        saveTemplates()
    }
    
    /**
     * Export template to JSON file
     */
    suspend fun exportTemplate(template: ContactTemplate): File = withContext(Dispatchers.IO) {
        val fileName = "template_${template.name.replace(" ", "_")}_${System.currentTimeMillis()}.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val templateJson = json.encodeToString(template)
        file.writeText(templateJson)
        
        file
    }
    
    /**
     * Export all templates to a single JSON file
     */
    suspend fun exportAllTemplates(): File = withContext(Dispatchers.IO) {
        val fileName = "all_templates_${System.currentTimeMillis()}.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val templatesJson = json.encodeToString(_templates.value)
        file.writeText(templatesJson)
        
        file
    }
    
    /**
     * Import template from JSON file
     */
    suspend fun importTemplate(uri: Uri): Result<ContactTemplate> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            
            if (jsonString.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty file"))
            }
            
            val template = json.decodeFromString<ContactTemplate>(jsonString)
            addTemplate(template.copy(id = java.util.UUID.randomUUID().toString())) // New ID to avoid conflicts
            
            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import multiple templates from a single JSON file
     */
    suspend fun importMultipleTemplates(uri: Uri): Result<List<ContactTemplate>> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            
            if (jsonString.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty file"))
            }
            
            val templates = json.decodeFromString<List<ContactTemplate>>(jsonString)
            templates.forEach { template ->
                addTemplate(template.copy(id = java.util.UUID.randomUUID().toString()))
            }
            
            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun clearAllTemplates() {
        _templates.value = emptyList()
        saveTemplates()
    }
}

