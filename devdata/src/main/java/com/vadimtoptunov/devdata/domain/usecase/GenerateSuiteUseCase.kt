package com.vadimtoptunov.devdata.domain.usecase

import com.vadimtoptunov.devdata.data.repository.IdentityRepository
import com.vadimtoptunov.devdata.db.TestSuiteEntity
import com.vadimtoptunov.generators.identity.SyntheticIdentityGenerator
import java.util.UUID

/**
 * Generates a named test suite of [SyntheticIdentity] objects and persists them.
 *
 * @param type   Which factory method to call (NFC or document-state)
 * @return The id of the created [TestSuiteEntity]
 */
class GenerateSuiteUseCase(private val repo: IdentityRepository) {

    enum class Type { NFC, STATE, FULL }

    suspend operator fun invoke(countryCode: String, type: Type): String {
        val (identities, name, description) = when (type) {
            Type.NFC -> Triple(
                SyntheticIdentityGenerator.nfcTestSuite(countryCode),
                "$countryCode — NFC test suite",
                "All NFC chip failure modes for $countryCode"
            )
            Type.STATE -> Triple(
                SyntheticIdentityGenerator.fullTestSuite(countryCode),
                "$countryCode — document state suite",
                "All document states for $countryCode"
            )
            Type.FULL -> Triple(
                SyntheticIdentityGenerator.fullTestSuite(countryCode) +
                    SyntheticIdentityGenerator.nfcTestSuite(countryCode) +
                    SyntheticIdentityGenerator.mrzMismatchSuite(countryCode),
                "$countryCode — full regression suite",
                "Document states + NFC failure modes + MRZ mismatches for $countryCode"
            )
        }

        val suiteId = UUID.randomUUID().toString()
        repo.saveSuite(
            TestSuiteEntity(
                id          = suiteId,
                name        = name,
                description = description,
                countryCode = countryCode,
                totalCount  = identities.size
            )
        )
        repo.saveAll(identities, suiteId)
        return suiteId
    }
}
