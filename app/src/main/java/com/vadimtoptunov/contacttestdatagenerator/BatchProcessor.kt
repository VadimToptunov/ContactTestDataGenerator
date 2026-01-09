package com.vadimtoptunov.contacttestdatagenerator

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages batch processing of contact generation
 */
class BatchProcessor(
    private val context: Context,
    private val vcfGenerator: VcfGenerator,
    private val coroutineScope: CoroutineScope
) {
    private val _currentBatch = MutableStateFlow<List<BatchJob>>(emptyList())
    val currentBatch: StateFlow<List<BatchJob>> = _currentBatch.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _currentJobIndex = MutableStateFlow(-1)
    val currentJobIndex: StateFlow<Int> = _currentJobIndex.asStateFlow()
    
    private var isCancelled = false
    
    /**
     * Start processing batch jobs
     */
    suspend fun processBatch(
        jobs: List<BatchJob>,
        onJobProgress: (jobIndex: Int, current: Int, total: Int) -> Unit,
        onJobComplete: (jobIndex: Int, file: File) -> Unit,
        onJobFailed: (jobIndex: Int, error: String) -> Unit,
        onBatchComplete: (successCount: Int, failedCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        _isProcessing.value = true
        _currentBatch.value = jobs.map { it.copy(status = BatchJobStatus.PENDING) }
        isCancelled = false
        
        var successCount = 0
        var failedCount = 0
        
        jobs.forEachIndexed { index, job ->
            if (isCancelled) {
                // Mark remaining jobs as cancelled
                _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                    if (i >= index) j.copy(status = BatchJobStatus.CANCELLED) else j
                }
                withContext(Dispatchers.Main) {
                    onBatchComplete(successCount, failedCount)
                }
                _isProcessing.value = false
                return@withContext
            }
            
            _currentJobIndex.value = index
            
            // Update job status to RUNNING
            _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                if (i == index) j.copy(status = BatchJobStatus.RUNNING, progress = 0) else j
            }
            
            try {
                val file = vcfGenerator.generateVcfFile(
                    count = job.contactCount,
                    settings = job.fieldSettings
                ) { current, total ->
                    val progress = (current * 100 / total).coerceIn(0, 100)
                    
                    // Update job progress
                    _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                        if (i == index) j.copy(progress = progress) else j
                    }
                    
                    // Call onJobProgress on Main dispatcher
                    coroutineScope.launch(Dispatchers.Main) {
                        onJobProgress(index, current, total)
                    }
                }
                
                // Update job status to COMPLETED
                _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                    if (i == index) j.copy(
                        status = BatchJobStatus.COMPLETED,
                        progress = 100,
                        filePath = file.absolutePath
                    ) else j
                }
                
                successCount++
                withContext(Dispatchers.Main) {
                    onJobComplete(index, file)
                }
                
            } catch (e: CancellationException) {
                _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                    if (i == index) j.copy(status = BatchJobStatus.CANCELLED) else j
                }
                throw e
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                
                // Update job status to FAILED
                _currentBatch.value = _currentBatch.value.mapIndexed { i, j ->
                    if (i == index) j.copy(
                        status = BatchJobStatus.FAILED,
                        errorMessage = errorMsg
                    ) else j
                }
                
                failedCount++
                withContext(Dispatchers.Main) {
                    onJobFailed(index, errorMsg)
                }
            }
        }
        
        _isProcessing.value = false
        _currentJobIndex.value = -1
        
        withContext(Dispatchers.Main) {
            onBatchComplete(successCount, failedCount)
        }
    }
    
    /**
     * Cancel current batch processing
     */
    fun cancelBatch() {
        isCancelled = true
    }
    
    /**
     * Clear current batch
     */
    fun clearBatch() {
        if (!_isProcessing.value) {
            _currentBatch.value = emptyList()
            _currentJobIndex.value = -1
        }
    }
    
    /**
     * Get batch results as file URIs
     */
    fun getBatchResults(): List<Pair<BatchJob, Uri?>> {
        return _currentBatch.value.map { job ->
            val uri = if (job.status == BatchJobStatus.COMPLETED && job.filePath != null) {
                val file = File(job.filePath)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else null
            job to uri
        }
    }
}

