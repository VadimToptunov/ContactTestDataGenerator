package com.vadimtoptunov.generators.security

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import com.vadimtoptunov.generators.core.SeededRandom
import kotlin.math.log2

/**
 * Generates random passwords according to a configurable [PasswordPolicy].
 *
 * Features:
 * - Configurable length range
 * - Required character classes (uppercase, lowercase, digits, special)
 * - Ambiguous character exclusion (O/0, l/1/I)
 * - Entropy estimation
 * - Seed-based reproducibility
 *
 * Use cases:
 * - QA: Test password validation logic
 * - Dev: Generate test accounts with realistic passwords
 * - Security: Assess password strength requirements
 *
 * Example:
 * ```
 * val generator = PasswordGenerator(PasswordPolicy.STRONG)
 * val record = generator.generate()
 * println("${record.password} (${record.entropyFormatted}, ${record.strength})")
 * ```
 */
class PasswordGenerator(
    private val policy: PasswordPolicy = PasswordPolicy.STANDARD,
    private val seed: Long? = null
) : DataGenerator<PasswordRecord> {

    private val random = SeededRandom(seed)

    override val name = "Password Generator"
    override val description = "Generates random passwords with ${policy.minLength}-${policy.maxLength} chars"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): PasswordRecord {
        val length = with(random) { (policy.minLength..policy.maxLength).randomElement() }
        val charset = buildCharset()

        // Ensure at least one char from each required category
        val required = mutableListOf<Char>()
        with(random) {
            if (policy.requireUppercase) required += getUppercaseChars().randomElement()
            if (policy.requireLowercase) required += getLowercaseChars().randomElement()
            if (policy.requireDigits) required += getDigitChars().randomElement()
            if (policy.requireSpecial && policy.specialChars.isNotEmpty()) {
                required += policy.specialChars.randomElement()
            }
        }

        // Fill remaining with random chars from full charset
        val remainingCount = (length - required.size).coerceAtLeast(0)
        val remaining = with(random) {
            (1..remainingCount).map { charset.randomElement() }
        }

        // Shuffle to avoid predictable positions of required chars
        val password = with(random) { (required + remaining).shuffled() }.joinToString("")

        return PasswordRecord(
            password = password,
            policy = policy,
            entropy = calculateEntropy(password.length, charset.size)
        )
    }

    override fun generate(seed: Long): PasswordRecord {
        return PasswordGenerator(policy, seed).generate()
    }

    private fun buildCharset(): List<Char> {
        val chars = mutableListOf<Char>()

        // Add character classes based on policy
        if (policy.requireUppercase) chars += getUppercaseChars()
        if (policy.requireLowercase) chars += getLowercaseChars()
        if (policy.requireDigits) chars += getDigitChars()
        if (policy.requireSpecial) chars += policy.specialChars.toList()

        // Fallback: if no classes required, use alphanumeric
        if (chars.isEmpty()) {
            chars += getUppercaseChars()
            chars += getLowercaseChars()
            chars += getDigitChars()
        }

        return chars
    }

    private fun getUppercaseChars(): List<Char> =
        if (policy.excludeAmbiguous) {
            ('A'..'Z').filter { it !in AMBIGUOUS_UPPER }
        } else {
            ('A'..'Z').toList()
        }

    private fun getLowercaseChars(): List<Char> =
        if (policy.excludeAmbiguous) {
            ('a'..'z').filter { it !in AMBIGUOUS_LOWER }
        } else {
            ('a'..'z').toList()
        }

    private fun getDigitChars(): List<Char> =
        if (policy.excludeAmbiguous) {
            ('0'..'9').filter { it !in AMBIGUOUS_DIGITS }
        } else {
            ('0'..'9').toList()
        }

    private fun calculateEntropy(length: Int, charsetSize: Int): Double {
        if (charsetSize <= 1) return 0.0
        // entropy = length * log2(charsetSize)
        return length * log2(charsetSize.toDouble())
    }

    override fun serialize(record: PasswordRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV ->
            "${record.password},${record.length},${record.entropy},${record.strength.name}"
        OutputFormat.JSON -> buildString {
            append("""{"password":"${record.password}"""")
            append(""","length":${record.length}""")
            append(""","entropy":${record.entropy}""")
            append(""","strength":"${record.strength.name}"}""")
        }
        OutputFormat.TXT ->
            "${record.password}  (${record.entropyFormatted}, ${record.strength.label})"
        else -> throw UnsupportedOperationException("$format not supported by PasswordGenerator")
    }

    override fun serializeBatch(records: List<PasswordRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("password,length,entropy,strength")
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
        const val ID = "password_generator"

        // Ambiguous characters that look similar in some fonts
        private val AMBIGUOUS_UPPER = listOf('O', 'I')
        private val AMBIGUOUS_LOWER = listOf('l')
        private val AMBIGUOUS_DIGITS = listOf('0', '1')

        fun registerAll() {
            // Register common presets
            GeneratorRegistry.register("${ID}_weak", PasswordGenerator(PasswordPolicy.WEAK))
            GeneratorRegistry.register("${ID}_standard", PasswordGenerator(PasswordPolicy.STANDARD))
            GeneratorRegistry.register("${ID}_strong", PasswordGenerator(PasswordPolicy.STRONG))
            GeneratorRegistry.register("${ID}_passphrase", PasswordGenerator(PasswordPolicy.PASSPHRASE))
            GeneratorRegistry.register("${ID}_pin4", PasswordGenerator(PasswordPolicy.PIN_4))
            GeneratorRegistry.register("${ID}_pin6", PasswordGenerator(PasswordPolicy.PIN_6))
        }
    }
}
