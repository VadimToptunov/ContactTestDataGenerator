package com.vadimtoptunov.contacttestdatagenerator

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single job in batch processing
 */
@Serializable
data class BatchJob(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contactCount: Int,
    val fieldSettings: ContactFieldSettings,
    val status: BatchJobStatus = BatchJobStatus.PENDING,
    val progress: Int = 0,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class BatchJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Batch configuration
 */
@Serializable
data class BatchConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val jobs: List<BatchJob>,
    val createdAt: Long = System.currentTimeMillis()
)

