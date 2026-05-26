package com.vadimtoptunov.contacttestdatagenerator.core

/**
 * Central registry of all available generators.
 *
 * New tools are added here — the UI discovers them automatically.
 * This is the "factory" part of the DevData Factory architecture.
 *
 * Usage:
 *   GeneratorRegistry.all          → list for the tool picker
 *   GeneratorRegistry.find("card") → get generator by id
 */
object GeneratorRegistry {

    private val registry = mutableMapOf<String, DataGenerator<*>>()

    /** Register a generator. Typically called from each tool's companion object. */
    fun register(id: String, generator: DataGenerator<*>) {
        registry[id] = generator
    }

    /** All registered generators, in registration order. */
    val all: List<DataGenerator<*>>
        get() = registry.values.toList()

    /** Find a generator by its [id], or null if not found. */
    fun find(id: String): DataGenerator<*>? = registry[id]

    /** Remove all registrations (useful in tests). */
    fun clear() = registry.clear()
}
