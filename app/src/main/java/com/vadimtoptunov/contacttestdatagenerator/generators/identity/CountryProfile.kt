package com.vadimtoptunov.contacttestdatagenerator.generators.identity

/**
 * Country-specific configuration for synthetic identity generation.
 *
 * Covers 50 countries / territories across all major KYC markets.
 * Each profile defines:
 *   - Which document types are issued
 *   - Document number format (regex pattern + generator hint)
 *   - Locale-specific name datasets
 *   - Address format
 *   - ICAO 3-letter nationality code
 *   - Whether NFC / eMRTD chips are typical for this country
 */
data class CountryProfile(
    val countryCode: String,          // ISO 3166-1 alpha-2
    val countryNameEn: String,
    val nationality: String,          // ICAO 3-letter (Doc 9303)
    val locale: String,               // BCP-47
    val documentTypes: List<DocumentTypeSpec>,
    val nameDataset: NameDataset,
    val addressFormat: AddressFormat,
    val taxIdCountryCode: String?,    // maps to TaxIdGenerator, null if not supported yet
    val typicalNfcDocs: List<DocumentType>,  // which doc types usually have NFC chips
    val notes: String = ""
)

data class DocumentTypeSpec(
    val type: DocumentType,
    val numberFormat: String,         // human description e.g. "2 letters + 7 digits"
    val numberRegex: String,          // for validation display only
    val numberLength: Int,
    val validityYears: Int,
    val hasBiometricChip: Boolean
)

data class NameDataset(
    val firstNamesMale: List<String>,
    val firstNamesFemale: List<String>,
    val lastNames: List<String>,
    val script: NameScript = NameScript.LATIN
)

enum class NameScript { LATIN, CYRILLIC, ARABIC, CHINESE, DEVANAGARI, GEORGIAN, ARMENIAN, HEBREW }

data class AddressFormat(
    val streetFirst: Boolean = true,     // true = "123 Main St, City" style
    val postalCodeRegex: String,
    val postalCodeExample: String,
    val regions: List<String>,
    val cities: List<String>,
    val streetNames: List<String>
)

// ─── Registry ─────────────────────────────────────────────────────────────

object CountryProfiles {

    val ALL: Map<String, CountryProfile> by lazy {
        listOf(
            russia(), ukraine(), usa(), uk(), germany(), france(), spain(),
            italy(), poland(), brazil(), netherlands(), sweden(), switzerland(),
            austria(), belgium(), portugal(), czechia(), hungary(), romania(),
            bulgaria(), greece(), turkey(), israel(), uae(), saudiArabia(),
            india(), china(), japan(), southKorea(), singapore(), australia(),
            newZealand(), canada(), mexico(), argentina(), colombia(), chile(),
            southAfrica(), nigeria(), egypt(), indonesia(), thailand(), vietnam(),
            malaysia(), philippines(), pakistan(), bangladesh(), georgia(),
            armenia(), kazakhstan()
        ).associateBy { it.countryCode }
    }

    operator fun get(code: String): CountryProfile? = ALL[code.uppercase()]

    // ─── Europe ──────────────────────────────────────────────────────────

    private fun russia() = CountryProfile(
        countryCode = "RU", countryNameEn = "Russia", nationality = "RUS",
        locale = "ru-RU",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 digits + 6 digits", "\\d{2}\\s\\d{6}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Internal passport series+number", "\\d{4}\\s\\d{6}", 10, 10, false),
            DocumentTypeSpec(DocumentType.FOREIGN_PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Александр","Дмитрий","Михаил","Иван","Алексей","Сергей","Андрей","Никита","Кирилл","Павел"),
            firstNamesFemale = listOf("Анна","Мария","Елена","Ольга","Наталья","Екатерина","Татьяна","Юлия","Ирина","Дарья"),
            lastNames        = listOf("Иванов","Смирнов","Кузнецов","Попов","Васильев","Петров","Соколов","Михайлов","Новиков","Федоров"),
            script = NameScript.CYRILLIC
        ),
        addressFormat = AddressFormat(
            streetFirst = false,
            postalCodeRegex = "\\d{6}", postalCodeExample = "123456",
            regions = listOf("Москва","Санкт-Петербург","Краснодарский край","Свердловская обл.","Татарстан"),
            cities  = listOf("Москва","Санкт-Петербург","Екатеринбург","Казань","Новосибирск","Краснодар"),
            streetNames = listOf("ул. Ленина","пр. Мира","ул. Советская","ул. Гагарина","пр. Победы")
        ),
        taxIdCountryCode = "RU",
        typicalNfcDocs = listOf(DocumentType.FOREIGN_PASSPORT)
    )

    private fun ukraine() = CountryProfile(
        countryCode = "UA", countryNameEn = "Ukraine", nationality = "UKR",
        locale = "uk-UA",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 6 digits", "[А-ЯІЇЄ]{2}\\d{6}", 9, 10, false),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "9 digits (ID card)", "\\d{9}", 9, 10, true),
            DocumentTypeSpec(DocumentType.FOREIGN_PASSPORT, "2 letters + 6 digits", "[A-Z]{2}\\d{6}", 8, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Олексій","Михайло","Іван","Андрій","Василь","Сергій","Дмитро","Олег","Максим","Тарас"),
            firstNamesFemale = listOf("Оксана","Наталія","Ірина","Тетяна","Людмила","Марія","Ганна","Валентина","Олена","Юлія"),
            lastNames        = listOf("Шевченко","Бондаренко","Коваленко","Бойко","Ткаченко","Кравченко","Кovalenko","Мороз","Лисенко","Марченко"),
            script = NameScript.CYRILLIC
        ),
        addressFormat = AddressFormat(
            streetFirst = false,
            postalCodeRegex = "\\d{5}", postalCodeExample = "01001",
            regions = listOf("Київська","Харківська","Одеська","Дніпропетровська","Львівська"),
            cities  = listOf("Київ","Харків","Одеса","Дніпро","Львів","Запоріжжя"),
            streetNames = listOf("вул. Хрещатик","вул. Шевченка","пр. Перемоги","вул. Богдана Хмельницького")
        ),
        taxIdCountryCode = "UA",
        typicalNfcDocs = listOf(DocumentType.NATIONAL_ID, DocumentType.FOREIGN_PASSPORT)
    )

