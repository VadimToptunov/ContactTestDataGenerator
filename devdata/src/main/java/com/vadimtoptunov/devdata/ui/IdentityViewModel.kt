package com.vadimtoptunov.devdata.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vadimtoptunov.devdata.db.DevDataDatabase
import com.vadimtoptunov.devdata.db.IdentityEntity
import com.vadimtoptunov.devdata.db.TestSuiteEntity
import com.vadimtoptunov.devdata.hce.ActiveChipRegistry
import com.vadimtoptunov.generators.identity.DocumentRequest
import com.vadimtoptunov.generators.identity.DocumentState
import com.vadimtoptunov.generators.identity.DocumentType
import com.vadimtoptunov.generators.identity.MrzMismatch
import com.vadimtoptunov.generators.identity.NfcChipState
import com.vadimtoptunov.generators.identity.NfcRequest
import com.vadimtoptunov.generators.identity.SyntheticIdentity
import com.vadimtoptunov.generators.identity.SyntheticIdentityGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class IdentityViewModel(app: Application) : AndroidViewModel(app) {

    private val db  = DevDataDatabase.get(app)
    private val ser = Json { prettyPrint = false }

    // ── Observed lists ──────────────────────────────────────────────────────

    val identities: StateFlow<List<IdentityEntity>> =
        db.identityDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suites: StateFlow<List<TestSuiteEntity>> =
        db.testSuiteDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeChipIdentityId: StateFlow<String?> =
        db.hceStateDao().observeActiveId()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generate a single identity from a [DocumentRequest] and persist it.
     */
    fun generate(request: DocumentRequest) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val gen      = SyntheticIdentityGenerator()
                val identity = gen.generate(request)
                persist(identity, suiteId = null)
            }.fold(
                onSuccess = { _uiState.value = UiState.Idle },
                onFailure = { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    /**
     * Generate a full NFC test suite for [countryCode] and persist all identities
     * as a named [TestSuiteEntity].
     */
    fun generateNfcSuite(countryCode: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val identities = SyntheticIdentityGenerator.nfcTestSuite(countryCode)
                val suiteId    = UUID.randomUUID().toString()
                val suite = TestSuiteEntity(
                    id          = suiteId,
                    name        = "$countryCode — NFC test suite",
                    description = "All NFC chip failure modes for $countryCode",
                    countryCode = countryCode,
                    totalCount  = identities.size
                )
                db.testSuiteDao().insert(suite)
                identities.forEach { persist(it, suiteId) }
            }.fold(
                onSuccess = { _uiState.value = UiState.Idle },
                onFailure = { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    /**
     * Generate a full state suite (one per [DocumentState]) and save as a suite.
     */
    fun generateStateSuite(countryCode: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val identities = SyntheticIdentityGenerator.fullTestSuite(countryCode)
                val suiteId    = UUID.randomUUID().toString()
                val suite = TestSuiteEntity(
                    id          = suiteId,
                    name        = "$countryCode — document state suite",
                    description = "All document states for $countryCode",
                    countryCode = countryCode,
                    totalCount  = identities.size
                )
                db.testSuiteDao().insert(suite)
                identities.forEach { persist(it, suiteId) }
            }.fold(
                onSuccess = { _uiState.value = UiState.Idle },
                onFailure = { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    // ── HCE control ───────────────────────────────────────────────────────────

    /** Load an identity into the HCE service — phone now emulates its NFC chip. */
    fun activateForHce(identityId: String) {
        viewModelScope.launch {
            ActiveChipRegistry.activate(getApplication(), identityId)
        }
    }

    /** Unload — HCE service returns NOT_FOUND until next activation. */
    fun deactivateHce() {
        viewModelScope.launch {
            ActiveChipRegistry.deactivate(getApplication())
        }
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    fun deleteIdentity(id: String) {
        viewModelScope.launch {
            db.identityDao().deleteById(id)
        }
    }

    fun deleteSuite(suite: TestSuiteEntity) {
        viewModelScope.launch {
            db.identityDao().deleteBySuite(suite.id)
            db.testSuiteDao().delete(suite)
        }
    }

    fun dismissError() { _uiState.value = UiState.Idle }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun persist(identity: SyntheticIdentity, suiteId: String?) {
        val doc  = identity.primaryDocument
        val chip = doc?.nfcChip
        val entity = IdentityEntity(
            id              = UUID.randomUUID().toString(),
            countryCode     = identity.profile.countryCode,
            documentType    = doc?.type?.name ?: "UNKNOWN",
            documentState   = doc?.state?.name ?: DocumentState.VALID.name,
            scenarioId      = identity.scenario.id,
            hasNfcChip      = chip != null,
            nfcChipState    = chip?.chipState?.name ?: NfcChipState.NO_CHIP.name,
            chipIdentitySrc = chip?.identitySource?.name ?: "NONE",
            seed            = identity.seed,
            json            = ser.encodeToString(identity),
            suiteId         = suiteId
        )
        db.identityDao().insert(entity)
    }
}

sealed class UiState {
    object Idle    : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
}
