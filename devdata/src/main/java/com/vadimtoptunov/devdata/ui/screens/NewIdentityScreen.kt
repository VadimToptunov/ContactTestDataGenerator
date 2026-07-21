package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.ui.IdentityViewModel
import com.vadimtoptunov.devdata.ui.UiState
import com.vadimtoptunov.generators.identity.CountryProfiles
import com.vadimtoptunov.generators.identity.DocumentRequest
import com.vadimtoptunov.generators.identity.DocumentState
import com.vadimtoptunov.generators.identity.DocumentType
import com.vadimtoptunov.generators.identity.FacePhotoState
import com.vadimtoptunov.generators.identity.MrzMismatch
import com.vadimtoptunov.generators.identity.NfcChipState
import com.vadimtoptunov.generators.identity.NfcRequest

/**
 * Screen for building a [DocumentRequest] and triggering generation.
 *
 * Exposes all key test dimensions:
 *   Country → Document type → Document state →
 *   MRZ mismatch → NFC mode → Chip state → Face photo state →
 *   Extra data flags → Single generate or suite
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
    var facePhotoState by remember { mutableStateOf(FacePhotoState.MATCHES_DOCUMENT) }
    var thirdPartyCode by remember { mutableStateOf("DE") }
    var includeCard    by remember { mutableStateOf(false) }
    var includeIban    by remember { mutableStateOf(false) }

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

        // ── Document ────────────────────────────────────────────────────────
        Text("Document", style = MaterialTheme.typography.titleSmall)

        DropdownField(
            label    = "Country",
            options  = countries,
            value    = countryCode,
            onSelect = { countryCode = it }
        )
        DropdownField(
            label    = "Document type",
            options  = DocumentType.values().map { it.name },
            value    = documentType.name,
            onSelect = { documentType = DocumentType.valueOf(it) }
        )
        DropdownField(
            label    = "Document state",
            options  = DocumentState.values().map { it.name },
            value    = documentState.name,
            onSelect = { documentState = DocumentState.valueOf(it) }
        )

        HorizontalDivider()

        // ── MRZ ─────────────────────────────────────────────────────────────
        Text("MRZ ↔ Visual mismatch", style = MaterialTheme.typography.titleSmall)

        DropdownField(
            label    = "MRZ mismatch",
            options  = MrzMismatch.values().map { it.name },
            value    = mrzMismatch.name,
            onSelect = { mrzMismatch = MrzMismatch.valueOf(it) }
        )

        HorizontalDivider()

        // ── NFC chip ────────────────────────────────────────────────────────
        Text("NFC chip", style = MaterialTheme.typography.titleSmall)

        DropdownField(
            label    = "NFC mode",
            options  = NfcMode.values().map { it.label },
            value    = nfcMode.label,
            onSelect = { label -> nfcMode = NfcMode.values().first { it.label == label } }
        )

        if (nfcMode != NfcMode.NONE) {
            DropdownField(
                label    = "Chip state",
                options  = NfcChipState.values().map { it.name },
                value    = nfcChipState.name,
                onSelect = { nfcChipState = NfcChipState.valueOf(it) }
            )
            DropdownField(
                label    = "Face photo (chip)",
                options  = FacePhotoState.values().map { it.name },
                value    = facePhotoState.name,
                onSelect = { facePhotoState = FacePhotoState.valueOf(it) }
            )
        }

        if (nfcMode == NfcMode.THIRD_PARTY) {
            DropdownField(
                label    = "Third-party country",
                options  = countries,
                value    = thirdPartyCode,
                onSelect = { thirdPartyCode = it }
            )
        }

        HorizontalDivider()

        // ── Extra data ──────────────────────────────────────────────────────
        Text("Extra test data", style = MaterialTheme.typography.titleSmall)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f)
            ) {
                Checkbox(checked = includeCard, onCheckedChange = { includeCard = it })
                Text("Payment card", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f)
            ) {
                Checkbox(checked = includeIban, onCheckedChange = { includeIban = it })
                Text("IBAN", style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()

        // ── Suite buttons ───────────────────────────────────────────────────
        Text("Generate suite for $countryCode", style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick  = { vm.generateNfcSuite(countryCode) },
                enabled  = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("NFC") }
            OutlinedButton(
                onClick  = { vm.generateStateSuite(countryCode) },
                enabled  = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("States") }
            OutlinedButton(
                onClick  = { vm.generateFullSuite(countryCode) },
                enabled  = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("Full") }
        }

        // ── Generate single ─────────────────────────────────────────────────
        Button(
            onClick = {
                val nfcRequest = when (nfcMode) {
                    NfcMode.NONE           -> NfcRequest.None
                    NfcMode.SAME_AS_HOLDER -> NfcRequest.SameAsHolder(
                        chipState = nfcChipState,
                        faceState = facePhotoState
                    )
                    NfcMode.THIRD_PARTY    -> NfcRequest.ThirdParty(
                        thirdPartyCountry = thirdPartyCode,
                        chipState         = nfcChipState,
                        faceState         = facePhotoState
                    )
                }
                vm.generate(
                    DocumentRequest(
                        countryCode  = countryCode,
                        documentType = documentType,
                        state        = documentState,
                        mrzMismatch  = mrzMismatch,
                        nfc          = nfcRequest,
                        includeCard  = includeCard,
                        includeIban  = includeIban
                    )
                )
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Generate identity")
        }

        // ── Error card ──────────────────────────────────────────────────────
        if (uiState is UiState.Error) {
            Card(
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = (uiState as UiState.Error).message,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = vm::dismissError) { Text("OK") }
                }
            }
        }
    }
}

// ── NFC mode UI enum (maps to NfcRequest) ──────────────────────────────────

private enum class NfcMode(val label: String) {
    NONE("No chip"),
    SAME_AS_HOLDER("Same as document holder"),
    THIRD_PARTY("Third-party identity")
}

// ── Generic dropdown ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
            value         = value,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier
                .menuAnchor()
                .fillMaxWidth()
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