    private fun usa() = CountryProfile(
        countryCode = "US", countryNameEn = "United States", nationality = "USA",
        locale = "en-US",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "9 digits", "\\d{9}", 9, 10, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "State-specific (6-12 alphanum)", "[A-Z0-9]{6,12}", 10, 8, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("James","John","Robert","Michael","William","David","Richard","Joseph","Thomas","Charles"),
            firstNamesFemale = listOf("Mary","Patricia","Jennifer","Linda","Barbara","Elizabeth","Susan","Jessica","Sarah","Karen"),
            lastNames        = listOf("Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis","Wilson","Anderson")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}(-\\d{4})?", postalCodeExample = "10001",
            regions = listOf("California","Texas","New York","Florida","Illinois","Pennsylvania","Ohio","Georgia"),
            cities  = listOf("New York","Los Angeles","Chicago","Houston","Phoenix","Philadelphia","San Antonio","San Diego"),
            streetNames = listOf("Main St","Oak Ave","Maple Dr","Cedar Ln","Park Blvd","Washington St","Lake Rd")
        ),
        taxIdCountryCode = "US",
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun uk() = CountryProfile(
        countryCode = "GB", countryNameEn = "United Kingdom", nationality = "GBR",
        locale = "en-GB",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "9 digits", "\\d{9}", 9, 10, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "Surname+letters+digits (18 chars)", "[A-Z9]{5}\\d{6}[A-Z9]{2}\\d[A-Z]{2}", 18, 10, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Oliver","George","Harry","Jack","Noah","Charlie","Jacob","Alfie","Freddie","Oscar"),
            firstNamesFemale = listOf("Olivia","Amelia","Isla","Ava","Emily","Isabella","Mia","Poppy","Ella","Lily"),
            lastNames        = listOf("Smith","Jones","Williams","Taylor","Brown","Davies","Evans","Wilson","Thomas","Roberts")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "[A-Z]{1,2}\\d[A-Z\\d]? \\d[A-Z]{2}", postalCodeExample = "SW1A 1AA",
            regions = listOf("England","Scotland","Wales","Northern Ireland"),
            cities  = listOf("London","Manchester","Birmingham","Leeds","Glasgow","Liverpool","Edinburgh","Bristol"),
            streetNames = listOf("High Street","Church Road","Victoria Road","Park Lane","Station Road","The Green")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT),
        notes = "UK does not have a national ID card for citizens; passport is primary photo ID"
    )

    private fun germany() = CountryProfile(
        countryCode = "DE", countryNameEn = "Germany", nationality = "DEU",
        locale = "de-DE",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "C + 8 alphanum", "C[A-Z0-9]{8}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "L + 8 digits or 9 alphanum", "[A-Z0-9]{9}", 9, 10, true),
            DocumentTypeSpec(DocumentType.RESIDENCE_PERMIT, "9 alphanum", "[A-Z0-9]{9}", 9, 3, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Luca","Paul","Felix","Jonas","Leon","Elias","Finn","Noah","Luis","Lukas"),
            firstNamesFemale = listOf("Emma","Mia","Hannah","Sofia","Lea","Lena","Anna","Lara","Julia","Laura"),
            lastNames        = listOf("Müller","Schmidt","Schneider","Fischer","Weber","Meyer","Wagner","Becker","Schulz","Hoffmann")
        ),
        addressFormat = AddressFormat(
            streetFirst = true,
            postalCodeRegex = "\\d{5}", postalCodeExample = "10115",
            regions = listOf("Bayern","Nordrhein-Westfalen","Baden-Württemberg","Niedersachsen","Berlin","Hamburg","Sachsen"),
            cities  = listOf("Berlin","Hamburg","München","Köln","Frankfurt","Stuttgart","Düsseldorf","Leipzig"),
            streetNames = listOf("Hauptstraße","Bahnhofstraße","Kirchgasse","Schulstraße","Gartenstraße","Bergstraße")
        ),
        taxIdCountryCode = "DE",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID, DocumentType.RESIDENCE_PERMIT)
    )

    private fun france() = CountryProfile(
        countryCode = "FR", countryNameEn = "France", nationality = "FRA",
        locale = "fr-FR",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 digits + 7 alphanum", "\\d{2}[A-Z]{2}\\d{5}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "12 digits", "\\d{12}", 12, 10, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "12 alphanum", "[A-Z0-9]{12}", 12, 15, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Gabriel","Raphaël","Lucas","Hugo","Louis","Nathan","Tom","Théo","Mathis","Maxime"),
            firstNamesFemale = listOf("Emma","Jade","Louise","Manon","Camille","Chloé","Léa","Sarah","Inès","Zoé"),
            lastNames        = listOf("Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "75001",
            regions = listOf("Île-de-France","Provence-Alpes-Côte d'Azur","Occitanie","Auvergne-Rhône-Alpes","Bretagne"),
            cities  = listOf("Paris","Marseille","Lyon","Toulouse","Nice","Nantes","Strasbourg","Bordeaux"),
            streetNames = listOf("Rue de la Paix","Avenue Victor Hugo","Rue du Général de Gaulle","Boulevard Saint-Michel")
        ),
        taxIdCountryCode = "FR",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun spain() = CountryProfile(
        countryCode = "ES", countryNameEn = "Spain", nationality = "ESP",
        locale = "es-ES",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "3 letters + 6 digits", "[A-Z]{3}\\d{6}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "8 digits + letter (DNI)", "\\d{8}[A-Z]", 9, 10, true),
            DocumentTypeSpec(DocumentType.RESIDENCE_PERMIT, "X/Y/Z + 7 digits + letter (NIE)", "[XYZ]\\d{7}[A-Z]", 9, 2, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Alejandro","Daniel","Pablo","Carlos","Adrián","Miguel","Javier","David","Sergio","Jorge"),
            firstNamesFemale = listOf("Lucía","María","Paula","Laura","Marta","Ana","Sara","Carmen","Elena","Isabel"),
            lastNames        = listOf("García","Martínez","López","Sánchez","González","Rodríguez","Fernández","Pérez","Gómez","Díaz")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "28001",
            regions = listOf("Madrid","Cataluña","Andalucía","Comunidad Valenciana","País Vasco","Galicia"),
            cities  = listOf("Madrid","Barcelona","Valencia","Sevilla","Zaragoza","Málaga","Murcia","Palma"),
            streetNames = listOf("Calle Mayor","Avenida de España","Calle del Carmen","Paseo de la Castellana")
        ),
        taxIdCountryCode = "ES",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun italy() = CountryProfile(
        countryCode = "IT", countryNameEn = "Italy", nationality = "ITA",
        locale = "it-IT",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "AJ + 7 digits or CA + 5 digits + letter (CIE)", "[A-Z]{2}[0-9A-Z]{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Francesco","Alessandro","Andrea","Matteo","Lorenzo","Luca","Marco","Davide","Giovanni","Federico"),
            firstNamesFemale = listOf("Sofia","Giulia","Martina","Sara","Laura","Valentina","Federica","Elisa","Alessia","Chiara"),
            lastNames        = listOf("Rossi","Ferrari","Esposito","Bianchi","Romano","Colombo","Ricci","Marino","Greco","Bruno")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "00100",
            regions = listOf("Lombardia","Lazio","Campania","Sicilia","Veneto","Emilia-Romagna","Piemonte","Toscana"),
            cities  = listOf("Roma","Milano","Napoli","Torino","Palermo","Genova","Bologna","Firenze"),
            streetNames = listOf("Via Roma","Corso Italia","Via Garibaldi","Piazza della Repubblica","Via Nazionale")
        ),
        taxIdCountryCode = "IT",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun poland() = CountryProfile(
        countryCode = "PL", countryNameEn = "Poland", nationality = "POL",
        locale = "pl-PL",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "3 letters + 6 digits", "[A-Z]{3}\\d{6}", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Jakub","Jan","Mateusz","Kacper","Mikołaj","Szymon","Piotr","Adam","Łukasz","Tomasz"),
            firstNamesFemale = listOf("Julia","Zuzanna","Zofia","Maja","Aleksandra","Natalia","Wiktoria","Karolina","Martyna","Klaudia"),
            lastNames        = listOf("Nowak","Kowalski","Wiśniewski","Wójcik","Kowalczyk","Kamiński","Lewandowski","Zieliński","Szymański","Woźniak")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{2}-\\d{3}", postalCodeExample = "00-001",
            regions = listOf("Mazowieckie","Małopolskie","Śląskie","Wielkopolskie","Dolnośląskie","Łódź"),
            cities  = listOf("Warszawa","Kraków","Łódź","Wrocław","Poznań","Gdańsk","Szczecin","Bydgoszcz"),
            streetNames = listOf("ul. Główna","ul. Lipowa","ul. Szkolna","ul. Leśna","al. Niepodległości")
        ),
        taxIdCountryCode = "PL",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun brazil() = CountryProfile(
        countryCode = "BR", countryNameEn = "Brazil", nationality = "BRA",
        locale = "pt-BR",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 6 digits", "[A-Z]{2}\\d{6}", 8, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "RG: 7-9 digits + optional letter", "\\d{7,9}[0-9X]?", 9, 10, false),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "CNH: 11 digits", "\\d{11}", 11, 10, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Miguel","Arthur","Heitor","Bernardo","Samuel","João","Pedro","Lucas","Matheus","Gabriel"),
            firstNamesFemale = listOf("Alice","Sophia","Isabella","Manuela","Júlia","Heloísa","Luísa","Laura","Valentina","Maria"),
            lastNames        = listOf("Silva","Santos","Oliveira","Souza","Lima","Pereira","Carvalho","Ferreira","Rodrigues","Almeida")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}-\\d{3}", postalCodeExample = "01310-100",
            regions = listOf("São Paulo","Rio de Janeiro","Minas Gerais","Bahia","Paraná","Rio Grande do Sul"),
            cities  = listOf("São Paulo","Rio de Janeiro","Brasília","Salvador","Fortaleza","Belo Horizonte","Curitiba","Manaus"),
            streetNames = listOf("Rua das Flores","Avenida Brasil","Rua Sete de Setembro","Avenida Paulista","Rua do Comércio")
        ),
        taxIdCountryCode = "BR",
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun netherlands() = CountryProfile(
        countryCode = "NL", countryNameEn = "Netherlands", nationality = "NLD",
        locale = "nl-NL",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "1 letter + 8 digits", "[A-Z]\\d{8}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "4 letters + 4 digits + 1 letter", "[A-Z]{4}\\d{4}[A-Z]", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Liam","Noah","Lucas","Finn","Sem","Jesse","Julian","Dylan","Daan","Thomas"),
            firstNamesFemale = listOf("Emma","Olivia","Ava","Sophie","Mila","Noa","Lisa","Julia","Lotte","Anna"),
            lastNames        = listOf("de Jong","van den Berg","van Dijk","Bakker","Janssen","Visser","Smit","Meijer","de Boer","Mulder")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}\\s?[A-Z]{2}", postalCodeExample = "1011 AB",
            regions = listOf("Noord-Holland","Zuid-Holland","Utrecht","Noord-Brabant","Gelderland","Overijssel"),
            cities  = listOf("Amsterdam","Rotterdam","Den Haag","Utrecht","Eindhoven","Groningen","Tilburg","Almere"),
            streetNames = listOf("Kalverstraat","Herengracht","Keizersgracht","Leidsestraat","Nieuwendijk")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun sweden() = CountryProfile(
        countryCode = "SE", countryNameEn = "Sweden", nationality = "SWE",
        locale = "sv-SE",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "8 alphanum", "[A-Z]{2}\\d{6}", 8, 5, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "SIS ID: 8 digits", "\\d{8}", 8, 5, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("William","Oscar","Lucas","Liam","Noah","Hugo","Oliver","Elias","Erik","Emil"),
            firstNamesFemale = listOf("Alice","Maja","Elsa","Wilma","Ella","Ebba","Astrid","Linnea","Agnes","Elin"),
            lastNames        = listOf("Andersson","Johansson","Karlsson","Nilsson","Eriksson","Larsson","Olsson","Persson","Svensson","Gustafsson")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{3}\\s\\d{2}", postalCodeExample = "111 22",
            regions = listOf("Stockholm","Västra Götaland","Skåne","Östergötland","Jönköping"),
            cities  = listOf("Stockholm","Göteborg","Malmö","Uppsala","Västerås","Örebro","Linköping","Helsingborg"),
            streetNames = listOf("Storgatan","Kungsgatan","Drottninggatan","Vasagatan","Linnégatan")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun switzerland() = CountryProfile(
        countryCode = "CH", countryNameEn = "Switzerland", nationality = "CHE",
        locale = "de-CH",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "X + 7 digits", "X\\d{7}", 8, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "A + 8 digits", "A\\d{8}", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Luca","Noah","David","Jonas","Simon","Nico","Jan","Lukas","Benjamin","Fabian"),
            firstNamesFemale = listOf("Mia","Emma","Lea","Laura","Sara","Julia","Anna","Lena","Sophie","Lisa"),
            lastNames        = listOf("Müller","Meier","Schmid","Keller","Weber","Huber","Schneider","Meyer","Steiner","Fischer")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}", postalCodeExample = "8001",
            regions = listOf("Zürich","Bern","Basel-Stadt","Genf","St. Gallen","Aargau","Luzern"),
            cities  = listOf("Zürich","Genf","Basel","Bern","Lausanne","Winterthur","Luzern","St. Gallen"),
            streetNames = listOf("Bahnhofstrasse","Hauptgasse","Dorfstrasse","Kirchgasse","Seestrasse")
        ),
        taxIdCountryCode = "CH",
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun austria()    = simpleEuropean("AT","Austria","AUT","de-AT",
        listOf("Luca","Lukas","David","Tobias","Florian"),listOf("Sarah","Hannah","Laura","Lisa","Anna"),
        listOf("Gruber","Huber","Bauer","Wagner","Müller"),"\\d{4}","1010","AT")
    private fun belgium()    = simpleEuropean("BE","Belgium","BEL","nl-BE",
        listOf("Luca","Noah","Louis","Arthur","Alexis"),listOf("Emma","Marie","Lea","Julie","Alice"),
        listOf("Peeters","Janssen","Maes","Jacobs","Willems"),"\\d{4}","1000","BE")
    private fun portugal()   = simpleEuropean("PT","Portugal","PRT","pt-PT",
        listOf("João","Pedro","André","Miguel","Francisco"),listOf("Ana","Maria","Sofia","Beatriz","Inês"),
        listOf("Silva","Santos","Ferreira","Pereira","Oliveira"),"\\d{4}-\\d{3}","1000-001","PT")
    private fun czechia()    = simpleEuropean("CZ","Czech Republic","CZE","cs-CZ",
        listOf("Jakub","Jan","Tomáš","Marek","Lukáš"),listOf("Tereza","Eliška","Karolína","Lucie","Monika"),
        listOf("Novák","Svoboda","Novotný","Dvořák","Černý"),"\\d{3}\\s\\d{2}","110 00","CZ")
    private fun hungary()    = simpleEuropean("HU","Hungary","HUN","hu-HU",
        listOf("Bence","Máté","Dávid","Ádám","Péter"),listOf("Viktória","Zsófia","Réka","Anna","Eszter"),
        listOf("Nagy","Kovács","Tóth","Szabó","Horváth"),"\\d{4}","1011","HU")
    private fun romania()    = simpleEuropean("RO","Romania","ROU","ro-RO",
        listOf("Alexandru","Andrei","Mihai","Gabriel","Cristian"),listOf("Maria","Elena","Ana","Ioana","Andreea"),
        listOf("Popescu","Ionescu","Popa","Gheorghe","Stoica"),"\\d{6}","010011","RO")
    private fun bulgaria()   = simpleEuropean("BG","Bulgaria","BGR","bg-BG",
        listOf("Александър","Георги","Иван","Никола","Петър"),listOf("Мария","Ивана","Елена","Антония","Симона"),
        listOf("Иванов","Георгиев","Петров","Димитров","Тодоров"),"\\d{4}","1000","BG")
    private fun greece()     = simpleEuropean("GR","Greece","GRC","el-GR",
        listOf("Γιώργης","Νίκος","Κώστας","Δημήτρης","Παναγιώτης"),listOf("Μαρία","Ελένη","Κατερίνα","Αγγελική","Σοφία"),
        listOf("Παπαδόπουλος","Παπαδημητρίου","Νικολάου","Γεωργίου","Δημητρίου"),"\\d{5}","10431","GR")

    // ─── Middle East ──────────────────────────────────────────────────────

    private fun turkey() = CountryProfile(
        countryCode = "TR", countryNameEn = "Turkey", nationality = "TUR",
        locale = "tr-TR",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "11 digits (TC Kimlik)", "\\d{11}", 11, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Ahmet","Mehmet","Mustafa","Ali","Ibrahim","Hasan","Ömer","Murat","Emre","Burak"),
            firstNamesFemale = listOf("Fatma","Emine","Ayşe","Hatice","Zeynep","Elif","Merve","Selin","Büşra","Esra"),
            lastNames        = listOf("Yılmaz","Kaya","Demir","Şahin","Çelik","Yıldız","Yıldırım","Öztürk","Aydın","Arslan")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "34000",
            regions = listOf("İstanbul","Ankara","İzmir","Bursa","Antalya","Adana"),
            cities  = listOf("İstanbul","Ankara","İzmir","Bursa","Antalya","Konya","Adana","Gaziantep"),
            streetNames = listOf("Atatürk Caddesi","Cumhuriyet Bulvarı","İstiklal Caddesi","Bağdat Caddesi")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun israel() = CountryProfile(
        countryCode = "IL", countryNameEn = "Israel", nationality = "ISR",
        locale = "he-IL",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Teudat Zehut: 9 digits", "\\d{9}", 9, 10, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Noam","Ori","Lior","Yoav","Ido","Tal","Eitan","Amit","Nir","Guy"),
            firstNamesFemale = listOf("Noa","Maya","Tamar","Shira","Yael","Lior","Avigail","Gal","Roni","Dana"),
            lastNames        = listOf("Cohen","Levi","Mizrahi","Peretz","Biton","Dahan","Friedman","Shapiro","Goldberg","Klein"),
            script = NameScript.HEBREW
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{7}", postalCodeExample = "6100000",
            regions = listOf("Tel Aviv","Jerusalem","Haifa","Central","Southern","Northern"),
            cities  = listOf("Tel Aviv","Jerusalem","Haifa","Be'er Sheva","Holon","Bnei Brak","Petah Tikva","Netanya"),
            streetNames = listOf("Dizengoff St","Ben Yehuda St","Rothschild Blvd","King George St")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun uae() = CountryProfile(
        countryCode = "AE", countryNameEn = "United Arab Emirates", nationality = "ARE",
        locale = "ar-AE",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Emirates ID: 15 digits", "784-\\d{4}-\\d{7}-\\d", 15, 5, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Mohammed","Ahmed","Ali","Omar","Khalid","Abdullah","Sultan","Hamad","Saeed","Rashid"),
            firstNamesFemale = listOf("Fatima","Aisha","Mariam","Noura","Sara","Hessa","Moza","Latifa","Shaikha","Mahra"),
            lastNames        = listOf("Al Maktoum","Al Nahyan","Al Thani","Al Falasi","Al Mansouri","Al Hashemi","Al Kaabi","Al Mazrouei"),
            script = NameScript.ARABIC
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "00000",
            regions = listOf("Dubai","Abu Dhabi","Sharjah","Ajman","Ras Al Khaimah","Fujairah","Umm Al Quwain"),
            cities  = listOf("Dubai","Abu Dhabi","Sharjah","Al Ain","Ajman","Ras Al Khaimah"),
            streetNames = listOf("Sheikh Zayed Road","Corniche Road","Al Wasl Road","Airport Road")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun saudiArabia() = CountryProfile(
        countryCode = "SA", countryNameEn = "Saudi Arabia", nationality = "SAU",
        locale = "ar-SA",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "1 letter + 8 digits", "[A-Z]\\d{8}", 9, 5, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "10 digits (Hawiyya)", "\\d{10}", 10, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Abdullah","Mohammed","Ibrahim","Omar","Khalid","Ali","Fahad","Faisal","Turki","Sultan"),
            firstNamesFemale = listOf("Fatima","Aisha","Sara","Nora","Haya","Maha","Reem","Lina","Deema","Abeer"),
            lastNames        = listOf("Al-Ghamdi","Al-Zahrani","Al-Qahtani","Al-Otaibi","Al-Harbi","Al-Shehri","Al-Dosari","Al-Rashidi"),
            script = NameScript.ARABIC
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{5}", postalCodeExample = "11564",
            regions = listOf("Riyadh","Makkah","Eastern Province","Madinah","Asir","Qassim"),
            cities  = listOf("Riyadh","Jeddah","Mecca","Medina","Dammam","Taif","Tabuk","Buraidah"),
            streetNames = listOf("King Fahd Road","Olaya Street","Prince Sultan Road","King Abdullah Road")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    // ─── Asia-Pacific ─────────────────────────────────────────────────────

    private fun india() = CountryProfile(
        countryCode = "IN", countryNameEn = "India", nationality = "IND",
        locale = "en-IN",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "1 letter + 7 digits", "[A-Z]\\d{7}", 8, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Aadhaar: 12 digits", "\\d{4}\\s\\d{4}\\s\\d{4}", 12, 0, false),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "State code + 2 digits + year + 7 digits", "[A-Z]{2}\\d{2}\\d{4}\\d{7}", 15, 20, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Aarav","Vihaan","Arjun","Sai","Arnav","Aditya","Vivaan","Dhruv","Ananya","Rohan"),
            firstNamesFemale = listOf("Aadhya","Aanya","Ananya","Pari","Anushka","Sia","Priya","Divya","Sneha","Pooja"),
            lastNames        = listOf("Sharma","Singh","Verma","Patel","Kumar","Gupta","Joshi","Rao","Nair","Reddy"),
            script = NameScript.DEVANAGARI
        ),
        addressFormat = AddressFormat(
            streetFirst = true,
            postalCodeRegex = "\\d{6}", postalCodeExample = "110001",
            regions = listOf("Maharashtra","Delhi","Karnataka","Tamil Nadu","Gujarat","Uttar Pradesh","Rajasthan","West Bengal"),
            cities  = listOf("Mumbai","Delhi","Bengaluru","Hyderabad","Ahmedabad","Chennai","Kolkata","Pune","Jaipur"),
            streetNames = listOf("MG Road","Gandhi Nagar","Nehru Street","Rajaji Road","Anna Salai")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun china() = CountryProfile(
        countryCode = "CN", countryNameEn = "China", nationality = "CHN",
        locale = "zh-CN",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "E + 8 digits", "E\\d{8}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Resident ID: 18 digits", "\\d{17}[0-9X]", 18, 0, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("伟","强","磊","军","勇","超","杰","涛","明","刚"),
            firstNamesFemale = listOf("芳","娜","秀英","华","英","玲","凤","雪","燕","静"),
            lastNames        = listOf("王","李","张","刘","陈","杨","赵","黄","周","吴"),
            script = NameScript.CHINESE
        ),
        addressFormat = AddressFormat(
            streetFirst = false,
            postalCodeRegex = "\\d{6}", postalCodeExample = "100000",
            regions = listOf("北京","上海","广东","江苏","浙江","四川","湖北","湖南"),
            cities  = listOf("北京","上海","广州","深圳","成都","杭州","武汉","西安","南京","天津"),
            streetNames = listOf("中关村大街","长安街","建国门大街","朝阳路","人民路")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun japan() = CountryProfile(
        countryCode = "JP", countryNameEn = "Japan", nationality = "JPN",
        locale = "ja-JP",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "My Number: 12 digits", "\\d{12}", 12, 0, false),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "12 digits", "\\d{12}", 12, 3, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Haruto","Yuto","Sota","Yuki","Hayato","Haruki","Ryusei","Kento","Shota","Daiki"),
            firstNamesFemale = listOf("Himari","Yui","Hina","Koharu","Yuna","Mio","Sakura","Ichika","Aoi","Miyu"),
            lastNames        = listOf("佐藤","鈴木","高橋","田中","伊藤","渡辺","山本","中村","小林","加藤")
        ),
        addressFormat = AddressFormat(
            streetFirst = false,
            postalCodeRegex = "\\d{3}-\\d{4}", postalCodeExample = "100-0001",
            regions = listOf("東京都","大阪府","神奈川県","愛知県","埼玉県","千葉県","兵庫県","北海道"),
            cities  = listOf("東京","大阪","名古屋","札幌","福岡","神戸","京都","川崎"),
            streetNames = listOf("銀座通り","新宿区","渋谷区","池袋","丸の内")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun southKorea() = simpleAsia("KR","South Korea","KOR","ko-KR",
        listOf("민준","서준","도윤","예준","시우"),listOf("서연","서윤","지우","채원","수아"),
        listOf("김","이","박","최","정","강","조","윤","장","임"),
        "\\d{5}","04524")

    private fun singapore()  = simpleAsia("SG","Singapore","SGP","en-SG",
        listOf("Liam","Noah","Ethan","Ryan","Joshua"),listOf("Sophia","Emma","Olivia","Chloe","Isabelle"),
        listOf("Tan","Lee","Ng","Lim","Wong","Goh","Ong","Ho","Teo","Low"),
        "\\d{6}","018989")

    private fun australia()  = CountryProfile(
        countryCode = "AU", countryNameEn = "Australia", nationality = "AUS",
        locale = "en-AU",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "State-specific (6-10 alphanum)", "[A-Z0-9]{6,10}", 9, 3, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Oliver","William","Jack","Noah","Thomas","James","Liam","Ethan","Lucas","Mason"),
            firstNamesFemale = listOf("Olivia","Charlotte","Ava","Mia","Amelia","Grace","Isla","Chloe","Ruby","Sophie"),
            lastNames        = listOf("Smith","Jones","Williams","Brown","Wilson","Taylor","Johnson","White","Martin","Anderson")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}", postalCodeExample = "2000",
            regions = listOf("New South Wales","Victoria","Queensland","Western Australia","South Australia","Tasmania"),
            cities  = listOf("Sydney","Melbourne","Brisbane","Perth","Adelaide","Gold Coast","Canberra","Newcastle"),
            streetNames = listOf("George Street","King Street","Elizabeth Street","Pitt Street","Market Street")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun newZealand() = simpleAsia("NZ","New Zealand","NZL","en-NZ",
        listOf("Oliver","William","Jack","Noah","Lucas"),listOf("Olivia","Isla","Charlotte","Ava","Aria"),
        listOf("Smith","Jones","Williams","Taylor","Brown"),
        "\\d{4}","1010")

    private fun canada() = CountryProfile(
        countryCode = "CA", countryNameEn = "Canada", nationality = "CAN",
        locale = "en-CA",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 6 digits", "[A-Z]{2}\\d{6}", 8, 5, true),
            DocumentTypeSpec(DocumentType.DRIVERS_LICENSE, "Province-specific (9-15 chars)", "[A-Z0-9]{9,15}", 12, 5, false)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Liam","Noah","William","Oliver","James","Ethan","Lucas","Mason","Logan","Jackson"),
            firstNamesFemale = listOf("Olivia","Emma","Ava","Sophia","Isabella","Mia","Charlotte","Amelia","Harper","Evelyn"),
            lastNames        = listOf("Smith","Brown","Tremblay","Martin","Roy","Wilson","MacDonald","Taylor","Campbell","Anderson")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "[A-Z]\\d[A-Z]\\s\\d[A-Z]\\d", postalCodeExample = "M5V 2T6",
            regions = listOf("Ontario","Quebec","British Columbia","Alberta","Manitoba","Saskatchewan","Nova Scotia"),
            cities  = listOf("Toronto","Montreal","Vancouver","Calgary","Edmonton","Ottawa","Winnipeg","Quebec City"),
            streetNames = listOf("Main Street","King Street","Queen Street","Yonge Street","Bay Street","Bloor Street")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    // ─── Latin America ────────────────────────────────────────────────────

    private fun mexico() = simpleLatam("MX","Mexico","MEX","es-MX",
        listOf("Santiago","Mateo","Sebastián","Diego","Alejandro"),listOf("Valentina","Sofía","Regina","Camila","Valeria"),
        listOf("García","Martínez","López","González","Hernández","Rodríguez","Pérez","Sánchez"),
        "\\d{5}","06600")

    private fun argentina() = simpleLatam("AR","Argentina","ARG","es-AR",
        listOf("Luca","Mateo","Santiago","Lucas","Benjamín"),listOf("Valentina","Sofía","Emma","Martina","Lucía"),
        listOf("González","Rodríguez","Gómez","Fernández","López","Díaz","Martínez","Pérez"),
        "\\d{4}","C1001")

    private fun colombia() = simpleLatam("CO","Colombia","COL","es-CO",
        listOf("Santiago","Sebastián","Mateo","Miguel","Samuel"),listOf("Valentina","Sofía","Isabella","Camila","Valeria"),
        listOf("González","Rodríguez","García","Martínez","Hernández"),
        "\\d{6}","110111")

    private fun chile() = simpleLatam("CL","Chile","CHL","es-CL",
        listOf("Mateo","Benjamín","Emilio","Sebastián","Lucas"),listOf("Sofía","Emilia","Agustina","Valentina","Florencia"),
        listOf("Muñoz","González","Rojas","Díaz","Pérez","Soto","Contreras","Silva"),
        "\\d{7}","8320000")

    // ─── Africa ───────────────────────────────────────────────────────────

    private fun southAfrica() = CountryProfile(
        countryCode = "ZA", countryNameEn = "South Africa", nationality = "ZAF",
        locale = "en-ZA",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "A + 8 digits", "A\\d{8}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "13 digits (ID Book/Smart ID)", "\\d{13}", 13, 0, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Liam","Noah","Ethan","Oliver","James","Thabo","Sipho","Lungelo","Bandile","Sanele"),
            firstNamesFemale = listOf("Olivia","Emma","Nomvula","Zanele","Thandi","Nompumelelo","Ayanda","Lungile","Faith","Grace"),
            lastNames        = listOf("Dlamini","Ndlovu","Khumalo","Ntuli","Nkosi","Zulu","Mthembu","Mahlangu","Molefe","Sithole")
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}", postalCodeExample = "2000",
            regions = listOf("Gauteng","Western Cape","KwaZulu-Natal","Eastern Cape","Limpopo","Mpumalanga"),
            cities  = listOf("Johannesburg","Cape Town","Durban","Pretoria","Port Elizabeth","Bloemfontein","Soweto"),
            streetNames = listOf("Main Road","Church Street","Voortrekker Road","Commissioner Street")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun nigeria()    = simpleAfrica("NG","Nigeria","NGA","en-NG",
        listOf("Chukwuemeka","Babatunde","Adewale","Oluwaseun","Emeka"),listOf("Ngozi","Chidinma","Adaeze","Amaka","Blessing"),
        listOf("Okonkwo","Adesanya","Adeyemi","Okafor","Ibrahim","Musa","Suleiman"),
        "\\d{6}","100001")

    private fun egypt()      = simpleAfrica("EG","Egypt","EGY","ar-EG",
        listOf("Mohamed","Ahmed","Mahmoud","Omar","Ali"),listOf("Fatma","Nour","Sara","Hana","Mariam"),
        listOf("Hassan","Hussein","Ibrahim","Mostafa","Abdelrahman"),
        "\\d{5}","11511")

    // ─── Central & South Asia ────────────────────────────────────────────

    private fun indonesia()  = simpleAsia("ID","Indonesia","IDN","id-ID",
        listOf("Muhammad","Rizki","Dimas","Aldi","Bagas"),listOf("Siti","Dewi","Putri","Rizka","Nurul"),
        listOf("Susanto","Wijaya","Santoso","Kusuma","Hidayat","Setiawan","Hartono","Pranoto"),
        "\\d{5}","10110")

    private fun thailand()   = simpleAsia("TH","Thailand","THA","th-TH",
        listOf("Somchai","Narong","Piti","Chai","Wanchai"),listOf("Malee","Prani","Saowaluk","Nisa","Porn"),
        listOf("Srimuang","Jaidee","Wongsawat","Rojanasiri","Boonma"),
        "\\d{5}","10100")

    private fun vietnam()    = simpleAsia("VN","Vietnam","VNM","vi-VN",
        listOf("Minh","Hùng","Đức","Tuấn","Thành"),listOf("Lan","Hương","Hoa","Thảo","Linh"),
        listOf("Nguyễn","Trần","Lê","Phạm","Hoàng","Phan","Vũ","Đặng"),
        "\\d{6}","100000")

    private fun malaysia()   = simpleAsia("MY","Malaysia","MYS","ms-MY",
        listOf("Muhammad","Ahmad","Amirul","Haziq","Hafiz"),listOf("Nur","Siti","Aisyah","Farah","Nadia"),
        listOf("bin Abdullah","binti Ahmad","bin Ismail","binti Hassan","bin Rahman"),
        "\\d{5}","50000")

    private fun philippines()= simpleAsia("PH","Philippines","PHL","en-PH",
        listOf("Juan","Jose","Miguel","Carlos","Angelo"),listOf("Maria","Ana","Rosa","Luz","Elena"),
        listOf("Santos","Reyes","Cruz","Bautista","Ocampo","Garcia","Torres","Castillo"),
        "\\d{4}","1000")

    private fun pakistan()   = simpleAsia("PK","Pakistan","PAK","ur-PK",
        listOf("Muhammad","Ahmed","Ali","Omar","Usman"),listOf("Fatima","Ayesha","Zainab","Sana","Hira"),
        listOf("Khan","Malik","Chaudhry","Butt","Sheikh","Ahmed","Ali","Hassan"),
        "\\d{5}","75500")

    private fun bangladesh() = simpleAsia("BD","Bangladesh","BGD","bn-BD",
        listOf("Mohammad","Md.","Rahim","Karim","Hasan"),listOf("Fatema","Nasrin","Riya","Mim","Tania"),
        listOf("Rahman","Islam","Ahmed","Ali","Khan","Hossain","Chowdhury","Begum"),
        "\\d{4}","1000")

    // ─── Caucasus ─────────────────────────────────────────────────────────

    private fun georgia() = CountryProfile(
        countryCode = "GE", countryNameEn = "Georgia", nationality = "GEO",
        locale = "ka-GE",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "9 digits", "\\d{9}", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("გიორგი","ლუკა","დავით","ნიკა","ალეკო","სანდრო","ლაშა","გიო","ბაჩო","ვახო"),
            firstNamesFemale = listOf("მარიამი","ნინო","ანა","ელენე","სოფო","ნიკა","ლელა","მაკა","ნატო","ქეთი"),
            lastNames        = listOf("ბერიძე","კვარაცხელია","ჩიქოვანი","გიგაური","მელაძე","ჯავახიშვილი","ხვედელიძე"),
            script = NameScript.GEORGIAN
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}", postalCodeExample = "0105",
            regions = listOf("თბილისი","კახეთი","შიდა ქართლი","იმერეთი","სამეგრელო","გურია"),
            cities  = listOf("თბილისი","ბათუმი","ქუთაისი","რუსთავი","ზუგდიდი","გორი"),
            streetNames = listOf("რუსთაველის გამზირი","ვარაზისხევი","ჩარდინის ქ.","ერეკლე II-ის ქ.")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun armenia() = CountryProfile(
        countryCode = "AM", countryNameEn = "Armenia", nationality = "ARM",
        locale = "hy-AM",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "9 digits", "\\d{9}", 9, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Արամ","Ժiр","Ваhan","David","Narek","Arman","Sargis","Tigran","Hakob","Hovhannes"),
            firstNamesFemale = listOf("Ani","Marine","Arevik","Nare","Lilit","Anna","Mariam","Lusine","Kristine","Hasmik"),
            lastNames        = listOf("Grigoryan","Harutyunyan","Petrosyan","Hovhannisyan","Sargsyan","Mkrtchyan","Hakobyan","Karapetyan"),
            script = NameScript.ARMENIAN
        ),
        addressFormat = AddressFormat(
            postalCodeRegex = "\\d{4}", postalCodeExample = "0001",
            regions = listOf("Yerevan","Aragatsotn","Ararat","Armavir","Gegharkunik","Lori"),
            cities  = listOf("Yerevan","Gyumri","Vanadzor","Vagharshapat","Abovyan","Kapan"),
            streetNames = listOf("Mashtots Ave","Baghramyan Ave","Tigranyan Street","Sayat-Nova Ave")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun kazakhstan() = CountryProfile(
        countryCode = "KZ", countryNameEn = "Kazakhstan", nationality = "KAZ",
        locale = "kk-KZ",
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "N + 8 digits", "N\\d{8}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "12 digits (ИИН)", "\\d{12}", 12, 10, true)
        ),
        nameDataset = NameDataset(
            firstNamesMale   = listOf("Нурлан","Арман","Бауыржан","Даулет","Ерлан","Асан","Серік","Мұрат","Жандос","Азамат"),
            firstNamesFemale = listOf("Айгерим","Назерке","Дана","Жансая","Арайлым","Айсұлу","Гүлнар","Зарина","Камила","Меруерт"),
            lastNames        = listOf("Қасымов","Ахметов","Жақсыбеков","Нұрмағамбетов","Сейтқали","Бекқали","Омаров"),
            script = NameScript.CYRILLIC
        ),
        addressFormat = AddressFormat(
            streetFirst = false,
            postalCodeRegex = "\\d{6}", postalCodeExample = "010000",
            regions = listOf("Алматы","Нұр-Сұлтан","Шымкент","Ақтөбе","Қарағанды","Тараз"),
            cities  = listOf("Алматы","Нұр-Сұлтан","Шымкент","Ақтөбе","Қарағанды","Атырау"),
            streetNames = listOf("Абай даңғылы","Республика даңғылы","Достык даңғылы","Назарбаев даңғылы")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    // ─── Helper builders ──────────────────────────────────────────────────

    private fun simpleEuropean(
        cc: String, name: String, nat: String, locale: String,
        male: List<String>, female: List<String>, last: List<String>,
        pcRegex: String, pcExample: String, taxCc: String? = null
    ) = CountryProfile(
        countryCode = cc, countryNameEn = name, nationality = nat, locale = locale,
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "9 alphanum", "[A-Z0-9]{9}", 9, 10, true)
        ),
        nameDataset = NameDataset(male, female, last),
        addressFormat = AddressFormat(
            postalCodeRegex = pcRegex, postalCodeExample = pcExample,
            regions = listOf("Region A","Region B","Region C"),
            cities  = listOf("Capital","Second City","Third City"),
            streetNames = listOf("Main Street","Central Avenue","Park Road","Station Road")
        ),
        taxIdCountryCode = taxCc,
        typicalNfcDocs = listOf(DocumentType.PASSPORT, DocumentType.NATIONAL_ID)
    )

    private fun simpleAsia(
        cc: String, name: String, nat: String, locale: String,
        male: List<String>, female: List<String>, last: List<String>,
        pcRegex: String, pcExample: String
    ) = CountryProfile(
        countryCode = cc, countryNameEn = name, nationality = nat, locale = locale,
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true)
        ),
        nameDataset = NameDataset(male, female, last),
        addressFormat = AddressFormat(
            postalCodeRegex = pcRegex, postalCodeExample = pcExample,
            regions = listOf("Region 1","Region 2","Region 3"),
            cities  = listOf("Capital","Second City","Third City"),
            streetNames = listOf("Main Road","Central Street","Park Avenue")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun simpleLatam(
        cc: String, name: String, nat: String, locale: String,
        male: List<String>, female: List<String>, last: List<String>,
        pcRegex: String, pcExample: String
    ) = CountryProfile(
        countryCode = cc, countryNameEn = name, nationality = nat, locale = locale,
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true),
            DocumentTypeSpec(DocumentType.NATIONAL_ID, "Country-specific", "[A-Z0-9]{8,12}", 10, 10, false)
        ),
        nameDataset = NameDataset(male, female, last),
        addressFormat = AddressFormat(
            postalCodeRegex = pcRegex, postalCodeExample = pcExample,
            regions = listOf("Región 1","Región 2","Región 3"),
            cities  = listOf("Capital","Segunda ciudad","Tercera ciudad"),
            streetNames = listOf("Calle Principal","Avenida Central","Paseo del Parque")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )

    private fun simpleAfrica(
        cc: String, name: String, nat: String, locale: String,
        male: List<String>, female: List<String>, last: List<String>,
        pcRegex: String, pcExample: String
    ) = CountryProfile(
        countryCode = cc, countryNameEn = name, nationality = nat, locale = locale,
        documentTypes = listOf(
            DocumentTypeSpec(DocumentType.PASSPORT, "2 letters + 7 digits", "[A-Z]{2}\\d{7}", 9, 10, true)
        ),
        nameDataset = NameDataset(male, female, last),
        addressFormat = AddressFormat(
            postalCodeRegex = pcRegex, postalCodeExample = pcExample,
            regions = listOf("Region A","Region B","Region C"),
            cities  = listOf("Capital","Second City","Third City"),
            streetNames = listOf("Main Street","Market Road","Victoria Avenue")
        ),
        taxIdCountryCode = null,
        typicalNfcDocs = listOf(DocumentType.PASSPORT)
    )
}
