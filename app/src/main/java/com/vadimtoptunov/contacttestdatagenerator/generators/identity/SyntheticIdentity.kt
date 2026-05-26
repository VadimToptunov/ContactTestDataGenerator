package com.vadimtoptunov.contacttestdatagenerator.generators.identity

import com.vadimtoptunov.contacttestdatagenerator.generators.finance.CardRecord
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.IbanRecord

/**
 * A complete synthetic test identity.
 *
 * IMPORTANT — LEGAL NOTICE:
 * All data in this class is algorithmically generated and does NOT correspond
 * to any real person, living or deceased. Numbers are mathematically valid
 * (pass checksum algorithms) but are NOT registered in any government database,
 * NOT backed by real documents, and NOT usable for any official purpose.
 *
 * Intended use: software testing, QA automation, KYC flow development,
 * security research in authorized environments.
 *
 * Every rendered output carries a SPECIMEN / TEST DATA watermark.
 */
data class SyntheticIdentity(

    // ── Core identity ──────────────────────────────────────────────────────
    val profile: IdentityProfile,

    // ── Documents ──────────────────────────────────────────────────────────
    val documents: List<SyntheticDocument>,

    // ── Contact data ───────────────────────────────────────────────────────
    val address: SyntheticAddress,
    val phone: String,
    val email: String,

    // ── Financial ──────────────────────────────────────────────────────────
    val taxId: TaxIdRecord?,
    val bankCard: CardRecord?,
    val iban: IbanRecord?,

    // ── Test metadata ──────────────────────────────────────────────────────
    val scenario: TestScenario,
    val generatedAt: Long = System.currentTimeMillis(),
    val seed: Long? = null    // if set, identity is reproducible
) {
    /** Human-readable label for logs / UI list items. */
    val label: String
        get() = "[${profile.countryCode}] ${profile.fullNameLatin} — ${scenario.label}"

    /** All documents of a specific type. */
    fun documentsOfType(type: DocumentType) = documents.filter { it.type == type }

    /** Primary document (passport if available, else first in list). */
    val primaryDocument: SyntheticDocument?
        get() = documents.firstOrNull { it.type == DocumentType.PASSPORT }
            ?: documents.firstOrNull()
}

// ─── Identity profile ──────────────────────────────────────────────────────

data class IdentityProfile(
    val countryCode: String,          // ISO 3166-1 alpha-2
    val locale: String,               // BCP-47 e.g. "ru-RU", "pt-BR"
    val gender: Gender,
    val dateOfBirth: SimpleDate,
    val fullNameNative: String,       // Name in the country's script
    val fullNameLatin: String,        // Transliterated for MRZ / passports
    val nationality: String           // ICAO 3-letter e.g. "RUS", "BRA"
)

data class SimpleDate(val year: Int, val month: Int, val day: Int) {
    override fun toString() = "%04d-%02d-%02d".format(year, month, day)
    val shortYear: String get() = (year % 100).toString().padStart(2, '0')
    val monthPadded: String get() = month.toString().padStart(2, '0')
    val dayPadded: String get() = day.toString().padStart(2, '0')
    /** YYMMDD format used in MRZ */
    val mrz: String get() = "$shortYear$monthPadded$dayPadded"
}

enum class Gender(val mrzChar: Char) { MALE('M'), FEMALE('F') }

// ─── Synthetic document ────────────────────────────────────────────────────

data class SyntheticDocument(
    val type: DocumentType,
    val countryCode: String,
    val documentNumber: String,       // Series + number in country format
    val documentNumberMrz: String,    // MRZ-safe version (uppercase, no spaces)
    val issueDate: SimpleDate,
    val expiryDate: SimpleDate,
    val issuingAuthority: String,
    val mrz: MrzData?,                // null for non-MRZ documents
    val state: DocumentState,
    val artifacts: List<DocumentArtifact>,

    // ── SPECIMEN marker — always present ──────────────────────────────────
    val specimenLabel: String = "SPECIMEN — TEST DOCUMENT — NOT VALID"
) {
    val isExpired: Boolean get() {
        val now = java.util.Calendar.getInstance()
        return expiryDate.year < now.get(java.util.Calendar.YEAR) ||
            (expiryDate.year == now.get(java.util.Calendar.YEAR) &&
             expiryDate.month < now.get(java.util.Calendar.MONTH) + 1)
    }
}

