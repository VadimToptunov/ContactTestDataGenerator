package com.vadimtoptunov.contacttestdatagenerator.generators.identity

/**
 * Pure check-digit algorithms for Tax ID numbers across countries.
 *
 * Each function returns the expected check digit(s) given the base digits,
 * or validates a full number. No side effects, fully testable.
 *
 * Sources:
 *   - Brazil CPF: Receita Federal algorithm (two Mod-11 passes)
 *   - Germany IdNr: ISO 7064 Mod-11,10
 *   - Russia INN: Federal Tax Service weights
 *   - Ukraine РНОКПП: State Tax Administration algorithm
 *   - Spain NIF/NIE: MOD-23 letter table
 *   - USA SSN: format rules only (no mathematical check digit)
 *   - UK UTR: Mod-11 with specific weights
 *   - Poland NIP/PESEL: Mod-11 with published weights
 *   - Italy CF: Agenzia delle Entrate character table
 *   - France NIR (Numéro de Sécurité Sociale): Mod-97 complement
 */
object TaxIdAlgorithms {

    // ─── Brazil CPF ───────────────────────────────────────────────────────────
    // 11 digits: 9 base + 2 check digits
    // Each check digit: sum(digit[i] * weight[i]) mod 11 → if < 2 then 0 else 11 - remainder

    fun cpfCheckDigits(base9: String): String {
        require(base9.length == 9 && base9.all { it.isDigit() })
        val d1 = cpfDigit(base9, weights = (10 downTo 2).toList())
        val d2 = cpfDigit(base9 + d1, weights = (11 downTo 2).toList())
        return "$d1$d2"
    }

    private fun cpfDigit(digits: String, weights: List<Int>): String {
        val sum = digits.mapIndexed { i, c -> c.digitToInt() * weights[i] }.sum()
        val rem = sum % 11
        return (if (rem < 2) 0 else 11 - rem).toString()
    }

    fun cpfValid(cpf: String): Boolean {
        val d = cpf.filter { it.isDigit() }
        if (d.length != 11) return false
        if (d.all { it == d[0] }) return false   // 000...0 and 111...1 etc. are invalid
        val expected = cpfCheckDigits(d.take(9))
        return d.takeLast(2) == expected
    }

    // ─── Germany Steuerliche Identifikationsnummer (IdNr) ────────────────────
    // 11 digits. ISO 7064 Mod-11,10.
    // First digit 1–9. Exactly one digit appears 2–3 times, rest appear 0 or 1 time.

    fun idnrCheckDigit(base10: String): String {
        require(base10.length == 10 && base10.all { it.isDigit() })
        var product = 10
        for (digit in base10) {
            var sum = (digit.digitToInt() + product) % 10
            if (sum == 0) sum = 10
            product = (sum * 2) % 11
        }
        val check = (11 - product) % 10
        return check.toString()
    }

    fun idnrValid(idnr: String): Boolean {
        val d = idnr.filter { it.isDigit() }
        if (d.length != 11) return false
        if (d[0] == '0') return false
        return idnrCheckDigit(d.take(10)) == d.last().toString()
    }

    // ─── Russia INN (10-digit legal / 12-digit personal) ────────────────────
    // 12-digit personal INN: 2 check digits
    // weights_11 = [7,2,4,10,3,5,9,4,6,8] → check digit 11
    // weights_12 = [3,7,2,4,10,3,5,9,4,6,8] → check digit 12

