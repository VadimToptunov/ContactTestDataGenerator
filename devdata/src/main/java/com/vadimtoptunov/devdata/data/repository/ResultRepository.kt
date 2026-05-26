package com.vadimtoptunov.devdata.data.repository

import android.content.Context
import com.vadimtoptunov.devdata.db.DevDataDatabase
import com.vadimtoptunov.devdata.db.RunResultEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Access point for [RunResultEntity] — test run history per identity / suite.
 */
class ResultRepository(context: Context) {

    private val dao = DevDataDatabase.get(context).runResultDao()

    fun observeBySuite(suiteId: String): Flow<List<RunResultEntity>> =
        dao.observeBySuite(suiteId)

    fun observeByIdentity(identityId: String): Flow<List<RunResultEntity>> =
        dao.observeByIdentity(identityId)

    /**
     * Record a new test run result for a single identity inside a suite.
     *
     * @param identityId       The identity that was scanned
     * @param suiteId          Suite this run belongs to
     * @param expectedOutcome  What the test expected (e.g. "PASS" or "FAIL_PA")
     * @param actualOutcome    What the scanner actually returned (null if not yet determined)
     * @param passed           Did actual == expected? (null = pending)
     * @param notes            Free-text notes from the tester / SDK log
     */
    suspend fun record(
        identityId:      String,
        suiteId:         String,
        expectedOutcome: String,
        actualOutcome:   String? = null,
        passed:          Boolean? = null,
        notes:           String   = ""
    ): String {
        val id = UUID.randomUUID().toString()
        dao.insert(
            RunResultEntity(
                id              = id,
                identityId      = identityId,
                suiteId         = suiteId,
                expectedOutcome = expectedOutcome,
                actualOutcome   = actualOutcome,
                passed          = passed,
                notes           = notes
            )
        )
        return id
    }

    /** Update an existing result once the scan completes. */
    suspend fun update(result: RunResultEntity) = dao.update(result)
}