enum class DocumentType(val label: String, val hasMrz: Boolean, val hasChin: Boolean) {
    PASSPORT("Passport", hasMrz = true,  hasChin = false),
    NATIONAL_ID("National ID Card", hasMrz = true,  hasChin = true),
    RESIDENCE_PERMIT("Residence Permit", hasMrz = true,  hasChin = false),
    DRIVERS_LICENSE("Driver's License", hasMrz = false, hasChin = false),
    FOREIGN_PASSPORT("Foreign Passport", hasMrz = true,  hasChin = false),
    REFUGEE_TRAVEL("Refugee Travel Document", hasMrz = true, hasChin = false)
}

// ─── MRZ ──────────────────────────────────────────────────────────────────

/**
 * ICAO 9303 Machine-Readable Zone data.
 * TD1 = 3 lines × 30 chars (ID cards)
 * TD3 = 2 lines × 44 chars (passports)
 */
data class MrzData(
    val format: MrzFormat,
    val line1: String,
    val line2: String,
    val line3: String? = null   // TD1 only
) {
    val raw: String get() = listOfNotNull(line1, line2, line3).joinToString("\n")
}

enum class MrzFormat { TD1, TD2, TD3 }

// ─── Document state and artifacts ──────────────────────────────────────────

/**
 * The "validity state" of a document — for QA test scenarios.
 * Controls how the generated document should behave when scanned.
 */
enum class DocumentState(val label: String) {
    VALID("Valid — all checks pass"),
    EXPIRED("Expired — past expiry date"),
    EXPIRING_SOON("Expiring soon — within 30 days"),
    WRONG_CHECKSUM("Wrong checksum — MRZ check digits corrupt"),
    MISSING_FIELD("Missing field — required field blank"),
    UNDERAGE("Underage — DOB makes holder < 18"),
    FUTURE_ISSUE_DATE("Future issue date — issued tomorrow"),
    TAMPERED_NUMBER("Tampered — document number altered"),
    BLACKLISTED("Blacklisted — number on a deny list"),
}

/**
 * Visual / scan artifacts that stress-test the OCR and image-quality checks
 * of a KYC system (e.g. Regula, Onfido, Jumio).
 */
enum class DocumentArtifact(val label: String) {
    NONE("Clean — no artifacts"),

    // Photo quality
    DARK_PHOTO("Dark photo — underexposed"),
    OVEREXPOSED("Overexposed — washed out"),
    BLURRY("Blurry — out of focus"),
    GLARE("Glare — light reflection on document"),
    PARTIAL_CROP("Partial crop — document edges cut off"),
    ROTATED_90("Rotated 90°"),
    ROTATED_SLIGHT("Slightly rotated (~10°)"),
    LOW_RESOLUTION("Low resolution — below 100 DPI"),

    // Document condition
    FOLDED("Folded — crease across document"),
    DAMAGED_CORNER("Damaged corner"),
    WATER_DAMAGE("Water damage marks"),
    HANDWRITTEN_OVER("Handwritten marks over printed text"),

    // Scan/capture conditions
    SCREEN_CAPTURE("Screen capture of another screen (moire)"),
    BLACK_AND_WHITE("B&W photocopy"),
    COVERED_FACE("Face partially covered"),
    DIFFERENT_PERSON("Photo replaced — visual mismatch"),

