package com.vadimtoptunov.devdata.data.repository

import android.content.Context
import com.vadimtoptunov.devdata.db.DevDataDatabase
import com.vadimtoptunov.devdata.db.IdentityEntity
import com.vadimtoptunov.devdata.db.TestSuiteEntity
import com.vadimtoptunov.devdata.hce.ActiveChipRegistry
import com.vadimtoptunov.generators.identity.DocumentState
import com.vadimtoptunov.generators.identity.NfcChipState
import com.vadimtoptunov.generators.identity.SyntheticIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Single access point for identity and suite data.
 *
 * Hides Room DAOs and serialization from the rest of the app.
 * All reads are Flows; all writes are suspend functions.
 */
class IdentityRepository(private val context: Context) {

    private val db  = DevDataDatabase.get(context)
    private val ser = Json { prettyPrint = false }

    // ── Identities ─────────────────────────────────────────────────────────

    fun observeAll(): Flow<List<IdentityEntity>> = db.identityDao().observeAll()

    fun observeStandalone(): Flow<List<IdentityEntity>> = db.identityDao().observeStandalone()

    fun observeBySuite(suiteId: String): Flow<List<IdentityEntity>> =
        db.identityDao().observeBySuite(suiteId)

    fun observeByCountry(code: String): Flow<List<IdentityEntity>> =
        db.identityDao().observeByCountry(code)

    suspend fun findById(id: String): IdentityEntity? = db.identityDao().findById(id)

    suspend fun save(identity: SyntheticIdentity, suiteId: String?): String {
        val doc    = identity.primaryDocument
        val chip   = doc?.nfcChip
        val id     = UUID.randomUUID().toString()
        val entity = IdentityEntity(
            id              = id,
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
        return id
    }

    suspend fun saveAll(identities: List<SyntheticIdentity>, suiteId: String?) {
        identities.forEach { save(it, suiteId) }
    }

    suspend fun deleteById(id: String) = db.identityDao().deleteById(id)

    suspend fun deleteBySuite(suiteId: String) = db.identityDao().deleteBySuite(suiteId)

    // ── Suites ─────────────────────────────────────────────────────────────

    fun observeSuites(): Flow<List<TestSuiteEntity>> = db.testSuiteDao().observeAll()

    suspend fun findSuite(id: String): TestSuiteEntity? = db.testSuiteDao().findById(id)

    suspend fun saveSuite(suite: TestSuiteEntity) = db.testSuiteDao().insert(suite)

    suspend fun updateSuite(suite: TestSuiteEntity) = db.testSuiteDao().update(suite)

    suspend fun deleteSuite(suite: TestSuiteEntity) {
        db.identityDao().deleteBySuite(suite.id)
        db.testSuiteDao().delete(suite)
    }

    // ── HCE state ──────────────────────────────────────────────────────────

    fun observeActiveChipId(): Flow<String?> = db.hceStateDao().observeActiveId()

    suspend fun activateChip(identityId: String) =
        ActiveChipRegistry.activate(context, identityId)

    suspend fun deactivateChip() = ActiveChipRegistry.deactivate(context)
}
