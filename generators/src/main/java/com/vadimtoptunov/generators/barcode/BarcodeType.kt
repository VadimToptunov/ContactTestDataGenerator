package com.vadimtoptunov.generators.barcode

import kotlinx.serialization.Serializable

/**
 * Types of barcodes supported by [BarcodeGenerator].
 *
 * All three formats use the same check digit algorithm (mod 10, alternating weights 1 and 3).
 *
 * @property label Human-readable name
 * @property length Total length including check digit
 * @property description Brief description of the format
 */
@Serializable
enum class BarcodeType(
    val label: String,
    val length: Int,
    val description: String
) {
    /**
     * European Article Number (EAN-13).
     *
     * Used worldwide for retail products.
     * Format: Country code (2-3 digits) + Manufacturer code + Product code + Check digit
     */
    EAN_13(
        label = "EAN-13",
        length = 13,
        description = "European Article Number for retail products"
    ),

    /**
     * Universal Product Code (UPC-A).
     *
     * Primarily used in North America.
     * Format: Number system digit + Manufacturer code (5) + Product code (5) + Check digit
     */
    UPC_A(
        label = "UPC-A",
        length = 12,
        description = "Universal Product Code for North American retail"
    ),

    /**
     * International Standard Book Number (ISBN-13).
     *
     * Used for books and publications worldwide.
     * Format: 978 or 979 prefix + Registration group + Publisher + Title + Check digit
     */
    ISBN_13(
        label = "ISBN-13",
        length = 13,
        description = "International Standard Book Number for publications"
    )
}
