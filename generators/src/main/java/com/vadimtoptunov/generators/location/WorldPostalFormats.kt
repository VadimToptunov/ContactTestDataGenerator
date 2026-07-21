package com.vadimtoptunov.generators.location

/**
 * Comprehensive postal code database covering ~200 countries/territories.
 *
 * Each entry defines:
 * - [PostalFormat.countryCode]: ISO 3166-1 alpha-2 code
 * - [PostalFormat.countryName]: English country name
 * - [PostalFormat.regex]: Validation pattern
 * - [PostalFormat.generator]: Generation pattern (N=digit, A=letter)
 * - [PostalFormat.hasPostalCode]: Whether country uses postal codes
 *
 * Generator pattern syntax:
 * - N: Random digit (0-9)
 * - A: Random uppercase letter (A-Z)
 * - Other characters: Literal (spaces, hyphens, prefixes)
 *
 * Example: "NNN NN" generates "123 45"
 *          "ANA NAN" generates "M5V 2T6"
 */
object WorldPostalFormats {

    data class PostalFormat(
        val countryCode: String,
        val countryName: String,
        val regex: String,
        val generator: String,
        val hasPostalCode: Boolean = true
    )

    val ALL: Map<String, PostalFormat> by lazy {
        buildMap {
            // ═══════════════════════════════════════════════════════════════════
            // EUROPE (50 countries/territories)
            // ═══════════════════════════════════════════════════════════════════
            put("AD", PostalFormat("AD", "Andorra", "AD\\d{3}", "ADNNN"))
            put("AL", PostalFormat("AL", "Albania", "\\d{4}", "NNNN"))
            put("AT", PostalFormat("AT", "Austria", "\\d{4}", "NNNN"))
            put("AX", PostalFormat("AX", "Åland Islands", "\\d{5}", "NNNNN"))
            put("BA", PostalFormat("BA", "Bosnia and Herzegovina", "\\d{5}", "NNNNN"))
            put("BE", PostalFormat("BE", "Belgium", "\\d{4}", "NNNN"))
            put("BG", PostalFormat("BG", "Bulgaria", "\\d{4}", "NNNN"))
            put("BY", PostalFormat("BY", "Belarus", "\\d{6}", "NNNNNN"))
            put("CH", PostalFormat("CH", "Switzerland", "\\d{4}", "NNNN"))
            put("CY", PostalFormat("CY", "Cyprus", "\\d{4}", "NNNN"))
            put("CZ", PostalFormat("CZ", "Czech Republic", "\\d{3} \\d{2}", "NNN NN"))
            put("DE", PostalFormat("DE", "Germany", "\\d{5}", "NNNNN"))
            put("DK", PostalFormat("DK", "Denmark", "\\d{4}", "NNNN"))
            put("EE", PostalFormat("EE", "Estonia", "\\d{5}", "NNNNN"))
            put("ES", PostalFormat("ES", "Spain", "\\d{5}", "NNNNN"))
            put("FI", PostalFormat("FI", "Finland", "\\d{5}", "NNNNN"))
            put("FO", PostalFormat("FO", "Faroe Islands", "FO-\\d{3}", "FO-NNN"))
            put("FR", PostalFormat("FR", "France", "\\d{5}", "NNNNN"))
            put("GB", PostalFormat("GB", "United Kingdom", "[A-Z]{1,2}\\d[A-Z\\d]? \\d[A-Z]{2}", "AN NAA"))
            put("GI", PostalFormat("GI", "Gibraltar", "GX11 1AA", "GX11 1AA", hasPostalCode = false))
            put("GL", PostalFormat("GL", "Greenland", "\\d{4}", "NNNN"))
            put("GR", PostalFormat("GR", "Greece", "\\d{3} \\d{2}", "NNN NN"))
            put("HR", PostalFormat("HR", "Croatia", "\\d{5}", "NNNNN"))
            put("HU", PostalFormat("HU", "Hungary", "\\d{4}", "NNNN"))
            put("IE", PostalFormat("IE", "Ireland", "[A-Z]\\d{2} [A-Z0-9]{4}", "ANN AAAA"))
            put("IS", PostalFormat("IS", "Iceland", "\\d{3}", "NNN"))
            put("IT", PostalFormat("IT", "Italy", "\\d{5}", "NNNNN"))
            put("LI", PostalFormat("LI", "Liechtenstein", "\\d{4}", "NNNN"))
            put("LT", PostalFormat("LT", "Lithuania", "LT-\\d{5}", "LT-NNNNN"))
            put("LU", PostalFormat("LU", "Luxembourg", "\\d{4}", "NNNN"))
            put("LV", PostalFormat("LV", "Latvia", "LV-\\d{4}", "LV-NNNN"))
            put("MC", PostalFormat("MC", "Monaco", "980\\d{2}", "980NN"))
            put("MD", PostalFormat("MD", "Moldova", "MD-\\d{4}", "MD-NNNN"))
            put("ME", PostalFormat("ME", "Montenegro", "\\d{5}", "NNNNN"))
            put("MK", PostalFormat("MK", "North Macedonia", "\\d{4}", "NNNN"))
            put("MT", PostalFormat("MT", "Malta", "[A-Z]{3} \\d{4}", "AAA NNNN"))
            put("NL", PostalFormat("NL", "Netherlands", "\\d{4} [A-Z]{2}", "NNNN AA"))
            put("NO", PostalFormat("NO", "Norway", "\\d{4}", "NNNN"))
            put("PL", PostalFormat("PL", "Poland", "\\d{2}-\\d{3}", "NN-NNN"))
            put("PT", PostalFormat("PT", "Portugal", "\\d{4}-\\d{3}", "NNNN-NNN"))
            put("RO", PostalFormat("RO", "Romania", "\\d{6}", "NNNNNN"))
            put("RS", PostalFormat("RS", "Serbia", "\\d{5}", "NNNNN"))
            put("RU", PostalFormat("RU", "Russia", "\\d{6}", "NNNNNN"))
            put("SE", PostalFormat("SE", "Sweden", "\\d{3} \\d{2}", "NNN NN"))
            put("SI", PostalFormat("SI", "Slovenia", "\\d{4}", "NNNN"))
            put("SK", PostalFormat("SK", "Slovakia", "\\d{3} \\d{2}", "NNN NN"))
            put("SM", PostalFormat("SM", "San Marino", "4789\\d", "4789N"))
            put("UA", PostalFormat("UA", "Ukraine", "\\d{5}", "NNNNN"))
            put("VA", PostalFormat("VA", "Vatican City", "00120", "00120", hasPostalCode = false))
            put("XK", PostalFormat("XK", "Kosovo", "\\d{5}", "NNNNN"))

            // ═══════════════════════════════════════════════════════════════════
            // AMERICAS (35 countries/territories)
            // ═══════════════════════════════════════════════════════════════════
            put("US", PostalFormat("US", "United States", "\\d{5}(-\\d{4})?", "NNNNN"))
            put("CA", PostalFormat("CA", "Canada", "[A-Z]\\d[A-Z] \\d[A-Z]\\d", "ANA NAN"))
            put("MX", PostalFormat("MX", "Mexico", "\\d{5}", "NNNNN"))
            put("AR", PostalFormat("AR", "Argentina", "[A-Z]\\d{4}[A-Z]{3}", "ANNNNAAA"))
            put("BR", PostalFormat("BR", "Brazil", "\\d{5}-\\d{3}", "NNNNN-NNN"))
            put("CL", PostalFormat("CL", "Chile", "\\d{7}", "NNNNNNN"))
            put("CO", PostalFormat("CO", "Colombia", "\\d{6}", "NNNNNN"))
            put("PE", PostalFormat("PE", "Peru", "\\d{5}", "NNNNN"))
            put("VE", PostalFormat("VE", "Venezuela", "\\d{4}", "NNNN"))
            put("EC", PostalFormat("EC", "Ecuador", "\\d{6}", "NNNNNN"))
            put("BO", PostalFormat("BO", "Bolivia", "\\d{4}", "NNNN"))
            put("PY", PostalFormat("PY", "Paraguay", "\\d{4}", "NNNN"))
            put("UY", PostalFormat("UY", "Uruguay", "\\d{5}", "NNNNN"))
            put("GY", PostalFormat("GY", "Guyana", "", "", hasPostalCode = false))
            put("SR", PostalFormat("SR", "Suriname", "", "", hasPostalCode = false))
            put("GF", PostalFormat("GF", "French Guiana", "973\\d{2}", "973NN"))
            put("PA", PostalFormat("PA", "Panama", "\\d{4}", "NNNN"))
            put("CR", PostalFormat("CR", "Costa Rica", "\\d{5}", "NNNNN"))
            put("NI", PostalFormat("NI", "Nicaragua", "\\d{5}", "NNNNN"))
            put("HN", PostalFormat("HN", "Honduras", "\\d{5}", "NNNNN"))
            put("SV", PostalFormat("SV", "El Salvador", "\\d{4}", "NNNN"))
            put("GT", PostalFormat("GT", "Guatemala", "\\d{5}", "NNNNN"))
            put("BZ", PostalFormat("BZ", "Belize", "", "", hasPostalCode = false))
            put("CU", PostalFormat("CU", "Cuba", "\\d{5}", "NNNNN"))
            put("DO", PostalFormat("DO", "Dominican Republic", "\\d{5}", "NNNNN"))
            put("HT", PostalFormat("HT", "Haiti", "\\d{4}", "NNNN"))
            put("JM", PostalFormat("JM", "Jamaica", "", "", hasPostalCode = false))
            put("PR", PostalFormat("PR", "Puerto Rico", "\\d{5}(-\\d{4})?", "NNNNN"))
            put("TT", PostalFormat("TT", "Trinidad and Tobago", "\\d{6}", "NNNNNN"))
            put("BB", PostalFormat("BB", "Barbados", "BB\\d{5}", "BBNNNNN"))
            put("BS", PostalFormat("BS", "Bahamas", "", "", hasPostalCode = false))
            put("KY", PostalFormat("KY", "Cayman Islands", "KY\\d-\\d{4}", "KYN-NNNN"))
            put("VI", PostalFormat("VI", "US Virgin Islands", "\\d{5}", "NNNNN"))
            put("AW", PostalFormat("AW", "Aruba", "", "", hasPostalCode = false))
            put("CW", PostalFormat("CW", "Curaçao", "", "", hasPostalCode = false))

            // ═══════════════════════════════════════════════════════════════════
            // ASIA (50 countries/territories)
            // ═══════════════════════════════════════════════════════════════════
            put("CN", PostalFormat("CN", "China", "\\d{6}", "NNNNNN"))
            put("JP", PostalFormat("JP", "Japan", "\\d{3}-\\d{4}", "NNN-NNNN"))
            put("KR", PostalFormat("KR", "South Korea", "\\d{5}", "NNNNN"))
            put("IN", PostalFormat("IN", "India", "\\d{6}", "NNNNNN"))
            put("ID", PostalFormat("ID", "Indonesia", "\\d{5}", "NNNNN"))
            put("TH", PostalFormat("TH", "Thailand", "\\d{5}", "NNNNN"))
            put("VN", PostalFormat("VN", "Vietnam", "\\d{6}", "NNNNNN"))
            put("MY", PostalFormat("MY", "Malaysia", "\\d{5}", "NNNNN"))
            put("SG", PostalFormat("SG", "Singapore", "\\d{6}", "NNNNNN"))
            put("PH", PostalFormat("PH", "Philippines", "\\d{4}", "NNNN"))
            put("PK", PostalFormat("PK", "Pakistan", "\\d{5}", "NNNNN"))
            put("BD", PostalFormat("BD", "Bangladesh", "\\d{4}", "NNNN"))
            put("LK", PostalFormat("LK", "Sri Lanka", "\\d{5}", "NNNNN"))
            put("NP", PostalFormat("NP", "Nepal", "\\d{5}", "NNNNN"))
            put("MM", PostalFormat("MM", "Myanmar", "\\d{5}", "NNNNN"))
            put("KH", PostalFormat("KH", "Cambodia", "\\d{5}", "NNNNN"))
            put("LA", PostalFormat("LA", "Laos", "\\d{5}", "NNNNN"))
            put("BN", PostalFormat("BN", "Brunei", "[A-Z]{2}\\d{4}", "AANNNN"))
            put("TW", PostalFormat("TW", "Taiwan", "\\d{3}(\\d{2})?", "NNNNN"))
            put("HK", PostalFormat("HK", "Hong Kong", "", "", hasPostalCode = false))
            put("MO", PostalFormat("MO", "Macau", "", "", hasPostalCode = false))
            put("MN", PostalFormat("MN", "Mongolia", "\\d{5}", "NNNNN"))
            put("KP", PostalFormat("KP", "North Korea", "", "", hasPostalCode = false))
            put("KZ", PostalFormat("KZ", "Kazakhstan", "\\d{6}", "NNNNNN"))
            put("UZ", PostalFormat("UZ", "Uzbekistan", "\\d{6}", "NNNNNN"))
            put("TM", PostalFormat("TM", "Turkmenistan", "\\d{6}", "NNNNNN"))
            put("TJ", PostalFormat("TJ", "Tajikistan", "\\d{6}", "NNNNNN"))
            put("KG", PostalFormat("KG", "Kyrgyzstan", "\\d{6}", "NNNNNN"))
            put("AF", PostalFormat("AF", "Afghanistan", "\\d{4}", "NNNN"))
            put("IR", PostalFormat("IR", "Iran", "\\d{10}", "NNNNNNNNNN"))
            put("IQ", PostalFormat("IQ", "Iraq", "\\d{5}", "NNNNN"))
            put("SA", PostalFormat("SA", "Saudi Arabia", "\\d{5}(-\\d{4})?", "NNNNN"))
            put("AE", PostalFormat("AE", "UAE", "", "", hasPostalCode = false))
            put("QA", PostalFormat("QA", "Qatar", "", "", hasPostalCode = false))
            put("KW", PostalFormat("KW", "Kuwait", "\\d{5}", "NNNNN"))
            put("BH", PostalFormat("BH", "Bahrain", "\\d{3,4}", "NNNN"))
            put("OM", PostalFormat("OM", "Oman", "\\d{3}", "NNN"))
            put("YE", PostalFormat("YE", "Yemen", "", "", hasPostalCode = false))
            put("JO", PostalFormat("JO", "Jordan", "\\d{5}", "NNNNN"))
            put("LB", PostalFormat("LB", "Lebanon", "\\d{4}( \\d{4})?", "NNNN NNNN"))
            put("SY", PostalFormat("SY", "Syria", "", "", hasPostalCode = false))
            put("IL", PostalFormat("IL", "Israel", "\\d{7}", "NNNNNNN"))
            put("PS", PostalFormat("PS", "Palestine", "\\d{3}", "NNN"))
            put("TR", PostalFormat("TR", "Turkey", "\\d{5}", "NNNNN"))
            put("GE", PostalFormat("GE", "Georgia", "\\d{4}", "NNNN"))
            put("AM", PostalFormat("AM", "Armenia", "\\d{4}", "NNNN"))
            put("AZ", PostalFormat("AZ", "Azerbaijan", "AZ \\d{4}", "AZ NNNN"))

            // ═══════════════════════════════════════════════════════════════════
            // AFRICA (55 countries/territories)
            // ═══════════════════════════════════════════════════════════════════
            put("ZA", PostalFormat("ZA", "South Africa", "\\d{4}", "NNNN"))
            put("EG", PostalFormat("EG", "Egypt", "\\d{5}", "NNNNN"))
            put("NG", PostalFormat("NG", "Nigeria", "\\d{6}", "NNNNNN"))
            put("KE", PostalFormat("KE", "Kenya", "\\d{5}", "NNNNN"))
            put("ET", PostalFormat("ET", "Ethiopia", "\\d{4}", "NNNN"))
            put("TZ", PostalFormat("TZ", "Tanzania", "\\d{5}", "NNNNN"))
            put("UG", PostalFormat("UG", "Uganda", "", "", hasPostalCode = false))
            put("GH", PostalFormat("GH", "Ghana", "", "", hasPostalCode = false))
            put("CI", PostalFormat("CI", "Côte d'Ivoire", "", "", hasPostalCode = false))
            put("SN", PostalFormat("SN", "Senegal", "\\d{5}", "NNNNN"))
            put("CM", PostalFormat("CM", "Cameroon", "", "", hasPostalCode = false))
            put("CD", PostalFormat("CD", "DR Congo", "", "", hasPostalCode = false))
            put("MA", PostalFormat("MA", "Morocco", "\\d{5}", "NNNNN"))
            put("DZ", PostalFormat("DZ", "Algeria", "\\d{5}", "NNNNN"))
            put("TN", PostalFormat("TN", "Tunisia", "\\d{4}", "NNNN"))
            put("LY", PostalFormat("LY", "Libya", "", "", hasPostalCode = false))
            put("SD", PostalFormat("SD", "Sudan", "\\d{5}", "NNNNN"))
            put("AO", PostalFormat("AO", "Angola", "", "", hasPostalCode = false))
            put("MZ", PostalFormat("MZ", "Mozambique", "\\d{4}", "NNNN"))
            put("ZW", PostalFormat("ZW", "Zimbabwe", "", "", hasPostalCode = false))
            put("ZM", PostalFormat("ZM", "Zambia", "\\d{5}", "NNNNN"))
            put("BW", PostalFormat("BW", "Botswana", "", "", hasPostalCode = false))
            put("NA", PostalFormat("NA", "Namibia", "\\d{5}", "NNNNN"))
            put("MU", PostalFormat("MU", "Mauritius", "\\d{5}", "NNNNN"))
            put("MG", PostalFormat("MG", "Madagascar", "\\d{3}", "NNN"))
            put("RE", PostalFormat("RE", "Réunion", "974\\d{2}", "974NN"))
            put("RW", PostalFormat("RW", "Rwanda", "", "", hasPostalCode = false))
            put("BI", PostalFormat("BI", "Burundi", "", "", hasPostalCode = false))
            put("ML", PostalFormat("ML", "Mali", "", "", hasPostalCode = false))
            put("NE", PostalFormat("NE", "Niger", "\\d{4}", "NNNN"))
            put("BF", PostalFormat("BF", "Burkina Faso", "", "", hasPostalCode = false))
            put("TG", PostalFormat("TG", "Togo", "", "", hasPostalCode = false))
            put("BJ", PostalFormat("BJ", "Benin", "", "", hasPostalCode = false))
            put("MR", PostalFormat("MR", "Mauritania", "", "", hasPostalCode = false))
            put("GA", PostalFormat("GA", "Gabon", "", "", hasPostalCode = false))
            put("CG", PostalFormat("CG", "Congo", "", "", hasPostalCode = false))
            put("CF", PostalFormat("CF", "Central African Republic", "", "", hasPostalCode = false))
            put("TD", PostalFormat("TD", "Chad", "", "", hasPostalCode = false))
            put("GN", PostalFormat("GN", "Guinea", "\\d{3}", "NNN"))
            put("LR", PostalFormat("LR", "Liberia", "\\d{4}", "NNNN"))
            put("SL", PostalFormat("SL", "Sierra Leone", "", "", hasPostalCode = false))
            put("GM", PostalFormat("GM", "Gambia", "", "", hasPostalCode = false))
            put("GW", PostalFormat("GW", "Guinea-Bissau", "\\d{4}", "NNNN"))
            put("CV", PostalFormat("CV", "Cape Verde", "\\d{4}", "NNNN"))
            put("ST", PostalFormat("ST", "São Tomé and Príncipe", "", "", hasPostalCode = false))
            put("GQ", PostalFormat("GQ", "Equatorial Guinea", "", "", hasPostalCode = false))
            put("SS", PostalFormat("SS", "South Sudan", "", "", hasPostalCode = false))
            put("ER", PostalFormat("ER", "Eritrea", "", "", hasPostalCode = false))
            put("DJ", PostalFormat("DJ", "Djibouti", "", "", hasPostalCode = false))
            put("SO", PostalFormat("SO", "Somalia", "", "", hasPostalCode = false))
            put("SC", PostalFormat("SC", "Seychelles", "", "", hasPostalCode = false))
            put("KM", PostalFormat("KM", "Comoros", "", "", hasPostalCode = false))
            put("MW", PostalFormat("MW", "Malawi", "", "", hasPostalCode = false))
            put("LS", PostalFormat("LS", "Lesotho", "\\d{3}", "NNN"))
            put("SZ", PostalFormat("SZ", "Eswatini", "[A-Z]\\d{3}", "ANNN"))

            // ═══════════════════════════════════════════════════════════════════
            // OCEANIA (15 countries/territories)
            // ═══════════════════════════════════════════════════════════════════
            put("AU", PostalFormat("AU", "Australia", "\\d{4}", "NNNN"))
            put("NZ", PostalFormat("NZ", "New Zealand", "\\d{4}", "NNNN"))
            put("FJ", PostalFormat("FJ", "Fiji", "", "", hasPostalCode = false))
            put("PG", PostalFormat("PG", "Papua New Guinea", "\\d{3}", "NNN"))
            put("NC", PostalFormat("NC", "New Caledonia", "988\\d{2}", "988NN"))
            put("PF", PostalFormat("PF", "French Polynesia", "987\\d{2}", "987NN"))
            put("WS", PostalFormat("WS", "Samoa", "", "", hasPostalCode = false))
            put("TO", PostalFormat("TO", "Tonga", "", "", hasPostalCode = false))
            put("VU", PostalFormat("VU", "Vanuatu", "", "", hasPostalCode = false))
            put("SB", PostalFormat("SB", "Solomon Islands", "", "", hasPostalCode = false))
            put("FM", PostalFormat("FM", "Micronesia", "\\d{5}", "NNNNN"))
            put("GU", PostalFormat("GU", "Guam", "\\d{5}", "NNNNN"))
            put("MH", PostalFormat("MH", "Marshall Islands", "\\d{5}", "NNNNN"))
            put("PW", PostalFormat("PW", "Palau", "\\d{5}", "NNNNN"))
            put("AS", PostalFormat("AS", "American Samoa", "\\d{5}", "NNNNN"))

            // ═══════════════════════════════════════════════════════════════════
            // ADDITIONAL TERRITORIES (10)
            // ═══════════════════════════════════════════════════════════════════
            put("GP", PostalFormat("GP", "Guadeloupe", "971\\d{2}", "971NN"))
            put("MQ", PostalFormat("MQ", "Martinique", "972\\d{2}", "972NN"))
            put("YT", PostalFormat("YT", "Mayotte", "976\\d{2}", "976NN"))
            put("PM", PostalFormat("PM", "Saint Pierre and Miquelon", "975\\d{2}", "975NN"))
            put("WF", PostalFormat("WF", "Wallis and Futuna", "986\\d{2}", "986NN"))
            put("BL", PostalFormat("BL", "Saint Barthélemy", "97133", "97133", hasPostalCode = false))
            put("MF", PostalFormat("MF", "Saint Martin", "97150", "97150", hasPostalCode = false))
            put("SX", PostalFormat("SX", "Sint Maarten", "", "", hasPostalCode = false))
            put("BQ", PostalFormat("BQ", "Bonaire, Sint Eustatius and Saba", "", "", hasPostalCode = false))
            put("IM", PostalFormat("IM", "Isle of Man", "IM\\d \\d[A-Z]{2}", "IMN NAA"))
            put("JE", PostalFormat("JE", "Jersey", "JE\\d \\d[A-Z]{2}", "JEN NAA"))
            put("GG", PostalFormat("GG", "Guernsey", "GY\\d \\d[A-Z]{2}", "GYN NAA"))
        }
    }

    operator fun get(code: String): PostalFormat? = ALL[code.uppercase()]

    /** Get all country codes that have postal codes. */
    val countriesWithPostalCodes: List<String>
        get() = ALL.values.filter { it.hasPostalCode }.map { it.countryCode }

    /** Get all country codes that don't use postal codes. */
    val countriesWithoutPostalCodes: List<String>
        get() = ALL.values.filter { !it.hasPostalCode }.map { it.countryCode }

    /** Total number of countries/territories in the database. */
    val size: Int get() = ALL.size
}
