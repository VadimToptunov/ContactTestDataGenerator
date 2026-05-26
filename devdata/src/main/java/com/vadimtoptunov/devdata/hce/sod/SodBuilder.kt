package com.vadimtoptunov.devdata.hce.sod

import java.security.MessageDigest
import java.security.Signature
import java.security.cert.X509Certificate

/**
 * Builds a valid ICAO 9303 EF.SOD (Security Object Document) structure.
 *
 * EF.SOD is a PKCS#7 / CMS SignedData that contains:
 *   - Hashes of each Data Group (DG1=MRZ, DG2=face photo, …)
 *   - The Document Signer (DS) certificate
 *   - A signature over the hash-map, made by the DS private key
 *
 * Regula's Passive Authentication verifies:
 *   1. DS cert is signed by a trusted CSCA  ← our CsaKeyStore handles this
 *   2. Signature in SOD is valid for DS cert ← we compute this here
 *   3. DG hashes in SOD match the actual DGs ← we hash the same bytes
 *
 * All three checks pass when using our test CSCA registered via
 *   DocumentReader.addPKDCertificates(listOf(PKDCertificate(csca.encoded, …)))
 *
 * ASN.1 structure (simplified — we hand-encode the critical subset):
 *
 *   EF.SOD ::= SEQUENCE {
 *     oid   OBJECT IDENTIFIER  -- 2.23.136.1.1.1 (LDS Security Object)
 *     content [0] EXPLICIT SEQUENCE {
 *       version      INTEGER (0)
 *       hashAlgorithm AlgorithmIdentifier (SHA-256)
 *       encapContentInfo EncapsulatedContentInfo {
 *         eContentType OID  -- id-smime-ct-mrtd-SecurityObject
 *         eContent [0] OCTET STRING {
 *           LdsSecurityObject ::= SEQUENCE {
 *             version INTEGER (0)
 *             hashAlgorithmIdentifier AlgorithmIdentifier
 *             dataGroupHashValues SEQUENCE OF DataGroupHash {
 *               DataGroupHash ::= SEQUENCE {
 *                 dataGroupNumber INTEGER
 *                 dataGroupHashValue OCTET STRING
 *               }
 *             }
 *           }
 *         }
 *       }
 *       certificates [0] IMPLICIT SET { dsCertificate }
 *       signerInfos SET {
 *         SignerInfo ::= SEQUENCE {
 *           version INTEGER (1)
 *           issuerAndSerialNumber IssuerAndSerialNumber
 *           digestAlgorithm AlgorithmIdentifier
 *           signedAttrs [0] IMPLICIT SET OF Attribute
 *           signatureAlgorithm AlgorithmIdentifier
 *           signature OCTET STRING
 *         }
 *       }
 *     }
 *   }
 */
object SodBuilder {

