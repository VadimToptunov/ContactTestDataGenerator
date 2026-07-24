package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import androidx.core.net.toUri

/**
 * Repository for managing VCF file history
 */
class FileHistoryRepository(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("file_history", Context.MODE_PRIVATE)
    private val KEY_HISTORY = "history_json"
    private val mutex = Mutex()
    
    suspend fun addFile(fileInfo: VcfFileInfo) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val history = getHistoryInternal().toMutableList()
            history.add(0, fileInfo) // Add to beginning
            
            // Keep only last 20 files
            if (history.size > 20) {
                history.subList(20, history.size).clear()
            }
            
            saveHistory(history)
        }
    }
    
    suspend fun getHistory(): List<VcfFileInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            getHistoryInternal()
        }
    }
    
    private fun getHistoryInternal(): List<VcfFileInfo> {
        try {
            val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
            val jsonArray = JSONArray(json)
            
            return (0 until jsonArray.length()).mapNotNull { i ->
                try {
                    val obj = jsonArray.getJSONObject(i)
                    val uri = obj.getString("uri").toUri()
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
                        absolutePath = absolutePath,
                        // Older entries predate these fields → default to contacts/VCF.
                        dataTypeLabel = obj.optString("dataTypeLabel", "Contacts"),
                        format = obj.optString("format", "vcf")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    suspend fun deleteFile(fileInfo: VcfFileInfo) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Delete from storage using absolute path
            try {
                val file = File(fileInfo.absolutePath)
                file.delete()
            } catch (e: Exception) {
                // Ignore
            }
            
            // Remove from history
            val history = getHistoryInternal().filter { it.uri != fileInfo.uri }
            saveHistory(history)
        }
    }
    
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val history = getHistoryInternal()
            
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
            saveHistory(emptyList())
        }
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
                put("dataTypeLabel", fileInfo.dataTypeLabel)
                put("format", fileInfo.format)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit {
            putString(KEY_HISTORY, jsonArray.toString())
        }
    }
}

