package com.vadimtoptunov.contacttestdatagenerator.generators.identity

/**
 * Validates Tax ID strings for all supported countries.
 *
 * Returns a structured [ValidationResult] with:
 * - whether it passed
 * - which specific rule failed (format, length, checksum, blacklist)
 * - a human-readable message
 *
 * Useful for:
 *   - QA: verify the app rejects each invalid variant the generator produced
 *   - Bug bounty: probe server-side vs. client-side validation differences
 *   - Dev: sanity-check numbers copied from test reports
 */
object TaxIdValidator {

    data class ValidationResult(
        val input: String,
        val country: String,
        val typeName: String,
        val isValid: Boolean,
        val failureReason: FailureReason?,
        val message: String
    )

    enum class FailureReason {
        WRONG_LENGTH,
        WRONG_CHARACTERS,
        FAILED_CHECKSUM,
        BLACKLISTED,
        UNKNOWN_COUNTRY
    }

    fun validate(input: String, countryCode: String): ValidationResult {
        return when (countryCode.uppercase()) {
            "BR" -> validateCpf(input)
            "DE" -> validateIdnr(input)
            "RU" -> validateInn(input)
            "UA" -> validateRnokpp(input)
            "ES" -> validateNif(input)
            "US" -> validateSsn(input)
            "PL" -> validateNip(input)
            "FR" -> validateNir(input)
            "IT" -> validateCodiceFiscale(input)
            else -> ValidationResult(
                input, countryCode, "?",
                false, FailureReason.UNKNOWN_COUNTRY,
                "Country '$countryCode' is not supported"
            )
        }
    }

    // ─── Brazil CPF ───────────────────────────────────────────────────────────

