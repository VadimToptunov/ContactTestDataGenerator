package com.vadimtoptunov.contacttestdatagenerator.generators.identity

import com.vadimtoptunov.contacttestdatagenerator.generators.finance.CardRecord
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.IbanRecord

/**
 * A complete synthetic test identity.
 *
 * IMPORTANT — LEGAL NOTICE:
 * All data is algorithmically generated and does NOT correspond to any real person.
 * Numbers pass checksum algorithms but are NOT registered in any government database,
 * NOT backed by real documents, and NOT usable for any official purpose.
 *
 * Intended use: software testing, QA automation, KYC flow development,
 * security research in authorized environments.
 *
 * Every rendered output carries a SPECIMEN / TEST DATA watermark.
 */
data class SyntheticIdentity(
    val profile:     IdentityProfile,
    val documents:   List<SyntheticDocument>,
    val address:     SyntheticAddress,
    val phone:       String,
    val email:       String,
    val taxId:       TaxIdRecord?,
    val bankCard:    CardRecord?,
    val iban:        IbanRecord?,
    val scenario:    TestScenario,
    val generatedAt: Long = System.currentTimeMillis(),
    val seed:        Long? = null
) {
    val label: String
        get() = "[${profile.countryCode}] ${profile.fullNameLatin} — ${scenario.label}"

    val primaryDocument: SyntheticDocument?
        get() = documents.firstOrNull { it.type == DocumentType.PASSPORT }
            ?: documents.firstOrNull()

    fun documentsOfType(type: DocumentType) = documents.filter { it.type == type }
}

// ─── Identity profile ──────────────────────────────────────────────────────

data class IdentityProfile(
    val countryCode:    String,   // ISO 3166-1 alpha-2
    val locale:         String,   // BCP-47 e.g. "ru-RU"
    val gender:         Gender,
    val dateOfBirth:    SimpleDate,
    val fullNameNative: String,   // In the country's script
    val fullNameLatin:  String,   // Transliterated for MRZ
    val nationality:    String    // ICAO 3-letter e.g. "RUS"
)

data class SimpleDate(val year: Int, val month: Int, val day: Int) {
    override fun toString()    = "%04d-%02d-%02d".format(year, month, day)
    val shortYear:   String get() = (year % 100).toString().padStart(2, '0')
    val monthPadded: String get() = month.toString().padStart(2, '0')
    val dayPadded:   String get() = day.toString().padStart(2, '0')
    /** YYMMDD — MRZ date format (ICAO 9303) */
    val mrz: String get() = "$shortYear$monthPadded$dayPadded"
}

enum class Gender(val mrzChar: Char) { MALE('M'), FEMALE('F') }

// ─── NFC chip data ─────────────────────────────────────────────────────────

/**
 * Data that lives inside the NFC eMRTD chip (ICAO 9303 LDS).
 *
 * Can be:
 *   - Consistent with the visual document (normal case)
 *   - From a completely different identity ([ChipIdentitySource.THIRD_PARTY])
 *   - A copy of the document holder but with altered fields ([ChipIdentitySource.TAMPERED])
 *   - Not present at all ([NfcChipState.NO_CHIP])
 *
 * Key data groups modelled:
 *   DG1  — MRZ (encoded as [MrzData])
 *   DG2  — Face photo (represented as [FacePhotoState])
 *   SOD  — Security Object / Passive Authentication result
 */
data class NfcChipData(
    /** Whose identity is stored in DG1/DG2 — may differ from the document holder. */
    val chipProfile:    IdentityProfile,

    /** DG1: MRZ stored on chip. May differ from the printed MRZ. */
    val chipMrz:        MrzData?,

    /** DG2: face photo state — affects biometric matching. */
    val facePhotoState: FacePhotoState = FacePhotoState.MATCHES_DOCUMENT,

    /** Overall chip behaviour during NFC scan. */
    val chipState:      NfcChipState = NfcChipState.READABLE,

    /** Relationship between chip data and the visual/printed document. */
    val identitySource: ChipIdentitySource = ChipIdentitySource.SAME_AS_DOCUMENT,

    /** Human-readable note for QA reports. */
    val note: String = ""
) {
    val specimenLabel: String = "SPECIMEN — TEST NFC DATA — NOT VALID"
}

