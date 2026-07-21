package com.vadimtoptunov.devdata.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.vadimtoptunov.devdata.hce.sod.CsaKeyStore
import com.vadimtoptunov.devdata.hce.sod.SodBuilder
import com.vadimtoptunov.devdata.ui.screens.ApduDirection
import com.vadimtoptunov.devdata.ui.screens.ApduLogEntry
import com.vadimtoptunov.devdata.ui.screens.NfcSessionLog
import com.vadimtoptunov.generators.identity.MrzData
import com.vadimtoptunov.generators.identity.NfcChipState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * HCE (Host Card Emulation) service that emulates an ICAO 9303 eMRTD chip.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * PASSIVE AUTHENTICATION — HOW IT PASSES
 *
 * Standard chips are signed by government CSCA keys (not held by anyone
 * outside national authorities). This service instead uses a test CSCA
 * generated in Android Keystore ([CsaKeyStore]):
 *
 *   Test CSCA (self-signed, RSA-2048, valid 10 yr)
 *     └─ signs Test DS cert (RSA-2048, valid 2 yr)
 *           └─ signs EF.SOD (SHA-256 hashes of DG1 + DG2)
 *
 * The host app must register the test CSCA once:
 *   DocumentReader.addPKDCertificates(
 *     listOf(PKDCertificate(CsaKeyStore.csaCertificate(ctx).encoded,
 *                           PKDCertificate.CERT_TYPE_CSCA, null))
 *   )
 *
 * After that, Regula's PA verifies:
 *   1. DS cert chain → CSCA         ✓ (our test CSCA is trusted)
 *   2. SOD signature → DS cert      ✓ (SodBuilder signs with DS key)
 *   3. DG hashes in SOD == actual   ✓ (we hash the same bytes we serve)
 *
 * For NfcChipState.PA_FAIL we intentionally return an unsigned/empty SOD
 * so test cases that expect PA to fail still work.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * APDU flow:
 *   Reader → SELECT AID (A0000002471001)   → SW 9000
 *   Reader → SELECT EF.COM (011E)          → EF.COM data + SW 9000
 *   Reader → SELECT DG1   (0101)           → DG1 (MRZ)  + SW 9000
 *   Reader → SELECT DG2   (0102)           → DG2 stub   + SW 9000
 *   Reader → SELECT EF.SOD (011D)          → signed SOD + SW 9000
 *   Reader → READ BINARY  (00 B0 …)        → chunk of selected file
 *
 * File is cached in [currentFileContent] and served in chunks via READ BINARY.
 */
class HcePassportService : HostApduService() {

    private val tag = "HcePassportService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeChip: ActiveChipState? = null

    // Per-session DG cache — needed so SOD hashes stay consistent
    private var cachedDg1: ByteArray? = null
    private var cachedDg2: ByteArray? = null
    private var currentFileContent: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        // Ensure CSCA + DS keys exist before any NFC session starts
        try {
            CsaKeyStore.ensureInitialised()
        } catch (e: Exception) {
            Log.e(tag, "CsaKeyStore init failed", e)
        }
        scope.launch {
            ActiveChipRegistry.observeChip(applicationContext).collect { chip ->
                activeChip = chip
                Log.d(tag, "Active chip updated: ${chip?.label ?: "none"}")
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(tag, "Deactivated reason=$reason — clearing session")
        cachedDg1 = null
        cachedDg2 = null
        currentFileContent = null
    }

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        val chip = activeChip ?: return SW_NOT_FOUND
        Log.d(tag, "APDU ← ${apdu.hex()}")

        val annotation = when {
            apdu.isSelectAid(AID_MRTD) -> "SELECT AID"
            apdu.isSelectEf(EF_COM)    -> "SELECT EF.COM"
            apdu.isSelectEf(EF_DG1)   -> "SELECT DG1"
            apdu.isSelectEf(EF_DG2)   -> "SELECT DG2"
            apdu.isSelectEf(EF_SOD)   -> "SELECT EF.SOD"
            apdu.isReadBinary()        -> "READ BINARY"
            else                       -> "?"
        }
        NfcSessionLog.emit(ApduLogEntry(ApduDirection.IN, apdu.hex(), annotation))

        val response = when {
            apdu.isSelectAid(AID_MRTD) -> handleSelectAid(chip)
            apdu.isSelectEf(EF_COM)    -> handleEfCom(chip)
            apdu.isSelectEf(EF_DG1)   -> handleDg1(chip)
            apdu.isSelectEf(EF_DG2)   -> handleDg2(chip)
            apdu.isSelectEf(EF_SOD)   -> handleSod(chip)
            apdu.isReadBinary()        -> handleReadBinary(apdu)
            else                       -> SW_INS_NOT_SUPPORTED
        }
        Log.d(tag, "APDU → ${response.hex()}")
        NfcSessionLog.emit(ApduLogEntry(ApduDirection.OUT, response.hex()))
        return response
    }

