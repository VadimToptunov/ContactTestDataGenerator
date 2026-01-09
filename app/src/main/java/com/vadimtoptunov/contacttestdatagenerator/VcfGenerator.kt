package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * Generator for creating VCF (vCard) files
 */
class VcfGenerator(private val context: Context) {

    /**
     * Generates a VCF file with specified number of contacts
     * @param count Number of contacts to generate
     * @param settings Field settings for customization
     * @param onProgress Callback for progress updates
     * @return Generated File
     */
    suspend fun generateVcfFile(
        count: Int,
        settings: ContactFieldSettings,
        onProgress: (current: Int, total: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val fileName = "contacts_${System.currentTimeMillis()}.vcf"
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IllegalStateException("External storage is unavailable")
        val file = File(externalDir, fileName)
        
        FileWriter(file).use { writer ->
            repeat(count) { index ->
                val contact = generateContact(settings)
                writer.write(contact)
                writer.write("\n")
                
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, count)
                }
            }
        }
        
        file
    }

    /**
     * Generates a single contact in VCF format
     */
    private fun generateContact(settings: ContactFieldSettings): String {
        val name = if (settings.includeName) FakeDataGenerator.generateFullName() else "Contact"
        val phone = if (settings.includePhone) FakeDataGenerator.generatePhoneNumber() else null
        val email = if (settings.includeEmail) FakeDataGenerator.generateEmail(name) else null
        val company = if (settings.includeCompany) FakeDataGenerator.generateCompany() else null
        val jobTitle = if (settings.includeJobTitle) FakeDataGenerator.generateJobTitle() else null
        
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            
            if (settings.includeName) {
                appendLine("FN:$name")
                
                // Split name into first and last name
                val nameParts = name.split(" ")
                if (nameParts.size >= 2) {
                    appendLine("N:${nameParts.last()};${nameParts.first()};;;")
                } else {
                    appendLine("N:$name;;;;")
                }
            }
            
            if (phone != null) {
                appendLine("TEL;TYPE=CELL:$phone")
            }
            
            if (email != null) {
                appendLine("EMAIL;TYPE=INTERNET:$email")
            }
            
            if (company != null) {
                appendLine("ORG:$company")
            }
            
            if (jobTitle != null) {
                appendLine("TITLE:$jobTitle")
            }
            
            appendLine("END:VCARD")
        }
    }
}