    private val INN_W11 = intArrayOf(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val INN_W12 = intArrayOf(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)

    fun innPersonalCheckDigits(base10: String): String {
        require(base10.length == 10 && base10.all { it.isDigit() })
        val c11 = innDigit(base10, INN_W11)
        val c12 = innDigit(base10 + c11, INN_W12)
        return "$c11$c12"
    }

    private fun innDigit(digits: String, weights: IntArray): String {
        val sum = digits.mapIndexed { i, c -> c.digitToInt() * weights[i] }.sum()
        return (sum % 11 % 10).toString()
    }

    fun innPersonalValid(inn: String): Boolean {
        val d = inn.filter { it.isDigit() }
        if (d.length != 12) return false
        return innPersonalCheckDigits(d.take(10)) == d.takeLast(2)
    }

    // ─── Ukraine РНОКПП (Реєстраційний номер облікової картки) ──────────────
    // 10 digits. weights = [-1,5,7,9,4,6,10,5,7] → sum mod 11 mod 10

    private val UA_WEIGHTS = intArrayOf(-1, 5, 7, 9, 4, 6, 10, 5, 7)

    fun rnokppCheckDigit(base9: String): String {
        require(base9.length == 9 && base9.all { it.isDigit() })
        val sum = base9.mapIndexed { i, c -> c.digitToInt() * UA_WEIGHTS[i] }.sum()
        return (sum % 11 % 10).toString()
    }

    fun rnokppValid(id: String): Boolean {
        val d = id.filter { it.isDigit() }
        if (d.length != 10) return false
        return rnokppCheckDigit(d.take(9)) == d.last().toString()
    }

    // ─── Spain NIF (DNI) ─────────────────────────────────────────────────────
    // 8 digits + 1 letter. Letter = NIF_LETTERS[number % 23]

    private const val NIF_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE"

    fun nifLetter(digits8: String): Char {
        require(digits8.length == 8 && digits8.all { it.isDigit() })
        return NIF_LETTERS[digits8.toInt() % 23]
    }

    fun nifValid(nif: String): Boolean {
        val clean = nif.trim().uppercase()
        if (clean.length != 9) return false
        val digits = clean.take(8)
        val letter = clean.last()
        if (!digits.all { it.isDigit() }) return false
        return nifLetter(digits) == letter
    }

    // ─── Italy Codice Fiscale ─────────────────────────────────────────────────
    // 16 chars: 6 letters (name) + 2 digits (year) + 1 letter (month) +
    //           2 digits (day+sex) + 4 chars (municipality code) + 1 check letter
    // Check: alternating odd/even position values summed mod 26

    private val CF_ODD = mapOf(
        '0' to 1, '1' to 0, '2' to 5, '3' to 7, '4' to 9, '5' to 13,
        '6' to 15, '7' to 17, '8' to 19, '9' to 21,
        'A' to 1, 'B' to 0, 'C' to 5, 'D' to 7, 'E' to 9, 'F' to 13,
        'G' to 15, 'H' to 17, 'I' to 19, 'J' to 21, 'K' to 2, 'L' to 4,
        'M' to 18, 'N' to 20, 'O' to 11, 'P' to 3, 'Q' to 6, 'R' to 8,
        'S' to 12, 'T' to 14, 'U' to 16, 'V' to 10, 'W' to 22, 'X' to 25,
        'Y' to 24, 'Z' to 23
    )
    private val CF_EVEN = ('A'..'Z').mapIndexed { i, c -> c to i }.toMap() +
        ('0'..'9').mapIndexed { i, c -> c to i }.toMap()

    fun codiceFiscaleCheckChar(base15: String): Char {
        require(base15.length == 15)
        val upper = base15.uppercase()
        val sum = upper.mapIndexed { i, c ->
            if ((i + 1) % 2 != 0) CF_ODD[c] ?: 0 else CF_EVEN[c] ?: 0
        }.sum()
        return ('A' + (sum % 26))
    }

    fun codiceFiscaleValid(cf: String): Boolean {
        val clean = cf.trim().uppercase()
        if (clean.length != 16) return false
        return codiceFiscaleCheckChar(clean.take(15)) == clean.last()
    }

    // ─── France NIR (Numéro de Sécurité Sociale) ─────────────────────────────
    // 15 digits: 13 base + 2 check digits = 97 - (base13 mod 97)

    fun nirCheckDigits(base13: String): String {
        require(base13.length == 13 && base13.all { it.isDigit() })
        val rem = base13.toLong() % 97
        val check = 97 - rem
        return check.toString().padStart(2, '0')
    }

    fun nirValid(nir: String): Boolean {
        val d = nir.filter { it.isDigit() }
        if (d.length != 15) return false
        return nirCheckDigits(d.take(13)) == d.takeLast(2)
    }

    // ─── Poland NIP ──────────────────────────────────────────────────────────
    // 10 digits. weights = [6,5,7,2,3,4,5,6,7]. sum mod 11 must equal last digit.

    private val NIP_WEIGHTS = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)

    fun nipCheckDigit(base9: String): String {
        require(base9.length == 9 && base9.all { it.isDigit() })
        val sum = base9.mapIndexed { i, c -> c.digitToInt() * NIP_WEIGHTS[i] }.sum()
        val rem = sum % 11
        return if (rem == 10) "" else rem.toString()   // "" signals invalid (can't construct)
    }

    fun nipValid(nip: String): Boolean {
        val d = nip.filter { it.isDigit() }
        if (d.length != 10) return false
        val check = nipCheckDigit(d.take(9))
        return check.isNotEmpty() && check == d.last().toString()
    }

    // ─── UK UTR (Unique Taxpayer Reference) ──────────────────────────────────
    // 10 digits. weights = [2,1,2,1,2,1,2,1,2] on first 9 digits.
    // Each product: if > 9 subtract 9. Sum mod 10. Check = (10 - sum%10) % 10.
    // Note: HMRC does not officially publish the algorithm;
    // this is the widely-accepted reverse-engineered version.

    fun utrCheckDigit(base9: String): String {
        require(base9.length == 9 && base9.all { it.isDigit() })
        val weights = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
        val sum = base9.mapIndexed { i, c ->
            var p = c.digitToInt() * weights[i]
            if (p > 9) p -= 9
            p
        }.sum()
        return ((10 - sum % 10) % 10).toString()
    }

    // ─── USA SSN ──────────────────────────────────────────────────────────────
    // No mathematical check digit. Format: AAA-BB-CCCC
    // Invalid if: area = 000 or 666 or 900-999; group = 00; serial = 0000
    // Also invalid: 123-45-6789 (test number), 219-09-9999, 078-05-1120

    private val SSN_BLACKLIST = setOf("123456789", "219099999", "078051120")

    fun ssnValid(ssn: String): Boolean {
        val d = ssn.filter { it.isDigit() }
        if (d.length != 9) return false
        val area = d.take(3).toInt()
        val group = d.substring(3, 5).toInt()
        val serial = d.takeLast(4).toInt()
        if (area == 0 || area == 666 || area >= 900) return false
        if (group == 0) return false
        if (serial == 0) return false
        return d !in SSN_BLACKLIST
    }
}
