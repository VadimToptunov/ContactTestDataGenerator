package com.vadimtoptunov.contacttestdatagenerator

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for MainActivity - manages UI state and business logic
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val vcfGenerator = VcfGenerator(application)
    private val fileHistoryRepository = FileHistoryRepository(application)
    val billingManager = BillingManager(application, viewModelScope)
    val settingsRepository = SettingsRepository(application)
    val templateRepository = TemplateRepository(application)
    val batchProcessor = BatchProcessor(application, vcfGenerator, viewModelScope)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _fileHistory = MutableStateFlow<List<VcfFileInfo>>(emptyList())
    val fileHistory: StateFlow<List<VcfFileInfo>> = _fileHistory.asStateFlow()
    
    private val _batchState = MutableStateFlow<BatchState>(BatchState.Idle)
    val batchState: StateFlow<BatchState> = _batchState.asStateFlow()
    
    private var currentJob: Job? = null

    init {
        loadHistory()
        billingManager.queryPurchases()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            _fileHistory.value = fileHistoryRepository.getHistory()
        }
    }
    
    fun getMaxContacts(): Int {
        return if (billingManager.isPremium.value) {
            BillingManager.PREMIUM_MAX_CONTACTS
        } else {
            BillingManager.FREE_MAX_CONTACTS
        }
    }

    fun startGenerating(count: Int) {
        val context = getApplication<Application>()
        val maxContacts = getMaxContacts()
        
        if (count <= 0) {
            _uiState.value = UiState.Error("Number must be positive")
            return
        }
        
        if (count > maxContacts) {
            _uiState.value = UiState.Error("Maximum $maxContacts contacts allowed${if (!billingManager.isPremium.value) " (upgrade to Pro for 10,000)" else ""}")
            return
        }

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.value = UiState.Loading(current = 0, total = count)
            
            try {
                val file = vcfGenerator.generateVcfFile(
                    count = count,
                    settings = settingsRepository.settings.value
                ) { current, total ->
                    _uiState.value = UiState.Loading(current = current, total = total)
                }
                
                // Save to history
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val fileInfo = VcfFileInfo(
                    uri = uri,
                    fileName = file.name,
                    contactCount = count,
                    fileSizeBytes = file.length(),
                    absolutePath = file.absolutePath
                )
                fileHistoryRepository.addFile(fileInfo)
                loadHistory()
                
                _uiState.value = UiState.Success(
                    message = "$count contact${if (count > 1) "s" else ""} generated successfully!",
                    count = count,
                    fileUri = uri
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
    
    fun deleteFile(fileInfo: VcfFileInfo) {
        viewModelScope.launch {
            fileHistoryRepository.deleteFile(fileInfo)
            loadHistory()
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            fileHistoryRepository.clearHistory()
            loadHistory()
        }
    }
    
    // Template Functions
    fun saveTemplate(name: String, count: Int) {
        val template = ContactTemplate(
            name = name,
            contactCount = count,
            fieldSettings = settingsRepository.settings.value
        )
        templateRepository.addTemplate(template)
    }
    
    fun loadTemplate(template: ContactTemplate) {
        settingsRepository.updateSettings(template.fieldSettings)
    }
    
    fun deleteTemplate(templateId: String) {
        templateRepository.deleteTemplate(templateId)
    }
    
    fun exportTemplate(template: ContactTemplate, onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = templateRepository.exportTemplate(template)
                withContext(Dispatchers.Main) {
                    onSuccess(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun exportAllTemplates(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = templateRepository.exportAllTemplates()
                withContext(Dispatchers.Main) {
                    onSuccess(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun importTemplate(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = templateRepository.importTemplate(uri)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun importMultipleTemplates(uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = templateRepository.importMultipleTemplates(uri)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    onSuccess(result.getOrNull()?.size ?: 0)
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun purchasePremium(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
    
    // Batch Processing Functions
    fun startBatchProcessing(jobs: List<BatchJob>) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _batchState.value = BatchState.Processing(currentJobIndex = 0, totalJobs = jobs.size)
            
            batchProcessor.processBatch(
                jobs = jobs,
                onJobProgress = { jobIndex, current, total ->
                    _batchState.value = BatchState.Processing(
                        currentJobIndex = jobIndex,
                        totalJobs = jobs.size,
                        currentProgress = current,
                        totalProgress = total
                    )
                },
                onJobComplete = { jobIndex, file ->
                    val uri = FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        file
                    )
                    val fileInfo = VcfFileInfo(
                        uri = uri,
                        fileName = file.name,
                        contactCount = jobs[jobIndex].contactCount,
                        fileSizeBytes = file.length(),
                        absolutePath = file.absolutePath
                    )
                    viewModelScope.launch {
                        fileHistoryRepository.addFile(fileInfo)
                        loadHistory()
                    }
                },
                onJobFailed = { _, _ ->
                    // Error is already tracked in BatchJob
                },
                onBatchComplete = { successCount, failedCount ->
                    _batchState.value = BatchState.Completed(
                        successCount = successCount,
                        failedCount = failedCount,
                        results = batchProcessor.getBatchResults()
                    )
                }
            )
        }
    }
    
    fun cancelBatch() {
        batchProcessor.cancelBatch()
        currentJob?.cancel()
        _batchState.value = BatchState.Idle
    }
    
    fun resetBatchState() {
        batchProcessor.clearBatch()
        _batchState.value = BatchState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        billingManager.onDestroy()
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

sealed class BatchState {
    object Idle : BatchState()
    
    data class Processing(
        val currentJobIndex: Int,
        val totalJobs: Int,
        val currentProgress: Int = 0,
        val totalProgress: Int = 0
    ) : BatchState() {
        val overallProgress: Int
            get() = if (totalJobs > 0) {
                val jobProgress = currentJobIndex * 100 / totalJobs
                val currentJobContribution = if (totalProgress > 0) {
                    (currentProgress * 100 / totalProgress) / totalJobs
                } else 0
                (jobProgress + currentJobContribution).coerceIn(0, 100)
            } else 0
    }
    
    data class Completed(
        val successCount: Int,
        val failedCount: Int,
        val results: List<Pair<BatchJob, Uri?>>
    ) : BatchState()
}

