package com.vadimtoptunov.generators.identity

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlin.random.Random

/**
 * Generates Tax ID numbers for multiple countries, both valid and intentionally
 * invalid, for use in QA test suites and security research.
 *
 * Supported countries:
 *   BR — CPF (Cadastro de Pessoas Físicas)
 *   DE — Steuerliche Identifikationsnummer (IdNr)
 *   RU — ИНН физического лица (12 digits)
 *   UA — РНОКПП (Реєстраційний номер облікової картки платника податків)
 *   ES — NIF (Número de Identificación Fiscal / DNI)
 *   US — SSN (Social Security Number)
 *   UK — UTR (Unique Taxpayer Reference)
 *   PL — NIP (Numer Identyfikacji Podatkowej)
 *   IT — Codice Fiscale
 *   FR — NIR (Numéro de Sécurité Sociale)
 *
 * Each country generates all [TaxIdIntent] variants so you get a ready-made
 * test suite covering happy paths and every common failure mode.
 */
class TaxIdGenerator(
    val countrySpec: CountrySpec
) : DataGenerator<TaxIdRecord> {

    override val name = "Tax ID Generator"
    override val description = "Generates ${countrySpec.typeName} numbers (${countrySpec.countryCode}) — valid + invalid variants"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    // ─── DataGenerator implementation ────────────────────────────────────────

    /** Generates one VALID record. Use [generateAll] for a full test set. */
    override fun generate(): TaxIdRecord = countrySpec.generateValid()

    /** Full test set: one valid + all failure modes. */
    fun generateAll(): List<TaxIdRecord> = listOf(
        countrySpec.generateValid(),
        countrySpec.generateInvalidChecksum(),
        countrySpec.generateTooShort(),
        countrySpec.generateTooLong(),
        countrySpec.generateAllSameDigit(),
        countrySpec.generateWrongCharacters(),
        countrySpec.generateBlacklisted()
    )

    // ─── Convenience delegates to the country spec ───────────────────────────
    // Let callers request a single variant without reaching into `countrySpec`.

    /** One VALID record (same as [generate]). */
    fun generateValid(): TaxIdRecord = countrySpec.generateValid()

    /** INVALID — correct format but wrong check digit. */
    fun generateInvalidChecksum(): TaxIdRecord = countrySpec.generateInvalidChecksum()

    /** INVALID — fewer characters than required. */
    fun generateTooShort(): TaxIdRecord = countrySpec.generateTooShort()

    /** INVALID — more characters than allowed. */
    fun generateTooLong(): TaxIdRecord = countrySpec.generateTooLong()

    /** INVALID — all-same-digit sequence (000…, 111…). */
    fun generateAllSameDigit(): TaxIdRecord = countrySpec.generateAllSameDigit()

    /** INVALID — letters where only digits are allowed. */
    fun generateWrongCharacters(): TaxIdRecord = countrySpec.generateWrongCharacters()

    /** INVALID — known blacklisted/reserved sequence. */
    fun generateBlacklisted(): TaxIdRecord = countrySpec.generateBlacklisted()

    override fun serialize(record: TaxIdRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.country},${record.type},${record.value},${record.formatted},${record.intent.label}"
        OutputFormat.JSON ->
            """{"country":"${record.country}","type":"${record.type}","value":"${record.value}","formatted":"${record.formatted}","intent":"${record.intent.label}","valid":${record.intent.isValid}}"""
        OutputFormat.TXT ->
            "[${record.country}] ${record.type}: ${record.formatted}  →  ${record.intent.label}"
        else -> throw UnsupportedOperationException("$format not supported by TaxIdGenerator")
    }

    override fun serializeBatch(records: List<TaxIdRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("country,type,value,formatted,intent")
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

    // ─── Country spec abstraction ─────────────────────────────────────────────

    abstract class CountrySpec(
        val countryCode: String,
        val typeName: String
    ) {
        abstract fun generateValid(): TaxIdRecord
        abstract fun generateInvalidChecksum(): TaxIdRecord
        abstract fun generateTooShort(): TaxIdRecord
        abstract fun generateTooLong(): TaxIdRecord
        abstract fun generateAllSameDigit(): TaxIdRecord
        abstract fun generateWrongCharacters(): TaxIdRecord
        abstract fun generateBlacklisted(): TaxIdRecord

        protected fun randomDigits(n: Int) = (1..n).map { Random.nextInt(0, 10) }
            .joinToString("")
        protected fun randomDigitsNonZeroFirst(n: Int) =
            Random.nextInt(1, 10).toString() + (1 until n).map { Random.nextInt(0, 10) }.joinToString("")
    }

    // ─── Brazil CPF ───────────────────────────────────────────────────────────

    class BrazilCpf : CountrySpec("BR", "CPF") {
        override fun generateValid(): TaxIdRecord {
            var base: String
            do { base = randomDigits(9) } while (base.all { it == base[0] })
            val full = base + TaxIdAlgorithms.cpfCheckDigits(base)
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            var base: String
            do { base = randomDigits(9) } while (base.all { it == base[0] })
            val correct = TaxIdAlgorithms.cpfCheckDigits(base)
            val wrong = ((correct.take(1).toInt() + 1) % 10).toString() + correct.last()
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record(randomDigits(9),  TaxIdIntent.TooShort)
        override fun generateTooLong()        = record(randomDigits(13), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("11111111111",    TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("123.456.78X-09", TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("00000000000",    TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent): TaxIdRecord {
            val digits = value.filter { it.isDigit() }
            val fmt = if (digits.length == 11)
                "${digits.take(3)}.${digits.substring(3,6)}.${digits.substring(6,9)}-${digits.takeLast(2)}"
            else value
            return TaxIdRecord("BR", typeName, value, fmt, intent)
        }
    }

    // ─── Germany IdNr ─────────────────────────────────────────────────────────

    class GermanyIdnr : CountrySpec("DE", "Steuer-IdNr") {
        override fun generateValid(): TaxIdRecord {
            // 11 digits total: first 1-9, then 9 more, then the Mod-11,10 check digit.
            val first = Random.nextInt(1, 10).toString()
            val middle = (1..9).map { Random.nextInt(0, 10) }.joinToString("")
            val base10 = first + middle
            val full = base10 + TaxIdAlgorithms.idnrCheckDigit(base10)
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            val first = Random.nextInt(1, 10).toString()
            val rest  = randomDigits(9)
            val base  = first + rest
            val correct = TaxIdAlgorithms.idnrCheckDigit(base)
            val wrong = ((correct.toInt() + 1) % 10).toString()
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record(randomDigits(9),  TaxIdIntent.TooShort)
        override fun generateTooLong()        = record(randomDigits(13), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("55555555555",    TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("1234567890X",    TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("00000000000",    TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent) =
            TaxIdRecord("DE", typeName, value, value, intent)
    }

    // ─── Russia INN (personal, 12 digits) ────────────────────────────────────

    class RussiaInn : CountrySpec("RU", "ИНН физлица") {
        override fun generateValid(): TaxIdRecord {
            val base = randomDigitsNonZeroFirst(10)
            val full = base + TaxIdAlgorithms.innPersonalCheckDigits(base)
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            val base = randomDigitsNonZeroFirst(10)
            val correct = TaxIdAlgorithms.innPersonalCheckDigits(base)
            val wrong = ((correct.toInt() + 11) % 100).toString().padStart(2, '0')
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record(randomDigits(10), TaxIdIntent.TooShort)
        override fun generateTooLong()        = record(randomDigits(14), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("222222222222",   TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("12345678901X",   TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("000000000000",   TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent) =
            TaxIdRecord("RU", typeName, value, value, intent)
    }

    // ─── Ukraine РНОКПП ───────────────────────────────────────────────────────

    class UkraineRnokpp : CountrySpec("UA", "РНОКПП") {
        override fun generateValid(): TaxIdRecord {
            val base = randomDigitsNonZeroFirst(9)
            val full = base + TaxIdAlgorithms.rnokppCheckDigit(base)
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            val base = randomDigitsNonZeroFirst(9)
            val correct = TaxIdAlgorithms.rnokppCheckDigit(base)
            val wrong = ((correct.toInt() + 1) % 10).toString()
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record(randomDigits(8),  TaxIdIntent.TooShort)
        override fun generateTooLong()        = record(randomDigits(12), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("3333333333",     TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("12345678XX",     TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("0000000000",     TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent) =
            TaxIdRecord("UA", typeName, value, value, intent)
    }

    // ─── Spain NIF ────────────────────────────────────────────────────────────

    class SpainNif : CountrySpec("ES", "NIF/DNI") {
        override fun generateValid(): TaxIdRecord {
            val digits = randomDigits(8).padStart(8, '0')
            val full = digits + TaxIdAlgorithms.nifLetter(digits.padStart(8, '0').takeLast(8))
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            val digits = randomDigits(8).padStart(8, '0')
            val correct = TaxIdAlgorithms.nifLetter(digits)
            val wrong = if (correct == 'A') 'B' else 'A'
            return record(digits + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record("1234567A",  TaxIdIntent.TooShort)
        override fun generateTooLong()        = record("123456789A",TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("00000000T", TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("1234ABCDT", TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("00000000T", TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent) =
            TaxIdRecord("ES", typeName, value, value, intent)
    }

    // ─── USA SSN ──────────────────────────────────────────────────────────────

    class UsaSsn : CountrySpec("US", "SSN") {
        override fun generateValid(): TaxIdRecord {
            var area: Int; var group: Int; var serial: Int
            do {
                area   = Random.nextInt(1, 900).let { if (it == 666) 667 else it }
                group  = Random.nextInt(1, 100)
                serial = Random.nextInt(1, 10000)
            } while (!TaxIdAlgorithms.ssnValid("%03d%02d%04d".format(area, group, serial)))
            val value = "%03d%02d%04d".format(area, group, serial)
            val fmt   = "%03d-%02d-%04d".format(area, group, serial)
            return TaxIdRecord("US", typeName, value, fmt, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum() =   // SSN has no checksum — use area=000 instead
            TaxIdRecord("US", typeName, "000121234", "000-12-1234", TaxIdIntent.InvalidChecksum)
        override fun generateTooShort()        =
            TaxIdRecord("US", typeName, "12345678",  "123-45-678",  TaxIdIntent.TooShort)
        override fun generateTooLong()         =
            TaxIdRecord("US", typeName, "1234567890","123-45-67890",TaxIdIntent.TooLong)
        override fun generateAllSameDigit()    =
            TaxIdRecord("US", typeName, "000000000", "000-00-0000", TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters() =
            TaxIdRecord("US", typeName, "123-4A-6789","123-4A-6789",TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()     =
            TaxIdRecord("US", typeName, "123456789", "123-45-6789", TaxIdIntent.Blacklisted)
    }

    // ─── Poland NIP ───────────────────────────────────────────────────────────

    class PolandNip : CountrySpec("PL", "NIP") {
        override fun generateValid(): TaxIdRecord {
            var base: String; var check: String
            do {
                base  = randomDigitsNonZeroFirst(9)
                check = TaxIdAlgorithms.nipCheckDigit(base)
            } while (check.isEmpty())   // rem==10 is invalid, retry
            return record(base + check, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            var base: String; var check: String
            do { base = randomDigitsNonZeroFirst(9); check = TaxIdAlgorithms.nipCheckDigit(base) } while (check.isEmpty())
            val wrong = ((check.toInt() + 1) % 10).toString()
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record(randomDigits(8),  TaxIdIntent.TooShort)
        override fun generateTooLong()        = record(randomDigits(12), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("4444444444",     TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("123456789X",     TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("0000000000",     TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent): TaxIdRecord {
            val d = value.filter { it.isDigit() }
            val fmt = if (d.length == 10)
                "${d.take(3)}-${d.substring(3,6)}-${d.substring(6,8)}-${d.takeLast(2)}"
            else value
            return TaxIdRecord("PL", typeName, value, fmt, intent)
        }
    }

    // ─── France NIR ───────────────────────────────────────────────────────────

    class FranceNir : CountrySpec("FR", "NIR (Sécu)") {
        override fun generateValid(): TaxIdRecord {
            val sex    = Random.nextInt(1, 3).toString()          // 1 or 2
            val year   = Random.nextInt(0, 100).toString().padStart(2, '0')
            val month  = Random.nextInt(1, 13).toString().padStart(2, '0')
            val dept   = Random.nextInt(1, 96).toString().padStart(2, '0')
            val city   = Random.nextInt(1, 1000).toString().padStart(3, '0')
            val order  = Random.nextInt(1, 1000).toString().padStart(3, '0')
            val base   = sex + year + month + dept + city + order
            val check  = TaxIdAlgorithms.nirCheckDigits(base)
            val full   = base + check
            return record(full, TaxIdIntent.Valid)
        }
        override fun generateInvalidChecksum(): TaxIdRecord {
            val base  = "1" + randomDigits(12)
            val correct = TaxIdAlgorithms.nirCheckDigits(base)
            val wrong = ((correct.toInt() + 1) % 98 + 1).toString().padStart(2, '0')
            return record(base + wrong, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()       = record("1" + randomDigits(13), TaxIdIntent.TooShort)
        override fun generateTooLong()        = record("1" + randomDigits(16), TaxIdIntent.TooLong)
        override fun generateAllSameDigit()   = record("111111111111111",      TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters()= record("1XXXXXXXXXXX97",       TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()    = record("000000000000000",      TaxIdIntent.Blacklisted)

        private fun record(value: String, intent: TaxIdIntent): TaxIdRecord {
            val d = value.filter { it.isDigit() }
            val fmt = if (d.length == 15)
                "${d[0]} ${d.substring(1,3)} ${d.substring(3,5)} ${d.substring(5,7)} ${d.substring(7,10)} ${d.substring(10,13)} ${d.takeLast(2)}"
            else value
            return TaxIdRecord("FR", typeName, value, fmt, intent)
        }
    }

    // ─── Italy Codice Fiscale ─────────────────────────────────────────────────

    class ItalyCodiceFiscale : CountrySpec("IT", "Codice Fiscale") {

        private val MONTHS = "ABCDEHLMPRST"
        private val MUNICIPALITIES = listOf(
            "H501", "F205", "L219", "D612", "G273",
            "A944", "C351", "E379", "B354", "G388"
        )
        private val CONSONANTS = "BCDFGHJKLMNPQRSTVWXYZ"
        private val VOWELS     = "AEIOU"

        override fun generateValid(): TaxIdRecord {
            val lastName  = randomCodePart()
            val firstName = randomCodePart()
            val year      = Random.nextInt(0, 100).toString().padStart(2, '0')
            val month     = MONTHS[Random.nextInt(MONTHS.length)].toString()
            val day       = Random.nextInt(1, 29).toString().padStart(2, '0')   // male
            val muni      = MUNICIPALITIES.random()
            val base15    = lastName + firstName + year + month + day + muni
            val check     = TaxIdAlgorithms.codiceFiscaleCheckChar(base15)
            val full      = base15 + check
            return TaxIdRecord("IT", typeName, full, full, TaxIdIntent.Valid)
        }

        override fun generateInvalidChecksum(): TaxIdRecord {
            val lastName  = randomCodePart()
            val firstName = randomCodePart()
            val base15    = lastName + firstName + "80A01H501"
            val correct   = TaxIdAlgorithms.codiceFiscaleCheckChar(base15)
            val wrong     = if (correct == 'A') 'B' else 'A'
            val full      = base15 + wrong
            return TaxIdRecord("IT", typeName, full, full, TaxIdIntent.InvalidChecksum)
        }
        override fun generateTooShort()        = TaxIdRecord("IT", typeName, "RSSMRA80A01", "RSSMRA80A01",    TaxIdIntent.TooShort)
        override fun generateTooLong()         = TaxIdRecord("IT", typeName, "RSSMRA80A01H501AA", "RSSMRA80A01H501AA", TaxIdIntent.TooLong)
        override fun generateAllSameDigit()    = TaxIdRecord("IT", typeName, "AAAAAAAAAAAAAAAAAA", "AA...",   TaxIdIntent.AllSameDigit)
        override fun generateWrongCharacters() = TaxIdRecord("IT", typeName, "RSS1RA80A01H501A",  "RSS1RA...",TaxIdIntent.WrongCharacters)
        override fun generateBlacklisted()     = TaxIdRecord("IT", typeName, "0000000000000000",  "0000...",  TaxIdIntent.Blacklisted)

        private fun randomCodePart(): String {
            val c1 = CONSONANTS[Random.nextInt(CONSONANTS.length)]
            val c2 = CONSONANTS[Random.nextInt(CONSONANTS.length)]
            val v  = VOWELS[Random.nextInt(VOWELS.length)]
            return "$c1$c2$v"
        }
    }

    // ─── Registry ─────────────────────────────────────────────────────────────

    companion object {
        val ALL_SPECS: List<CountrySpec> = listOf(
            BrazilCpf(),
            GermanyIdnr(),
            RussiaInn(),
            UkraineRnokpp(),
            SpainNif(),
            UsaSsn(),
            PolandNip(),
            FranceNir(),
            ItalyCodiceFiscale()
        )

        fun registerAll() {
            ALL_SPECS.forEach { spec ->
                GeneratorRegistry.register(
                    "taxid_${spec.countryCode.lowercase()}",
                    TaxIdGenerator(spec)
                )
            }
        }
    }
}
