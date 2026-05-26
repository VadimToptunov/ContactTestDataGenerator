package com.vadimtoptunov.generators.identity

import com.vadimtoptunov.generators.finance.CardGenerator
import com.vadimtoptunov.generators.finance.CardRecord
import com.vadimtoptunov.generators.finance.CardScheme
import com.vadimtoptunov.generators.finance.IbanGenerator
import com.vadimtoptunov.generators.finance.IbanRecord
import kotlin.random.Random

// ─── Request DSL ───────────────────────────────────────────────────────────

/**
 * Describes what NFC chip data should look like on the generated document.
 *
 * Three mutually exclusive modes:
 *  - [None]         → document has no NFC chip
 *  - [SameAsHolder] → chip matches the document holder (normal case)
 *  - [ThirdParty]   → chip holds a completely different person's identity
 *
 * In all cases [chipState] controls the scan-level behaviour (PA_FAIL, LOCKED, etc.).
 */
sealed class NfcRequest {

    /** Document has no NFC chip at all. */
    object None : NfcRequest()

    /**
     * Chip contains the same identity as the visual document.
     * Optionally degrade [chipState] to simulate read failures.
     */
    data class SameAsHolder(
        val chipState: NfcChipState = NfcChipState.READABLE,
        val faceState: FacePhotoState = FacePhotoState.MATCHES_DOCUMENT
    ) : NfcRequest()

    /**
     * Chip holds a completely different person — generated from [thirdPartyCountry]
     * (defaults to the same country as the document).
     *
     * The resulting [DocumentDiscrepancy.ChipIdentityVsDocument] will be recorded
     * so QA assertions can verify the engine detected the mismatch.
     */
    data class ThirdParty(
        val thirdPartyCountry: String? = null,   // null = same country
        val chipState: NfcChipState = NfcChipState.READABLE,
        val faceState: FacePhotoState = FacePhotoState.DIFFERENT_PERSON
    ) : NfcRequest()
}

/**
 * Full specification for a single generated identity.
 *
 * Usage:
 * ```
 * val identity = SyntheticIdentityGenerator().generate(
 *     DocumentRequest(
 *         countryCode  = "ES",
 *         documentType = DocumentType.PASSPORT,
 *         state        = DocumentState.EXPIRED,
 *         mrzMismatch  = MrzMismatch.NAME,
 *         nfc          = NfcRequest.ThirdParty()
 *     )
 * )
 * ```
 */
data class DocumentRequest(
    /** ISO 3166-1 alpha-2 (e.g. "ES", "DE", "BR"). */
    val countryCode:  String,

    /** Document type to generate. Null → first available for the country. */
    val documentType: DocumentType? = null,

    /** Document validity state (expiry, checksum, etc.). */
    val state:        DocumentState = DocumentState.VALID,

    /** Visual / image artifacts for OCR stress-testing. */
    val artifacts:    List<DocumentArtifact> = listOf(DocumentArtifact.NONE),

    /**
     * MRZ ↔ VIZ discrepancy to inject.
     * The MRZ is built from a *different* identity while the VIZ fields come from
     * the document holder, so the engine's cross-check fires.
     */
    val mrzMismatch:  MrzMismatch = MrzMismatch.NONE,

    /** NFC chip configuration — see [NfcRequest]. */
    val nfc:          NfcRequest = NfcRequest.SameAsHolder(),

    /** Override the auto-resolved scenario. */
    val scenario:     TestScenario? = null,

    val includeCard:  Boolean = true,
    val includeIban:  Boolean = true
)

/**
 * Which MRZ ↔ Visual Inspection Zone discrepancy to inject.
 * Applied by building the MRZ from a different [IdentityProfile] than the visual fields.
 */
enum class MrzMismatch {
    /** No discrepancy — MRZ matches visual zone. */
    NONE,
    /** MRZ uses a different person's name. */
    NAME,
    /** MRZ uses a different date of birth. */
    DATE_OF_BIRTH,
    /** MRZ encodes a different document number. */
    DOCUMENT_NUMBER,
    /** MRZ uses a different expiry date. */
    EXPIRY_DATE,
    /** All key fields in MRZ come from a different person. */
    ALL
}

// ─── Generator ─────────────────────────────────────────────────────────────

/**
 * Assembles a fully-populated [SyntheticIdentity] from a [DocumentRequest].
 *
 * LEGAL NOTICE — test data only. See [SyntheticIdentity] doc for details.
 */