enum class NfcChipState(val label: String) {
    READABLE("Chip readable, all DGs accessible"),
    LOCKED("Chip locked — BAC/PACE authentication fails"),
    NO_CHIP("No chip / chip not responding"),
    PARTIAL_READ("Partial read — DG2 (face) missing"),
    PA_FAIL("Passive Authentication fails — no valid CSCA signature"),
    WRONG_MRZ_KEY("BAC key derived from MRZ does not match chip"),
}

enum class FacePhotoState(val label: String) {
    MATCHES_DOCUMENT("Face matches visual document photo"),
    DIFFERENT_PERSON("Face photo is from a different person"),
    LOW_QUALITY("Low-quality / unrecognisable photo"),
    MISSING("DG2 absent — no biometric data"),
}

/**
 * Describes whose identity data is stored inside the chip —
 * the key dimension for NFC fraud simulation.
 */
enum class ChipIdentitySource(val label: String) {
    SAME_AS_DOCUMENT("Chip contains the same identity as the document"),
    THIRD_PARTY("Chip contains a completely different person's identity"),
    TAMPERED("Chip data matches document holder but with altered fields"),
    CLONED("Chip is a copy of a different valid document"),
}

// ─── Document discrepancy ──────────────────────────────────────────────────

/**
 * An explicit, testable discrepancy between data layers of the document.
 *
 * Regula and similar KYC engines cross-check:
 *   - Visual Inspection Zone (VIZ) vs MRZ
 *   - MRZ vs NFC chip DG1
 *   - NFC DG2 face vs selfie
 *
 * Each [DocumentDiscrepancy] represents one such mismatch, so QA tests
 * can assert exactly which check should fire.
 */
sealed class DocumentDiscrepancy(open val description: String) {

    /** Name in MRZ differs from name in VIZ (visual zone). */
    data class MrzNameVsVisual(
        val mrzName:    String,
        val visualName: String
    ) : DocumentDiscrepancy("MRZ name '$mrzName' ≠ VIZ name '$visualName'")

    /** Date of birth in MRZ differs from VIZ. */
    data class MrzDobVsVisual(
        val mrzDob:    SimpleDate,
        val visualDob: SimpleDate
    ) : DocumentDiscrepancy("MRZ DOB ${mrzDob.mrz} ≠ VIZ DOB ${visualDob.mrz}")

    /** Document number in MRZ differs from VIZ. */
    data class MrzNumberVsVisual(
        val mrzNumber:    String,
        val visualNumber: String
    ) : DocumentDiscrepancy("MRZ number '$mrzNumber' ≠ VIZ number '$visualNumber'")

    /** Chip DG1 MRZ differs from printed MRZ. */
    data class ChipMrzVsPrinted(
        val chipMrz:    String,
        val printedMrz: String
    ) : DocumentDiscrepancy("Chip DG1 MRZ ≠ printed MRZ")

    /** Chip holder name differs from document holder name. */
    data class ChipIdentityVsDocument(
        val chipName:     String,
        val documentName: String
    ) : DocumentDiscrepancy("Chip identity '$chipName' ≠ document holder '$documentName'")

    /** Chip face (DG2) does not match document holder. */
    data class ChipFaceVsDocument(
        val faceState: FacePhotoState
    ) : DocumentDiscrepancy("Chip DG2 face: ${faceState.label}")

    /** Expiry in MRZ differs from VIZ expiry. */
    data class MrzExpiryVsVisual(
        val mrzExpiry:    SimpleDate,
        val visualExpiry: SimpleDate
    ) : DocumentDiscrepancy("MRZ expiry ${mrzExpiry.mrz} ≠ VIZ expiry ${visualExpiry.mrz}")
}

// ─── Synthetic document ────────────────────────────────────────────────────

