package com.vadimtoptunov.generators.contacts

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import com.vadimtoptunov.generators.core.SeededRandom

/**
 * Generates realistic test contacts (name, phone, email, company, job title).
 *
 * This is the DevData Factory replacement for the app's old `FakeDataGenerator`
 * + `VcfGenerator` pair: it implements [DataGenerator] so contacts gain the same
 * multi-format export (VCF/CSV/JSON/SQL), reproducible seeds, and registry-based
 * discovery as every other generator.
 *
 * All data is fictional and safe for testing — names, companies and job titles
 * are drawn from fixed sample pools; phone numbers use realistic international
 * dialling formats but are not assigned to any real subscriber.
 *
 * @param fields which fields to populate (defaults to all).
 */
class ContactGenerator(
    private val fields: ContactFields = ContactFields(),
) : DataGenerator<ContactRecord> {

    override val name = "Contact Generator"
    override val description = "Generates fake contacts (name, phone, email, company, job title)"
    override val supportedFormats = listOf(
        OutputFormat.VCF,
        OutputFormat.CSV,
        OutputFormat.JSON,
        OutputFormat.SQL,
    )

    override fun generate(): ContactRecord = generateWith(SeededRandom())

    override fun generate(seed: Long): ContactRecord = generateWith(SeededRandom(seed))

    private fun generateWith(random: SeededRandom): ContactRecord {
        // A name is generated even when it is excluded from the record, because
        // the email address is derived from it (matching the original behaviour).
        val nameForEmail = if (fields.includeName) randomFullName(random) else "Contact"

        return ContactRecord(
            fullName = if (fields.includeName) nameForEmail else null,
            phone = if (fields.includePhone) randomPhoneNumber(random) else null,
            email = if (fields.includeEmail) randomEmail(nameForEmail, random) else null,
            company = if (fields.includeCompany) pick(COMPANIES, random) else null,
            jobTitle = if (fields.includeJobTitle) pick(JOB_TITLES, random) else null,
        )
    }

    // ── Generation helpers ──────────────────────────────────────────────────

    private fun randomFullName(random: SeededRandom): String =
        "${pick(FIRST_NAMES, random)} ${pick(LAST_NAMES, random)}"

    private fun randomEmail(name: String, random: SeededRandom): String {
        val localPart = name.replace(" ", ".").lowercase()
        return "$localPart@${pick(EMAIL_DOMAINS, random)}"
    }

    private fun randomPhoneNumber(random: SeededRandom): String {
        val format = pick(PHONE_FORMATS, random)
        val numberParts = format.digitGroups.map { digitCount -> randomDigitGroup(digitCount, random) }
        return "${format.countryCode}${format.separator}${numberParts.joinToString(format.separator)}"
    }

    private fun randomDigitGroup(length: Int, random: SeededRandom): String {
        val min = if (length == 1) 1 else tenPow(length - 1)
        val max = tenPow(length) - 1
        return random.nextInt(min, max + 1).toString().padStart(length, '0')
    }

    private fun <T> pick(items: List<T>, random: SeededRandom): T = items[random.nextInt(items.size)]

    // ── Serialization ───────────────────────────────────────────────────────

    override fun serialize(record: ContactRecord, format: OutputFormat): String = when (format) {
        OutputFormat.VCF -> toVCard(record)
        OutputFormat.CSV -> toCsvRow(record)
        OutputFormat.JSON -> toJson(record)
        OutputFormat.SQL -> toSqlInsert(record)
        else -> throw UnsupportedOperationException("$format not supported by ContactGenerator")
    }

    override fun serializeBatch(records: List<ContactRecord>, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> buildString {
            appendLine(CSV_HEADER)
            records.forEach { appendLine(toCsvRow(it)) }
        }.trimEnd('\n')

        OutputFormat.JSON -> buildString {
            appendLine("[")
            records.forEachIndexed { index, record ->
                append("  ").append(toJson(record))
                appendLine(if (index < records.lastIndex) "," else "")
            }
            append("]")
        }

        OutputFormat.SQL -> records.joinToString("\n") { toSqlInsert(it) }
        OutputFormat.VCF -> records.joinToString("\n") { toVCard(it) }
        else -> super.serializeBatch(records, format)
    }

    private fun toVCard(record: ContactRecord): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        record.fullName?.let { name ->
            appendLine("FN:$name")
            val parts = name.split(" ")
            if (parts.size >= 2) appendLine("N:${parts.last()};${parts.first()};;;")
            else appendLine("N:$name;;;;")
        }
        record.phone?.let { appendLine("TEL;TYPE=CELL:$it") }
        record.email?.let { appendLine("EMAIL;TYPE=INTERNET:$it") }
        record.company?.let { appendLine("ORG:$it") }
        record.jobTitle?.let { appendLine("TITLE:$it") }
        append("END:VCARD")
    }

    private fun toCsvRow(record: ContactRecord): String = listOf(
        record.fullName, record.phone, record.email, record.company, record.jobTitle,
    ).joinToString(",") { csvEscape(it.orEmpty()) }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun toJson(record: ContactRecord): String = buildString {
        append("{")
        append("\"fullName\":").append(jsonValue(record.fullName)).append(",")
        append("\"phone\":").append(jsonValue(record.phone)).append(",")
        append("\"email\":").append(jsonValue(record.email)).append(",")
        append("\"company\":").append(jsonValue(record.company)).append(",")
        append("\"jobTitle\":").append(jsonValue(record.jobTitle))
        append("}")
    }

    private fun jsonValue(value: String?): String =
        if (value == null) "null" else "\"${jsonEscape(value)}\""

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun toSqlInsert(record: ContactRecord): String {
        val values = listOf(
            record.fullName, record.phone, record.email, record.company, record.jobTitle,
        ).joinToString(", ") { sqlValue(it) }
        return "INSERT INTO contacts (full_name, phone, email, company, job_title) VALUES ($values);"
    }

    private fun sqlValue(value: String?): String =
        if (value == null) "NULL" else "'${value.replace("'", "''")}'"

    companion object {
        const val ID = "contact_generator"

        private const val CSV_HEADER = "full_name,phone,email,company,job_title"

        /** Register the default contact generator (all fields) with the registry. */
        fun registerAll() {
            GeneratorRegistry.register(ID, ContactGenerator())
        }

        private fun tenPow(exponent: Int): Int {
            var result = 1
            repeat(exponent) { result *= 10 }
            return result
        }

        // ── Sample data pools (fictional) ───────────────────────────────────

        private data class PhoneFormat(
            val countryCode: String,
            val digitGroups: List<Int>,
            val separator: String = "-",
        )

        private val PHONE_FORMATS = listOf(
            PhoneFormat("+1", listOf(3, 3, 4)),            // USA, Canada
            PhoneFormat("+44", listOf(4, 3, 4)),           // UK
            PhoneFormat("+49", listOf(3, 4, 4)),           // Germany
            PhoneFormat("+33", listOf(1, 2, 2, 2, 2)),     // France
            PhoneFormat("+34", listOf(3, 3, 3)),           // Spain
            PhoneFormat("+39", listOf(3, 3, 4)),           // Italy
            PhoneFormat("+48", listOf(3, 3, 3)),           // Poland
            PhoneFormat("+31", listOf(2, 3, 4)),           // Netherlands
            PhoneFormat("+46", listOf(2, 3, 4)),           // Sweden
            PhoneFormat("+41", listOf(2, 3, 4)),           // Switzerland
            PhoneFormat("+7", listOf(3, 3, 2, 2)),         // Russia, Kazakhstan
            PhoneFormat("+380", listOf(2, 3, 2, 2)),       // Ukraine
            PhoneFormat("+375", listOf(2, 3, 2, 2)),       // Belarus
            PhoneFormat("+86", listOf(3, 4, 4)),           // China
            PhoneFormat("+81", listOf(2, 4, 4)),           // Japan
            PhoneFormat("+82", listOf(2, 4, 4)),           // South Korea
            PhoneFormat("+91", listOf(5, 5), " "),         // India
            PhoneFormat("+65", listOf(4, 4)),              // Singapore
            PhoneFormat("+852", listOf(4, 4)),             // Hong Kong
            PhoneFormat("+62", listOf(3, 4, 4)),           // Indonesia
            PhoneFormat("+66", listOf(2, 3, 4)),           // Thailand
            PhoneFormat("+84", listOf(2, 4, 4)),           // Vietnam
            PhoneFormat("+971", listOf(2, 3, 4)),          // UAE
            PhoneFormat("+966", listOf(2, 3, 4)),          // Saudi Arabia
            PhoneFormat("+972", listOf(2, 3, 4)),          // Israel
            PhoneFormat("+90", listOf(3, 3, 4)),           // Turkey
            PhoneFormat("+55", listOf(2, 5, 4)),           // Brazil
            PhoneFormat("+52", listOf(3, 3, 4)),           // Mexico
            PhoneFormat("+54", listOf(2, 4, 4)),           // Argentina
            PhoneFormat("+56", listOf(1, 4, 4)),           // Chile
            PhoneFormat("+57", listOf(3, 3, 4)),           // Colombia
            PhoneFormat("+61", listOf(3, 3, 3)),           // Australia
            PhoneFormat("+64", listOf(2, 3, 4)),           // New Zealand
            PhoneFormat("+27", listOf(2, 3, 4)),           // South Africa
            PhoneFormat("+20", listOf(3, 3, 4)),           // Egypt
            PhoneFormat("+234", listOf(3, 3, 4)),          // Nigeria
        )

        private val FIRST_NAMES = listOf(
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
            "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Christopher", "Karen", "Daniel", "Nancy", "Matthew", "Lisa",
            "Anthony", "Betty", "Mark", "Margaret", "Donald", "Sandra", "Steven", "Ashley",
            "Paul", "Kimberly", "Andrew", "Emily", "Joshua", "Donna", "Kenneth", "Michelle",
            "Kevin", "Carol", "Brian", "Amanda", "George", "Dorothy", "Timothy", "Melissa",
            "Ronald", "Deborah", "Edward", "Stephanie", "Jason", "Rebecca", "Jeffrey", "Sharon",
            "Ryan", "Laura", "Jacob", "Cynthia", "Gary", "Kathleen", "Nicholas", "Amy",
            "Eric", "Angela", "Jonathan", "Shirley", "Stephen", "Anna", "Larry", "Brenda",
        )

        private val LAST_NAMES = listOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
            "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
            "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
            "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
            "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
            "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy",
        )

        private val COMPANIES = listOf(
            "TechCorp", "Innovate Solutions", "Global Systems", "Digital Dynamics", "Quantum Industries",
            "NextGen Technologies", "Fusion Enterprises", "Vertex Corp", "Axiom Systems", "Pinnacle Group",
            "Catalyst Inc", "Horizon Technologies", "Stellar Solutions", "Momentum Corp", "Velocity Systems",
            "Nexus Enterprises", "Summit Industries", "Apex Corporation", "Prime Technologies", "Zenith Group",
            "Vanguard Systems", "Odyssey Corp", "Atlas Technologies", "Frontier Solutions", "Titan Industries",
            "Phoenix Enterprises", "Spectrum Corp", "Infinity Systems", "Meridian Group", "Eclipse Technologies",
        )

        private val JOB_TITLES = listOf(
            "Software Engineer", "Product Manager", "Data Analyst", "UX Designer", "Marketing Manager",
            "Sales Representative", "Project Manager", "Business Analyst", "DevOps Engineer", "HR Manager",
            "Financial Analyst", "Customer Success Manager", "Operations Manager", "Quality Assurance Engineer", "Content Writer",
            "Accountant", "Legal Counsel", "Systems Administrator", "Network Engineer", "Database Administrator",
            "Frontend Developer", "Backend Developer", "Full Stack Developer", "Mobile Developer", "Security Analyst",
            "Research Scientist", "Technical Writer", "Business Development Manager", "Product Designer", "Solutions Architect",
        )

        private val EMAIL_DOMAINS = listOf(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com",
            "protonmail.com", "mail.com", "aol.com", "zoho.com", "fastmail.com",
        )
    }
}