class SyntheticIdentityGenerator(private val seed: Long? = null) {

    private val rng: Random = if (seed != null) Random(seed) else Random.Default

    // ── Public entry point ─────────────────────────────────────────────────

    fun generate(request: DocumentRequest): SyntheticIdentity {
        val profile = requireCountryProfile(request.countryCode)
        val docSpec = resolveDocumentSpec(profile, request.documentType)

        val holder   = buildIdentityProfile(profile, request.state)
        val address  = buildAddress(profile)
        val document = buildDocument(request, profile, docSpec, holder)

        return SyntheticIdentity(
            profile    = holder,
            documents  = listOf(document),
            address    = address,
            phone      = buildPhone(profile.countryCode),
            email      = buildEmail(holder.fullNameLatin),
            taxId      = buildTaxId(profile),
            bankCard   = if (request.includeCard) buildCard() else null,
            iban       = if (request.includeIban) buildIban(profile.countryCode) else null,
            scenario   = request.scenario ?: resolveScenario(request),
            seed       = seed
        )
    }

    // ── Country profile ─────────────────────────────────────────────────────

    private fun requireCountryProfile(code: String): CountryProfile =
        CountryProfiles[code] ?: error(
            "Country '$code' not found. Available: ${CountryProfiles.ALL.keys.sorted().joinToString()}"
        )

    private fun resolveDocumentSpec(profile: CountryProfile, requested: DocumentType?): DocumentTypeSpec =
        if (requested == null) profile.documentTypes.first()
        else profile.documentTypes.firstOrNull { it.type == requested } ?: profile.documentTypes.first()

    // ── Identity profile ────────────────────────────────────────────────────

    private fun buildIdentityProfile(profile: CountryProfile, state: DocumentState): IdentityProfile {
        val gender     = randomGender()
        val dob        = generateDateOfBirth(state)
        val nameNative = randomName(profile.nameDataset, gender)
        val nameLatin  = toLatin(nameNative, profile.nameDataset.script)
        return IdentityProfile(
            countryCode    = profile.countryCode,
            locale         = profile.locale,
            gender         = gender,
            dateOfBirth    = dob,
            fullNameNative = nameNative,
            fullNameLatin  = nameLatin,
            nationality    = profile.nationality
        )
    }

    // ── Document ────────────────────────────────────────────────────────────

    private fun buildDocument(
        request:  DocumentRequest,
        profile:  CountryProfile,
        docSpec:  DocumentTypeSpec,
        holder:   IdentityProfile
    ): SyntheticDocument {
        val docType   = docSpec.type
        val docNumber = generateDocumentNumber(docSpec)
        val docNumMrz = docNumber.uppercase().replace(" ", "").replace("-", "")
        val (issueDate, expiryDate) = generateDocumentDates(request.state, docSpec.validityYears)
        val issuingAuthority        = randomIssuingAuthority(profile.countryCode)

        // ── MRZ (optionally with injected mismatch) ───────────────────────
        val discrepancies = mutableListOf<DocumentDiscrepancy>()
        val mrz = if (docType.hasMrz) {
            buildMrzWithMismatch(
                request      = request,
                profile      = profile,
                holder       = holder,
                docNumMrz    = docNumMrz,
                expiryDate   = expiryDate,
                discrepancies = discrepancies
            )
        } else null

        // ── NFC chip ──────────────────────────────────────────────────────
        val nfcChip = buildNfcChip(
            request       = request,
            profile       = profile,
            holder        = holder,
            docNumMrz     = docNumMrz,
            expiryDate    = expiryDate,
            discrepancies = discrepancies
        )

        return SyntheticDocument(
            type               = docType,
            countryCode        = profile.countryCode,
            documentNumber     = docNumber,
            documentNumberMrz  = docNumMrz,
            issueDate          = issueDate,
            expiryDate         = expiryDate,
            issuingAuthority   = issuingAuthority,
            mrz                = mrz,
            nfcChip            = nfcChip,
            state              = request.state,
            artifacts          = request.artifacts,
            discrepancies      = discrepancies
        )
    }

    // ── MRZ with optional mismatch injection ────────────────────────────────

