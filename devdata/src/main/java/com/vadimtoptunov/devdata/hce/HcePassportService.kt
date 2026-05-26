package com.vadimtoptunov.devdata.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
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
 * IMPORTANT — TEST DATA ONLY
 * This service serves algorithmically generated data.
 * Passive Authentication (SOD) will always FAIL because no government
 * CSCA private key is held. DG1 (MRZ) and DG2 stub are readable.
 * Intended use: authorised KYC SDK integration testing only.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * APDU flow (simplified):
 *   Reader → SELECT AID (A0000002471001)   → SW 9000
 *   Reader → SELECT EF.COM (011E)          → EF.COM data + SW 9000
 *   Reader → SELECT DG1 (0101)             → DG1 (MRZ) data + SW 9000
 *   Reader → SELECT DG2 (0102)             → DG2 stub + SW 9000
 *   Reader → SELECT EF.SOD (011D)          → empty SOD + SW 9000 (PA fails)
 *
 * The active identity is read from [ActiveChipRegistry] which pulls from Room.
 */
class HcePassportService : HostApduService() {

    private val tag = "HcePassportService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Loaded lazily when the first SELECT comes in
    private var activeChip: ActiveChipState? = null

    override fun onCreate() {
        super.onCreate()
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

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        val chip = activeChip
        if (chip == null) {
            Log.d(tag, "No active chip — returning not found")
            return SW_NOT_FOUND
        }

        Log.d(tag, "APDU: ${apdu.hex()}")

        return when {
            apdu.isSelectAid(AID_MRTD)  -> handleSelectAid(chip)
            apdu.isSelectEf(EF_COM)      -> handleEfCom(chip)
            apdu.isSelectEf(EF_DG1)      -> handleDg1(chip)
            apdu.isSelectEf(EF_DG2)      -> handleDg2(chip)
            apdu.isSelectEf(EF_SOD)      -> handleSod(chip)
            apdu.isReadBinary()          -> handleReadBinary(apdu, chip)
            else                         -> SW_INS_NOT_SUPPORTED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(tag, "Deactivated, reason=$reason")
        currentFile = null
        currentFileContent = null
    }

    // ── Selected file state (per NFC session) ──────────────────────────────

    private var currentFile: ByteArray? = null
    private var currentFileContent: ByteArray? = null

    // ── APDU handlers ──────────────────────────────────────────────────────

    private fun handleSelectAid(chip: ActiveChipState): ByteArray {
        if (chip.chipState == NfcChipState.NO_CHIP) return SW_NOT_FOUND
        if (chip.chipState == NfcChipState.LOCKED) return SW_SECURITY_STATUS_NOT_SATISFIED
        Log.d(tag, "SELECT AID — serving ${chip.label}")
        return SW_OK
    }

    private fun handleEfCom(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        // EF.COM: tag 60, length, version 01 01, list of DGs present: 01 (DG1), 02 (DG2)
        val efCom = buildTlv(0x60, byteArrayOf(
            0x5F.toByte(), 0x01, 0x01, 0x01,      // version 1.1
            0x5C.toByte(), 0x02, 0x01, 0x02        // DG list: DG1, DG2
        ))
        currentFileContent = efCom
        return efCom + SW_OK
    }

    private fun handleDg1(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        if (chip.chipState == NfcChipState.PARTIAL_READ) return SW_FILE_NOT_FOUND
        val mrz = chip.mrzData ?: return SW_FILE_NOT_FOUND
        val mrzBytes = mrz.raw.toByteArray(Charsets.US_ASCII)
        // DG1: tag 61, contents: tag 5F1F (MRZ data)
        val dg1 = buildTlv(0x61, buildTlv(0x5F1F, mrzBytes))
        currentFileContent = dg1
        return dg1 + SW_OK
    }

    private fun handleDg2(chip: ActiveChipState): ByteArray {
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        if (chip.chipState == NfcChipState.PARTIAL_READ) return SW_FILE_NOT_FOUND
        // Minimal DG2 stub — biometric header only, no actual image
        // Real DG2 would contain a JPEG-2000 face image; we signal its absence.
        val dg2stub = buildTlv(0x75, byteArrayOf(
            0x7F.toByte(), 0x61, 0x00   // Biometric Information Group — empty
        ))
        currentFileContent = dg2stub
        return dg2stub + SW_OK
    }

    private fun handleSod(chip: ActiveChipState): ByteArray {
        // Return an empty SOD — this guarantees PA_FAIL, which is the expected
        // and documented behaviour for synthetic test data.
        if (!chip.isReadable) return SW_SECURITY_STATUS_NOT_SATISFIED
        currentFileContent = byteArrayOf()
        return SW_OK
    }

    private fun handleReadBinary(apdu: ByteArray, chip: ActiveChipState): ByteArray {
        val content = currentFileContent ?: return SW_FILE_NOT_FOUND
        val offset = ((apdu.getOrElse(2) { 0 }.toInt() and 0xFF) shl 8) or
                      (apdu.getOrElse(3) { 0 }.toInt() and 0xFF)
        val le = apdu.getOrElse(4) { 0xFF.toByte() }.toInt() and 0xFF

        if (offset >= content.size) return SW_WRONG_LENGTH
        val end = minOf(offset + le, content.size)
        return content.copyOfRange(offset, end) + SW_OK
    }

    // ── APDU helpers ──────────────────────────────────────────────────────

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

    private fun buildTlv(tag: Int, value: ByteArray): ByteArray {
        val tagByte = tag.toByte()
        val len = value.size
        return when {
            len <= 127 -> byteArrayOf(tagByte, len.toByte()) + value
            len <= 255 -> byteArrayOf(tagByte, 0x81.toByte(), len.toByte()) + value
            else       -> byteArrayOf(tagByte, 0x82.toByte(),
                              (len shr 8).toByte(), (len and 0xFF).toByte()) + value
        }
    }

    private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }

    // ── Status words ──────────────────────────────────────────────────────

    companion object {
        private val AID_MRTD = byteArrayOf(
            0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
        )
        private val EF_COM  = byteArrayOf(0x01, 0x1E)
        private val EF_DG1  = byteArrayOf(0x01, 0x01)
        private val EF_DG2  = byteArrayOf(0x01, 0x02)
        private val EF_SOD  = byteArrayOf(0x01, 0x1D)

        val SW_OK                              = byteArrayOf(0x90.toByte(), 0x00)
        val SW_NOT_FOUND                       = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_FILE_NOT_FOUND                  = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_SECURITY_STATUS_NOT_SATISFIED   = byteArrayOf(0x69.toByte(), 0x82.toByte())
        val SW_INS_NOT_SUPPORTED               = byteArrayOf(0x6D.toByte(), 0x00)
        val SW_WRONG_LENGTH                    = byteArrayOf(0x67.toByte(), 0x00)
    }
}

/**
 * The data the HCE service actually needs at runtime —
 * a distilled view of [NfcChipData] that avoids passing
 * the full SyntheticIdentity across the DB ↔ service boundary.
 */
data class ActiveChipState(
    val identityId: String,
    val label:      String,        // "ES Passport — Juan García — EXPIRED"
    val chipState:  NfcChipState,
    val mrzData:    MrzData?
) {
    val isReadable: Boolean get() = chipState == NfcChipState.READABLE ||
                                    chipState == NfcChipState.PA_FAIL ||
                                    chipState == NfcChipState.PARTIAL_READ
}
