package com.vadimtoptunov.devdata.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A persisted synthetic identity.
 *
 * The full [SyntheticIdentity] is stored as JSON so schema changes
 * in the generators library don't require DB migrations.
 * Indexed fields are kept as columns for fast queries / filtering.
 */
@Entity(tableName = "identities")
data class IdentityEntity(
    @PrimaryKey val id: String,                 // UUID

    // ── Indexed for filtering ──────────────────────────────────────────────
    val countryCode:    String,                 // "ES", "DE" …
    val documentType:   String,                 // DocumentType.name
    val documentState:  String,                 // DocumentState.name
    val scenarioId:     String,                 // TestScenario.id

    // ── NFC chip state — drives HCE behaviour ─────────────────────────────
    /** Whether a chip is present and should be served by HCE. */
    val hasNfcChip:     Boolean,
    /** NfcChipState.name — controls what HcePassportService returns. */
    val nfcChipState:   String,
    /** ChipIdentitySource.name — is the chip the same person or third-party? */
    val chipIdentitySrc: String,

    // ── Reproducibility ───────────────────────────────────────────────────
    val seed:           Long?,

    // ── Full serialised object ─────────────────────────────────────────────
    /** Complete SyntheticIdentity as JSON (kotlinx.serialization). */
    val json:           String,

    // ── Suite membership ──────────────────────────────────────────────────
    /** FK to [TestSuiteEntity.id] — null if standalone identity. */
    val suiteId:        String?,

    val createdAt:      Long = System.currentTimeMillis()
)

/**
 * A named collection of identities, e.g. "ES passport regression" or "NFC failure suite".
 * Analogous to a test suite in JUnit — has a label, expected pass rate, and run history.
 */
@Entity(tableName = "test_suites")
data class TestSuiteEntity(
    @PrimaryKey val id: String,                 // UUID
    val name:           String,
    val description:    String,
    val countryCode:    String?,                // null = multi-country suite
    val createdAt:      Long = System.currentTimeMillis(),
    val lastRunAt:      Long? = null,
    val totalCount:     Int = 0,
    val passCount:      Int = 0,
    val failCount:      Int = 0
)

/**
 * One run result for a single identity within a suite.
 * Records whether the KYC scan matched the [expectedOutcome].
 */
@Entity(
    tableName = "run_results",
    foreignKeys = [
        ForeignKey(
            entity = IdentityEntity::class,
            parentColumns = ["id"],
            childColumns  = ["identityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TestSuiteEntity::class,
            parentColumns = ["id"],
            childColumns  = ["suiteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("identityId"),
        Index("suiteId")
    ]
)
data class RunResultEntity(
    @PrimaryKey val id:       String,           // UUID
    val identityId:           String,
    val suiteId:              String,
    val expectedOutcome:      String,           // ExpectedOutcome.name
    val actualOutcome:        String?,          // null = not yet run
    val passed:               Boolean?,         // null = not yet run
    val notes:                String = "",
    val runAt:                Long = System.currentTimeMillis()
)