    /**
     * Builds the MRZ.
     *
     * When [DocumentRequest.mrzMismatch] is not NONE, a second "imposter" identity
     * is generated and its fields are substituted into the MRZ selectively,
     * while the visual zone retains the real holder's data.
     * The resulting [DocumentDiscrepancy] is appended to [discrepancies].
     */
    private fun buildMrzWithMismatch(
        request:       DocumentRequest,
        profile:       CountryProfile,
        holder:        IdentityProfile,
        docNumMrz:     String,
        expiryDate:    SimpleDate,
        discrepancies: MutableList<DocumentDiscrepancy>
    ): MrzData {
        if (request.mrzMismatch == MrzMismatch.NONE) {
            return buildMrz(
                docType    = resolveDocumentSpec(profile, request.documentType).type,
                profile    = profile,
                identity   = holder,
                docNumber  = docNumMrz,
                expiryDate = expiryDate,
                corrupt    = request.state == DocumentState.WRONG_CHECKSUM
            )
        }

        val imposter      = buildIdentityProfile(profile, DocumentState.VALID)
        val mrzIdentity   = applyMismatch(request.mrzMismatch, holder, imposter, discrepancies)
        val mrzDocNumber  = if (request.mrzMismatch == MrzMismatch.DOCUMENT_NUMBER ||
                                request.mrzMismatch == MrzMismatch.ALL) {
            generateDocumentNumber(resolveDocumentSpec(profile, request.documentType))
                .uppercase().replace(" ", "").replace("-", "")
                .also { alt -> discrepancies += DocumentDiscrepancy.MrzNumberVsVisual(alt, docNumMrz) }
        } else docNumMrz

        val mrzExpiry = if (request.mrzMismatch == MrzMismatch.EXPIRY_DATE ||
                            request.mrzMismatch == MrzMismatch.ALL) {
            generateDocumentDates(DocumentState.VALID,
                resolveDocumentSpec(profile, request.documentType).validityYears).second
                .also { alt -> discrepancies += DocumentDiscrepancy.MrzExpiryVsVisual(alt, expiryDate) }
        } else expiryDate

        return buildMrz(
            docType    = resolveDocumentSpec(profile, request.documentType).type,
            profile    = profile,
            identity   = mrzIdentity,
            docNumber  = mrzDocNumber,
            expiryDate = mrzExpiry,
            corrupt    = request.state == DocumentState.WRONG_CHECKSUM
        )
    }

    /** Selectively substitute [imposter] fields into [holder] per [mismatch] mode. */
    private fun applyMismatch(
        mismatch:      MrzMismatch,
        holder:        IdentityProfile,
        imposter:      IdentityProfile,
        discrepancies: MutableList<DocumentDiscrepancy>
    ): IdentityProfile {
        var result = holder
        if (mismatch == MrzMismatch.NAME || mismatch == MrzMismatch.ALL) {
            discrepancies += DocumentDiscrepancy.MrzNameVsVisual(imposter.fullNameLatin, holder.fullNameLatin)
            result = result.copy(fullNameLatin = imposter.fullNameLatin, fullNameNative = imposter.fullNameNative)
        }
        if (mismatch == MrzMismatch.DATE_OF_BIRTH || mismatch == MrzMismatch.ALL) {
            discrepancies += DocumentDiscrepancy.MrzDobVsVisual(imposter.dateOfBirth, holder.dateOfBirth)
            result = result.copy(dateOfBirth = imposter.dateOfBirth)
        }
        return result
    }

    // ── NFC chip ─────────────────────────────────────────────────────────────

