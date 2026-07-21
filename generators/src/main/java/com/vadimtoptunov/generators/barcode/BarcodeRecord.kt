package com.vadimtoptunov.generators.barcode

import kotlinx.serialization.Serializable

/**
 * A generated barcode with its check digit.
 *
 * @property type The barcode format (EAN-13, UPC-A, or ISBN-13)
 * @property digits Full barcode string including check digit
 * @property checkDigit The calculated check digit (0-9)
 */
@Serializable
data class BarcodeRecord(
    val type: BarcodeType,
    val digits: String,
    val checkDigit: Int
) {
    init {
        require(digits.length == type.length) {
            "Invalid length for ${type.label}: expected ${type.length}, got ${digits.length}"
        }
        require(digits.all { it.isDigit() }) {
            "Barcode must contain only digits"
        }
    }

    /**
     * Formatted for display.
     * - ISBN: "978-3-16-148410-0" (hyphenated)
     * - EAN-13/UPC-A: as-is
     */
    val formatted: String
        get() = when (type) {
            BarcodeType.ISBN_13 -> formatIsbn(digits)
            else -> digits
        }

    /**
     * Without check digit (useful for validation testing).
     */
    val withoutCheck: String
        get() = digits.dropLast(1)

    /**
     * Human-readable label with type prefix.
     */
    val labeled: String
        get() = "${type.label}: $formatted"

    /**
     * Country/prefix info for EAN-13.
     */
    val countryPrefix: String?
        get() = when (type) {
            BarcodeType.EAN_13 -> {
                val prefix = digits.take(3)
                EAN_COUNTRY_PREFIXES[prefix] ?: EAN_COUNTRY_PREFIXES[digits.take(2)]
            }
            else -> null
        }

    /**
     * ISBN prefix (978 or 979) info.
     */
    val isbnPrefix: String?
        get() = when (type) {
            BarcodeType.ISBN_13 -> {
                when (digits.take(3)) {
                    "978" -> "ISBN-13 (Bookland)"
                    "979" -> "ISBN-13 (Musicland/Extended)"
                    else -> null
                }
            }
            else -> null
        }

    private fun formatIsbn(isbn: String): String {
        // Standard ISBN-13 hyphenation: prefix-group-publisher-title-check
        // Simplified: 978-X-XX-XXXXXX-X pattern
        return buildString {
            append(isbn.substring(0, 3))  // EAN prefix (978/979)
            append("-")
            append(isbn.substring(3, 4))  // Registration group
            append("-")
            append(isbn.substring(4, 6))  // Publisher
            append("-")
            append(isbn.substring(6, 12)) // Title
            append("-")
            append(isbn.substring(12, 13)) // Check digit
        }
    }

    companion object {
        // Common EAN country prefixes
        private val EAN_COUNTRY_PREFIXES = mapOf(
            "00" to "USA/Canada",
            "01" to "USA/Canada",
            "02" to "USA/Canada",
            "03" to "USA/Canada",
            "04" to "USA/Canada",
            "05" to "USA/Canada",
            "06" to "USA/Canada",
            "07" to "USA/Canada",
            "08" to "USA/Canada",
            "09" to "USA/Canada",
            "30" to "France",
            "31" to "France",
            "32" to "France",
            "33" to "France",
            "34" to "France",
            "35" to "France",
            "36" to "France",
            "37" to "France",
            "40" to "Germany",
            "41" to "Germany",
            "42" to "Germany",
            "43" to "Germany",
            "44" to "Germany",
            "45" to "Japan",
            "46" to "Russia",
            "49" to "Japan",
            "50" to "UK",
            "54" to "Belgium/Luxembourg",
            "57" to "Denmark",
            "64" to "Finland",
            "70" to "Norway",
            "73" to "Sweden",
            "76" to "Switzerland",
            "80" to "Italy",
            "84" to "Spain",
            "87" to "Netherlands",
            "90" to "Austria",
            "93" to "Australia",
            "94" to "New Zealand",
            "460" to "Russia",
            "470" to "Kyrgyzstan",
            "471" to "Taiwan",
            "474" to "Estonia",
            "475" to "Latvia",
            "476" to "Azerbaijan",
            "477" to "Lithuania",
            "478" to "Uzbekistan",
            "479" to "Sri Lanka",
            "480" to "Philippines",
            "481" to "Belarus",
            "482" to "Ukraine",
            "484" to "Moldova",
            "485" to "Armenia",
            "486" to "Georgia",
            "487" to "Kazakhstan",
            "489" to "Hong Kong",
            "500" to "UK",
            "520" to "Greece",
            "528" to "Lebanon",
            "529" to "Cyprus",
            "530" to "Albania",
            "531" to "North Macedonia",
            "535" to "Malta",
            "539" to "Ireland",
            "560" to "Portugal",
            "569" to "Iceland",
            "590" to "Poland",
            "594" to "Romania",
            "599" to "Hungary",
            "600" to "South Africa",
            "609" to "Mauritius",
            "611" to "Morocco",
            "613" to "Algeria",
            "616" to "Kenya",
            "618" to "Ivory Coast",
            "619" to "Tunisia",
            "621" to "Syria",
            "622" to "Egypt",
            "624" to "Libya",
            "625" to "Jordan",
            "626" to "Iran",
            "627" to "Kuwait",
            "628" to "Saudi Arabia",
            "629" to "UAE"
        )
    }
}
