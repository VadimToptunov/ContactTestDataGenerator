package com.vadimtoptunov.contacttestdatagenerator.devtools

import com.vadimtoptunov.generators.GeneratorCatalog
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat

/**
 * Groups the raw generators from [GeneratorRegistry] into user-facing categories,
 * so the Developer Tools picker can show them as tidy, labelled sections.
 *
 * Category membership is decided purely from each generator's registration id
 * prefix, which keeps this UI layer decoupled from the concrete generator classes.
 */
enum class DevToolCategory(
    val displayName: String,
    val emoji: String,
    val requiresPremium: Boolean,
    private val idPrefixes: List<String>,
) {
    FINANCE("Finance", "💳", requiresPremium = true, idPrefixes = listOf("card_generator", "iban_generator")),
    IDENTITY("Identity", "🪪", requiresPremium = true, idPrefixes = listOf("taxid_")),
    NETWORK("Network", "🌐", requiresPremium = false, idPrefixes = listOf("ipv4_generator", "ipv6_generator", "mac_generator")),
    WEB("Web & Tokens", "🔑", requiresPremium = false, idPrefixes = listOf("uuid_generator", "jwt_generator")),
    CRYPTO("Crypto Addresses", "₿", requiresPremium = false, idPrefixes = listOf("crypto_address_generator")),
    SECURITY("Passwords", "🔒", requiresPremium = false, idPrefixes = listOf("password_generator")),
    LOCATION("Addresses", "📍", requiresPremium = false, idPrefixes = listOf("address_generator")),
    BARCODE("Barcodes", "📦", requiresPremium = false, idPrefixes = listOf("barcode_generator")),
    OTHER("Other Tools", "🧰", requiresPremium = false, idPrefixes = emptyList());

    fun matchesGeneratorId(generatorId: String): Boolean =
        idPrefixes.any { prefix -> generatorId.startsWith(prefix) }

    companion object {
        /** Find the category a generator id belongs to, falling back to [OTHER]. */
        fun forGeneratorId(generatorId: String): DevToolCategory =
            entries.firstOrNull { it != OTHER && it.matchesGeneratorId(generatorId) } ?: OTHER
    }
}

/** A single generator as presented in the Developer Tools UI. */
data class DevTool(
    val id: String,
    val displayName: String,
    val description: String,
    val supportedFormats: List<OutputFormat>,
    val category: DevToolCategory,
)

/**
 * Builds the categorized list of tools shown in the picker.
 *
 * Ensures all generators are registered first, then converts every registry
 * entry into a [DevTool], drops duplicate display names within a category
 * (some generators register several id variants that look identical to the
 * user), and returns them grouped and ordered by [DevToolCategory].
 */
object DevToolsCatalog {

    fun toolsByCategory(): Map<DevToolCategory, List<DevTool>> {
        GeneratorCatalog.registerAllGenerators()

        val seenNamesPerCategory = mutableMapOf<DevToolCategory, MutableSet<String>>()
        val tools = mutableListOf<DevTool>()

        for ((generatorId, generator) in GeneratorRegistry.entries) {
            val category = DevToolCategory.forGeneratorId(generatorId)
            val alreadySeen = seenNamesPerCategory.getOrPut(category) { mutableSetOf() }
            if (!alreadySeen.add(generator.name)) continue // skip duplicate display name

            tools += DevTool(
                id = generatorId,
                displayName = generator.name,
                description = generator.description,
                supportedFormats = generator.supportedFormats,
                category = category,
            )
        }

        return tools
            .groupBy { it.category }
            .toSortedMap(compareBy { it.ordinal })
    }
}