    private fun buildNfcChip(
        request:       DocumentRequest,
        profile:       CountryProfile,
        holder:        IdentityProfile,
        docNumMrz:     String,
        expiryDate:    SimpleDate,
        discrepancies: MutableList<DocumentDiscrepancy>
    ): NfcChipData? {
        val docSpec = resolveDocumentSpec(profile, request.documentType)
        if (!docSpec.hasBiometricChip) return null

        return when (val nfcReq = request.nfc) {
            is NfcRequest.None -> null

            is NfcRequest.SameAsHolder -> {
                val chipMrz = if (docSpec.type.hasMrz)
                    buildMrz(docSpec.type, profile, holder, docNumMrz, expiryDate, corrupt = false)
                else null
                NfcChipData(
                    chipProfile    = holder,
                    chipMrz        = chipMrz,
                    facePhotoState = nfcReq.faceState,
                    chipState      = nfcReq.chipState,
                    identitySource = ChipIdentitySource.SAME_AS_DOCUMENT
                )
            }

            is NfcRequest.ThirdParty -> {
                val thirdCountryCode = nfcReq.thirdPartyCountry ?: profile.countryCode
                val thirdProfile     = requireCountryProfile(thirdCountryCode)
                val thirdHolder      = buildIdentityProfile(thirdProfile, DocumentState.VALID)
                val thirdDocSpec     = resolveDocumentSpec(thirdProfile, request.documentType)
                val chipMrz = if (thirdDocSpec.type.hasMrz)
                    buildMrz(thirdDocSpec.type, thirdProfile, thirdHolder, docNumMrz, expiryDate, corrupt = false)
                else null

                discrepancies += DocumentDiscrepancy.ChipIdentityVsDocument(
                    chipName     = thirdHolder.fullNameLatin,
                    documentName = holder.fullNameLatin
                )
                discrepancies += DocumentDiscrepancy.ChipFaceVsDocument(nfcReq.faceState)
                if (chipMrz != null) {
                    discrepancies += DocumentDiscrepancy.ChipMrzVsPrinted(
                        chipMrz    = chipMrz.raw,
                        printedMrz = buildMrz(docSpec.type, profile, holder, docNumMrz, expiryDate, corrupt = false).raw
                    )
                }

                NfcChipData(
                    chipProfile    = thirdHolder,
                    chipMrz        = chipMrz,
                    facePhotoState = nfcReq.faceState,
                    chipState      = nfcReq.chipState,
                    identitySource = ChipIdentitySource.THIRD_PARTY,
                    note           = "Chip identity: ${thirdHolder.fullNameLatin} " +
                                     "(${thirdHolder.nationality}) — not the document holder"
                )
            }
        }
    }

    // ── Core MRZ builder ────────────────────────────────────────────────────

    private fun buildMrz(
        docType:    DocumentType,
        profile:    CountryProfile,
        identity:   IdentityProfile,
        docNumber:  String,
        expiryDate: SimpleDate,
        corrupt:    Boolean
    ): MrzData {
        val parts   = identity.fullNameLatin.trim().split("\\s+".toRegex())
        val surname = parts.firstOrNull() ?: "SPECIMEN"
        val given   = parts.drop(1).joinToString(" ").ifBlank { "TEST" }
        return when (MrzBuilder.formatFor(docType)) {
            MrzFormat.TD3 -> MrzBuilder.buildTd3(
                documentTypeChar = 'P',
                documentSubType  = if (docType == DocumentType.FOREIGN_PASSPORT) 'F' else '<',
                issuingCountry   = profile.nationality,
                surname          = surname,
                givenNames       = given,
                documentNumber   = docNumber,
                nationality      = identity.nationality,
                dob              = identity.dateOfBirth,
                sex              = identity.gender,
                expiry           = expiryDate,
                corruptChecksum  = corrupt
            )
            MrzFormat.TD1, MrzFormat.TD2 -> MrzBuilder.buildTd1(
                documentTypeCode = MrzBuilder.typeCodeFor(docType),
                issuingCountry   = profile.nationality,
                documentNumber   = docNumber,
                dob              = identity.dateOfBirth,
                sex              = identity.gender,
                expiry           = expiryDate,
                nationality      = identity.nationality,
                surname          = surname,
                givenNames       = given,
                corruptChecksum  = corrupt
            )
        }
    }

    // ── Scenario resolution ─────────────────────────────────────────────────