    // ── APDU handlers ──────────────────────────────────────────────────────

    private fun handleSelectAid(chip: ActiveChipState): ByteArray {
        return when (chip.chipState) {
            NfcChipState.NO_CHIP -> SW_NOT_FOUND
            NfcChipState.LOCKED  -> SW_SECURITY_STATUS_NOT_SATISFIED
            else                 -> {
                Log.d(tag, "SELECT AID — serving ${chip.label}")
                SW_OK
            }
        }
    }

    private fun handleEfCom(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        // EF.COM: LDS version 01.07, unicode version 04.00.00, DG list: DG1+DG2
        val efCom = buildTlv(0x60, byteArrayOf(
            0x5F.toByte(), 0x01, 0x04, 0x01, 0x07, 0x00, 0x00,   // LDS version
            0x5C.toByte(), 0x02, 0x01, 0x02                        // DG list: DG1, DG2
        ))
        currentFileContent = efCom
        return SW_OK
    }

    private fun handleDg1(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        if (chip.chipState == NfcChipState.PARTIAL_READ) return SW_FILE_NOT_FOUND
        val mrz = chip.mrzData ?: return SW_FILE_NOT_FOUND
        val mrzBytes = mrz.raw.toByteArray(Charsets.US_ASCII)
        // DG1 = tag 0x61, contains tag 0x5F1F (MRZ data)
        val dg1 = buildTlv(0x61, buildTlv(0x5F1F, mrzBytes))
        cachedDg1 = dg1
        currentFileContent = dg1
        return SW_OK
    }

    private fun handleDg2(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        if (chip.chipState == NfcChipState.PARTIAL_READ) return SW_FILE_NOT_FOUND
        // Minimal DG2 stub — empty Biometric Information Group template
        // Real chips carry a JPEG-2000 face; we serve a valid but imageless structure
        val dg2 = buildTlv(0x75, buildTlv2(0x7F61, byteArrayOf(
            0x02, 0x01, 0x01,   // numberOfInstances = 1
            0x7F.toByte(), 0x60, 0x00   // biometricTemplate — empty
        )))
        cachedDg2 = dg2
        currentFileContent = dg2
        return SW_OK
    }

    private fun handleSod(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED

        if (chip.chipState == NfcChipState.PA_FAIL) {
            // Deliberate PA failure — serve an unsigned/empty SOD
            currentFileContent = byteArrayOf()
            return SW_OK
        }

        val dg1 = cachedDg1
        if (dg1 == null) {
            // SOD was requested before DG1 — build DG1 now from mrzData
            val mrz = chip.mrzData ?: return SW_FILE_NOT_FOUND
            val mrzBytes = mrz.raw.toByteArray(Charsets.US_ASCII)
            cachedDg1 = buildTlv(0x61, buildTlv(0x5F1F, mrzBytes))
        }

        return try {
            val dsCert = CsaKeyStore.dsCertificate()
            val dsKey  = CsaKeyStore.dsPrivateKey()

            val sod = SodBuilder.build(
                dg1Bytes = cachedDg1!!,
                dg2Bytes = cachedDg2,
                dsCert   = dsCert,
                dsKey    = dsKey
            )
            currentFileContent = sod
            Log.d(tag, "SOD built — ${sod.size} bytes")
            SW_OK
        } catch (e: Exception) {
            Log.e(tag, "SOD build failed", e)
            SW_SECURITY_STATUS_NOT_SATISFIED
        }
    }

