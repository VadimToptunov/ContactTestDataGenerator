package com.vadimtoptunov.contacttestdatagenerator.generators.finance

import com.vadimtoptunov.contacttestdatagenerator.core.DataGenerator
import com.vadimtoptunov.contacttestdatagenerator.core.GeneratorRegistry
import com.vadimtoptunov.contacttestdatagenerator.core.OutputFormat
import java.math.BigInteger
import kotlin.random.Random

/**
 * Generates structurally valid IBANs with correct Mod-97 check digits.
 *
 * These IBANs are syntactically valid per ISO 13616 but are NOT real accounts.
 * They will NOT pass bank-level BIC/BBAN lookups or SWIFT validation.
 *
 * Use cases (inspired by bankutils.com):
 *   - QA: test IBAN input validation in banking/fintech apps
 *   - Dev: seed test databases with SEPA-compatible identifiers
 *   - Bug bounty: probe for client vs. server-side IBAN validation discrepancies
 */
class IbanGenerator(
    private val countryCode: String = "DE"
) : DataGenerator<IbanRecord> {

    override val name = "IBAN Generator"
    override val description = "Generates Mod-97 valid IBANs for $countryCode"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): IbanRecord {
        val spec = COUNTRY_SPECS[countryCode]
            ?: throw IllegalArgumentException("Country '$countryCode' is not supported")

        val bban = buildString {
            spec.bbanFormat.forEach { segment ->
                when (segment.type) {
                    'n' -> repeat(segment.length) { append(Random.nextInt(0, 10)) }
                    'a' -> repeat(segment.length) { append(('A'..'Z').random()) }
                    'c' -> repeat(segment.length) {
                        val chars = ('A'..'Z').toList() + ('0'..'9').toList()
                        append(chars.random())
                    }
                }
            }
        }

        val checkDigits = computeCheckDigits(countryCode, bban)
        val bankName = FICTIONAL_BANKS.random()

        return IbanRecord(
            countryCode = countryCode,
            checkDigits = checkDigits,
            bban = bban,
            bankName = bankName
        )
    }

    override fun serialize(record: IbanRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.countryCode},${record.raw},${record.formatted},${record.bic},${record.bankName}"
        OutputFormat.JSON ->
            """{"country":"${record.countryCode}","iban":"${record.raw}","formatted":"${record.formatted}","bic":"${record.bic}","bank":"${record.bankName}"}"""
        OutputFormat.TXT ->
            "IBAN: ${record.formatted}  BIC: ${record.bic}  Bank: ${record.bankName}"
        else -> throw UnsupportedOperationException("$format not supported by IbanGenerator")
    }

    override fun serializeBatch(records: List<IbanRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("country,iban_raw,iban_formatted,bic,bank")
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

    // ─── Mod-97 check digit computation ──────────────────────────────────────

    private fun computeCheckDigits(country: String, bban: String): String {
        // Rearrange: BBAN + CountryCode + "00", then convert letters to digits (A=10..Z=35)
        val rearranged = (bban + country + "00").map { c ->
            if (c.isLetter()) (c.code - 'A'.code + 10).toString() else c.toString()
        }.joinToString("")

        val remainder = BigInteger(rearranged).mod(BigInteger.valueOf(97))
        val checkDigit = 98 - remainder.toInt()
        return checkDigit.toString().padStart(2, '0')
    }

    // ─── BBAN format specification ────────────────────────────────────────────

    private data class BbanSegment(val type: Char, val length: Int)

    private data class CountrySpec(val totalLength: Int, val bbanFormat: List<BbanSegment>)

    private val COUNTRY_SPECS: Map<String, CountrySpec> = mapOf(
        "DE" to CountrySpec(22, listOf(BbanSegment('n', 8), BbanSegment('n', 10))),
        "GB" to CountrySpec(22, listOf(BbanSegment('a', 4), BbanSegment('n', 6), BbanSegment('n', 8))),
        "FR" to CountrySpec(27, listOf(BbanSegment('n', 10), BbanSegment('c', 11), BbanSegment('n', 2))),
        "ES" to CountrySpec(24, listOf(BbanSegment('n', 8), BbanSegment('n', 10), BbanSegment('n', 2))),
        "IT" to CountrySpec(27, listOf(BbanSegment('a', 1), BbanSegment('n', 10), BbanSegment('c', 12))),
        "NL" to CountrySpec(18, listOf(BbanSegment('a', 4), BbanSegment('n', 10))),
        "PL" to CountrySpec(28, listOf(BbanSegment('n', 8), BbanSegment('n', 16))),
        "UA" to CountrySpec(29, listOf(BbanSegment('n', 6), BbanSegment('n', 19))),
        "SE" to CountrySpec(24, listOf(BbanSegment('n', 3), BbanSegment('n', 16), BbanSegment('n', 1))),
        "CH" to CountrySpec(21, listOf(BbanSegment('n', 5), BbanSegment('c', 12))),
        "US" to CountrySpec(20, listOf(BbanSegment('n', 9), BbanSegment('n', 7)))  // unofficial, used in testing
    )

    private val FICTIONAL_BANKS = listOf(
        "NordBank", "DigitalFirst", "AlphaCredit", "MetroFinance", "ClearPay",
        "TrustNord", "OpenBank", "FinTech One", "SkyLedger", "BridgeBank"
    )

    companion object {
        const val ID = "iban_generator"

        fun registerAll() {
            listOf("DE", "GB", "FR", "ES", "NL", "PL", "UA", "CH").forEach { cc ->
                GeneratorRegistry.register("${ID}_${cc.lowercase()}", IbanGenerator(cc))
            }
        }
    }
}