    private fun resolveScenario(request: DocumentRequest): TestScenario {
        // Artifact-based scenarios first (most specific)
        request.artifacts.forEach { artifact ->
            when (artifact) {
                DocumentArtifact.DARK_PHOTO      -> return TestScenarios.DARK_PHOTO
                DocumentArtifact.PARTIAL_CROP    -> return TestScenarios.PARTIAL_CROP
                DocumentArtifact.SCREEN_CAPTURE  -> return TestScenarios.SCREEN_RECAPTURE
                DocumentArtifact.DIFFERENT_PERSON -> return TestScenarios.MISMATCHED_FACE
                DocumentArtifact.NFC_PA_FAIL     -> return TestScenarios.NFC_PA_FAIL
                DocumentArtifact.NFC_THIRD_PARTY -> return TestScenarios.NFC_THIRD_PARTY
                else -> { /* continue */ }
            }
        }
        // NFC request type
        if (request.nfc is NfcRequest.ThirdParty) return TestScenarios.NFC_THIRD_PARTY
        if (request.nfc is NfcRequest.SameAsHolder &&
            request.nfc.chipState == NfcChipState.WRONG_MRZ_KEY) return TestScenarios.NFC_WRONG_MRZ_KEY
        if (request.nfc is NfcRequest.SameAsHolder &&
            request.nfc.chipState == NfcChipState.PA_FAIL) return TestScenarios.NFC_PA_FAIL
        // MRZ mismatch
        if (request.mrzMismatch == MrzMismatch.NAME) return TestScenarios.MRZ_NAME_MISMATCH
        // Document state
        return when (request.state) {
            DocumentState.VALID          -> TestScenarios.HAPPY_PATH
            DocumentState.EXPIRED        -> TestScenarios.EXPIRED_DOCUMENT
            DocumentState.EXPIRING_SOON  -> TestScenarios.VALID_NEAR_EXPIRY
            DocumentState.WRONG_CHECKSUM -> TestScenarios.WRONG_MRZ_CHECKSUM
            DocumentState.UNDERAGE       -> TestScenarios.UNDERAGE
            else                         -> TestScenarios.HAPPY_PATH
        }
    }

    // ── Date generation ─────────────────────────────────────────────────────

    private fun generateDateOfBirth(state: DocumentState): SimpleDate {
        val today   = java.util.Calendar.getInstance()
        val ageYears = if (state == DocumentState.UNDERAGE) rng.nextInt(13, 17) else rng.nextInt(18, 65)
        val year    = today.get(java.util.Calendar.YEAR) - ageYears
        val month   = rng.nextInt(1, 13)
        val maxDay  = java.util.Calendar.getInstance()
            .apply { set(year, month - 1, 1) }
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        return SimpleDate(year, month, rng.nextInt(1, maxDay + 1))
    }

