package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.os.Environment
import com.vadimtoptunov.generators.contacts.ContactFields
import com.vadimtoptunov.generators.contacts.ContactGenerator
import com.vadimtoptunov.generators.core.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * Writes a VCF (vCard) file of generated contacts.
 *
 * The actual contact generation and vCard formatting now live in the shared
 * [ContactGenerator] (`:generators` library); this class only streams records
 * to a file and reports progress.
 */
class VcfGenerator(private val context: Context) {

    /**
     * Generates a VCF file with the specified number of contacts.
     *
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

        val generator = ContactGenerator(settings.toContactFields())

        FileWriter(file).use { writer ->
            repeat(count) { index ->
                val record = generator.generate()
                writer.write(generator.serialize(record, OutputFormat.VCF))
                writer.write("\n")

                withContext(Dispatchers.Main) {
                    onProgress(index + 1, count)
                }
            }
        }

        file
    }
}

/** Map the app's persisted field settings onto the library's [ContactFields]. */
fun ContactFieldSettings.toContactFields(): ContactFields = ContactFields(
    includeName = includeName,
    includePhone = includePhone,
    includeEmail = includeEmail,
    includeCompany = includeCompany,
    includeJobTitle = includeJobTitle,
)
