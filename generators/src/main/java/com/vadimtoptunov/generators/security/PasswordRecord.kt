package com.vadimtoptunov.generators.security

import kotlinx.serialization.Serializable

/**
 * Strength category based on estimated entropy bits.
 *
 * Categories follow common security guidelines:
 * - VERY_WEAK: < 28 bits — trivially crackable
 * - WEAK: 28-35 bits — vulnerable to targeted attacks
 * - REASONABLE: 36-59 bits — adequate for low-value accounts
 * - STRONG: 60-127 bits — good for most purposes
 * - VERY_STRONG: 128+ bits — cryptographic strength
 */
@Serializable
enum class PasswordStrength(val label: String) {
    VERY_WEAK("Very Weak"),
    WEAK("Weak"),
    REASONABLE("Reasonable"),
    STRONG("Strong"),
    VERY_STRONG("Very Strong")
}

/**
 * A generated password with its associated metadata.
 *
 * @property password The generated password string
 * @property policy The policy used to generate this password
 * @property entropy Estimated bits of entropy (log2(charsetSize^length))
 */
@Serializable
data class PasswordRecord(
    val password: String,
    val policy: PasswordPolicy,
    val entropy: Double
) {
    /** Length of the generated password. */
    val length: Int get() = password.length

    /**
     * Masked version for display: shows first 2 and last 2 chars.
     * Example: "P@ss****rd"
     */
    val masked: String
        get() = when {
            password.length <= 4 -> "*".repeat(password.length)
            password.length <= 6 -> "${password.take(1)}${"*".repeat(password.length - 2)}${password.takeLast(1)}"
            else -> "${password.take(2)}${"*".repeat(password.length - 4)}${password.takeLast(2)}"
        }

    /**
     * Strength category based on entropy.
     */
    val strength: PasswordStrength
        get() = when {
            entropy < 28 -> PasswordStrength.VERY_WEAK
            entropy < 36 -> PasswordStrength.WEAK
            entropy < 60 -> PasswordStrength.REASONABLE
            entropy < 128 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }

    /** Human-readable entropy display. */
    val entropyFormatted: String
        get() = "%.1f bits".format(entropy)

    /** Check if password contains uppercase letters. */
    val hasUppercase: Boolean get() = password.any { it.isUpperCase() }

    /** Check if password contains lowercase letters. */
    val hasLowercase: Boolean get() = password.any { it.isLowerCase() }

    /** Check if password contains digits. */
    val hasDigits: Boolean get() = password.any { it.isDigit() }

    /** Check if password contains special characters. */
    val hasSpecial: Boolean get() = password.any { !it.isLetterOrDigit() }
}