    private fun generateDocumentDates(state: DocumentState, validityYears: Int): Pair<SimpleDate, SimpleDate> {
        val now   = java.util.Calendar.getInstance()
        val year  = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1
        val day   = now.get(java.util.Calendar.DAY_OF_MONTH)
        return when (state) {
            DocumentState.EXPIRED -> {
                val expYear  = year - rng.nextInt(1, 5)
                val expMonth = rng.nextInt(1, 13)
                SimpleDate(expYear - validityYears, expMonth, 1) to
                SimpleDate(expYear, expMonth, rng.nextInt(1, 28))
            }
            DocumentState.EXPIRING_SOON -> {
                val expCal = java.util.Calendar.getInstance()
                    .apply { add(java.util.Calendar.DAY_OF_MONTH, rng.nextInt(5, 29)) }
                SimpleDate(year - validityYears, month, day) to
                SimpleDate(
                    expCal.get(java.util.Calendar.YEAR),
                    expCal.get(java.util.Calendar.MONTH) + 1,
                    expCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
            }
            DocumentState.FUTURE_ISSUE_DATE -> {
                val issueCal = java.util.Calendar.getInstance()
                    .apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }
                val issue = SimpleDate(
                    issueCal.get(java.util.Calendar.YEAR),
                    issueCal.get(java.util.Calendar.MONTH) + 1,
                    issueCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
                issue to SimpleDate(issue.year + validityYears, issue.month, issue.day)
            }
            else -> {
                val issueYear = year - rng.nextInt(0, validityYears)
                val issue     = SimpleDate(issueYear, rng.nextInt(1, 13), rng.nextInt(1, 28))
                issue to SimpleDate(issue.year + validityYears, issue.month, issue.day)
            }
        }
    }

    // ── Document number ──────────────────────────────────────────────────────

    private fun generateDocumentNumber(spec: DocumentTypeSpec): String {
        val length = spec.numberLength.coerceIn(6, 12)
        val hasLetters = "letter" in spec.numberFormat.lowercase() || "[A-Z]" in spec.numberRegex
        return if (hasLetters) {
            val letterCount = extractLetterCount(spec.numberRegex)
            randomLetters(letterCount) + randomDigits((length - letterCount).coerceAtLeast(4))
        } else {
            randomDigits(length)
        }
    }

    private fun extractLetterCount(regex: String): Int {
        val match = Regex("\\[A-Z]\\{(\\d+)\\}").find(regex)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 2
    }

    // ── Address ──────────────────────────────────────────────────────────────

    private fun buildAddress(profile: CountryProfile): SyntheticAddress {
        val fmt         = profile.addressFormat
        val region      = fmt.regions.random(rng)
        val city        = fmt.cities.random(rng)
        val street      = fmt.streetNames.random(rng)
        val houseNumber = rng.nextInt(1, 200)
        val postal      = generatePostalCode(fmt.postalCodeExample)
        val streetLine1 = if (fmt.streetFirst) "$houseNumber $street" else "$street, $houseNumber"
        val formatted   = if (fmt.streetFirst)
            "$streetLine1\n$city, $region $postal\n${profile.countryNameEn}"
        else
            "$postal $city, $region\n$streetLine1\n${profile.countryNameEn}"
        return SyntheticAddress(
            countryCode  = profile.countryCode,
            countryName  = profile.countryNameEn,
            region       = region,
            city         = city,
            postalCode   = postal,
            streetLine1  = streetLine1,
            formatted    = formatted
        )
    }

    private fun generatePostalCode(example: String): String =
        example.map { c ->
            when {
                c.isDigit()  -> rng.nextInt(0, 10).toString()
                c.isLetter() -> randomLetters(1)
                else         -> c.toString()
            }
        }.joinToString("")

    // ── Contact ──────────────────────────────────────────────────────────────

    private fun buildPhone(countryCode: String): String {
        val prefix = PHONE_PREFIXES[countryCode] ?: "+1"
        return "$prefix ${randomDigits(rng.nextInt(7, 10))}"
    }

    private fun buildEmail(latinName: String): String {
        val parts = latinName.lowercase().replace("[^a-z ]".toRegex(), "").trim().split(" ")
        val local = when {
            parts.size >= 2 -> "${parts[0]}.${parts[1]}"
            parts.size == 1 -> parts[0]
            else            -> "testuser"
        }
        return "$local@${EMAIL_DOMAINS.random(rng)}"
    }

    // ── Financial ────────────────────────────────────────────────────────────

    private fun buildTaxId(profile: CountryProfile): TaxIdRecord? {
        val code = profile.taxIdCountryCode ?: return null
        return TaxIdGenerator.ALL_SPECS.firstOrNull { it.countryCode == code }?.generateValid()
    }

    private fun buildCard(): CardRecord =
        CardGenerator(CardScheme.values().random(rng)).generate()

    private fun buildIban(countryCode: String): IbanRecord? = try {
        IbanGenerator(countryCode).generate()
    } catch (_: Exception) { null }

    // ── Name generation + transliteration ───────────────────────────────────

    private fun randomGender(): Gender = if (rng.nextBoolean()) Gender.MALE else Gender.FEMALE

    private fun randomName(dataset: NameDataset, gender: Gender): String {
        val first = if (gender == Gender.MALE) dataset.firstNamesMale.random(rng)
                    else dataset.firstNamesFemale.random(rng)
        return "$first ${dataset.lastNames.random(rng)}"
    }

    private fun toLatin(name: String, script: NameScript): String = when (script) {
        NameScript.LATIN     -> name.replace("[^A-Za-z ]".toRegex(), "").trim()
        NameScript.CYRILLIC  -> transliterate(name.uppercase(), CYRILLIC_MAP)
        NameScript.GEORGIAN  -> transliterate(name, GEORGIAN_MAP).uppercase()
        NameScript.ARMENIAN  -> transliterate(name.uppercase(), ARMENIAN_MAP)
        NameScript.ARABIC,
        NameScript.CHINESE,
        NameScript.DEVANAGARI,
        NameScript.HEBREW    -> romanizedPlaceholder(name)
    }

    /** Generic transliteration: map each char through [table], skip unknowns. */
    private fun transliterate(text: String, table: Map<Char, String>): String =
        text.map { c -> table[c] ?: if (c == ' ') " " else "" }.joinToString("")

    /** For non-Latin scripts: generate a romanized placeholder from name token count. */
    private fun romanizedPlaceholder(name: String): String =
        name.split(" ").mapIndexed { i, _ ->
            if (i == 0) LATIN_GIVEN_PLACEHOLDERS.random(rng)
            else LATIN_SURNAME_PLACEHOLDERS.random(rng)
        }.joinToString(" ")

    // ── Issuing authority ────────────────────────────────────────────────────

    private fun randomIssuingAuthority(countryCode: String): String =
        ISSUING_AUTHORITIES[countryCode]?.random(rng) ?: "Ministry of Interior"

    // ── Primitives ───────────────────────────────────────────────────────────

    private fun randomLetters(count: Int): String {
        val pool = "ABCDEFGHJKLMNPQRSTUVWXYZ"   // no I/O to avoid visual confusion
        return (1..count).map { pool[rng.nextInt(pool.length)] }.joinToString("")
    }

    private fun randomDigits(count: Int): String =
        (1..count).map { rng.nextInt(0, 10) }.joinToString("")

    // ─── Static tables ────────────────────────────────────────────────────────

    companion object {

        private val EMAIL_DOMAINS = listOf(
            "test.example.com", "qa-data.example.org",
            "synthetic.example.net", "kyctest.example.dev"
        )

        private val PHONE_PREFIXES = mapOf(
            "RU" to "+7",   "UA" to "+380", "US" to "+1",   "GB" to "+44",
            "DE" to "+49",  "FR" to "+33",  "ES" to "+34",  "IT" to "+39",
            "PL" to "+48",  "BR" to "+55",  "NL" to "+31",  "SE" to "+46",
            "CH" to "+41",  "AT" to "+43",  "BE" to "+32",  "PT" to "+351",
            "CZ" to "+420", "HU" to "+36",  "RO" to "+40",  "BG" to "+359",
            "GR" to "+30",  "TR" to "+90",  "IL" to "+972", "AE" to "+971",
            "SA" to "+966", "IN" to "+91",  "CN" to "+86",  "JP" to "+81",
            "KR" to "+82",  "SG" to "+65",  "AU" to "+61",  "NZ" to "+64",
            "CA" to "+1",   "MX" to "+52",  "AR" to "+54",  "CO" to "+57",
            "CL" to "+56",  "ZA" to "+27",  "NG" to "+234", "EG" to "+20",
            "ID" to "+62",  "TH" to "+66",  "VN" to "+84",  "MY" to "+60",
            "PH" to "+63",  "PK" to "+92",  "BD" to "+880", "GE" to "+995",
            "AM" to "+374", "KZ" to "+7"
        )

        private val ISSUING_AUTHORITIES = mapOf(
            "RU" to listOf("МВД России", "УМВД по г. Москве", "УМВД по г. Санкт-Петербургу"),
            "UA" to listOf("ДМС України", "Головне управління ДМС"),
            "US" to listOf("U.S. Department of State", "U.S. Citizenship and Immigration Services"),
            "GB" to listOf("HM Passport Office", "DVLA"),
            "DE" to listOf("Einwohnermeldeamt", "Bundesdruckerei GmbH", "Ausländerbehörde"),
            "FR" to listOf("Préfecture de Police", "Ministère de l'Intérieur"),
            "ES" to listOf("Dirección General de la Policía", "Ministerio del Interior"),
            "IT" to listOf("Ministero dell'Interno", "Questura di Roma"),
            "PL" to listOf("Ministerstwo Spraw Wewnętrznych", "Urząd Miejski"),
            "BR" to listOf("Polícia Federal", "Secretaria de Segurança Pública"),
            "IN" to listOf("Ministry of External Affairs", "Regional Passport Office"),
            "CN" to listOf("Ministry of Public Security", "Exit-Entry Administration Bureau"),
            "JP" to listOf("Ministry of Foreign Affairs", "Regional Immigration Bureau"),
            "AU" to listOf("Department of Home Affairs", "Australian Passport Office"),
            "CA" to listOf("Passport Canada", "Immigration, Refugees and Citizenship Canada")
        )

        private val LATIN_GIVEN_PLACEHOLDERS = listOf(
            "Ali", "Omar", "Hassan", "Wei", "Ming", "Raj", "Arjun", "Jin", "Park", "Kim"
        )
        private val LATIN_SURNAME_PLACEHOLDERS = listOf(
            "Ahmad", "Chen", "Wang", "Kumar", "Singh", "Kim", "Lee", "Nguyen", "Patel", "Sharma"
        )

        private val CYRILLIC_MAP = mapOf(
            'А' to "A",  'Б' to "B",  'В' to "V",  'Г' to "G",  'Д' to "D",
            'Е' to "E",  'Ё' to "YO", 'Ж' to "ZH", 'З' to "Z",  'И' to "I",
            'Й' to "Y",  'К' to "K",  'Л' to "L",  'М' to "M",  'Н' to "N",
            'О' to "O",  'П' to "P",  'Р' to "R",  'С' to "S",  'Т' to "T",
            'У' to "U",  'Ф' to "F",  'Х' to "KH", 'Ц' to "TS", 'Ч' to "CH",
            'Ш' to "SH", 'Щ' to "SCH",'Ъ' to "",   'Ы' to "Y",  'Ь' to "",
            'Э' to "E",  'Ю' to "YU", 'Я' to "YA",
            'І' to "I",  'Ї' to "YI", 'Є' to "YE", 'Ґ' to "G"
        )

        private val GEORGIAN_MAP = mapOf(
            'ა' to "a", 'ბ' to "b", 'გ' to "g", 'დ' to "d", 'ე' to "e",
            'ვ' to "v", 'ზ' to "z", 'თ' to "t", 'ი' to "i", 'კ' to "k",
            'ლ' to "l", 'მ' to "m", 'ნ' to "n", 'ო' to "o", 'პ' to "p",
            'ჟ' to "zh",'რ' to "r", 'ს' to "s", 'ტ' to "t", 'უ' to "u",
            'ფ' to "f", 'ქ' to "k", 'ღ' to "gh",'ყ' to "q", 'შ' to "sh",
            'ჩ' to "ch",'ც' to "ts",'ძ' to "dz",'წ' to "ts",'ჭ' to "ch",
            'ხ' to "kh",'ჯ' to "j", 'ჰ' to "h"
        )

        private val ARMENIAN_MAP = mapOf(
            'Ա' to "A", 'Բ' to "B", 'Գ' to "G", 'Դ' to "D", 'Ե' to "E",
            'Զ' to "Z", 'Է' to "E", 'Ը' to "Y", 'Թ' to "T", 'Ժ' to "ZH",
            'Ի' to "I", 'Լ' to "L", 'Խ' to "KH",'Ծ' to "TS",'Կ' to "K",
            'Հ' to "H", 'Ձ' to "DZ",'Ղ' to "GH",'Ճ' to "CH",'Մ' to "M",
            'Յ' to "Y", 'Ն' to "N", 'Շ' to "SH",'Ո' to "VO",'Չ' to "CH",
            'Պ' to "P", 'Ջ' to "J", 'Ռ' to "R", 'Ս' to "S", 'Վ' to "V",
            'Տ' to "T", 'Ր' to "R", 'Ց' to "TS",'Փ' to "P", 'Ք' to "K",
            'Օ' to "O", 'Ֆ' to "F"
        )

        // ── Factory helpers ────────────────────────────────────────────────

        /** Convenience: valid happy-path identity with no configuration. */
        fun happyPath(countryCode: String, seed: Long? = null): SyntheticIdentity =
            SyntheticIdentityGenerator(seed).generate(
                DocumentRequest(countryCode = countryCode)
            )

        /**
         * One identity per [DocumentState] — complete regression baseline for a country.
         */
        fun fullTestSuite(countryCode: String): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return DocumentState.values().map { state ->
                gen.generate(DocumentRequest(countryCode = countryCode, state = state))
            }
        }

        /**
         * All NFC failure modes for a country — useful for chip-scanning regression tests.
         */
        fun nfcTestSuite(countryCode: String): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return listOf(
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.SameAsHolder())),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.SameAsHolder(NfcChipState.PA_FAIL))),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.SameAsHolder(NfcChipState.LOCKED))),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.SameAsHolder(NfcChipState.PARTIAL_READ,
                    FacePhotoState.MISSING))),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.SameAsHolder(NfcChipState.WRONG_MRZ_KEY))),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.ThirdParty())),
                gen.generate(DocumentRequest(countryCode, nfc = NfcRequest.None))
            )
        }

        /**
         * All MRZ ↔ VIZ mismatch modes.
         */
        fun mrzMismatchSuite(countryCode: String): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return MrzMismatch.values().map { mismatch ->
                gen.generate(DocumentRequest(countryCode = countryCode, mrzMismatch = mismatch))
            }
        }

        /**
         * Valid passport for each of the 50 supported countries.
         */
        fun multiCountryPassports(
            countryCodes: List<String> = CountryProfiles.ALL.keys.sorted()
        ): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return countryCodes.mapNotNull { code ->
                runCatching {
                    gen.generate(DocumentRequest(countryCode = code, documentType = DocumentType.PASSPORT))
                }.getOrNull()
            }
        }
    }
}
