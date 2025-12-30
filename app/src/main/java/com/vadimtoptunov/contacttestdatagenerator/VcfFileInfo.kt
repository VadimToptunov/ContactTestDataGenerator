package com.vadimtoptunov.contacttestdatagenerator

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a generated VCF file
 */
data class VcfFileInfo(
    val uri: Uri,
    val fileName: String,
    val contactCount: Int,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
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

