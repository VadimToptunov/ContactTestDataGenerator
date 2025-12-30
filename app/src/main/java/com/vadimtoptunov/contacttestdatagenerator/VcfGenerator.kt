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
     * @param onProgress Callback for progress updates
     * @return Uri of the generated VCF file
     */
    suspend fun generateVcfFile(
        count: Int,
        onProgress: (current: Int, total: Int) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        val fileName = "contacts_${System.currentTimeMillis()}.vcf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        FileWriter(file).use { writer ->
            repeat(count) { index ->
                val contact = generateContact()
                writer.write(contact)
                writer.write("\n")
                
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, count)
                }
            }
        }
        
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Generates a single contact in VCF format
     */
    private fun generateContact(): String {
        val name = FakeDataGenerator.generateFullName()
        val phone = FakeDataGenerator.generatePhoneNumber()
        val email = FakeDataGenerator.generateEmail(name)
        val company = FakeDataGenerator.generateCompany()
        val jobTitle = FakeDataGenerator.generateJobTitle()
        
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:$name")
            
            // Split name into first and last name
            val nameParts = name.split(" ")
            if (nameParts.size >= 2) {
                appendLine("N:${nameParts.last()};${nameParts.first()};;;")
            } else {
                appendLine("N:$name;;;;")
            }
            
            appendLine("TEL;TYPE=CELL:$phone")
            appendLine("EMAIL;TYPE=INTERNET:$email")
            appendLine("ORG:$company")
            appendLine("TITLE:$jobTitle")
            appendLine("END:VCARD")
        }
    }
}

