package com.vadimtoptunov.devdata.domain.usecase

import com.vadimtoptunov.devdata.data.repository.IdentityRepository
import com.vadimtoptunov.devdata.data.repository.ResultRepository
import com.vadimtoptunov.devdata.db.RunResultEntity
import com.vadimtoptunov.devdata.db.TestSuiteEntity

/**
 * Records the outcome of a single NFC scan against a test identity.
 *
 * Also updates the parent [TestSuiteEntity] pass/fail counters.
 */
class RecordResultUseCase(
    private val resultRepo: ResultRepository,
    private val identityRepo: IdentityRepository
) {

    /**
     * @param identityId      The identity that was scanned
     * @param suiteId         Suite this run belongs to
     * @param expectedOutcome What the scenario expected (e.g. "READABLE" / "PA_FAIL")
     * @param actualOutcome   What the Regula SDK returned
     * @param passed          True if actual matched expected
     * @param notes           Optional log excerpt or tester comment
     */
    suspend operator fun invoke(
        identityId:      String,
        suiteId:         String,
        expectedOutcome: String,
        actualOutcome:   String,
        passed:          Boolean,
        notes:           String = ""
    ) {
        resultRepo.record(
            identityId      = identityId,
            suiteId         = suiteId,
            expectedOutcome = expectedOutcome,
            actualOutcome   = actualOutcome,
            passed          = passed,
            notes           = notes
        )

        // Update suite counters
        val suite = identityRepo.findSuite(suiteId) ?: return
        val updated = suite.copy(
            lastRunAt  = System.currentTimeMillis(),
            passCount  = suite.passCount + if (passed) 1 else 0,
            failCount  = suite.failCount + if (!passed) 1 else 0
        )
        identityRepo.updateSuite(updated)
    }
}
