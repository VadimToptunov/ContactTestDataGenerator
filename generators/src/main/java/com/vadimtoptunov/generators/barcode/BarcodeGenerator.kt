package com.vadimtoptunov.generators.barcode

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import com.vadimtoptunov.generators.core.SeededRandom

/**
 * Generates valid barcodes with correct check digits.
 *
 * Supports EAN-13, UPC-A, and ISBN-13 formats. All three use the same
 * check digit algorithm (mod 10 with alternating weights 1 and 3).
 *
 * Use cases:
 * - QA: Test barcode scanning functionality
 * - Dev: Generate test product catalogs
 * - Demo: Create realistic retail data
 *
 * Example:
 * ```
 * val generator = BarcodeGenerator(BarcodeType.EAN_13)
 * val record = generator.generate()
 * println("${record.formatted} (${record.countryPrefix})")
 * ```
 *
 * Check digit algorithm (GS1 standard):
 * 1. Sum digits at odd positions (1, 3, 5, ...) × 1
 * 2. Sum digits at even positions (2, 4, 6, ...) × 3
 * 3. Check digit = (10 - (sum mod 10)) mod 10
 */
class BarcodeGenerator(
    private val type: BarcodeType = BarcodeType.EAN_13,
    private val seed: Long? = null
) : DataGenerator<BarcodeRecord> {

    private val random = SeededRandom(seed)

    override val name = "${type.label} Generator"
    override val description = "Generates valid ${type.label} barcodes with check digits"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): BarcodeRecord {
        val body = when (type) {
            BarcodeType.EAN_13 -> generateEan13Body()
            BarcodeType.UPC_A -> generateUpcABody()
            BarcodeType.ISBN_13 -> generateIsbn13Body()
        }
        val checkDigit = calculateCheckDigit(body)
        return BarcodeRecord(
            type = type,
            digits = body + checkDigit,
            checkDigit = checkDigit
        )
    }

    override fun generate(seed: Long): BarcodeRecord {
        return BarcodeGenerator(type, seed).generate()
    }

    /**
     * Generate EAN-13 body (12 digits, without check digit).
     * Uses realistic country prefixes for common markets.
     */
    private fun generateEan13Body(): String = with(random) {
        // Common country prefixes for realistic data
        val countryPrefixes = listOf(
            "00", "01", "02",        // USA/Canada
            "30", "31", "32", "33",  // France
            "40", "41", "42", "43",  // Germany
            "45", "49",              // Japan
            "50",                    // UK
            "69",                    // China
            "80",                    // Italy
            "84",                    // Spain
            "87",                    // Netherlands
            "460"                    // Russia
        )
        val prefix = countryPrefixes.randomElement()
        val remainingLength = 12 - prefix.length
        val rest = (1..remainingLength).map { (0..9).randomElement() }.joinToString("")
        prefix + rest
    }

    /**
     * Generate UPC-A body (11 digits, without check digit).
     * Number system digit (0-9) + Manufacturer (5) + Product (5)
     */
    private fun generateUpcABody(): String = with(random) {
        // Number system digits: 0-1 = regular items, 2 = variable weight, 3 = drugs, etc.
        val numberSystem = listOf("0", "1", "0", "0").randomElement()  // Bias toward regular items
        val manufacturer = (1..5).map { (0..9).randomElement() }.joinToString("")
        val product = (1..5).map { (0..9).randomElement() }.joinToString("")
        numberSystem + manufacturer + product
    }

    /**
     * Generate ISBN-13 body (12 digits, without check digit).
     * Starts with 978 (Bookland) or 979 (Musicland/newer books).
     */
    private fun generateIsbn13Body(): String = with(random) {
        // 978 is more common than 979
        val prefix = if (nextInt(10) < 9) "978" else "979"
        val rest = (1..9).map { (0..9).randomElement() }.joinToString("")
        prefix + rest
    }

    /**
     * Calculate GS1 check digit for EAN/UPC/ISBN.
     *
     * Algorithm:
     * 1. Starting from the right, alternate weights of 1 and 3
     * 2. Sum all weighted digits
     * 3. Check digit = (10 - (sum mod 10)) mod 10
     */
    private fun calculateCheckDigit(body: String): Int {
        val sum = body.mapIndexed { index, char ->
            val digit = char.digitToInt()
            // EAN/ISBN: odd positions (0, 2, 4, ...) × 1, even positions × 3
            if (index % 2 == 0) digit else digit * 3
        }.sum()
        return (10 - (sum % 10)) % 10
    }

    override fun serialize(record: BarcodeRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.type.name},${record.digits},${record.checkDigit}"
        OutputFormat.JSON -> buildString {
            append("""{"type":"${record.type.name}"""")
            append(""","digits":"${record.digits}"""")
            append(""","formatted":"${record.formatted}"""")
            append(""","checkDigit":${record.checkDigit}""")
            record.countryPrefix?.let { append(""","country":"$it"""") }
            append("}")
        }
        OutputFormat.TXT -> record.labeled
        else -> throw UnsupportedOperationException("$format not supported by BarcodeGenerator")
    }

    override fun serializeBatch(records: List<BarcodeRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("type,digits,check_digit")
                records.forEach { appendLine(serialize(it, format)) }
            }
            OutputFormat.JSON -> buildString {
                appendLine("[")
                records.forEachIndexed { i, r ->
                    append("  ${serialize(r, format)}")
                    if (i < records.lastIndex) appendLine(",") else appendLine()
                }
                append("]")
            }
            else -> super.serializeBatch(records, format)
        }

    companion object {
        const val ID = "barcode_generator"

        /**
         * Validate a barcode's check digit.
         * Returns true if the barcode has a valid check digit.
         */
        fun validate(barcode: String): Boolean {
            if (barcode.length !in listOf(12, 13) || !barcode.all { it.isDigit() }) {
                return false
            }
            val body = barcode.dropLast(1)
            val expected = calculateCheckDigitStatic(body)
            val actual = barcode.last().digitToInt()
            return expected == actual
        }

        private fun calculateCheckDigitStatic(body: String): Int {
            val sum = body.mapIndexed { index, char ->
                val digit = char.digitToInt()
                if (index % 2 == 0) digit else digit * 3
            }.sum()
            return (10 - (sum % 10)) % 10
        }

        fun registerAll() {
            BarcodeType.entries.forEach { type ->
                GeneratorRegistry.register("${ID}_${type.name.lowercase()}", BarcodeGenerator(type))
            }
        }
    }
}
