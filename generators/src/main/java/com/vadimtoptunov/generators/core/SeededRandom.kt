package com.vadimtoptunov.generators.core

import kotlin.random.Random

/**
 * Wrapper for Random that supports optional seeding for reproducible generation.
 *
 * When [seed] is provided, all random operations will produce the same sequence
 * when re-instantiated with the same seed — enabling reproducible test data.
 *
 * Usage:
 * ```
 * val random = SeededRandom(seed = 42L)
 * val value = random.nextInt(100)  // Always same for seed=42
 *
 * // Or without seed (uses Random.Default):
 * val random = SeededRandom()
 * ```
 */
class SeededRandom(seed: Long? = null) {

    private val random: Random = seed?.let { Random(it) } ?: Random.Default

    /** Returns a random Int in [0, until). */
    fun nextInt(until: Int): Int = random.nextInt(until)

    /** Returns a random Int in [from, until). */
    fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    /** Returns a random Long. */
    fun nextLong(): Long = random.nextLong()

    /** Returns a random Long in [0, until). */
    fun nextLong(until: Long): Long = random.nextLong(until)

    /** Returns a random Long in [from, until). */
    fun nextLong(from: Long, until: Long): Long = random.nextLong(from, until)

    /** Returns a random Double in [0.0, 1.0). */
    fun nextDouble(): Double = random.nextDouble()

    /** Returns a random Boolean. */
    fun nextBoolean(): Boolean = random.nextBoolean()

    /** Returns a ByteArray filled with random bytes. */
    fun nextBytes(size: Int): ByteArray = random.nextBytes(size)

    /**
     * Returns a random element from this list.
     * @throws NoSuchElementException if list is empty
     */
    fun <T> List<T>.randomElement(): T {
        if (isEmpty()) throw NoSuchElementException("List is empty")
        return this[nextInt(size)]
    }

    /**
     * Returns a random element from this array.
     * @throws NoSuchElementException if array is empty
     */
    fun <T> Array<T>.randomElement(): T {
        if (isEmpty()) throw NoSuchElementException("Array is empty")
        return this[nextInt(size)]
    }

    /**
     * Returns a random element from this CharArray.
     * @throws NoSuchElementException if array is empty
     */
    fun CharArray.randomElement(): Char {
        if (isEmpty()) throw NoSuchElementException("CharArray is empty")
        return this[nextInt(size)]
    }

    /**
     * Returns a random element from this String.
     * @throws NoSuchElementException if string is empty
     */
    fun String.randomElement(): Char {
        if (isEmpty()) throw NoSuchElementException("String is empty")
        return this[nextInt(length)]
    }

    /**
     * Returns a random element from this IntRange.
     */
    fun IntRange.randomElement(): Int = random.nextInt(first, last + 1)

    /**
     * Returns a random element from this CharRange.
     */
    fun CharRange.randomElement(): Char {
        val size = last - first + 1
        return (first + nextInt(size))
    }

    /**
     * Returns a random element from this list, or null if empty.
     */
    fun <T> List<T>.randomOrNull(): T? = if (isEmpty()) null else this[nextInt(size)]

    /**
     * Shuffles the list using this random source.
     */
    fun <T> List<T>.shuffled(): List<T> = shuffled(random)

    /**
     * Returns the underlying Kotlin Random instance.
     * Useful for interop with stdlib functions that accept Random.
     */
    fun toKotlinRandom(): Random = random
}
