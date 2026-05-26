package com.vadimtoptunov.generators.finance

/**
 * Pure Luhn algorithm implementation.
 *
 * Used by card number generators (Visa, Mastercard, Amex, etc.)
 * and any other tool that needs ISO/IEC 7812 check-digit logic.
 *
 * Relevant for:
 *   - QA testers seeding payment forms
 *   - Bug bounty hunters testing input validation bypasses
 *   - Developers building card-processing integrations
 */
object LuhnAlgorithm {

    /**
     * Compute the Luhn check digit for a number given as a string of digits
     * (without the check digit itself).
     *
     * Example: "79927398710" → check digit is 3, full number "79927398713"
     */
    fun checkDigit(digits: String): Int {
        require(digits.all { it.isDigit() }) { "Input must contain only digits" }
        val sum = digits
            .reversed()
            .mapIndexed { index, c ->
                var d = c.digitToInt()
                if (index % 2 == 0) {   // every second digit from the right (0-based)
                    d *= 2
                    if (d > 9) d -= 9
                }
                d
            }
            .sum()
        return (10 - (sum % 10)) % 10
    }

    /**
     * Validate a full card number string (including check digit).
     *
     * @return true if the number passes the Luhn check.
     */
    fun validate(number: String): Boolean {
        if (number.length < 2 || !number.all { it.isDigit() }) return false
        val withoutCheck = number.dropLast(1)
        return checkDigit(withoutCheck) == number.last().digitToInt()
    }

    /**
     * Append the correct Luhn check digit to [prefix] and return the full number.
     */
    fun complete(prefix: String): String = prefix + checkDigit(prefix)
}
