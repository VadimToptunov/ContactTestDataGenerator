package com.vadimtoptunov.contacttestdatagenerator.generators.finance

/**
 * A single generated payment card record.
 *
 * All numbers pass Luhn validation. Expiry dates are always in the future.
 * CVV/CVC values are random (not derived from card number — that would require
 * the issuer's key, which is intentionally out of scope).
 */
data class CardRecord(
    val scheme: CardScheme,
    val number: String,       // 13–19 digit Luhn-valid string
    val expiryMonth: Int,     // 1–12
    val expiryYear: Int,      // 4-digit year
    val cvv: String           // 3 or 4 digits depending on scheme
) {
    val expiryFormatted: String
        get() = "%02d/%02d".format(expiryMonth, expiryYear % 100)

    val numberFormatted: String
        get() = when (scheme) {
            CardScheme.AMEX -> number.chunked(4).let { parts ->
                "${parts[0]} ${parts[1]} ${parts.drop(2).joinToString("")}"
            }
            else -> number.chunked(4).joinToString(" ")
        }
}

enum class CardScheme(
    val label: String,
    val prefixes: List<String>,
    val lengths: List<Int>,
    val cvvLength: Int
) {
    VISA(
        label = "Visa",
        prefixes = listOf("4"),
        lengths = listOf(16),
        cvvLength = 3
    ),
    MASTERCARD(
        label = "Mastercard",
        prefixes = (51..55).map { it.toString() } + (2221..2720).map { it.toString() },
        lengths = listOf(16),
        cvvLength = 3
    ),
    AMEX(
        label = "American Express",
        prefixes = listOf("34", "37"),
        lengths = listOf(15),
        cvvLength = 4
    ),
    DISCOVER(
        label = "Discover",
        prefixes = listOf("6011", "622126", "644", "65"),
        lengths = listOf(16),
        cvvLength = 3
    ),
    UNIONPAY(
        label = "UnionPay",
        prefixes = listOf("62"),
        lengths = listOf(16, 17, 18, 19),
        cvvLength = 3
    ),
    MIR(
        label = "Mir",
        prefixes = (2200..2204).map { it.toString() },
        lengths = listOf(16),
        cvvLength = 3
    )
}
