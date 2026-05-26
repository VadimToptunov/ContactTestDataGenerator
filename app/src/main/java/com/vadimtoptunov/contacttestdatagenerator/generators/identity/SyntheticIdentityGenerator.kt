package com.vadimtoptunov.contacttestdatagenerator.generators.identity

import com.vadimtoptunov.contacttestdatagenerator.generators.finance.CardGenerator
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.CardScheme
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.IbanGenerator
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.CardRecord
import com.vadimtoptunov.contacttestdatagenerator.generators.finance.IbanRecord
import kotlin.random.Random

/**
 * Main assembler — builds a fully-populated [SyntheticIdentity] from parameters.
 *
 * LEGAL NOTICE:
 * All output is algorithmically generated test data. Numbers pass checksum
 * algorithms but are NOT registered in any government database, NOT usable
 * for any official purpose, and MUST NOT be presented as real documents.
 *
 * Every rendered identity carries a SPECIMEN / TEST DATA marker.
 *
 * Intended use: QA automation, KYC flow development, security testing
 * in authorized environments.
 */
class SyntheticIdentityGenerator(
    private val seed: Long? = null
) {

    private val rng: Random = if (seed != null) Random(seed) else Random.Default

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Generate a complete synthetic identity.
     *
     * @param countryCode   ISO 3166-1 alpha-2 country code (e.g. "DE", "BR")
     * @param documentType  Which document to create; if null → first available for country
     * @param state         Document validity state (VALID, EXPIRED, etc.)
     * @param artifacts     Visual/scan artifacts (NONE = clean document)
     * @param scenario      Named test scenario; if null → resolved from [state]
     * @param includeCard   Generate a synthetic bank card
     * @param includeIban   Generate a synthetic IBAN
     */
    fun generate(
        countryCode: String,
        documentType: DocumentType? = null,
        state: DocumentState = DocumentState.VALID,
        artifacts: List<DocumentArtifact> = listOf(DocumentArtifact.NONE),
        scenario: TestScenario? = null,
        includeCard: Boolean = true,
        includeIban: Boolean = true
    ): SyntheticIdentity {

        val profile = CountryProfiles[countryCode]
            ?: error("Country '$countryCode' not found in CountryProfiles. " +
                     "Available: ${CountryProfiles.ALL.keys.sorted().joinToString()}")

        val docSpec = resolveDocumentSpec(profile, documentType)
        val effectiveDocType = docSpec.type
        val resolvedScenario = scenario ?: scenarioFor(state, artifacts)

        // ── Identity profile ─────────────────────────────────────────────
        val gender = randomGender()
        val dob = generateDateOfBirth(state)
        val nameNative = randomName(profile.nameDataset, gender)
        val nameLatin = toLatin(nameNative, profile.nameDataset.script)

        val identityProfile = IdentityProfile(
            countryCode   = profile.countryCode,
            locale        = profile.locale,
            gender        = gender,
            dateOfBirth   = dob,
            fullNameNative = nameNative,
            fullNameLatin  = nameLatin,
            nationality    = profile.nationality
        )

        // ── Document ─────────────────────────────────────────────────────
        val document = buildDocument(
            profile    = profile,
            docSpec    = docSpec,
            identity   = identityProfile,
            state      = state,
            artifacts  = artifacts
        )

        // ── Address ──────────────────────────────────────────────────────
        val address = buildAddress(profile)

        // ── Contact ──────────────────────────────────────────────────────
        val phone = buildPhone(profile.countryCode)
        val email = buildEmail(nameLatin)

        // ── Financial ────────────────────────────────────────────────────
        val taxId = buildTaxId(profile)
        val bankCard = if (includeCard) buildCard() else null
        val iban = if (includeIban) buildIban(profile.countryCode) else null

        return SyntheticIdentity(
            profile    = identityProfile,
            documents  = listOf(document),
            address    = address,
            phone      = phone,
            email      = email,
            taxId      = taxId,
            bankCard   = bankCard,
            iban       = iban,
            scenario   = resolvedScenario,
            seed       = seed
        )
    }

    // ── Document spec resolution ───────────────────────────────────────────

    private fun resolveDocumentSpec(
        profile: CountryProfile,
        requested: DocumentType?
    ): DocumentTypeSpec {
        if (requested == null) return profile.documentTypes.first()
        return profile.documentTypes.firstOrNull { it.type == requested }
            ?: profile.documentTypes.first()  // fallback to primary if requested not available
    }

    // ── Document builder ───────────────────────────────────────────────────

    private fun buildDocument(
        profile: CountryProfile,
        docSpec: DocumentTypeSpec,
        identity: IdentityProfile,
        state: DocumentState,
        artifacts: List<DocumentArtifact>
    ): SyntheticDocument {

        val docType = docSpec.type
        val hasMrz = docType.hasMrz

        val docNumber = generateDocumentNumber(docSpec)
        val docNumberMrz = docNumber.uppercase().replace(" ", "").replace("-", "")

        val (issueDate, expiryDate) = generateDocumentDates(state, docSpec.validityYears)
        val issuingAuthority = randomIssuingAuthority(profile.countryCode)

        val corruptMrz = (state == DocumentState.WRONG_CHECKSUM)
        val mrz: MrzData? = if (hasMrz) {
            buildMrz(
                docType     = docType,
                profile     = profile,
                identity    = identity,
                docNumber   = docNumberMrz,
                issueDate   = issueDate,
                expiryDate  = expiryDate,
                corrupt     = corruptMrz
            )
        } else null

        return SyntheticDocument(
            type               = docType,
            countryCode        = profile.countryCode,
            documentNumber     = docNumber,
            documentNumberMrz  = docNumberMrz,
            issueDate          = issueDate,
            expiryDate         = expiryDate,
            issuingAuthority   = issuingAuthority,
            mrz                = mrz,
            state              = state,
            artifacts          = artifacts
        )
    }

    // ── MRZ builder ────────────────────────────────────────────────────────

    private fun buildMrz(
        docType:    DocumentType,
        profile:    CountryProfile,
        identity:   IdentityProfile,
        docNumber:  String,
        issueDate:  SimpleDate,
        expiryDate: SimpleDate,
        corrupt:    Boolean
    ): MrzData {
        val parts = identity.fullNameLatin.trim().split("\\s+".toRegex())
        val surname = parts.firstOrNull() ?: "SPECIMEN"
        val given = parts.drop(1).joinToString(" ").ifBlank { "TEST" }

        return when (MrzBuilder.formatFor(docType)) {
            MrzFormat.TD3 -> MrzBuilder.buildTd3(
                documentTypeChar = if (docType == DocumentType.REFUGEE_TRAVEL) 'P' else 'P',
                documentSubType  = if (docType == DocumentType.FOREIGN_PASSPORT) 'F' else '<',
                issuingCountry   = profile.nationality,
                surname          = surname,
                givenNames       = given,
                documentNumber   = docNumber,
                nationality      = identity.nationality,
                dob              = identity.dateOfBirth,
                sex              = identity.gender,
                expiry           = expiryDate,
                personalNumber   = "",
                corruptChecksum  = corrupt
            )
            MrzFormat.TD1, MrzFormat.TD2 -> MrzBuilder.buildTd1(
                documentTypeCode = MrzBuilder.typeCodeFor(docType),
                issuingCountry   = profile.nationality,
                documentNumber   = docNumber,
                optionalData1    = "",
                dob              = identity.dateOfBirth,
                sex              = identity.gender,
                expiry           = expiryDate,
                nationality      = identity.nationality,
                optionalData2    = "",
                surname          = surname,
                givenNames       = given,
                corruptChecksum  = corrupt
            )
        }
    }

    // ── Date generation ────────────────────────────────────────────────────

    private fun generateDateOfBirth(state: DocumentState): SimpleDate {
        val ageYears = when (state) {
            DocumentState.UNDERAGE -> rng.nextInt(13, 17)   // 13-16 years old
            else                   -> rng.nextInt(18, 65)
        }
        val today = java.util.Calendar.getInstance()
        val year  = today.get(java.util.Calendar.YEAR) - ageYears
        val month = rng.nextInt(1, 13)
        val maxDay = java.util.Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val day = rng.nextInt(1, maxDay + 1)
        return SimpleDate(year, month, day)
    }

    private fun generateDocumentDates(
        state: DocumentState,
        validityYears: Int
    ): Pair<SimpleDate, SimpleDate> {
        val today = java.util.Calendar.getInstance()
        val currentYear  = today.get(java.util.Calendar.YEAR)
        val currentMonth = today.get(java.util.Calendar.MONTH) + 1
        val currentDay   = today.get(java.util.Calendar.DAY_OF_MONTH)

        return when (state) {
            DocumentState.EXPIRED -> {
                val expYear  = currentYear - rng.nextInt(1, 5)
                val expMonth = rng.nextInt(1, 13)
                val issue = SimpleDate(expYear - validityYears, expMonth, 1)
                val expiry = SimpleDate(expYear, expMonth, rng.nextInt(1, 28))
                issue to expiry
            }
            DocumentState.EXPIRING_SOON -> {
                val daysUntilExpiry = rng.nextInt(5, 29)
                val expCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, daysUntilExpiry) }
                val expiry = SimpleDate(
                    expCal.get(java.util.Calendar.YEAR),
                    expCal.get(java.util.Calendar.MONTH) + 1,
                    expCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val issue = SimpleDate(currentYear - validityYears, currentMonth, currentDay)
                issue to expiry
            }
            DocumentState.FUTURE_ISSUE_DATE -> {
                // Issue date is tomorrow — should trigger a validation error
                val issueCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }
                val issue = SimpleDate(
                    issueCal.get(java.util.Calendar.YEAR),
                    issueCal.get(java.util.Calendar.MONTH) + 1,
                    issueCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val expiry = SimpleDate(issue.year + validityYears, issue.month, issue.day)
                issue to expiry
            }
            else -> {
                // Standard valid document
                val issueYear = currentYear - rng.nextInt(0, validityYears)
                val issue  = SimpleDate(issueYear, rng.nextInt(1, 13), rng.nextInt(1, 28))
                val expiry = SimpleDate(issue.year + validityYears, issue.month, issue.day)
                issue to expiry
            }
        }
    }

    // ── Document number generation ─────────────────────────────────────────

    private fun generateDocumentNumber(spec: DocumentTypeSpec): String {
        // Generate a random alphanumeric document number matching typical length
        val length = spec.numberLength.coerceIn(6, 12)
        // Simple heuristic: if numberFormat mentions "letters" use alpha prefix
        return if ("letter" in spec.numberFormat.lowercase() || spec.numberRegex.contains("[A-Z]")) {
            val letterCount = spec.numberRegex.countLetterGroupLength()
            val digitCount  = (length - letterCount).coerceAtLeast(4)
            randomLetters(letterCount) + randomDigits(digitCount)
        } else {
            randomDigits(length)
        }
    }

    private fun String.countLetterGroupLength(): Int {
        val match = Regex("\\[A-Z]\\{(\\d+)\\}|\\[A-Z]\\+(\\d+)").find(this)
        return match?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toIntOrNull() ?: 2
    }

    // ── Address builder ────────────────────────────────────────────────────

    private fun buildAddress(profile: CountryProfile): SyntheticAddress {
        val fmt    = profile.addressFormat
        val region = fmt.regions.random(rng)
        val city   = fmt.cities.random(rng)
        val street = fmt.streetNames.random(rng)
        val houseNumber = rng.nextInt(1, 200)
        val postal = generatePostalCode(fmt.postalCodeExample)

        val streetLine1 = if (fmt.streetFirst) "$houseNumber $street" else "$street, $houseNumber"
        val formatted = if (fmt.streetFirst) {
            "$streetLine1\n$city, $region $postal\n${profile.countryNameEn}"
        } else {
            "$postal $city, $region\n$streetLine1\n${profile.countryNameEn}"
        }

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

    private fun generatePostalCode(example: String): String {
        return example.map { c ->
            when {
                c.isDigit() -> rng.nextInt(0, 10).toString()
                c.isLetter() -> randomLetters(1)
                else -> c.toString()
            }
        }.joinToString("")
    }

    // ── Contact data ───────────────────────────────────────────────────────

    private fun buildPhone(countryCode: String): String {
        val prefix = PHONE_PREFIXES[countryCode] ?: "+1"
        val number = randomDigits(rng.nextInt(7, 10))
        return "$prefix $number"
    }

    private fun buildEmail(latinName: String): String {
        val parts = latinName.lowercase()
            .replace("[^a-z ]".toRegex(), "")
            .trim()
            .split(" ")
        val local = when {
            parts.size >= 2 -> "${parts[0]}.${parts[1]}"
            parts.size == 1 -> parts[0]
            else -> "testuser"
        }
        val domain = EMAIL_DOMAINS.random(rng)
        return "$local@$domain"
    }

    // ── Financial data ─────────────────────────────────────────────────────

    private fun buildTaxId(profile: CountryProfile): TaxIdRecord? {
        val code = profile.taxIdCountryCode ?: return null
        val spec = TaxIdGenerator.ALL_SPECS.firstOrNull { it.countryCode == code } ?: return null
        return spec.generateValid()
    }

    private fun buildCard(): CardRecord {
        val scheme = CardScheme.values().random(rng)
        return CardGenerator(scheme).generate()
    }

    private fun buildIban(countryCode: String): IbanRecord? {
        return try {
            IbanGenerator(countryCode).generate()
        } catch (_: Exception) {
            null  // country may not have IBAN support yet
        }
    }

    // ── Name helpers ───────────────────────────────────────────────────────

    private fun randomGender(): Gender = if (rng.nextBoolean()) Gender.MALE else Gender.FEMALE

    private fun randomName(dataset: NameDataset, gender: Gender): String {
        val first = if (gender == Gender.MALE)
            dataset.firstNamesMale.random(rng)
        else
            dataset.firstNamesFemale.random(rng)
        val last = dataset.lastNames.random(rng)
        return "$first $last"
    }

    /** Best-effort transliteration for MRZ. Cyrillic → Latin; others → simple ASCII strip. */
    private fun toLatin(name: String, script: NameScript): String = when (script) {
        NameScript.LATIN -> name.replace("[^A-Za-z ]".toRegex(), "").trim()
        NameScript.CYRILLIC -> cyrillicToLatin(name)
        NameScript.ARABIC, NameScript.CHINESE,
        NameScript.DEVANAGARI, NameScript.HEBREW -> {
            // For non-Latin scripts, generate a plausible romanized placeholder
            val tokens = name.split(" ")
            tokens.mapIndexed { i, t ->
                if (i == 0) LATIN_MALE_PLACEHOLDERS.random(rng)
                else LATIN_LAST_PLACEHOLDERS.random(rng)
            }.joinToString(" ")
        }
        NameScript.GEORGIAN -> georgianToLatin(name)
        NameScript.ARMENIAN -> armenianToLatin(name)
    }

    // ── Issuing authority ──────────────────────────────────────────────────

    private fun randomIssuingAuthority(countryCode: String): String {
        return ISSUING_AUTHORITIES[countryCode]?.random(rng) ?: "Ministry of Interior"
    }

    // ── Scenario mapping ───────────────────────────────────────────────────

    private fun scenarioFor(state: DocumentState, artifacts: List<DocumentArtifact>): TestScenario {
        // Check artifact-based scenario first
        if (DocumentArtifact.DARK_PHOTO in artifacts) return TestScenarios.DARK_PHOTO
        if (DocumentArtifact.PARTIAL_CROP in artifacts) return TestScenarios.PARTIAL_CROP
        if (DocumentArtifact.SCREEN_CAPTURE in artifacts) return TestScenarios.SCREEN_RECAPTURE
        if (DocumentArtifact.DIFFERENT_PERSON in artifacts) return TestScenarios.MISMATCHED_FACE
        if (DocumentArtifact.NFC_PA_FAIL in artifacts) return TestScenarios.NFC_PA_FAIL

        return when (state) {
            DocumentState.VALID           -> TestScenarios.HAPPY_PATH
            DocumentState.EXPIRED         -> TestScenarios.EXPIRED_DOCUMENT
            DocumentState.EXPIRING_SOON   -> TestScenarios.VALID_NEAR_EXPIRY
            DocumentState.WRONG_CHECKSUM  -> TestScenarios.WRONG_MRZ_CHECKSUM
            DocumentState.UNDERAGE        -> TestScenarios.UNDERAGE
            else                          -> TestScenarios.HAPPY_PATH
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private fun randomLetters(count: Int): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"   // skip I/O to avoid confusion
        return (1..count).map { letters[rng.nextInt(letters.length)] }.joinToString("")
    }

    private fun randomDigits(count: Int): String =
        (1..count).map { rng.nextInt(0, 10) }.joinToString("")

    // ── Static data tables ────────────────────────────────────────────────

    companion object {

        private val EMAIL_DOMAINS = listOf(
            "test.example.com", "qa-data.example.org", "synthetic.example.net",
            "devtest.example.io", "kyctest.example.dev"
        )

        private val PHONE_PREFIXES = mapOf(
            "RU" to "+7", "UA" to "+380", "US" to "+1", "GB" to "+44",
            "DE" to "+49", "FR" to "+33", "ES" to "+34", "IT" to "+39",
            "PL" to "+48", "BR" to "+55", "NL" to "+31", "SE" to "+46",
            "CH" to "+41", "AT" to "+43", "BE" to "+32", "PT" to "+351",
            "CZ" to "+420", "HU" to "+36", "RO" to "+40", "BG" to "+359",
            "GR" to "+30", "TR" to "+90", "IL" to "+972", "AE" to "+971",
            "SA" to "+966", "IN" to "+91", "CN" to "+86", "JP" to "+81",
            "KR" to "+82", "SG" to "+65", "AU" to "+61", "NZ" to "+64",
            "CA" to "+1", "MX" to "+52", "AR" to "+54", "CO" to "+57",
            "CL" to "+56", "ZA" to "+27", "NG" to "+234", "EG" to "+20",
            "ID" to "+62", "TH" to "+66", "VN" to "+84", "MY" to "+60",
            "PH" to "+63", "PK" to "+92", "BD" to "+880", "GE" to "+995",
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

        // Placeholders for non-Latin name romanization
        private val LATIN_MALE_PLACEHOLDERS = listOf(
            "Ali", "Omar", "Hassan", "Wei", "Ming", "Raj", "Arjun", "Jin", "Park", "Kim"
        )
        private val LATIN_LAST_PLACEHOLDERS = listOf(
            "Ahmad", "Chen", "Wang", "Kumar", "Singh", "Kim", "Lee", "Nguyen", "Patel", "Sharma"
        )

        // ── Cyrillic → Latin transliteration (ICAO-style) ──────────────

        private val CYRILLIC_MAP = mapOf(
            'А' to "A",  'Б' to "B",  'В' to "V",  'Г' to "G",  'Д' to "D",
            'Е' to "E",  'Ё' to "YO", 'Ж' to "ZH", 'З' to "Z",  'И' to "I",
            'Й' to "Y",  'К' to "K",  'Л' to "L",  'М' to "M",  'Н' to "N",
            'О' to "O",  'П' to "P",  'Р' to "R",  'С' to "S",  'Т' to "T",
            'У' to "U",  'Ф' to "F",  'Х' to "KH", 'Ц' to "TS", 'Ч' to "CH",
            'Ш' to "SH", 'Щ' to "SCH",'Ъ' to "",   'Ы' to "Y",  'Ь' to "",
            'Э' to "E",  'Ю' to "YU", 'Я' to "YA",
            // Ukrainian extra
            'І' to "I",  'Ї' to "YI", 'Є' to "YE", 'Ґ' to "G"
        )

        private fun cyrillicToLatin(text: String): String =
            text.uppercase().map { c -> CYRILLIC_MAP[c] ?: if (c == ' ') " " else "" }.joinToString("")

        // ── Georgian → Latin ──────────────────────────────────────────────

        private val GEORGIAN_MAP = mapOf(
            'ა' to "a", 'ბ' to "b", 'გ' to "g", 'დ' to "d", 'ე' to "e",
            'ვ' to "v", 'ზ' to "z", 'თ' to "t", 'ი' to "i", 'კ' to "k",
            'ლ' to "l", 'მ' to "m", 'ნ' to "n", 'ო' to "o", 'პ' to "p",
            'ჟ' to "zh",'რ' to "r", 'ს' to "s", 'ტ' to "t", 'უ' to "u",
            'ფ' to "f", 'ქ' to "k", 'ღ' to "gh",'ყ' to "q", 'შ' to "sh",
            'ჩ' to "ch",'ც' to "ts",'ძ' to "dz",'წ' to "ts",'ჭ' to "ch",
            'ხ' to "kh",'ჯ' to "j", 'ჰ' to "h"
        )

        private fun georgianToLatin(text: String): String =
            text.map { c -> GEORGIAN_MAP[c] ?: if (c == ' ') " " else c.toString() }
                .joinToString("").uppercase()

        // ── Armenian → Latin ──────────────────────────────────────────────

        private val ARMENIAN_MAP = mapOf(
            'Ա' to "A", 'Բ' to "B", 'Գ' to "G", 'Դ' to "D", 'Ե' to "E",
            'Զ' to "Z", 'Է' to "E", 'Ը' to "Y", 'Թ' to "T", 'Ժ' to "ZH",
            'Ի' to "I", 'Լ' to "L", 'Խ' to "KH",'Ծ' to "TS",'Կ' to "K",
            'Հ' to "H", 'Ձ' to "DZ",'Ղ' to "GH",'Ճ' to "CH",'Մ' to "M",
            'Յ' to "Y", 'Ն' to "N", 'Շ' to "SH",'Ո' to "VO",'Չ' to "CH",
            'Պ' to "P", 'Ջ' to "J", 'Ռ' to "R", 'Ս' to "S", 'Վ' to "V",
            'Տ' to "T", 'Ր' to "R", 'Ց' to "TS",'Փ' to "P", 'Ք' to "K",
            'Եվ' to "EV",'Օ' to "O", 'Ֆ' to "F"
        )

        private fun armenianToLatin(text: String): String =
            text.uppercase().map { c -> ARMENIAN_MAP[c] ?: if (c == ' ') " " else "" }.joinToString("")

        // ── Factory helpers ───────────────────────────────────────────────

        /**
         * Quick-generate a valid happy-path identity for [countryCode].
         * Convenience shortcut — no configuration needed.
         */
        fun happyPath(countryCode: String, seed: Long? = null): SyntheticIdentity =
            SyntheticIdentityGenerator(seed).generate(
                countryCode  = countryCode,
                state        = DocumentState.VALID,
                artifacts    = listOf(DocumentArtifact.NONE),
                scenario     = TestScenarios.HAPPY_PATH
            )

        /**
         * Generate a batch of identities for [countryCode], one per [DocumentState].
         * Useful for building a complete regression test suite in one call.
         */
        fun fullTestSuite(countryCode: String): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return DocumentState.values().map { state ->
                gen.generate(countryCode = countryCode, state = state)
            }
        }

        /**
         * Generate one identity per supported country with a standard VALID passport.
         */
        fun multiCountryPassports(
            countryCodes: List<String> = CountryProfiles.ALL.keys.sorted()
        ): List<SyntheticIdentity> {
            val gen = SyntheticIdentityGenerator()
            return countryCodes.mapNotNull { code ->
                try {
                    gen.generate(
                        countryCode  = code,
                        documentType = DocumentType.PASSPORT,
                        state        = DocumentState.VALID
                    )
                } catch (_: Exception) { null }
            }
        }
    }
}