    // OIDs
    private val OID_SHA256             = byteArrayOf(0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01)
    private val OID_RSA_ENCRYPTION     = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)
    private val OID_SHA256_WITH_RSA    = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)
    private val OID_CONTENT_TYPE       = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x09, 0x03)
    private val OID_MESSAGE_DIGEST     = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x09, 0x04)
    // id-smime-ct-mrtd-SecurityObject (2.16.840.1.101.2.1.22.18)
    private val OID_LDS_SECURITY_OBJ   = byteArrayOf(0x60, 0x86.toByte(), 0x48, 0x01, 0x86.toByte(), 0xFE.toByte(), 0x35, 0x01, 0x16, 0x12)
    // 2.23.136.1.1.1 (icao-mrtd-security-object)
    private val OID_ICAO_MRTD_SOD      = byteArrayOf(0x60, 0x81.toByte(), 0x98.toByte(), 0x01, 0x01, 0x01)

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Build a DER-encoded EF.SOD from [dg1Bytes] (MRZ) and optional [dg2Bytes] (face stub).
     *
     * @param dg1Bytes    Raw DG1 TLV bytes (as returned by [HcePassportService.handleDg1])
     * @param dg2Bytes    Raw DG2 TLV bytes (null = omit from SOD hash list)
     * @param dsCert      Document Signer certificate ([CsaKeyStore.dsCertificate])
     * @param dsKey       Document Signer private key ([CsaKeyStore.dsPrivateKey])
     */
    fun build(
        dg1Bytes: ByteArray,
        dg2Bytes: ByteArray?,
        dsCert:   X509Certificate,
        dsKey:    java.security.PrivateKey
    ): ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")

        // 1 — Hash each DG
        val dg1Hash = sha256.digest(dg1Bytes)
        val dg2Hash = dg2Bytes?.let { sha256.digest(it) }

        // 2 — Build LdsSecurityObject (the payload that gets signed)
        val ldsSecObj = buildLdsSecurityObject(dg1Hash, dg2Hash)

        // 3 — Build signedAttrs (what DS key actually signs)
        val ldsHash    = sha256.digest(ldsSecObj)
        val signedAttrs = buildSignedAttrs(ldsHash)

        // 4 — Sign with DS key
        val sig = Signature.getInstance("SHA256withRSA").run {
            initSign(dsKey)
            // SignedData signs the DER encoding of signedAttrs
            // with tag 0x31 (SET), not 0xA0 (context-specific)
            val signedAttrsDer = asn1Set(signedAttrs)
            update(signedAttrsDer)
            sign()
        }

        // 5 — Build full SignedData CMS structure
        return buildSignedData(
            ldsSecObj   = ldsSecObj,
            dsCert      = dsCert,
            signedAttrs = signedAttrs,
            signature   = sig
        )
    }

    // ── LDS Security Object ────────────────────────────────────────────────

    private fun buildLdsSecurityObject(dg1Hash: ByteArray, dg2Hash: ByteArray?): ByteArray {
        val hashes = mutableListOf<ByteArray>()
        hashes += dataGroupHash(1, dg1Hash)
        dg2Hash?.let { hashes += dataGroupHash(2, it) }

        return asn1Sequence(
            asn1Integer(0),                             // version 0
            algorithmIdentifier(OID_SHA256),            // hashAlgorithm
            asn1Sequence(*hashes.toTypedArray())        // dataGroupHashValues
        )
    }

    private fun dataGroupHash(dgNumber: Int, hash: ByteArray): ByteArray =
        asn1Sequence(
            asn1Integer(dgNumber),
            asn1OctetString(hash)
        )

    // ── SignedAttrs ────────────────────────────────────────────────────────

    /**
     * SignedAttributes SET (RFC 5652 §5.4) — the actual bytes signed by DS key.
     * Must contain at minimum: content-type and message-digest attributes.
     */
    private fun buildSignedAttrs(contentHash: ByteArray): ByteArray {
        val contentTypeAttr = asn1Sequence(
            asn1Oid(OID_CONTENT_TYPE),
            asn1Set(asn1Oid(OID_LDS_SECURITY_OBJ))
        )
        val messageDigestAttr = asn1Sequence(
            asn1Oid(OID_MESSAGE_DIGEST),
            asn1Set(asn1OctetString(contentHash))
        )
        return contentTypeAttr + messageDigestAttr
    }

    // ── SignedData (outer CMS structure) ───────────────────────────────────

    private fun buildSignedData(
        ldsSecObj:   ByteArray,
        dsCert:      X509Certificate,
        signedAttrs: ByteArray,
        signature:   ByteArray
    ): ByteArray {
        val encapContent = asn1Sequence(
            asn1Oid(OID_LDS_SECURITY_OBJ),
            asn1ContextExplicit(0, asn1OctetString(ldsSecObj))
        )

        val certBytes = dsCert.encoded
        val certificates = asn1ContextImplicit(0, certBytes)

        val signerInfo = asn1Sequence(
            asn1Integer(1),                                    // version
            issuerAndSerialNumber(dsCert),
            algorithmIdentifier(OID_SHA256),                   // digestAlgorithm
            asn1ContextImplicit(0, signedAttrs),               // signedAttrs [0] IMPLICIT
            algorithmIdentifier(OID_SHA256_WITH_RSA),          // signatureAlgorithm
            asn1OctetString(signature)
        )

        val signedData = asn1Sequence(
            asn1Integer(1),                                    // version
            asn1Set(algorithmIdentifier(OID_SHA256)),          // digestAlgorithms
            encapContent,
            certificates,
            asn1Set(signerInfo)                                // signerInfos
        )

        // Wrap in ContentInfo with id-signedData OID
        val oidSignedData = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x02)
        return asn1Sequence(
            asn1Oid(oidSignedData),
            asn1ContextExplicit(0, signedData)
        )
    }

    private fun issuerAndSerialNumber(cert: X509Certificate): ByteArray {
        val issuerDer   = cert.issuerX500Principal.encoded
        val serialBytes = cert.serialNumber.toByteArray()
        return asn1Sequence(
            issuerDer,
            asn1Integer(serialBytes)
        )
    }

    private fun algorithmIdentifier(oid: ByteArray): ByteArray =
        asn1Sequence(asn1Oid(oid), asn1Null())

    // ── ASN.1 DER primitives ──────────────────────────────────────────────

    private fun tlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes  = if (tag <= 0xFF) byteArrayOf(tag.toByte())
                        else byteArrayOf((tag shr 8).toByte(), (tag and 0xFF).toByte())
        val lenBytes  = derLength(value.size)
        return tagBytes + lenBytes + value
    }

    private fun derLength(len: Int): ByteArray = when {
        len <= 127 -> byteArrayOf(len.toByte())
        len <= 255 -> byteArrayOf(0x81.toByte(), len.toByte())
        else       -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xFF).toByte())
    }

    private fun asn1Sequence(vararg parts: ByteArray) = tlv(0x30, parts.fold(byteArrayOf()) { a, b -> a + b })
    private fun asn1Set(vararg parts: ByteArray)      = tlv(0x31, parts.fold(byteArrayOf()) { a, b -> a + b })
    private fun asn1Integer(value: Int): ByteArray    = tlv(0x02, BigIntEncoder.encode(value))
    private fun asn1Integer(bytes: ByteArray)         = tlv(0x02, bytes)
    private fun asn1OctetString(bytes: ByteArray)     = tlv(0x04, bytes)
    private fun asn1Null()                            = tlv(0x05, byteArrayOf())
    private fun asn1Oid(oidBytes: ByteArray)          = tlv(0x06, oidBytes)
    private fun asn1ContextExplicit(tag: Int, value: ByteArray) = tlv(0xA0 or tag, value)
    private fun asn1ContextImplicit(tag: Int, value: ByteArray) = tlv(0x80 or tag, value)

    private object BigIntEncoder {
        fun encode(value: Int): ByteArray {
            val bytes = java.math.BigInteger.valueOf(value.toLong()).toByteArray()
            // Ensure minimal encoding (remove leading 0x00 except when needed for sign)
            return if (bytes.size > 1 && bytes[0] == 0x00.toByte() && bytes[1].toInt() and 0x80 == 0) {
                bytes.copyOfRange(1, bytes.size)
            } else bytes
        }
    }
}
