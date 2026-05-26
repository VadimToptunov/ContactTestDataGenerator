package com.vadimtoptunov.devdata.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vadimtoptunov.devdata.data.repository.IdentityRepository
import com.vadimtoptunov.devdata.data.repository.ResultRepository
import com.vadimtoptunov.devdata.db.IdentityEntity
import com.vadimtoptunov.devdata.db.RunResultEntity
import com.vadimtoptunov.devdata.db.TestSuiteEntity
import com.vadimtoptunov.devdata.domain.usecase.ActivateChipUseCase
import com.vadimtoptunov.devdata.domain.usecase.GenerateIdentityUseCase
import com.vadimtoptunov.devdata.domain.usecase.GenerateSuiteUseCase
import com.vadimtoptunov.devdata.domain.usecase.RecordResultUseCase
import com.vadimtoptunov.generators.identity.DocumentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IdentityViewModel(app: Application) : AndroidViewModel(app) {

    // ── Repositories & use cases ───────────────────────────────────────────

    private val identityRepo = IdentityRepository(app)
    private val resultRepo   = ResultRepository(app)

    private val generateIdentity = GenerateIdentityUseCase(identityRepo)
    private val generateSuite    = GenerateSuiteUseCase(identityRepo)
    private val activateChip     = ActivateChipUseCase(identityRepo)
    private val recordResult     = RecordResultUseCase(resultRepo, identityRepo)

    // ── Observed state ─────────────────────────────────────────────────────

    val identities: StateFlow<List<IdentityEntity>> =
        identityRepo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suites: StateFlow<List<TestSuiteEntity>> =
        identityRepo.observeSuites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeChipIdentityId: StateFlow<String?> =
        identityRepo.observeActiveChipId()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Generation ─────────────────────────────────────────────────────────

    fun generate(request: DocumentRequest) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { generateIdentity(request) }
                .onSuccess { _uiState.value = UiState.Idle }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun generateNfcSuite(countryCode: String) = launchSuite {
        generateSuite(countryCode, GenerateSuiteUseCase.Type.NFC)
    }

    fun generateStateSuite(countryCode: String) = launchSuite {
        generateSuite(countryCode, GenerateSuiteUseCase.Type.STATE)
    }

    fun generateFullSuite(countryCode: String) = launchSuite {
        generateSuite(countryCode, GenerateSuiteUseCase.Type.FULL)
    }

    private fun launchSuite(block: suspend () -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { block() }
                .onSuccess { _uiState.value = UiState.Idle }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    // ── HCE control ────────────────────────────────────────────────────────

    fun activateForHce(identityId: String) {
        viewModelScope.launch { activateChip.activate(identityId) }
    }

    fun deactivateHce() {
        viewModelScope.launch { activateChip.deactivate() }
    }

    // ── Results ────────────────────────────────────────────────────────────

    /**
     * Observe all results for a given suite (used by ResultsScreen).
     * Returns a new Flow on each call — collect inside LaunchedEffect.
     */
    fun observeResults(suiteId: String) = resultRepo.observeBySuite(suiteId)

    fun observeResultsForIdentity(identityId: String) =
        resultRepo.observeByIdentity(identityId)

    fun recordScanResult(
        identityId: String,
        suiteId: String,
        expectedOutcome: String,
        actualOutcome: String,
        passed: Boolean,
        notes: String = ""
    ) {
        viewModelScope.launch {
            recordResult(identityId, suiteId, expectedOutcome, actualOutcome, passed, notes)
        }
    }

    // ── Deletion ───────────────────────────────────────────────────────────

    fun deleteIdentity(id: String) {
        viewModelScope.launch { identityRepo.deleteById(id) }
    }

    fun deleteSuite(suite: TestSuiteEntity) {
        viewModelScope.launch { identityRepo.deleteSuite(suite) }
    }

    fun dismissError() { _uiState.value = UiState.Idle }
}

sealed class UiState {
    object Idle    : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
}
