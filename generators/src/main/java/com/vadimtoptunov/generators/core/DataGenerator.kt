package com.vadimtoptunov.generators.core

/**
 * Core abstraction for all data generators in the platform.
 *
 * Every tool in the DevData Factory implements this interface,
 * making generators composable, swappable, and independently testable.
 *
 * @param T The type of a single generated record (e.g. ContactRecord, CardRecord, IbanRecord)
 */
interface DataGenerator<T> {

    /** Human-readable name of this generator (shown in the tool picker UI). */
    val name: String

    /** Short description of what this generator produces. */
    val description: String

    /** The output formats this generator supports. */
    val supportedFormats: List<OutputFormat>

    /**
     * Generate a single record.
     * Must be pure (no side effects, no I/O) and thread-safe.
     */
    fun generate(): T

    /**
     * Generate [count] records.
     * Default implementation calls [generate] in a loop;
     * override for bulk-optimized generators.
     */
    fun generateBatch(count: Int): List<T> = List(count) { generate() }

    /**
     * Serialize a single record to the given [format].
     * @throws UnsupportedOperationException if [format] is not in [supportedFormats]
     */
    fun serialize(record: T, format: OutputFormat): String

    /**
     * Serialize a list of records to the given [format].
     * Default wraps [serialize] calls; override for formats with headers/footers (CSV, SQL).
     */
    fun serializeBatch(records: List<T>, format: OutputFormat): String =
        records.joinToString("\n") { serialize(it, format) }
}
