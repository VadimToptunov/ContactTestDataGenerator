package com.vadimtoptunov.devdata.hce

import android.content.Context
import com.vadimtoptunov.devdata.db.DevDataDatabase
import com.vadimtoptunov.generators.identity.NfcChipState
import com.vadimtoptunov.generators.identity.SyntheticIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Bridge between the Room database and [HcePassportService].
 *
 * Responsibilities:
 *  1. Track which identity is currently "loaded" into the HCE chip
 *  2. Convert the stored JSON → [ActiveChipState] for the service
 *  3. Provide a Flow so the service reacts to UI changes immediately
 *
 * Design notes:
 *  - The service is a long-lived Android Service; it cannot hold a ViewModel.
 *    Using Room Flow directly here avoids the need for a singleton ViewModel.
 *  - [ActiveChipState] is deliberately minimal — the service only needs
 *    the MRZ bytes and chip state, not the full identity.
 */
object ActiveChipRegistry {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Observe the currently active chip state.
     * Emits null when no identity is loaded (HCE service returns NOT_FOUND).
     */
    fun observeChip(context: Context): Flow<ActiveChipState?> {
        val db = DevDataDatabase.get(context)
        return db.identityDao().observeActiveForHce().map { entity ->
            entity ?: return@map null
            val identity = runCatching {
                json.decodeFromString<SyntheticIdentity>(entity.json)
            }.getOrNull() ?: return@map null

            val doc     = identity.primaryDocument ?: return@map null
            val chip    = doc.nfcChip ?: return@map null
            val mrzData = chip.chipMrz ?: doc.mrz

            ActiveChipState(
                identityId = entity.id,
                label      = identity.label,
                chipState  = chip.chipState,
                mrzData    = mrzData
            )
        }
    }

    /**
     * Set the active identity for HCE emulation.
     * Call from the UI when the user taps "Load to NFC chip".
     */
    suspend fun activate(context: Context, identityId: String) {
        DevDataDatabase.get(context).hceStateDao().setActive(identityId)
    }

    /**
     * Deactivate — HCE service will return NOT_FOUND until another identity is loaded.
     */
    suspend fun deactivate(context: Context) {
        DevDataDatabase.get(context).hceStateDao().clearActive()
    }
}