data class SyntheticDocument(
    val type:              DocumentType,
    val countryCode:       String,

    // ── Visual Inspection Zone (VIZ) ──────────────────────────────────────
    val documentNumber:    String,       // Country-format display (e.g. "AB 123456")
    val documentNumberMrz: String,       // MRZ-safe (uppercase, no spaces)
    val issueDate:         SimpleDate,
    val expiryDate:        SimpleDate,
    val issuingAuthority:  String,

    // ── Machine-Readable Zone ─────────────────────────────────────────────
    val mrz:               MrzData?,     // null for Driver's License etc.

    // ── NFC chip (eMRTD) ──────────────────────────────────────────────────
    val nfcChip:           NfcChipData?, // null = document has no chip

    // ── Test configuration ────────────────────────────────────────────────
    val state:             DocumentState,
    val artifacts:         List<DocumentArtifact>,

    /** Explicit list of cross-layer mismatches — populated when layers diverge. */
    val discrepancies:     List<DocumentDiscrepancy> = emptyList(),

    val specimenLabel:     String = "SPECIMEN — TEST DOCUMENT — NOT VALID"
) {
    val hasNfc: Boolean get() = nfcChip != null &&
        nfcChip.chipState != NfcChipState.NO_CHIP

    val isExpired: Boolean get() {
        val now = java.util.Calendar.getInstance()
        val y = now.get(java.util.Calendar.YEAR)
        val m = now.get(java.util.Calendar.MONTH) + 1
        return expiryDate.year < y || (expiryDate.year == y && expiryDate.month < m)
    }
}

enum class DocumentType(val label: String, val hasMrz: Boolean, val hasChin: Boolean) {
    PASSPORT("Passport",                   hasMrz = true,  hasChin = false),
    NATIONAL_ID("National ID Card",        hasMrz = true,  hasChin = true),
    RESIDENCE_PERMIT("Residence Permit",   hasMrz = true,  hasChin = false),
    DRIVERS_LICENSE("Driver's License",    hasMrz = false, hasChin = false),
    FOREIGN_PASSPORT("Foreign Passport",   hasMrz = true,  hasChin = false),
    REFUGEE_TRAVEL("Refugee Travel Doc",   hasMrz = true,  hasChin = false)
}

// ─── MRZ ──────────────────────────────────────────────────────────────────

/** ICAO 9303 MRZ data. TD1 = 3×30, TD2 = 2×36, TD3 = 2×44. */
data class MrzData(
    val format: MrzFormat,
    val line1:  String,
    val line2:  String,
    val line3:  String? = null   // TD1 only
) {
    val raw: String get() = listOfNotNull(line1, line2, line3).joinToString("\n")
}

enum class MrzFormat { TD1, TD2, TD3 }

// ─── Document states and artifacts ─────────────────────────────────────────

enum class DocumentState(val label: String) {
    VALID("Valid — all checks pass"),
    EXPIRED("Expired — past expiry date"),
    EXPIRING_SOON("Expiring soon — within 30 days"),
    WRONG_CHECKSUM("Wrong checksum — MRZ digits corrupt"),
    MISSING_FIELD("Missing field — required field blank"),
    UNDERAGE("Underage — holder < 18"),
    FUTURE_ISSUE_DATE("Future issue date — issued tomorrow"),
    TAMPERED_NUMBER("Tampered — document number altered"),
    BLACKLISTED("Blacklisted — number on deny list"),
}

enum class DocumentArtifact(val label: String) {
    NONE("Clean — no artifacts"),

    // Photo quality
    DARK_PHOTO("Dark photo — underexposed"),
    OVEREXPOSED("Overexposed — washed out"),
    BLURRY("Blurry — out of focus"),
    GLARE("Glare — reflection on surface"),
    PARTIAL_CROP("Partial crop — edges cut off"),
    ROTATED_90("Rotated 90°"),
    ROTATED_SLIGHT("Slightly rotated (~10°)"),
    LOW_RESOLUTION("Low resolution — below 100 DPI"),

    // Document condition
    FOLDED("Folded — crease across document"),
    DAMAGED_CORNER("Damaged corner"),
    WATER_DAMAGE("Water damage marks"),
    HANDWRITTEN_OVER("Handwritten marks over text"),

    // Capture conditions
    SCREEN_CAPTURE("Screen recapture (moire pattern)"),
    BLACK_AND_WHITE("B&W photocopy"),
    COVERED_FACE("Face partially covered"),
    DIFFERENT_PERSON("Photo replaced — face mismatch"),

    // NFC/chip conditions — complementary to NfcChipState
    NFC_CHIP_LOCKED("NFC chip locked — BAC fails"),
    NFC_NO_CHIP("No NFC chip response"),
    NFC_PA_FAIL("NFC Passive Auth fails — no CSCA"),
    NFC_PARTIAL_READ("NFC partial read — DG2 missing"),
    NFC_THIRD_PARTY("NFC chip belongs to a different person"),
}

