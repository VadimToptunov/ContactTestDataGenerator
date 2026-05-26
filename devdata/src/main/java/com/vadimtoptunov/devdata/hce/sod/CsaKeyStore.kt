package com.vadimtoptunov.devdata.hce.sod

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * Manages a test-only CSCA (Country Signing CA) and Document Signer (DS)
 * key pair stored in the Android Keystore.
 *
 * ═══════════════════════════════════════════════════════════════════
 * IMPORTANT — TEST / DEVELOPMENT USE ONLY
 *
 * These are self-generated RSA keys that have NO relationship to any
 * government CA. They are used exclusively to make Passive Authentication
 * pass inside a controlled test environment where the Regula SDK is
 * configured to trust our test CSCA certificate.
 *
 * The private keys never leave the Android Keystore (hardware-backed
 * where available). They cannot be used to forge real travel documents.
 * ═══════════════════════════════════════════════════════════════════
 *
 * Chain:
 *   CSCA (self-signed, CA:true, ~10 year validity)
 *     └── DS  (signed by CSCA, CA:false, ~2 year validity)
 *           └── SOD SignedData (per-document, signed by DS)
 *
 * Regula SDK integration:
 *   val csca = CsaKeyStore.csaCertificate()
 *   DocumentReader.instance().initializeReader(context, config) {
 *       it.functionality.databasePath = "..."
 *   }
 *   // Register as trusted PKD cert:
 *   DocumentReader.instance().addPKDCertificates(listOf(
 *       PKDCertificate(csca.encoded, PKDCertificate.CERT_TYPE_CSCA, null)
 *   ))
 */
object CsaKeyStore {

    private const val TAG = "CsaKeyStore"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    // Alias names inside Android Keystore
    private const val ALIAS_CSCA = "devdata_test_csca"
    private const val ALIAS_DS   = "devdata_test_ds"

    // Certificate DN — clearly marked as test infrastructure
    private const val CSCA_DN = "CN=DevData Test CSCA, O=DevData Test, C=ZZ"
    private const val DS_DN   = "CN=DevData Test DS,   O=DevData Test, C=ZZ"

    // Country ZZ is not a real ISO 3166 code — explicitly marks test docs
    const val TEST_COUNTRY_CODE = "ZZ"

    // ── Initialisation ─────────────────────────────────────────────────────

    /**
     * Ensure both CSCA and DS key pairs exist in the Keystore.
     * Call once at app start (idempotent — skips generation if keys already exist).
     */
    fun ensureInitialised() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(ALIAS_CSCA)) {
            Log.i(TAG, "Generating test CSCA key pair...")
            generateKeyPair(
                alias    = ALIAS_CSCA,
                subject  = CSCA_DN,
                isCA     = true,
                validity = 10 * 365
            )
            Log.i(TAG, "CSCA generated: ${csaCertificate().subjectX500Principal}")
        }
        if (!ks.containsAlias(ALIAS_DS)) {
            Log.i(TAG, "Generating test DS key pair...")
            generateKeyPair(
                alias    = ALIAS_DS,
                subject  = DS_DN,
                isCA     = false,
                validity = 2 * 365
            )
            Log.i(TAG, "DS generated: ${dsCertificate().subjectX500Principal}")
        }
    }

    // ── Public accessors ───────────────────────────────────────────────────

    /** X.509 certificate for the CSCA — register this in Regula SDK as trusted. */
    fun csaCertificate(): X509Certificate = certificate(ALIAS_CSCA)

    /** X.509 certificate for the Document Signer. */
    fun dsCertificate(): X509Certificate = certificate(ALIAS_DS)

    /** Private key for the Document Signer — used by [SodBuilder] to sign SOD. */
    fun dsPrivateKey(): PrivateKey = privateKey(ALIAS_DS)

    // ── Private helpers ────────────────────────────────────────────────────

    private fun certificate(alias: String): X509Certificate {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getCertificate(alias) as? X509Certificate
            ?: error("Certificate for alias '$alias' not found — call ensureInitialised() first")
    }

    private fun privateKey(alias: String): PrivateKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(alias, null) as? PrivateKey
            ?: error("Private key for alias '$alias' not found — call ensureInitialised() first")
    }

    /**
     * Generate an RSA-2048 key pair inside the Android Keystore.
     *
     * Android Keystore self-signs the certificate during key generation
     * (we get a real X.509 cert with the DN we specify).
     * For the CSCA we set PURPOSES to SIGN so it can sign the DS cert
     * indirectly via [SodBuilder].
     *
     * Note: Android Keystore's KeyPairGenerator produces a self-signed cert.
     * We use that cert directly for the CSCA. For the DS we replace
     * the self-signed cert with one signed by the CSCA (see [SodBuilder.buildDsCertificate]).
     * Signing cross-cert in the Keystore requires the signing key's operations
     * to use [Signature] with the CSCA private key — this is supported.
     */
    private fun generateKeyPair(
        alias:    String,
        subject:  String,
        isCA:     Boolean,
        validity: Int
    ) {
        val now   = Date()
        val until = Date(now.time + validity.toLong() * 24 * 60 * 60 * 1000)

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal(subject))
            .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            .setCertificateNotBefore(now)
            .setCertificateNotAfter(until)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }
}
