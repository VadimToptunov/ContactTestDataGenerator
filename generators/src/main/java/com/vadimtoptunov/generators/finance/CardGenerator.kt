package com.vadimtoptunov.generators.finance

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import java.util.Calendar
import kotlin.random.Random

/**
 * Generates test payment card numbers that pass Luhn validation.
 *
 * IMPORTANT: These numbers are for testing purposes only.
 * They will NOT pass BIN lookups, bank authorisation, or 3DS.
 * Do NOT use against real payment endpoints without explicit written permission.
 *
 * Use cases:
 *   - QA: fill payment forms in staging environments
 *   - Dev: seed test databases with realistic card data
 *   - Bug bounty: test client-side validation logic (format, length, Luhn)
 *
 * Inspired by tools like bankutils.com — bringing that capability into a
 * native Android developer toolkit.
 */
class CardGenerator(
    private val scheme: CardScheme = CardScheme.VISA
) : DataGenerator<CardRecord> {

    override val name = "Card Number Generator"
    override val description = "Generates Luhn-valid ${scheme.label} card numbers for testing"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): CardRecord {
        val length = scheme.lengths.random()
        val prefix = scheme.prefixes.random()
        val remainingDigits = length - prefix.length - 1  // -1 for check digit
        val body = (1..remainingDigits).joinToString("") { Random.nextInt(0, 10).toString() }
        val number = LuhnAlgorithm.complete(prefix + body)

        val now = Calendar.getInstance()
        val expiryMonth = Random.nextInt(1, 13)
        val expiryYear = now.get(Calendar.YEAR) + Random.nextInt(1, 6)  // 1–5 years ahead

        val cvv = (1..scheme.cvvLength).joinToString("") { Random.nextInt(0, 10).toString() }

        return CardRecord(
            scheme = scheme,
            number = number,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            cvv = cvv
        )
    }

    override fun serialize(record: CardRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.scheme.label},${record.number},${record.expiryFormatted},${record.cvv}"
        OutputFormat.JSON -> """{"scheme":"${record.scheme.label}","number":"${record.number}","expiry":"${record.expiryFormatted}","cvv":"${record.cvv}"}"""
        OutputFormat.TXT ->
            "${record.scheme.label}: ${record.numberFormatted}  Exp: ${record.expiryFormatted}  CVV: ${record.cvv}"
        else -> throw UnsupportedOperationException("$format not supported by CardGenerator")
    }

    override fun serializeBatch(records: List<CardRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("scheme,number,expiry,cvv")
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
        const val ID = "card_generator"

        fun registerAll() {
            CardScheme.values().forEach { scheme ->
                GeneratorRegistry.register("${ID}_${scheme.name.lowercase()}", CardGenerator(scheme))
            }
        }
    }
}