// ─── Address ───────────────────────────────────────────────────────────────

data class SyntheticAddress(
    val countryCode: String,
    val countryName: String,
    val region:      String,
    val city:        String,
    val postalCode:  String,
    val streetLine1: String,
    val streetLine2: String? = null,
    val formatted:   String
)

// ─── Test scenario ─────────────────────────────────────────────────────────

data class TestScenario(
    val id:              String,
    val label:           String,
    val description:     String,
    val expectedOutcome: ExpectedOutcome
)

enum class ExpectedOutcome(val label: String) {
    PASS("Should pass KYC"),
    FAIL_SOFT("Should fail — recoverable (re-upload)"),
    FAIL_HARD("Should fail — hard reject"),
    MANUAL_REVIEW("Should trigger manual review"),
    ERROR("Should produce an error / exception")
}

object TestScenarios {

    val HAPPY_PATH = TestScenario(
        id = "happy_path", label = "Happy path",
        description = "Valid document, clean photo, NFC matches, all checks pass",
        expectedOutcome = ExpectedOutcome.PASS
    )
    val EXPIRED_DOCUMENT = TestScenario(
        id = "expired_doc", label = "Expired document",
        description = "Document expiry date is in the past",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val DARK_PHOTO = TestScenario(
        id = "dark_photo", label = "Dark photo",
        description = "Document photo is severely underexposed",
        expectedOutcome = ExpectedOutcome.FAIL_SOFT
    )
    val WRONG_MRZ_CHECKSUM = TestScenario(
        id = "mrz_checksum", label = "Wrong MRZ checksum",
        description = "MRZ composite check digit is deliberately wrong",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val UNDERAGE = TestScenario(
        id = "underage", label = "Underage holder",
        description = "Date of birth makes the holder 16 years old",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val NFC_PA_FAIL = TestScenario(
        id = "nfc_pa_fail", label = "NFC Passive Auth failure",
        description = "Chip returns data but Passive Authentication fails (no valid CSCA)",
        expectedOutcome = ExpectedOutcome.MANUAL_REVIEW
    )
    val NFC_THIRD_PARTY = TestScenario(
        id = "nfc_third_party", label = "NFC — third-party identity",
        description = "Chip DG1/DG2 contains a completely different person's data",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val NFC_WRONG_MRZ_KEY = TestScenario(
        id = "nfc_wrong_key", label = "NFC — wrong BAC key",
        description = "BAC key derived from printed MRZ does not open the chip",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val MRZ_NAME_MISMATCH = TestScenario(
        id = "mrz_name_mismatch", label = "MRZ name ≠ VIZ name",
        description = "Name in the machine-readable zone differs from the visual zone",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val PARTIAL_CROP = TestScenario(
        id = "partial_crop", label = "Partially cropped",
        description = "Document edge cut off — corners not visible",
        expectedOutcome = ExpectedOutcome.FAIL_SOFT
    )
    val SCREEN_RECAPTURE = TestScenario(
        id = "screen_recapture", label = "Screen recapture",
        description = "Photo is a screenshot of another screen (moire pattern)",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val MISMATCHED_FACE = TestScenario(
        id = "face_mismatch", label = "Face mismatch",
        description = "Selfie does not match document photo",
        expectedOutcome = ExpectedOutcome.FAIL_HARD
    )
    val VALID_NEAR_EXPIRY = TestScenario(
        id = "near_expiry", label = "Expiring in 15 days",
        description = "Document valid but expires within 30 days",
        expectedOutcome = ExpectedOutcome.MANUAL_REVIEW
    )

    val ALL: List<TestScenario> = listOf(
        HAPPY_PATH, EXPIRED_DOCUMENT, DARK_PHOTO, WRONG_MRZ_CHECKSUM,
        UNDERAGE, NFC_PA_FAIL, NFC_THIRD_PARTY, NFC_WRONG_MRZ_KEY,
        MRZ_NAME_MISMATCH, PARTIAL_CROP, SCREEN_RECAPTURE,
        MISMATCHED_FACE, VALID_NEAR_EXPIRY
    )
}
