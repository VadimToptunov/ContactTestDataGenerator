package com.vadimtoptunov.generators.crypto

import org.junit.Assert.*
import org.junit.Test

class CryptoAddressGeneratorTest {

    @Test
    fun `Bitcoin legacy starts with 1`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.BITCOIN_LEGACY)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Bitcoin legacy should start with '1'", record.address.startsWith("1"))
            assertTrue(
                "Bitcoin legacy should be 25-34 chars",
                record.address.length in 25..34
            )
        }
    }

    @Test
    fun `Bitcoin segwit starts with bc1q`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.BITCOIN_SEGWIT)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Bitcoin SegWit should start with 'bc1q'", record.address.startsWith("bc1q"))
            assertEquals("Bitcoin SegWit should be 42 chars", 42, record.address.length)
            // Bech32 is lowercase only
            assertEquals(
                "Bech32 should be lowercase",
                record.address.lowercase(),
                record.address
            )
        }
    }

    @Test
    fun `Bitcoin taproot starts with bc1p`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.BITCOIN_TAPROOT)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Bitcoin Taproot should start with 'bc1p'", record.address.startsWith("bc1p"))
            assertEquals("Bitcoin Taproot should be 62 chars", 62, record.address.length)
        }
    }

    @Test
    fun `Ethereum is 42 chars with 0x prefix`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.ETHEREUM)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Ethereum should start with '0x'", record.address.startsWith("0x"))
            assertEquals("Ethereum should be 42 chars", 42, record.address.length)
            assertTrue("Ethereum should be checksummed", record.isChecksummed)
            // Body should be hex only
            assertTrue(
                "Ethereum body should be hex",
                record.address.drop(2).all { it in "0123456789abcdefABCDEF" }
            )
        }
    }

    @Test
    fun `Tron starts with T`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.TRON)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Tron should start with 'T'", record.address.startsWith("T"))
            assertEquals("Tron should be 34 chars", 34, record.address.length)
        }
    }

    @Test
    fun `Litecoin legacy starts with L or M`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.LITECOIN_LEGACY)

        repeat(50) {
            val record = generator.generate()

            assertTrue(
                "Litecoin legacy should start with 'L' or 'M'",
                record.address.startsWith("L") || record.address.startsWith("M")
            )
            assertEquals("Litecoin should be 34 chars", 34, record.address.length)
        }
    }

    @Test
    fun `Litecoin segwit starts with ltc1q`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.LITECOIN_SEGWIT)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Litecoin SegWit should start with 'ltc1q'", record.address.startsWith("ltc1q"))
        }
    }

    @Test
    fun `Dogecoin starts with D`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.DOGECOIN)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Dogecoin should start with 'D'", record.address.startsWith("D"))
            assertEquals("Dogecoin should be 34 chars", 34, record.address.length)
        }
    }

    @Test
    fun `Ripple starts with r`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.RIPPLE)

        repeat(50) {
            val record = generator.generate()

            assertTrue("Ripple should start with 'r'", record.address.startsWith("r"))
            assertTrue(
                "Ripple should be 25-35 chars",
                record.address.length in 25..35
            )
        }
    }

    @Test
    fun `Solana has no prefix`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.SOLANA)

        repeat(50) {
            val record = generator.generate()

            assertTrue(
                "Solana should be 32-44 chars",
                record.address.length in 32..44
            )
            // Should be Base58
            assertTrue(
                "Solana should be Base58",
                record.address.all { it in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" }
            )
        }
    }

    @Test
    fun `all addresses use valid character sets`() {
        CryptoNetwork.entries.forEach { network ->
            val generator = CryptoAddressGenerator(network)

            repeat(10) {
                val record = generator.generate()
                val address = record.address

                when (network) {
                    CryptoNetwork.BITCOIN_LEGACY,
                    CryptoNetwork.TRON,
                    CryptoNetwork.LITECOIN_LEGACY,
                    CryptoNetwork.DOGECOIN,
                    CryptoNetwork.RIPPLE,
                    CryptoNetwork.SOLANA -> {
                        // Base58
                        assertTrue(
                            "$network should not contain 0, O, I, l",
                            address.none { it in "0OIl" }
                        )
                    }
                    CryptoNetwork.BITCOIN_SEGWIT,
                    CryptoNetwork.BITCOIN_TAPROOT,
                    CryptoNetwork.LITECOIN_SEGWIT -> {
                        // Bech32 (after prefix)
                        val body = address.drop(4)  // Skip prefix
                        assertTrue(
                            "$network Bech32 should be lowercase",
                            body == body.lowercase()
                        )
                    }
                    CryptoNetwork.ETHEREUM -> {
                        // Hex
                        assertTrue(
                            "$network should be hex after 0x",
                            address.drop(2).all { it.lowercaseChar() in "0123456789abcdef" }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `seed produces reproducible addresses`() {
        val seed = 12345L

        CryptoNetwork.entries.forEach { network ->
            val address1 = CryptoAddressGenerator(network, seed).generate().address
            val address2 = CryptoAddressGenerator(network, seed).generate().address

            assertEquals("$network: Same seed should produce same address", address1, address2)
        }
    }

    @Test
    fun `different seeds produce different addresses`() {
        CryptoNetwork.entries.forEach { network ->
            val address1 = CryptoAddressGenerator(network, 111L).generate().address
            val address2 = CryptoAddressGenerator(network, 222L).generate().address

            assertNotEquals("$network: Different seeds should produce different addresses", address1, address2)
        }
    }

    @Test
    fun `URI format is correct`() {
        val btcRecord = CryptoAddressGenerator(CryptoNetwork.BITCOIN_LEGACY).generate()
        val ethRecord = CryptoAddressGenerator(CryptoNetwork.ETHEREUM).generate()
        val tronRecord = CryptoAddressGenerator(CryptoNetwork.TRON).generate()

        assertTrue("BTC URI should start with 'bitcoin:'", btcRecord.uri.startsWith("bitcoin:"))
        assertTrue("ETH URI should start with 'ethereum:'", ethRecord.uri.startsWith("ethereum:"))
        assertTrue("TRX URI should start with 'tron:'", tronRecord.uri.startsWith("tron:"))

        assertTrue("URI should contain address", btcRecord.uri.contains(btcRecord.address))
    }

    @Test
    fun `prefixed format includes symbol`() {
        CryptoNetwork.entries.forEach { network ->
            val record = CryptoAddressGenerator(network).generate()

            assertTrue(
                "$network prefixed should contain symbol",
                record.prefixed.startsWith("${network.symbol}:")
            )
        }
    }

    @Test
    fun `shortened format truncates long addresses`() {
        val record = CryptoAddressGenerator(CryptoNetwork.ETHEREUM).generate()

        assertTrue("Shortened should be shorter", record.shortened.length < record.address.length)
        assertTrue("Shortened should contain ellipsis", record.shortened.contains("..."))
    }

    @Test
    fun `type description is populated`() {
        CryptoNetwork.entries.forEach { network ->
            val record = CryptoAddressGenerator(network).generate()

            assertTrue(
                "$network should have type description",
                record.typeDescription.isNotEmpty()
            )
        }
    }

    @Test
    fun `token compatibility for ERC-20 and TRC-20`() {
        val ethRecord = CryptoAddressGenerator(CryptoNetwork.ETHEREUM).generate()
        val tronRecord = CryptoAddressGenerator(CryptoNetwork.TRON).generate()
        val btcRecord = CryptoAddressGenerator(CryptoNetwork.BITCOIN_LEGACY).generate()

        assertNotNull("ETH should have token compatibility", ethRecord.tokenCompatibility)
        assertNotNull("TRON should have token compatibility", tronRecord.tokenCompatibility)
        assertNull("BTC should not have token compatibility", btcRecord.tokenCompatibility)

        assertTrue(
            "ETH tokens should mention USDT",
            ethRecord.tokenCompatibility!!.contains("USDT")
        )
    }

    @Test
    fun `serialization formats work`() {
        val generator = CryptoAddressGenerator(CryptoNetwork.ETHEREUM)
        val record = generator.generate()

        val csv = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.CSV)
        val json = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.JSON)
        val txt = generator.serialize(record, com.vadimtoptunov.generators.core.OutputFormat.TXT)

        assertTrue("CSV should contain address", csv.contains(record.address))
        assertTrue("JSON should contain address", json.contains(record.address))
        assertTrue("JSON should be valid format", json.startsWith("{") && json.endsWith("}"))
        assertTrue("TXT should contain symbol", txt.contains("ETH:"))
    }
}
