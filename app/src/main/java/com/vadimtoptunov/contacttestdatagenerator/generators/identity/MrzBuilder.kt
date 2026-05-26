package com.vadimtoptunov.contacttestdatagenerator.generators.identity

/**
 * ICAO 9303 Machine-Readable Zone (MRZ) builder.
 *
 * Implements:
 *   TD1  – 3 lines × 30 characters (ID cards, residence permits)
 *   TD3  – 2 lines × 44 characters (passports, travel documents)
 *
 * Check digit algorithm (ICAO 9303 Part 3, §4.9):
 *   Weights: 7, 3, 1 (repeating)
 *   Character values: 0-9 → face value; A-Z → 10-35; '<' → 0
 *   Check digit = sum(value[i] * weight[i]) mod 10
 *
 * SPECIMEN / TEST USE ONLY — not valid for any official purpose.
 */
object MrzBuilder {

    // ── Character value table ──────────────────────────────────────────────

    private val CHAR_VALUE: Map<Char, Int> = buildMap {
        for (c in '0'..'9') put(c, c - '0')
        for (c in 'A'..'Z') put(c, c - 'A' + 10)
        put('<', 0)
    }

    private val WEIGHTS = intArrayOf(7, 3, 1)

    /** Compute ICAO 9303 check digit for [field]. Returns '0'..'9'. */
    fun checkDigit(field: String): Char {
        val sum = field.uppercase().mapIndexed { i, c ->
            (CHAR_VALUE[c] ?: 0) * WEIGHTS[i % 3]
        }.sum()
        return ('0' + (sum % 10))
    }

    // ── MRZ field helpers ─────────────────────────────────────────────────

    /**
     * Pad or truncate [value] to exactly [length] characters,
     * replacing spaces with '<' and uppercasing.
     */
    fun pad(value: String, length: Int): String {
        val clean = value.uppercase().replace(' ', '<').replace('-', '<')
        return if (clean.length >= length) clean.take(length)
        else clean.padEnd(length, '<')
    }

    /** Format a name into MRZ surname<<given format, padded to [length]. */
    fun nameField(surname: String, givenNames: String, length: Int): String {
        val s = surname.uppercase().replace(' ', '<').replace('-', '<')
        val g = givenNames.uppercase().replace(' ', '<').replace('-', '<')
        val raw = "${s}<<${g}"
        return pad(raw, length)
    }

    // ── TD3: 2 × 44  (Passports, Foreign Passports, Refugee Travel) ───────

    /**
     * Build TD3 MRZ from components.
     *
     * Line 1 (44 chars):
     *   [0]    Document type  (P for passport, or 2-char code)
     *   [1]    Sub-type       ('<' for standard)
     *   [2–4]  Issuing country (3-letter ICAO)
     *   [5–43] Name field     (surname<<givennames, padded with '<')
     *
     * Line 2 (44 chars):
     *   [0–8]   Document number   (9 chars)
     *   [9]     Check digit (doc number)
     *   [10–12] Nationality        (3-letter ICAO)
     *   [13–18] Date of birth      (YYMMDD)
     *   [19]    Check digit (DOB)
     *   [20]    Sex                (M/F/<)
     *   [21–26] Expiry date        (YYMMDD)
     *   [27]    Check digit (expiry)
     *   [28–41] Personal number    (optional, padded '<')
     *   [42]    Check digit (personal number)
     *   [43]    Composite check    (doc#+check+DOB+check+expiry+check+personal+check)
     *
     * @param documentTypeChar 'P' for passport, 'V' for visa, etc.
     * @param documentSubType  Sub-type char, '<' if none
     * @param issuingCountry   ICAO 3-letter (e.g. "RUS", "USA")
     * @param surname          Holder surname (latin)
     * @param givenNames       Holder given names (latin, space-separated)
     * @param documentNumber   Document number (up to 9 chars)
     * @param nationality      ICAO 3-letter nationality
     * @param dob              Date of birth as SimpleDate
     * @param sex              Gender
     * @param expiry           Expiry date as SimpleDate
     * @param personalNumber   Optional personal number (up to 14 chars)
     * @param corruptChecksum  If true, flip the composite check digit (WRONG_CHECKSUM scenario)
     */
    fun buildTd3(
        documentTypeChar: Char = 'P',
        documentSubType: Char = '<',
        issuingCountry: String,
        surname: String,
        givenNames: String,
        documentNumber: String,
        nationality: String,
        dob: SimpleDate,
        sex: Gender,
        expiry: SimpleDate,
        personalNumber: String = "",
        corruptChecksum: Boolean = false
    ): MrzData {
        // Line 1
        val type = "$documentTypeChar$documentSubType"
        val country = pad(issuingCountry, 3)
        val nameF = nameField(surname, givenNames, 39)
        val line1 = "$type$country$nameF"
        check(line1.length == 44) { "TD3 line1 length=${line1.length}" }

        // Line 2
        val docNum = pad(documentNumber, 9)
        val docCheck = checkDigit(docNum)
        val nat = pad(nationality, 3)
        val dobStr = dob.mrz
        val dobCheck = checkDigit(dobStr)
        val sexChar = sex.mrzChar
        val expStr = expiry.mrz
        val expCheck = checkDigit(expStr)
        val personal = pad(personalNumber, 14)
        val personalCheck = checkDigit(personal)

        // Composite check: doc_num(9) + doc_check(1) + dob(6) + dob_check(1) + exp(6) + exp_check(1) + personal(14) + personal_check(1)
        val compositeSource = "$docNum$docCheck$dobStr$dobCheck$expStr$expCheck$personal$personalCheck"
        var compositeCheck = checkDigit(compositeSource)
        if (corruptChecksum) compositeCheck = corruptDigit(compositeCheck)

        val line2 = "$docNum$docCheck$nat$dobStr$dobCheck$sexChar$expStr$expCheck$personal$personalCheck$compositeCheck"
        check(line2.length == 44) { "TD3 line2 length=${line2.length}" }

        return MrzData(format = MrzFormat.TD3, line1 = line1, line2 = line2)
    }

