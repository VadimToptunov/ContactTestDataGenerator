package com.vadimtoptunov.contacttestdatagenerator.generators.identity

/**
 * A single generated Tax ID record.
 *
 * @param country   ISO 3166-1 alpha-2 country code
 * @param type      Name of the identifier in the local language/system
 * @param value     The generated number/string (raw, no formatting)
 * @param formatted Human-readable display format (e.g. "123.456.789-09" for CPF)
 * @param intent    Whether this record is meant to be valid or to fail in a specific way
 */
data class TaxIdRecord(
    val country: String,
    val type: String,
    val value: String,
    val formatted: String,
    val intent: TaxIdIntent
)

/**
 * Describes the purpose of a generated Tax ID — valid or a specific failure mode.
 *
 * QA engineers use INVALID variants to verify that the app correctly rejects bad input.
 * Bug bounty hunters use them to probe server-side vs. client-side validation gaps.
 */
sealed class TaxIdIntent {

    /** Passes all structural and check-digit validations. */
    object Valid : TaxIdIntent() {
        override fun toString() = "VALID"
    }

    /** Wrong check digit — structurally correct format but math fails. */
    object InvalidChecksum : TaxIdIntent() {
        override fun toString() = "INVALID — wrong check digit"
    }

    /** Too short — below the required character count. */
    object TooShort : TaxIdIntent() {
        override fun toString() = "INVALID — too short"
    }

    /** Too long — above the required character count. */
    object TooLong : TaxIdIntent() {
        override fun toString() = "INVALID — too long"
    }

    /** All zeros / all nines — explicitly prohibited by many countries. */
    object AllSameDigit : TaxIdIntent() {
        override fun toString() = "INVALID — all same digit (000...)"
    }

    /** Contains letters where only digits are expected. */
    object WrongCharacters : TaxIdIntent() {
        override fun toString() = "INVALID — letters in numeric field"
    }

    /** Correct length and checksum but a known blacklisted/reserved sequence. */
    object Blacklisted : TaxIdIntent() {
        override fun toString() = "INVALID — blacklisted sequence"
    }

    val isValid: Boolean get() = this is Valid
    val label: String get() = toString()
}
