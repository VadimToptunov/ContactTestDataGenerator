package com.vadimtoptunov.generators.location

import kotlinx.serialization.Serializable

/**
 * A generated street address.
 *
 * Supports multiple formatting styles to accommodate different country conventions:
 * - [streetFirst]: US/UK style — "123 Main St, New York, NY 10001"
 * - [cityFirst]: RU/JP/CN style — "г. Москва, ул. Ленина, д. 1, кв. 5, 123456"
 * - [multiLine]: Mailing label format
 *
 * @property street Street name (may include type prefix/suffix)
 * @property houseNumber Building/house number
 * @property apartment Optional apartment/unit number
 * @property city City or locality name
 * @property state Optional state/province/region
 * @property postalCode Postal/ZIP code (empty for countries without postal codes)
 * @property countryCode ISO 3166-1 alpha-2 country code
 * @property countryName Full country name in English
 * @property locale BCP-47 locale tag
 */
@Serializable
data class AddressRecord(
    val street: String,
    val houseNumber: String,
    val apartment: String? = null,
    val city: String,
    val state: String? = null,
    val postalCode: String,
    val countryCode: String,
    val countryName: String,
    val locale: String
) {
    /**
     * Street-first format (US/UK style).
     * Example: "123 Main St, Apt 4B, New York, NY 10001"
     */
    val streetFirst: String
        get() = buildString {
            append("$houseNumber $street")
            apartment?.let { append(", Apt $it") }
            append(", $city")
            state?.let { append(", $it") }
            if (postalCode.isNotEmpty()) append(" $postalCode")
        }

    /**
     * City-first format (RU/JP/CN style).
     * Example: "г. Москва, ул. Ленина, д. 1, кв. 5, 123456"
     */
    val cityFirst: String
        get() = buildString {
            state?.let { append("$it, ") }
            append("$city, $street, $houseNumber")
            apartment?.let { append(", $it") }
            if (postalCode.isNotEmpty()) append(", $postalCode")
        }

    /**
     * Multi-line format for mailing labels.
     * Each line is separated by newline.
     */
    val multiLine: String
        get() = buildString {
            appendLine("$houseNumber $street")
            apartment?.let { appendLine("Apt $it") }
            val cityLine = buildString {
                append(city)
                state?.let { append(", $it") }
                if (postalCode.isNotEmpty()) append(" $postalCode")
            }
            appendLine(cityLine)
            append(countryName)
        }

    /**
     * Single-line format with country.
     */
    val fullAddress: String
        get() = "$streetFirst, $countryName"

    /**
     * Short format (street and city only).
     */
    val short: String
        get() = "$houseNumber $street, $city"

    /**
     * Check if this address has a postal code.
     */
    val hasPostalCode: Boolean
        get() = postalCode.isNotEmpty()

    /**
     * Check if this address has a state/region.
     */
    val hasState: Boolean
        get() = state != null

    /**
     * Check if this address has an apartment number.
     */
    val hasApartment: Boolean
        get() = apartment != null
}
