package com.vadimtoptunov.generators.location

import com.vadimtoptunov.generators.core.DataGenerator
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import com.vadimtoptunov.generators.core.SeededRandom
import com.vadimtoptunov.generators.identity.CountryProfiles

/**
 * Generates realistic street addresses for any country in the world.
 *
 * Strategy:
 * 1. **Rich data (50 countries)**: Uses [CountryProfiles] for countries with detailed
 *    street names, cities, and regions from the identity module.
 * 2. **Fallback (~150 countries)**: Uses [WorldPostalFormats] with generic templates
 *    for countries without detailed data.
 *
 * Use cases:
 * - QA: Test address form validation across different countries
 * - Dev: Seed databases with realistic international addresses
 * - Demo: Generate shipping/billing address samples
 *
 * Example:
 * ```
 * val generator = AddressGenerator("DE")
 * val address = generator.generate()
 * println(address.streetFirst)  // "42 Hauptstraße, Berlin, Berlin 10115"
 * ```
 */
class AddressGenerator(
    private val countryCode: String = "US",
    private val seed: Long? = null
) : DataGenerator<AddressRecord> {

    private val random = SeededRandom(seed)

    // Primary: use CountryProfiles (50 countries with full data)
    private val countryProfile = CountryProfiles[countryCode]

    // Fallback: use WorldPostalFormats (all ~200 countries)
    private val postalFormat = WorldPostalFormats[countryCode]

    override val name = "Address Generator ($countryCode)"
    override val description = "Generates addresses for ${postalFormat?.countryName ?: countryCode}"
    override val supportedFormats = listOf(OutputFormat.CSV, OutputFormat.JSON, OutputFormat.TXT)

    override fun generate(): AddressRecord {
        return when {
            countryProfile != null -> generateFromProfile()
            postalFormat != null -> generateFromFormat()
            else -> throw IllegalArgumentException("Unknown country code: $countryCode")
        }
    }

    override fun generate(seed: Long): AddressRecord {
        return AddressGenerator(countryCode, seed).generate()
    }

    /**
     * Generate address using rich data from CountryProfiles.
     */
    private fun generateFromProfile(): AddressRecord {
        val profile = countryProfile!!
        val format = profile.addressFormat

        return with(random) {
            AddressRecord(
                street = format.streetNames.randomElement(),
                houseNumber = generateHouseNumber(),
                apartment = if (nextBoolean()) generateApartment() else null,
                city = format.cities.randomElement(),
                state = format.regions.randomOrNull(),
                postalCode = generatePostalCodeFromExample(format.postalCodeExample),
                countryCode = profile.countryCode,
                countryName = profile.countryNameEn,
                locale = profile.locale
            )
        }
    }

    /**
     * Generate address using WorldPostalFormats with generic templates.
     */
    private fun generateFromFormat(): AddressRecord {
        val format = postalFormat!!

        return with(random) {
            AddressRecord(
                street = getGenericStreet(format.countryCode),
                houseNumber = generateHouseNumber(),
                apartment = if (nextBoolean()) generateApartment() else null,
                city = getGenericCity(format.countryCode),
                state = null,
                postalCode = if (format.hasPostalCode) generateFromPattern(format.generator) else "",
                countryCode = format.countryCode,
                countryName = format.countryName,
                locale = "en"
            )
        }
    }

    /**
     * Generate postal code from pattern.
     * N = random digit, A = random uppercase letter, other chars = literal.
     */
    private fun generateFromPattern(pattern: String): String = with(random) {
        pattern.map { c ->
            when (c) {
                'N' -> ('0'..'9').randomElement()
                'A' -> ('A'..'Z').randomElement()
                else -> c
            }
        }.joinToString("")
    }

    /**
     * Generate postal code based on example format.
     * Preserves separators (spaces, hyphens) and replaces digits/letters.
     */
    private fun generatePostalCodeFromExample(example: String): String = with(random) {
        example.map { c ->
            when {
                c.isDigit() -> ('0'..'9').randomElement()
                c.isLetter() -> ('A'..'Z').randomElement()
                else -> c
            }
        }.joinToString("")
    }

    /**
     * Generate a realistic house number.
     */
    private fun generateHouseNumber(): String = with(random) {
        val number = (1..999).randomElement()
        // Occasionally add letter suffix (1A, 42B)
        if (nextInt(10) == 0) {
            "$number${('A'..'D').randomElement()}"
        } else {
            number.toString()
        }
    }

    /**
     * Generate apartment/unit number.
     */
    private fun generateApartment(): String = with(random) {
        val formats = listOf(
            { "${(1..99).randomElement()}" },                    // 42
            { "${(1..20).randomElement()}${('A'..'D').randomElement()}" },  // 5B
            { "${(1..9).randomElement()}${(1..99).randomElement().toString().padStart(2, '0')}" }  // 305
        )
        formats.randomElement()()
    }

    /**
     * Get generic street name for countries without detailed data.
     */
    private fun getGenericStreet(countryCode: String): String = with(random) {
        val streets = when (countryCode) {
            // Arabic-speaking
            "SA", "AE", "KW", "BH", "QA", "OM", "YE", "JO", "LB", "SY", "IQ" ->
                listOf("شارع الملك فهد", "شارع الأمير سلطان", "طريق الملك عبدالله", "شارع التحلية")
            // Chinese-speaking
            "CN", "TW" ->
                listOf("中山路", "人民路", "建国路", "解放路", "长安街")
            // Japanese
            "JP" ->
                listOf("中央通り", "銀座通り", "大通り", "本町通り")
            // Korean
            "KR" ->
                listOf("강남대로", "테헤란로", "을지로", "종로")
            // Russian-speaking
            "KZ", "UZ", "TM", "TJ", "KG" ->
                listOf("улица Ленина", "проспект Независимости", "улица Мира", "бульвар Дружбы")
            // Spanish-speaking (Latin America)
            "MX", "AR", "CO", "CL", "PE", "VE", "EC", "BO", "PY", "UY",
            "PA", "CR", "NI", "HN", "SV", "GT", "CU", "DO" ->
                listOf("Calle Principal", "Avenida Central", "Paseo de la Reforma", "Calle del Comercio")
            // Portuguese-speaking
            "BR", "PT", "MZ", "AO" ->
                listOf("Rua Principal", "Avenida Brasil", "Rua do Comércio", "Avenida da Liberdade")
            // Default: English
            else ->
                listOf("Main Street", "High Street", "Central Avenue", "Park Road", "Station Street")
        }
        streets.randomElement()
    }

    /**
     * Get generic city name for countries without detailed data.
     */
    private fun getGenericCity(countryCode: String): String = with(random) {
        val cities = GENERIC_CITIES[countryCode]
            ?: listOf("Capital City", "Second City", "Port City")
        cities.randomElement()
    }

    override fun serialize(record: AddressRecord, format: OutputFormat): String = when (format) {
        OutputFormat.CSV -> buildString {
            append("\"${record.street}\",")
            append("\"${record.houseNumber}\",")
            append("\"${record.apartment ?: ""}\",")
            append("\"${record.city}\",")
            append("\"${record.state ?: ""}\",")
            append("\"${record.postalCode}\",")
            append("${record.countryCode}")
        }
        OutputFormat.JSON -> buildString {
            append("""{"street":"${record.street}"""")
            append(""","houseNumber":"${record.houseNumber}"""")
            record.apartment?.let { append(""","apartment":"$it"""") }
            append(""","city":"${record.city}"""")
            record.state?.let { append(""","state":"$it"""") }
            append(""","postalCode":"${record.postalCode}"""")
            append(""","countryCode":"${record.countryCode}"""")
            append(""","country":"${record.countryName}"""")
            append("}")
        }
        OutputFormat.TXT -> record.streetFirst
        else -> throw UnsupportedOperationException("$format not supported by AddressGenerator")
    }

    override fun serializeBatch(records: List<AddressRecord>, format: OutputFormat): String =
        when (format) {
            OutputFormat.CSV -> buildString {
                appendLine("street,house_number,apartment,city,state,postal_code,country_code")
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
        const val ID = "address_generator"

        // Major cities for countries without detailed CountryProfile data
        private val GENERIC_CITIES = mapOf(
            // Central Asia
            "KZ" to listOf("Almaty", "Nur-Sultan", "Shymkent", "Karaganda"),
            "UZ" to listOf("Tashkent", "Samarkand", "Bukhara", "Namangan"),
            "TM" to listOf("Ashgabat", "Turkmenabat", "Mary", "Dashoguz"),
            "TJ" to listOf("Dushanbe", "Khujand", "Kulob", "Bokhtar"),
            "KG" to listOf("Bishkek", "Osh", "Jalal-Abad", "Karakol"),

            // Middle East
            "IR" to listOf("Tehran", "Isfahan", "Mashhad", "Shiraz"),
            "IQ" to listOf("Baghdad", "Basra", "Mosul", "Erbil"),
            "SA" to listOf("Riyadh", "Jeddah", "Mecca", "Medina"),
            "AE" to listOf("Dubai", "Abu Dhabi", "Sharjah", "Ajman"),
            "QA" to listOf("Doha", "Al Wakrah", "Al Khor", "Umm Salal"),
            "KW" to listOf("Kuwait City", "Hawalli", "Salmiya", "Farwaniya"),
            "BH" to listOf("Manama", "Riffa", "Muharraq", "Hamad Town"),
            "OM" to listOf("Muscat", "Salalah", "Sohar", "Nizwa"),
            "JO" to listOf("Amman", "Zarqa", "Irbid", "Aqaba"),
            "LB" to listOf("Beirut", "Tripoli", "Sidon", "Tyre"),

            // Southeast Asia
            "MM" to listOf("Yangon", "Mandalay", "Naypyidaw", "Mawlamyine"),
            "KH" to listOf("Phnom Penh", "Siem Reap", "Battambang", "Sihanoukville"),
            "LA" to listOf("Vientiane", "Luang Prabang", "Savannakhet", "Pakse"),
            "BN" to listOf("Bandar Seri Begawan", "Kuala Belait", "Seria", "Tutong"),
            "TW" to listOf("Taipei", "Kaohsiung", "Taichung", "Tainan"),

            // Africa
            "NG" to listOf("Lagos", "Abuja", "Kano", "Ibadan"),
            "EG" to listOf("Cairo", "Alexandria", "Giza", "Luxor"),
            "ZA" to listOf("Johannesburg", "Cape Town", "Durban", "Pretoria"),
            "KE" to listOf("Nairobi", "Mombasa", "Kisumu", "Nakuru"),
            "ET" to listOf("Addis Ababa", "Dire Dawa", "Gondar", "Mek'ele"),
            "TZ" to listOf("Dar es Salaam", "Dodoma", "Mwanza", "Arusha"),
            "MA" to listOf("Casablanca", "Rabat", "Marrakech", "Fes"),
            "DZ" to listOf("Algiers", "Oran", "Constantine", "Annaba"),
            "TN" to listOf("Tunis", "Sfax", "Sousse", "Kairouan"),

            // Latin America (those not in CountryProfiles)
            "PA" to listOf("Panama City", "Colón", "David", "Santiago"),
            "CR" to listOf("San José", "Limón", "Alajuela", "Heredia"),
            "NI" to listOf("Managua", "León", "Masaya", "Chinandega"),
            "HN" to listOf("Tegucigalpa", "San Pedro Sula", "Choloma", "La Ceiba"),
            "SV" to listOf("San Salvador", "Santa Ana", "San Miguel", "Mejicanos"),
            "GT" to listOf("Guatemala City", "Mixco", "Villa Nueva", "Quetzaltenango"),
            "CU" to listOf("Havana", "Santiago de Cuba", "Camagüey", "Holguín"),
            "DO" to listOf("Santo Domingo", "Santiago", "Puerto Plata", "La Romana"),

            // Oceania
            "FJ" to listOf("Suva", "Nadi", "Lautoka", "Labasa"),
            "PG" to listOf("Port Moresby", "Lae", "Mount Hagen", "Madang"),
            "NC" to listOf("Nouméa", "Mont-Dore", "Dumbéa", "Païta"),
            "PF" to listOf("Papeete", "Faaa", "Punaauia", "Mahina")
        )

        fun registerAll() {
            // Register primary generators for major countries
            val primaryCountries = listOf(
                "US", "GB", "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH",  // Western Europe
                "PL", "CZ", "HU", "RO", "BG", "GR", "SE", "NO", "DK", "FI",  // Rest of Europe
                "RU", "UA", "BY", "KZ",                                       // Eastern Europe/CIS
                "CN", "JP", "KR", "IN", "ID", "TH", "VN", "MY", "SG", "PH",  // Asia
                "AU", "NZ",                                                   // Oceania
                "CA", "MX", "BR", "AR", "CO", "CL",                           // Americas
                "ZA", "EG", "NG", "KE", "MA"                                  // Africa
            )

            primaryCountries.forEach { code ->
                GeneratorRegistry.register("${ID}_${code.lowercase()}", AddressGenerator(code))
            }
        }

        /** Get all supported country codes. */
        val supportedCountries: Set<String>
            get() = (CountryProfiles.ALL.keys + WorldPostalFormats.ALL.keys)
    }
}
