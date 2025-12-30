package com.vadimtoptunov.contacttestdatagenerator

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity - manages UI state and business logic
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val vcfGenerator = VcfGenerator(application)
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var currentJob: Job? = null

    fun startGenerating(count: Int) {
        val context = getApplication<Application>()
        
        if (count <= 0) {
            _uiState.value = UiState.Error("Number must be positive")
            return
        }
        
        if (count > MAX_CONTACTS) {
            _uiState.value = UiState.Error("Maximum $MAX_CONTACTS contacts allowed")
            return
        }

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.value = UiState.Loading(current = 0, total = count)
            
            try {
                val fileUri = vcfGenerator.generateVcfFile(count) { current, total ->
                    _uiState.value = UiState.Loading(current = current, total = total)
                }
                
                _uiState.value = UiState.Success(
                    message = "$count contact${if (count > 1) "s" else ""} generated successfully!",
                    count = count,
                    fileUri = fileUri
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error generating contacts: ${e.message}")
            }
        }
    }

    fun cancelGeneration() {
        currentJob?.cancel()
        _uiState.value = UiState.Idle
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun shareVcfFile(fileUri: Uri) {
        val context = getApplication<Application>()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "text/x-vcard"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share contacts")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    companion object {
        private const val MAX_CONTACTS = 10000
    }
}

sealed class UiState {
    object Idle : UiState()
    
    data class Loading(
        val current: Int,
        val total: Int
    ) : UiState() {
        val progress: Int
            get() = if (total > 0) (current * 100L / total).toInt() else 0
    }
    
    data class Success(
        val message: String,
        val count: Int,
        val fileUri: Uri
    ) : UiState()
    
    data class Error(
        val message: String
    ) : UiState()
}

