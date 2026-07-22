package com.vadimtoptunov.contacttestdatagenerator.devtools

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.OutputFormat
import java.io.File

/**
 * Runs a generator and turns its output into shareable text or a file.
 *
 * Lives in the app layer (not the pure `generators` library) because it touches
 * Android storage and the share sheet.
 */
object DevToolsExporter {

    /**
     * Generate [count] records with [generator] and serialize them to [format].
     *
     * A generator only knows how to serialize its own record type, so we ask the
     * same instance to both produce and serialize the records. The unchecked cast
     * is safe because the produced list is fed straight back into the very
     * generator that created it.
     *
     * @param seed optional starting seed for reproducible output; null = random.
     */
    @Suppress("UNCHECKED_CAST")
    fun generateSerialized(
        generator: DataGenerator<*>,
        count: Int,
        format: OutputFormat,
        seed: Long?,
    ): String {
        val typedGenerator = generator as DataGenerator<Any?>
        val records = if (seed != null) {
            typedGenerator.generateBatch(count, seed)
        } else {
            typedGenerator.generateBatch(count)
        }
        return typedGenerator.serializeBatch(records, format)
    }

    /**
     * Write [content] to a file in the app's external documents directory and
     * return it, ready to be handed to [shareFile].
     */
    fun writeToFile(
        context: Context,
        toolId: String,
        content: String,
        format: OutputFormat,
    ): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IllegalStateException("External storage is unavailable")
        val fileName = "${toolId}_${System.currentTimeMillis()}.${format.extension}"
        val outputFile = File(documentsDir, fileName)
        outputFile.writeText(content)
        return outputFile
    }

    /** Open the system share sheet for a previously written [file]. */
    fun shareFile(context: Context, file: File, format: OutputFormat) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = format.mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share ${format.label}")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
