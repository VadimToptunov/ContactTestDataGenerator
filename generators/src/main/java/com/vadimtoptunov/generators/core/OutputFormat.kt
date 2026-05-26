package com.vadimtoptunov.generators.core

/**
 * Supported output formats across all generators in the platform.
 *
 * Each format has a [mimeType] and a [extension] used when saving files.
 */
sealed class OutputFormat(
    val label: String,
    val mimeType: String,
    val extension: String
) {
    /** vCard 3.0 — native format for contacts */
    object VCF : OutputFormat("vCard (.vcf)", "text/x-vcard", "vcf")

    /** Comma-separated values — universal for spreadsheets and DB import */
    object CSV : OutputFormat("CSV (.csv)", "text/csv", "csv")

    /** JSON array — for REST API mocking and Postman collections */
    object JSON : OutputFormat("JSON (.json)", "application/json", "json")

    /** SQL INSERT statements — direct DB seeding */
    object SQL : OutputFormat("SQL (.sql)", "application/sql", "sql")

    /** Plain text — one record per line, for simple tools */
    object TXT : OutputFormat("Plain text (.txt)", "text/plain", "txt")

    companion object {
        val all: List<OutputFormat> = listOf(VCF, CSV, JSON, SQL, TXT)
    }
}
