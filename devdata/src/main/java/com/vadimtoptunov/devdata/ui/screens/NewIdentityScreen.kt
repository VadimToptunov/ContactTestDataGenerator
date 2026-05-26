package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.ui.IdentityViewModel
import com.vadimtoptunov.devdata.ui.UiState
import com.vadimtoptunov.generators.identity.CountryProfiles
import com.vadimtoptunov.generators.identity.DocumentRequest
import com.vadimtoptunov.generators.identity.DocumentState
import com.vadimtoptunov.generators.identity.DocumentType
import com.vadimtoptunov.generators.identity.MrzMismatch
import com.vadimtoptunov.generators.identity.NfcChipState
import com.vadimtoptunov.generators.identity.NfcRequest

/**
 * Screen for building a [DocumentRequest] and triggering generation.
 *
 * Exposes all the key dimensions:
 *   Country → Document type → Document state →
 *   MRZ mismatch → NFC mode → NFC chip state
 */
@Composable
fun NewIdentityScreen(vm: IdentityViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()

    // ── Form state ──────────────────────────────────────────────────────────
    val countries      = remember { CountryProfiles.ALL.keys.sorted() }
    var countryCode    by remember { mutableStateOf("ES") }
    var documentType   by remember { mutableStateOf(DocumentType.PASSPORT) }
    var documentState  by remember { mutableStateOf(DocumentState.VALID) }
    var mrzMismatch    by remember { mutableStateOf(MrzMismatch.NONE) }
    var nfcMode        by remember { mutableStateOf(NfcMode.SAME_AS_HOLDER) }
    var nfcChipState   by remember { mutableStateOf(NfcChipState.READABLE) }
    var thirdPartyCode by remember { mutableStateOf("DE") }

    val isLoading = uiState is UiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New Synthetic Identity", style = MaterialTheme.typography.headlineSmall)
        Text(
            "SPECIMEN / TEST DATA — not valid for official use",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )

        HorizontalDivider()

        // ── Country ─────────────────────────────────────────────────────────
        DropdownField(
            label   = "Country",
            options = countries,
            value   = countryCode,
            onSelect = { countryCode = it }
        )

        // ── Document type ───────────────────────────────────────────────────
        DropdownField(
            label   = "Document type",
            options = DocumentType.values().map { it.name },
            value   = documentType.name,
            onSelect = { documentType = DocumentType.valueOf(it) }
        )

        // ── Document state ──────────────────────────────────────────────────
        DropdownField(
            label   = "Document state",
            options = DocumentState.values().map { it.name },
            value   = documentState.name,
            onSelect = { documentState = DocumentState.valueOf(it) }
        )

        HorizontalDivider()
        Text("MRZ ↔ Visual mismatch", style = MaterialTheme.typography.titleSmall)

        // ── MRZ mismatch ────────────────────────────────────────────────────
        DropdownField(
            label   = "MRZ mismatch",
            options = MrzMismatch.values().map { it.name },
            value   = mrzMismatch.name,
            onSelect = { mrzMismatch = MrzMismatch.valueOf(it) }
        )

        HorizontalDivider()
        Text("NFC chip", style = MaterialTheme.typography.titleSmall)

        // ── NFC mode ────────────────────────────────────────────────────────
        DropdownField(
            label   = "NFC mode",
            options = NfcMode.values().map { it.label },
            value   = nfcMode.label,
            onSelect = { label -> nfcMode = NfcMode.values().first { it.label == label } }
        )

        if (nfcMode != NfcMode.NONE) {
            DropdownField(
                label   = "Chip state",
                options = NfcChipState.values().map { it.name },
                value   = nfcChipState.name,
                onSelect = { nfcChipState = NfcChipState.valueOf(it) }
            )
        }

        if (nfcMode == NfcMode.THIRD_PARTY) {
            DropdownField(
                label   = "Third-party country",
                options = countries,
                value   = thirdPartyCode,
                onSelect = { thirdPartyCode = it }
            )
        }

        HorizontalDivider()

        // ── Quick suite buttons ──────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick  = { vm.generateNfcSuite(countryCode) },
                enabled  = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("NFC suite") }

            OutlinedButton(
                onClick  = { vm.generateStateSuite(countryCode) },
                enabled  = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("State suite") }
        }

        // ── Generate single ──────────────────────────────────────────────────
        Button(
            onClick = {
                val nfcRequest = when (nfcMode) {
                    NfcMode.NONE          -> NfcRequest.None
                    NfcMode.SAME_AS_HOLDER -> NfcRequest.SameAsHolder(nfcChipState)
                    NfcMode.THIRD_PARTY   -> NfcRequest.ThirdParty(
                        thirdPartyCountry = thirdPartyCode,
                        chipState         = nfcChipState
                    )
                }
                vm.generate(
                    DocumentRequest(
                        countryCode  = countryCode,
                        documentType = documentType,
                        state        = documentState,
                        mrzMismatch  = mrzMismatch,
                        nfc          = nfcRequest
                    )
                )
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Generate identity")
        }

        // ── Error snackbar ───────────────────────────────────────────────────
        if (uiState is UiState.Error) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        (uiState as UiState.Error).message,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = vm::dismissError) { Text("OK") }
                }
            }
        }
    }
}

// ── NFC mode enum for the UI (maps to NfcRequest) ─────────────────────────

private enum class NfcMode(val label: String) {
    NONE("No chip"),
    SAME_AS_HOLDER("Same as document holder"),
    THIRD_PARTY("Third-party identity")
}

// ── Generic dropdown ────────────────────────────────────────────────────────

@Composable
private fun DropdownField(
    label:    String,
    options:  List<String>,
    value:    String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value            = value,
            onValueChange    = {},
            readOnly         = true,
            label            = { Text(label) },
            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier         = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text    = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