    private fun validateCpf(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 11)
            return fail(input, "BR", "CPF", FailureReason.WRONG_LENGTH,
                "Expected 11 digits, got ${d.length}")
        if (d.all { it == d[0] })
            return fail(input, "BR", "CPF", FailureReason.BLACKLISTED,
                "All-same-digit CPF is reserved")
        if (!TaxIdAlgorithms.cpfValid(d))
            return fail(input, "BR", "CPF", FailureReason.FAILED_CHECKSUM,
                "Check digits do not match (expected ${TaxIdAlgorithms.cpfCheckDigits(d.take(9))})")
        return pass(input, "BR", "CPF")
    }

    // ─── Germany IdNr ─────────────────────────────────────────────────────────

    private fun validateIdnr(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 11)
            return fail(input, "DE", "Steuer-IdNr", FailureReason.WRONG_LENGTH,
                "Expected 11 digits, got ${d.length}")
        if (d[0] == '0')
            return fail(input, "DE", "Steuer-IdNr", FailureReason.BLACKLISTED,
                "First digit must not be 0")
        if (!TaxIdAlgorithms.idnrValid(d))
            return fail(input, "DE", "Steuer-IdNr", FailureReason.FAILED_CHECKSUM,
                "ISO 7064 Mod-11,10 check digit failed")
        return pass(input, "DE", "Steuer-IdNr")
    }

    // ─── Russia INN ───────────────────────────────────────────────────────────

    private fun validateInn(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 12)
            return fail(input, "RU", "ИНН", FailureReason.WRONG_LENGTH,
                "Expected 12 digits, got ${d.length}")
        if (!TaxIdAlgorithms.innPersonalValid(d))
            return fail(input, "RU", "ИНН", FailureReason.FAILED_CHECKSUM,
                "INN check digits (positions 11-12) do not match")
        return pass(input, "RU", "ИНН")
    }

    // ─── Ukraine РНОКПП ───────────────────────────────────────────────────────

    private fun validateRnokpp(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 10)
            return fail(input, "UA", "РНОКПП", FailureReason.WRONG_LENGTH,
                "Expected 10 digits, got ${d.length}")
        if (!TaxIdAlgorithms.rnokppValid(d))
            return fail(input, "UA", "РНОКПП", FailureReason.FAILED_CHECKSUM,
                "Check digit (position 10) does not match")
        return pass(input, "UA", "РНОКПП")
    }

    // ─── Spain NIF ────────────────────────────────────────────────────────────

    private fun validateNif(input: String): ValidationResult {
        val clean = input.trim().uppercase()
        if (clean.length != 9)
            return fail(input, "ES", "NIF", FailureReason.WRONG_LENGTH,
                "Expected 9 characters (8 digits + 1 letter), got ${clean.length}")
        if (!clean.take(8).all { it.isDigit() })
            return fail(input, "ES", "NIF", FailureReason.WRONG_CHARACTERS,
                "First 8 characters must be digits")
        if (!TaxIdAlgorithms.nifValid(clean))
            return fail(input, "ES", "NIF", FailureReason.FAILED_CHECKSUM,
                "Letter ${clean.last()} is incorrect (expected ${TaxIdAlgorithms.nifLetter(clean.take(8))})")
        return pass(input, "ES", "NIF")
    }

    // ─── USA SSN ──────────────────────────────────────────────────────────────

    private fun validateSsn(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 9)
            return fail(input, "US", "SSN", FailureReason.WRONG_LENGTH,
                "Expected 9 digits, got ${d.length}")
        if (!input.all { it.isDigit() || it == '-' || it == ' ' })
            return fail(input, "US", "SSN", FailureReason.WRONG_CHARACTERS,
                "SSN may only contain digits, dashes, or spaces")
        val area = d.take(3).toInt()
        if (area == 0)   return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Area 000 is invalid")
        if (area == 666) return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Area 666 is reserved")
        if (area >= 900) return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Areas 900-999 are reserved (ITIN range)")
        if (d.substring(3, 5) == "00") return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Group 00 is invalid")
        if (d.takeLast(4) == "0000")   return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Serial 0000 is invalid")
        if (!TaxIdAlgorithms.ssnValid(d))
            return fail(input, "US", "SSN", FailureReason.BLACKLISTED, "Known test/reserved SSN")
        return pass(input, "US", "SSN")
    }

    // ─── Poland NIP ───────────────────────────────────────────────────────────

    private fun validateNip(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 10)
            return fail(input, "PL", "NIP", FailureReason.WRONG_LENGTH,
                "Expected 10 digits, got ${d.length}")
        if (!TaxIdAlgorithms.nipValid(d))
            return fail(input, "PL", "NIP", FailureReason.FAILED_CHECKSUM,
                "Mod-11 check digit failed")
        return pass(input, "PL", "NIP")
    }

    // ─── France NIR ───────────────────────────────────────────────────────────

    private fun validateNir(input: String): ValidationResult {
        val d = input.filter { it.isDigit() }
        if (d.length != 15)
            return fail(input, "FR", "NIR", FailureReason.WRONG_LENGTH,
                "Expected 15 digits, got ${d.length}")
        if (!TaxIdAlgorithms.nirValid(d))
            return fail(input, "FR", "NIR", FailureReason.FAILED_CHECKSUM,
                "Mod-97 check digits failed (expected ${TaxIdAlgorithms.nirCheckDigits(d.take(13))})")
        return pass(input, "FR", "NIR")
    }

    // ─── Italy Codice Fiscale ─────────────────────────────────────────────────

    private fun validateCodiceFiscale(input: String): ValidationResult {
        val clean = input.trim().uppercase()
        if (clean.length != 16)
            return fail(input, "IT", "Codice Fiscale", FailureReason.WRONG_LENGTH,
                "Expected 16 characters, got ${clean.length}")
        if (!TaxIdAlgorithms.codiceFiscaleValid(clean))
            return fail(input, "IT", "Codice Fiscale", FailureReason.FAILED_CHECKSUM,
                "Check character ${clean.last()} is incorrect (expected ${TaxIdAlgorithms.codiceFiscaleCheckChar(clean.take(15))})")
        return pass(input, "IT", "Codice Fiscale")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun pass(input: String, country: String, type: String) =
        ValidationResult(input, country, type, true, null, "Valid $type")

    private fun fail(
        input: String, country: String, type: String,
        reason: FailureReason, message: String
    ) = ValidationResult(input, country, type, false, reason, message)
}
