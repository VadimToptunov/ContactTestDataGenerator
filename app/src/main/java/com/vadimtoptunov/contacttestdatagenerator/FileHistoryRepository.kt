package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository for managing VCF file history
 */
class FileHistoryRepository(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("file_history", Context.MODE_PRIVATE)
    private val KEY_HISTORY = "history_json"
    
    suspend fun addFile(fileInfo: VcfFileInfo) = withContext(Dispatchers.IO) {
        val history = getHistory().toMutableList()
        history.add(0, fileInfo) // Add to beginning
        
        // Keep only last 20 files
        if (history.size > 20) {
            history.subList(20, history.size).clear()
        }
        
        saveHistory(history)
    }
    
    suspend fun getHistory(): List<VcfFileInfo> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_HISTORY, null) ?: return@withContext emptyList()
            val jsonArray = JSONArray(json)
            
            (0 until jsonArray.length()).mapNotNull { i ->
                try {
                    val obj = jsonArray.getJSONObject(i)
                    val uri = Uri.parse(obj.getString("uri"))
                    val absolutePath = obj.getString("absolutePath")
                    
                    // Check if file still exists using absolute path
                    val file = File(absolutePath)
                    if (!file.exists()) return@mapNotNull null
                    
                    VcfFileInfo(
                        uri = uri,
                        fileName = obj.getString("fileName"),
                        contactCount = obj.getInt("contactCount"),
                        fileSizeBytes = obj.getLong("fileSizeBytes"),
                        timestamp = obj.getLong("timestamp"),
                        absolutePath = absolutePath
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteFile(fileInfo: VcfFileInfo) = withContext(Dispatchers.IO) {
        // Delete from storage using absolute path
        try {
            val file = File(fileInfo.absolutePath)
            file.delete()
        } catch (e: Exception) {
            // Ignore
        }
        
        // Remove from history
        val history = getHistory().filter { it.uri != fileInfo.uri }
        saveHistory(history)
    }
    
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val history = getHistory()
        
        // Delete all files using absolute paths
        history.forEach { fileInfo ->
            try {
                val file = File(fileInfo.absolutePath)
                file.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        // Clear history
        prefs.edit { remove(KEY_HISTORY) }
    }
    
    private fun saveHistory(history: List<VcfFileInfo>) {
        val jsonArray = JSONArray()
        history.forEach { fileInfo ->
            val obj = JSONObject().apply {
                put("uri", fileInfo.uri.toString())
                put("fileName", fileInfo.fileName)
                put("contactCount", fileInfo.contactCount)
                put("fileSizeBytes", fileInfo.fileSizeBytes)
                put("timestamp", fileInfo.timestamp)
                put("absolutePath", fileInfo.absolutePath)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit {
            putString(KEY_HISTORY, jsonArray.toString())
        }
    }
}

