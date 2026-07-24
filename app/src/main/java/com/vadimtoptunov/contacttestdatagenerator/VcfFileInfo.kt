package com.vadimtoptunov.contacttestdatagenerator

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a generated file in history.
 *
 * Originally VCF-only; now covers any generator output. [dataTypeLabel] is the
 * human name of what was generated ("Contacts", "Card Number Generator", …) and
 * [format] is the file's extension ("vcf", "csv", "json", "sql", "txt"). Both
 * default to the contacts/VCF case so older saved history stays valid.
 */
data class VcfFileInfo(
    val uri: Uri,
    val fileName: String,
    val contactCount: Int,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val absolutePath: String, // Added to store actual file path
    val dataTypeLabel: String = "Contacts",
    val format: String = "vcf",
) {
    /** True for the built-in contacts generator (VCF) — the legacy default. */
    val isContacts: Boolean get() = format == "vcf" && dataTypeLabel == "Contacts"

    /** MIME type for sharing, derived from [format]. */
    val mimeType: String
        get() = when (format) {
            "vcf" -> "text/x-vcard"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "sql" -> "application/sql"
            else -> "text/plain"
        }

    val fileSizeFormatted: String
        get() {
            val kb = fileSizeBytes / 1024.0
            return if (kb < 1024) {
                String.format("%.1f KB", kb)
            } else {
                String.format("%.1f MB", kb / 1024.0)
            }
        }
    
    val dateFormatted: String
        get() {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

