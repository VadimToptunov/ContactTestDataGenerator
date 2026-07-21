package com.vadimtoptunov.generators.security

import kotlinx.serialization.Serializable

/**
 * Defines complexity rules for password generation.
 *
 * Presets:
 * - [WEAK]: 6-8 chars, no special chars — for legacy systems
 * - [STANDARD]: 12-16 chars, all character classes — balanced security
 * - [STRONG]: 16-24 chars, all character classes — high security
 * - [PASSPHRASE]: 20-32 chars, no special — for memorable but long passwords
 */
@Serializable
data class PasswordPolicy(
    val minLength: Int = 12,
    val maxLength: Int = 16,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigits: Boolean = true,
    val requireSpecial: Boolean = true,
    val excludeAmbiguous: Boolean = false,  // Exclude O/0, l/1/I
    val specialChars: String = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
) {
    init {
        require(minLength >= 4) { "Minimum length must be at least 4" }
        require(maxLength >= minLength) { "Max length must be >= min length" }
        require(maxLength <= 128) { "Maximum length cannot exceed 128" }
    }

    /** Returns the charset size for entropy calculation. */
    val charsetSize: Int
        get() {
            var size = 0
            if (requireUppercase) size += if (excludeAmbiguous) 25 else 26  // -O, -I
            if (requireLowercase) size += if (excludeAmbiguous) 25 else 26  // -l
            if (requireDigits) size += if (excludeAmbiguous) 8 else 10       // -0, -1
            if (requireSpecial) size += specialChars.length
            return size.coerceAtLeast(1)
        }

    companion object {
        /** Legacy systems, minimal requirements. */
        val WEAK = PasswordPolicy(
            minLength = 6,
            maxLength = 8,
            requireSpecial = false
        )

        /** Standard security for most applications. */
        val STANDARD = PasswordPolicy()

        /** High security for sensitive systems. */
        val STRONG = PasswordPolicy(
            minLength = 16,
            maxLength = 24
        )

        /** Long, memorable passwords without special chars. */
        val PASSPHRASE = PasswordPolicy(
            minLength = 20,
            maxLength = 32,
            requireSpecial = false
        )

        /** PIN-style: digits only. */
        val PIN_4 = PasswordPolicy(
            minLength = 4,
            maxLength = 4,
            requireUppercase = false,
            requireLowercase = false,
            requireDigits = true,
            requireSpecial = false
        )

        /** PIN-style: 6 digits. */
        val PIN_6 = PasswordPolicy(
            minLength = 6,
            maxLength = 6,
            requireUppercase = false,
            requireLowercase = false,
            requireDigits = true,
            requireSpecial = false
        )
    }
}