    // ── TD1: 3 × 30  (National ID, Residence Permit, Driver's License) ────

    /**
     * Build TD1 MRZ from components.
     *
     * Line 1 (30 chars):
     *   [0–1]   Document type  (2 chars, e.g. "ID", "AC")
     *   [2–4]   Issuing country (3-letter ICAO)
     *   [5–13]  Document number (9 chars)
     *   [14]    Check digit (doc number)
     *   [15–29] Optional data 1 (15 chars, country-specific)
     *
     * Line 2 (30 chars):
     *   [0–5]   Date of birth (YYMMDD)
     *   [6]     Check digit (DOB)
     *   [7]     Sex
     *   [8–13]  Expiry date (YYMMDD)
     *   [14]    Check digit (expiry)
     *   [15–17] Nationality (3-letter ICAO)
     *   [18–29] Optional data 2 (12 chars)
     *
     * Line 3 (30 chars):
     *   [0–29]  Name field (surname<<givennames)
     *
     * Composite check (line2[0..28]): covers line1[5..29] + line2[0..6] + line2[8..14]
     * placed at line2[29].
     *
     * @param documentTypeCode 2-char code: "ID" for national ID, "AC" for residence permit, etc.
     * @param issuingCountry   ICAO 3-letter
     * @param documentNumber   Document number (up to 9 chars)
     * @param optionalData1    Optional data line 1 (up to 15 chars, e.g. personal number)
     * @param dob              Date of birth
     * @param sex              Gender
     * @param expiry           Expiry date
     * @param nationality      ICAO 3-letter nationality
     * @param optionalData2    Optional data line 2 (up to 12 chars)
     * @param surname          Holder surname (latin)
     * @param givenNames       Holder given names (latin)
     * @param corruptChecksum  If true, corrupt the composite check digit
     */
    fun buildTd1(
        documentTypeCode: String = "ID",
        issuingCountry: String,
        documentNumber: String,
        optionalData1: String = "",
        dob: SimpleDate,
        sex: Gender,
        expiry: SimpleDate,
        nationality: String,
        optionalData2: String = "",
        surname: String,
        givenNames: String,
        corruptChecksum: Boolean = false
    ): MrzData {
        // Line 1
        val typeCode = pad(documentTypeCode, 2)
        val country = pad(issuingCountry, 3)
        val docNum = pad(documentNumber, 9)
        val docCheck = checkDigit(docNum)
        val opt1 = pad(optionalData1, 15)
        val line1 = "$typeCode$country$docNum$docCheck$opt1"
        check(line1.length == 30) { "TD1 line1 length=${line1.length}" }

        // Line 2 (without composite check — placeholder)
        val dobStr = dob.mrz
        val dobCheck = checkDigit(dobStr)
        val sexChar = sex.mrzChar
        val expStr = expiry.mrz
        val expCheck = checkDigit(expStr)
        val nat = pad(nationality, 3)
        val opt2Raw = pad(optionalData2, 11)

        // Composite check covers line1[5..29] + line2[0..6] + line2[8..14]
        // i.e. doc_num(9) + doc_check(1) + opt1(15) + dob(6) + dob_check(1) + exp(6) + exp_check(1)
        val compositeSource = "$docNum$docCheck$opt1$dobStr$dobCheck$expStr$expCheck"
        var compositeCheck = checkDigit(compositeSource)
        if (corruptChecksum) compositeCheck = corruptDigit(compositeCheck)

        val line2 = "$dobStr$dobCheck$sexChar$expStr$expCheck$nat$opt2Raw$compositeCheck"
        check(line2.length == 30) { "TD1 line2 length=${line2.length}" }

        // Line 3
        val line3 = nameField(surname, givenNames, 30)
        check(line3.length == 30) { "TD1 line3 length=${line3.length}" }

        return MrzData(format = MrzFormat.TD1, line1 = line1, line2 = line2, line3 = line3)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Flip a check digit by +1 mod 10 (guaranteed wrong but still a digit). */
    private fun corruptDigit(c: Char): Char = ('0' + (c - '0' + 1) % 10)

    /**
     * Choose MRZ format for a document type:
     * - TD1 for National ID, Residence Permit, Driver's License
     * - TD3 for Passport, Foreign Passport, Refugee Travel
     */
    fun formatFor(type: DocumentType): MrzFormat = when (type) {
        DocumentType.NATIONAL_ID,
        DocumentType.RESIDENCE_PERMIT,
        DocumentType.DRIVERS_LICENSE -> MrzFormat.TD1

        DocumentType.PASSPORT,
        DocumentType.FOREIGN_PASSPORT,
        DocumentType.REFUGEE_TRAVEL -> MrzFormat.TD3
    }

    /**
     * Two-char TD1 type code for each document type.
     */
    fun typeCodeFor(type: DocumentType): String = when (type) {
        DocumentType.NATIONAL_ID      -> "ID"
        DocumentType.RESIDENCE_PERMIT -> "AC"
        DocumentType.DRIVERS_LICENSE  -> "DL"
        DocumentType.PASSPORT         -> "P<"   // not used in TD1 but kept for completeness
        DocumentType.FOREIGN_PASSPORT -> "P<"
        DocumentType.REFUGEE_TRAVEL   -> "PX"
    }
}
