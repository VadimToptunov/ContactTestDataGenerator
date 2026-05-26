package com.vadimtoptunov.contacttestdatagenerator.generators.finance

/**
 * Standalone Luhn validator — exposes validation as a named tool for the UI.
 *
 * Useful for:
 *   - QA: verify that generated numbers pass the app's own client-side check
 *   - Bug bounty: quickly test if a target endpoint accepts non-Luhn numbers
 *   - Dev: sanity-check numbers copied from logs or test reports
 */
object LuhnValidator {

    data class ValidationResult(
        val input: String,
        val isValid: Boolean,
        val sanitized: String,         // digits only
        val detectedScheme: String?,   // best-guess based on prefix
        val message: String
    )

    fun validate(input: String): ValidationResult {
        val sanitized = input.filter { it.isDigit() }

        if (sanitized.isEmpty()) {
            return ValidationResult(input, false, sanitized, null, "No digits found in input")
        }
        if (sanitized.length < 13) {
            return ValidationResult(input, false, sanitized, null,
                "Too short: ${sanitized.length} digits (minimum 13)")
        }
        if (sanitized.length > 19) {
            return ValidationResult(input, false, sanitized, null,
                "Too long: ${sanitized.length} digits (maximum 19)")
        }

        val valid = LuhnAlgorithm.validate(sanitized)
        val scheme = detectScheme(sanitized)
        val message = if (valid) "Valid Luhn checksum" else "Invalid Luhn checksum"

        return ValidationResult(input, valid, sanitized, scheme, message)
    }

    private fun detectScheme(digits: String): String? = when {
        digits.startsWith("4") -> "Visa"
        digits.length == 16 && digits.substring(0, 2).toInt() in 51..55 -> "Mastercard"
        digits.length == 16 && digits.substring(0, 4).toInt() in 2221..2720 -> "Mastercard"
        digits.startsWith("34") || digits.startsWith("37") -> "American Express"
        digits.startsWith("6011") || digits.startsWith("65") -> "Discover"
        digits.startsWith("62") -> "UnionPay"
        digits.substring(0, 4).toIntOrNull() in 2200..2204 -> "Mir"
        else -> null
    }
}
