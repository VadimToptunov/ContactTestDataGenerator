package com.vadimtoptunov.devdata.domain.usecase

import com.vadimtoptunov.devdata.data.repository.IdentityRepository

/**
 * Loads an identity into the HCE service (phone starts emulating the NFC chip)
 * or clears the active chip.
 */
class ActivateChipUseCase(private val repo: IdentityRepository) {

    suspend fun activate(identityId: String) = repo.activateChip(identityId)

    suspend fun deactivate() = repo.deactivateChip()
}
