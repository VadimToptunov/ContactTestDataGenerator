package com.vadimtoptunov.devdata.domain.usecase

import com.vadimtoptunov.devdata.data.repository.IdentityRepository
import com.vadimtoptunov.generators.identity.DocumentRequest
import com.vadimtoptunov.generators.identity.SyntheticIdentity
import com.vadimtoptunov.generators.identity.SyntheticIdentityGenerator

/**
 * Generates a single [SyntheticIdentity] from a [DocumentRequest] and persists it.
 *
 * Returns the DB row id of the saved entity.
 */
class GenerateIdentityUseCase(private val repo: IdentityRepository) {

    suspend operator fun invoke(request: DocumentRequest, suiteId: String? = null): String {
        val identity = SyntheticIdentityGenerator().generate(request)
        return repo.save(identity, suiteId)
    }
}