    private fun handleReadBinary(apdu: ByteArray): ByteArray {
        val content = currentFileContent ?: return SW_FILE_NOT_FOUND
        val offset  = ((apdu.getOrElse(2) { 0 }.toInt() and 0xFF) shl 8) or
                       (apdu.getOrElse(3) { 0 }.toInt() and 0xFF)
        val le      = apdu.getOrElse(4) { 0xFF.toByte() }.toInt() and 0xFF

        if (offset >= content.size) return SW_WRONG_LENGTH
        val end = minOf(offset + le, content.size)
        return content.copyOfRange(offset, end) + SW_OK
    }

    // ── TLV helpers ────────────────────────────────────────────────────────

    /** Single-byte tag TLV */
    private fun buildTlv(tag: Int, value: ByteArray): ByteArray {
        val len = value.size
        val header = when {
            len <= 127 -> byteArrayOf(tag.toByte(), len.toByte())
            len <= 255 -> byteArrayOf(tag.toByte(), 0x81.toByte(), len.toByte())
            else       -> byteArrayOf(tag.toByte(), 0x82.toByte(),
                              (len shr 8).toByte(), (len and 0xFF).toByte())
        }
        return header + value
    }

    /** Two-byte tag TLV (e.g. 0x7F61) */
    private fun buildTlv2(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = byteArrayOf((tag shr 8).toByte(), (tag and 0xFF).toByte())
        val len = value.size
        val lenBytes = when {
            len <= 127 -> byteArrayOf(len.toByte())
            len <= 255 -> byteArrayOf(0x81.toByte(), len.toByte())
            else       -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xFF).toByte())
        }
        return tagBytes + lenBytes + value
    }

    // ── APDU matchers ──────────────────────────────────────────────────────

    private fun ByteArray.isSelectAid(aid: ByteArray): Boolean =
        size >= 5 + aid.size &&
        this[0] == 0x00.toByte() && this[1] == 0xA4.toByte() &&
        this[2] == 0x04.toByte() && this[3] == 0x00.toByte() &&
        this[4].toInt() == aid.size &&
        copyOfRange(5, 5 + aid.size).contentEquals(aid)

    private fun ByteArray.isSelectEf(fid: ByteArray): Boolean =
        size >= 4 + fid.size &&
        this[0] == 0x00.toByte() && this[1] == 0xA4.toByte() &&
        this[2] == 0x02.toByte() &&
        fid.contentEquals(copyOfRange(4, 4 + fid.size))

    private fun ByteArray.isReadBinary(): Boolean =
        size >= 4 && this[0] == 0x00.toByte() && this[1] == 0xB0.toByte()

    private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }

    // ── Status words ───────────────────────────────────────────────────────

    companion object {
        private val AID_MRTD = byteArrayOf(
            0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
        )
        private val EF_COM = byteArrayOf(0x01, 0x1E)
        private val EF_DG1 = byteArrayOf(0x01, 0x01)
        private val EF_DG2 = byteArrayOf(0x01, 0x02)
        private val EF_SOD = byteArrayOf(0x01, 0x1D)

        val SW_OK                            = byteArrayOf(0x90.toByte(), 0x00)
        val SW_NOT_FOUND                     = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_FILE_NOT_FOUND                = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_SECURITY_STATUS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())
        val SW_INS_NOT_SUPPORTED             = byteArrayOf(0x6D.toByte(), 0x00)
        val SW_WRONG_LENGTH                  = byteArrayOf(0x67.toByte(), 0x00)
    }
}

/**
 * Distilled runtime view of the active chip — what [HcePassportService]
 * actually needs without dragging the full SyntheticIdentity across the
 * DB ↔ service boundary.
 */
data class ActiveChipState(
    val identityId: String,
    val label:      String,
    val chipState:  NfcChipState,
    val mrzData:    MrzData?
) {
    /** True when the chip responds to file reads (PARTIAL_READ and PA_FAIL still respond). */
    val isReadable: Boolean get() = chipState != NfcChipState.NO_CHIP &&
                                    chipState != NfcChipState.LOCKED
}