    // NFC-specific
    NFC_CHIP_LOCKED("NFC chip locked — BAC fails"),
    NFC_NO_CHIP("No NFC chip response"),
    NFC_PA_FAIL("NFC Passive Auth fails — no CSCA signature"),
    NFC_PARTIAL_READ("NFC partial read — DG2 missing"),
}

// ─── Address ───────────────────────────────────────────────────────────────

data class SyntheticAddress(
    val countryCode: String,
    val countryName: String,
    val region: String,           // State / Oblast / Bundesland
    val city: String,
    val postalCode: String,
    val streetLine1: String,
    val streetLine2: String? = null,
    val formatted: String         // Country-specific display format
)

// ─── Test scenario ─────────────────────────────────────────────────────────

/**
 * A named test scenario — describes what this identity is designed to test.
 * Shown in the UI so the QA engineer knows exactly what case they're running.
 */
data class TestScenario(
    val id: String,
    val label: String,
    val description: String,
    val expectedOutcome: ExpectedOutcome
)

enum class ExpectedOutcome(val label: String) {
    PASS("Should pass KYC"),
    FAIL_SOFT("Should fail — recoverable (re-upload)"),
    FAIL_HARD("Should fail — hard reject"),
    MANUAL_REVIEW("Should trigger manual review"),
    ERROR("Should produce an error / exception")
}

// ─── Predefined scenarios ──────────────────────────────────────────────────

object TestScenarios {

    val HAPPY_PATH = TestScenario(
        id = "happy_path",
        label = "Happy path",
        description = "Valid document, clean photo, all fields correct",
        expectedOutcome = ExpectedOutcome.PASS
    )

    val EXPIRED_DOCUMENT = TestScenario(
        id = "expired_doc",
        label = "Expired document",
        description = "Document expiry date is in the past",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )

    val DARK_PHOTO = TestScenario(
        id = "dark_photo",
        label = "Dark photo",
        description = "Document photo is severely underexposed",
        expectedOutcome = ExpectedOutcome.FAIL_SOFT
    )

    val WRONG_MRZ_CHECKSUM = TestScenario(
        id = "mrz_checksum",
        label = "Wrong MRZ checksum",
        description = "MRZ line 2 check digit is deliberately wrong",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )

    val UNDERAGE = TestScenario(
        id = "underage",
        label = "Underage holder",
        description = "Date of birth makes the holder 16 years old",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )

    val NFC_PA_FAIL = TestScenario(
        id = "nfc_pa_fail",
        label = "NFC Passive Auth failure",
        description = "NFC chip returns data but Passive Authentication fails (no valid CSCA)",
        expectedOutcome = ExpectedOutcome.MANUAL_REVIEW
    )

    val PARTIAL_CROP = TestScenario(
        id = "partial_crop",
        label = "Partially cropped",
        description = "Document is cut off at one edge — corners not visible",
        expectedOutcome = ExpectedOutcome.FAIL_SOFT
    )

    val SCREEN_RECAPTURE = TestScenario(
        id = "screen_recapture",
        label = "Screen recapture",
        description = "Photo is a screenshot of another screen (moire pattern)",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )

    val MISMATCHED_FACE = TestScenario(
        id = "face_mismatch",
        label = "Face mismatch",
        description = "Selfie does not match document photo",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )

    val VALID_NEAR_EXPIRY = TestScenario(
        id = "near_expiry",
        label = "Expiring in 15 days",
        description = "Document valid but expires within 30 days — some providers reject",
        expectedOutcome = ExpectedOutcome.MANUAL_REVIEW
    )

    val ALL: List<TestScenario> = listOf(
        HAPPY_PATH, EXPIRED_DOCUMENT, DARK_PHOTO, WRONG_MRZ_CHECKSUM,
        UNDERAGE, NFC_PA_FAIL, PARTIAL_CROP, SCREEN_RECAPTURE,
        MISMATCHED_FACE, VALID_NEAR_EXPIRY
    )
}
