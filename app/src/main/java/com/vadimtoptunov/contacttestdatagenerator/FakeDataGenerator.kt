package com.vadimtoptunov.contacttestdatagenerator

import kotlin.random.Random

object FakeDataGenerator {
    
    private data class PhoneFormat(
        val countryCode: String,
        val digitGroups: List<Int>,
        val separator: String = "-"
    )
    
    private val phoneFormats = listOf(
        // North America
        PhoneFormat("+1", listOf(3, 3, 4)),           // USA, Canada
        
        // Europe
        PhoneFormat("+44", listOf(4, 3, 4)),          // UK
        PhoneFormat("+49", listOf(3, 4, 4)),          // Germany
        PhoneFormat("+33", listOf(1, 2, 2, 2, 2)),    // France
        PhoneFormat("+34", listOf(3, 3, 3)),          // Spain
        PhoneFormat("+39", listOf(3, 3, 4)),          // Italy
        PhoneFormat("+48", listOf(3, 3, 3)),          // Poland
        PhoneFormat("+31", listOf(2, 3, 4)),          // Netherlands
        PhoneFormat("+46", listOf(2, 3, 4)),          // Sweden
        PhoneFormat("+41", listOf(2, 3, 4)),          // Switzerland
        
        // Eastern Europe & CIS
        PhoneFormat("+7", listOf(3, 3, 2, 2)),        // Russia, Kazakhstan
        PhoneFormat("+380", listOf(2, 3, 2, 2)),      // Ukraine
        PhoneFormat("+375", listOf(2, 3, 2, 2)),      // Belarus
        
        // Asia
        PhoneFormat("+86", listOf(3, 4, 4)),          // China
        PhoneFormat("+81", listOf(2, 4, 4)),          // Japan
        PhoneFormat("+82", listOf(2, 4, 4)),          // South Korea
        PhoneFormat("+91", listOf(5, 5), " "),        // India
        PhoneFormat("+65", listOf(4, 4)),             // Singapore
        PhoneFormat("+852", listOf(4, 4)),            // Hong Kong
        PhoneFormat("+62", listOf(3, 4, 4)),          // Indonesia
        PhoneFormat("+66", listOf(2, 3, 4)),          // Thailand
        PhoneFormat("+84", listOf(2, 4, 4)),          // Vietnam
        
        // Middle East
        PhoneFormat("+971", listOf(2, 3, 4)),         // UAE
        PhoneFormat("+966", listOf(2, 3, 4)),         // Saudi Arabia
        PhoneFormat("+972", listOf(2, 3, 4)),         // Israel
        PhoneFormat("+90", listOf(3, 3, 4)),          // Turkey
        
        // Latin America
        PhoneFormat("+55", listOf(2, 5, 4)),          // Brazil
        PhoneFormat("+52", listOf(3, 3, 4)),          // Mexico
        PhoneFormat("+54", listOf(2, 4, 4)),          // Argentina
        PhoneFormat("+56", listOf(1, 4, 4)),          // Chile
        PhoneFormat("+57", listOf(3, 3, 4)),          // Colombia
        
        // Oceania
        PhoneFormat("+61", listOf(3, 3, 3)),          // Australia
        PhoneFormat("+64", listOf(2, 3, 4)),          // New Zealand
        
        // Africa
        PhoneFormat("+27", listOf(2, 3, 4)),          // South Africa
        PhoneFormat("+20", listOf(3, 3, 4)),          // Egypt
        PhoneFormat("+234", listOf(3, 3, 4))          // Nigeria
    )
    
    private val firstNames = listOf(
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Christopher", "Karen", "Daniel", "Nancy", "Matthew", "Lisa",
        "Anthony", "Betty", "Mark", "Margaret", "Donald", "Sandra", "Steven", "Ashley",
        "Paul", "Kimberly", "Andrew", "Emily", "Joshua", "Donna", "Kenneth", "Michelle",
        "Kevin", "Carol", "Brian", "Amanda", "George", "Dorothy", "Timothy", "Melissa",
        "Ronald", "Deborah", "Edward", "Stephanie", "Jason", "Rebecca", "Jeffrey", "Sharon",
        "Ryan", "Laura", "Jacob", "Cynthia", "Gary", "Kathleen", "Nicholas", "Amy",
        "Eric", "Angela", "Jonathan", "Shirley", "Stephen", "Anna", "Larry", "Brenda"
    )
    
    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
        "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
        "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
        "Carter", "Roberts", "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker",
        "Cruz", "Edwards", "Collins", "Reyes", "Stewart", "Morris", "Morales", "Murphy"
    )
    
    private val companies = listOf(
        "TechCorp", "Innovate Solutions", "Global Systems", "Digital Dynamics", "Quantum Industries",
        "NextGen Technologies", "Fusion Enterprises", "Vertex Corp", "Axiom Systems", "Pinnacle Group",
        "Catalyst Inc", "Horizon Technologies", "Stellar Solutions", "Momentum Corp", "Velocity Systems",
        "Nexus Enterprises", "Summit Industries", "Apex Corporation", "Prime Technologies", "Zenith Group",
        "Vanguard Systems", "Odyssey Corp", "Atlas Technologies", "Frontier Solutions", "Titan Industries",
        "Phoenix Enterprises", "Spectrum Corp", "Infinity Systems", "Meridian Group", "Eclipse Technologies"
    )
    
    private val jobTitles = listOf(
        "Software Engineer", "Product Manager", "Data Analyst", "UX Designer", "Marketing Manager",
        "Sales Representative", "Project Manager", "Business Analyst", "DevOps Engineer", "HR Manager",
        "Financial Analyst", "Customer Success Manager", "Operations Manager", "Quality Assurance Engineer", "Content Writer",
        "Accountant", "Legal Counsel", "Systems Administrator", "Network Engineer", "Database Administrator",
        "Frontend Developer", "Backend Developer", "Full Stack Developer", "Mobile Developer", "Security Analyst",
        "Research Scientist", "Technical Writer", "Business Development Manager", "Product Designer", "Solutions Architect"
    )
    
    private val emailDomains = listOf(
        "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com",
        "protonmail.com", "mail.com", "aol.com", "zoho.com", "fastmail.com"
    )
    
    fun generateFullName(): String {
        return "${firstNames.random()} ${lastNames.random()}"
    }
    
    fun generatePhoneNumber(): String {
        val format = phoneFormats.random()
        val numberParts = format.digitGroups.map { digitCount ->
            generateDigitGroup(digitCount)
        }
        return "${format.countryCode}${format.separator}${numberParts.joinToString(format.separator)}"
    }
    
    private fun generateDigitGroup(length: Int): String {
        val min = if (length == 1) 1 else (10.0.pow(length - 1).toInt())
        val max = (10.0.pow(length).toInt()) - 1
        return Random.nextInt(min, max + 1).toString().padStart(length, '0')
    }
    
    private fun Double.pow(n: Int): Double {
        var result = 1.0
        repeat(n) { result *= this }
        return result
    }
    
    fun generateEmail(name: String): String {
        val cleanName = name.replace(" ", ".").lowercase()
        val domain = emailDomains.random()
        return "$cleanName@$domain"
    }
    
    fun generateCompany(): String {
        return companies.random()
    }
    
    fun generateJobTitle(): String {
        return jobTitles.random()
    }
}

