package com.vadimtoptunov.generators.core

import org.junit.Assert.*
import org.junit.Test

class SeededRandomTest {

    @Test
    fun `same seed produces same sequence`() {
        val seed = 42L
        val random1 = SeededRandom(seed)
        val random2 = SeededRandom(seed)

        // Generate sequence from first instance
        val seq1 = (1..10).map { random1.nextInt(100) }

        // Generate sequence from second instance
        val seq2 = (1..10).map { random2.nextInt(100) }

        assertEquals("Same seed should produce identical sequence", seq1, seq2)
    }

    @Test
    fun `different seeds produce different sequences`() {
        val random1 = SeededRandom(42L)
        val random2 = SeededRandom(43L)

        val seq1 = (1..10).map { random1.nextInt(100) }
        val seq2 = (1..10).map { random2.nextInt(100) }

        assertNotEquals("Different seeds should produce different sequences", seq1, seq2)
    }

    @Test
    fun `null seed uses Random Default`() {
        val random = SeededRandom(null)

        // Should not throw and produce valid values
        val values = (1..100).map { random.nextInt(100) }

        assertTrue("All values should be in range [0, 100)", values.all { it in 0 until 100 })
        // With 100 random values in range 0-99, we expect some variety
        assertTrue("Random should produce variety", values.distinct().size > 10)
    }

    @Test
    fun `nextInt with range produces values in range`() {
        val random = SeededRandom(123L)

        val values = (1..1000).map { random.nextInt(10, 20) }

        assertTrue("All values should be >= 10", values.all { it >= 10 })
        assertTrue("All values should be < 20", values.all { it < 20 })
    }

    @Test
    fun `nextBoolean produces both values`() {
        val random = SeededRandom(456L)

        val values = (1..100).map { random.nextBoolean() }

        assertTrue("Should produce some true values", values.any { it })
        assertTrue("Should produce some false values", values.any { !it })
    }

    @Test
    fun `nextBytes produces correct size`() {
        val random = SeededRandom(789L)

        val bytes = random.nextBytes(32)

        assertEquals("Should produce 32 bytes", 32, bytes.size)
    }

    @Test
    fun `randomElement on list works`() {
        val random = SeededRandom(111L)
        val list = listOf("a", "b", "c", "d", "e")

        val selected = with(random) { (1..100).map { list.randomElement() } }

        assertTrue("All selected should be in original list", selected.all { it in list })
        // With 100 samples from 5 elements, we should see variety
        assertTrue("Should select multiple distinct elements", selected.distinct().size >= 3)
    }

    @Test
    fun `randomElement on empty list throws`() {
        val random = SeededRandom(222L)
        val emptyList = emptyList<String>()

        assertThrows(NoSuchElementException::class.java) {
            with(random) { emptyList.randomElement() }
        }
    }

    @Test
    fun `randomOrNull on empty list returns null`() {
        val random = SeededRandom(333L)
        val emptyList = emptyList<String>()

        val result = with(random) { emptyList.randomOrNull() }

        assertNull("Should return null for empty list", result)
    }

    @Test
    fun `randomOrNull on non-empty list returns element`() {
        val random = SeededRandom(444L)
        val list = listOf("x", "y", "z")

        val result = with(random) { list.randomOrNull() }

        assertNotNull("Should return an element", result)
        assertTrue("Result should be in list", result in list)
    }

    @Test
    fun `IntRange randomElement produces values in range`() {
        val random = SeededRandom(555L)

        val values = with(random) { (1..100).map { (5..15).randomElement() } }

        assertTrue("All values should be >= 5", values.all { it >= 5 })
        assertTrue("All values should be <= 15", values.all { it <= 15 })
    }

    @Test
    fun `shuffled produces same result with same seed`() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        val shuffled1 = with(SeededRandom(666L)) { list.shuffled() }
        val shuffled2 = with(SeededRandom(666L)) { list.shuffled() }

        assertEquals("Same seed should produce same shuffle", shuffled1, shuffled2)
    }

    @Test
    fun `String randomElement selects character`() {
        val random = SeededRandom(777L)
        val str = "ABCDE"

        val selected = with(random) { (1..50).map { str.randomElement() } }

        assertTrue("All selected should be in string", selected.all { it in str })
    }

    @Test
    fun `toKotlinRandom returns usable Random`() {
        val random = SeededRandom(888L)
        val kotlinRandom = random.toKotlinRandom()

        // Should be usable with stdlib functions
        val value = kotlinRandom.nextInt(100)
        assertTrue("Should produce valid value", value in 0 until 100)
    }
}
