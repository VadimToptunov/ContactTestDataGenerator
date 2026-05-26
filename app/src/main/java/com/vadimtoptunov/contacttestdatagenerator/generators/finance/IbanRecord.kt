package com.vadimtoptunov.contacttestdatagenerator.generators.finance

/**
 * A generated IBAN (International Bank Account Number).
 *
 * Format: [CountryCode][CheckDigits][BBAN]
 * Example: GB29 NWBK 6016 1331 9268 19
 */
data class IbanRecord(
    val countryCode: String,     // ISO 3166-1 alpha-2
    val checkDigits: String,     // 2-digit Mod-97 check
    val bban: String,            // Basic Bank Account Number (country-specific)
    val bankName: String         // Fictional bank name for context
) {
    /** Full IBAN without spaces */
    val raw: String get() = "$countryCode$checkDigits$bban"

    /** IBAN with spaces every 4 characters (print format) */
    val formatted: String get() = raw.chunked(4).joinToString(" ")

    /** BIC/SWIFT placeholder (8 chars) based on country */
    val bic: String get() = "${bankName.take(4).uppercase().padEnd(4, 'X')}${countryCode}XX"
}
