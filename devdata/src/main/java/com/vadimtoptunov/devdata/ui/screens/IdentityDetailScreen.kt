package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.db.IdentityEntity
import com.vadimtoptunov.devdata.ui.IdentityViewModel
import com.vadimtoptunov.generators.identity.DocumentDiscrepancy
import com.vadimtoptunov.generators.identity.SyntheticIdentity
import kotlinx.serialization.json.Json

/**
 * Shows full detail for a single persisted identity:
 *   • MRZ lines (monospace)
 *   • NFC chip data: source, state, face photo state
 *   • All detected discrepancies (MRZ↔VIZ, Chip↔Document)
 *   • Load / unload chip button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityDetailScreen(
    identityId:  String,
    onBack:      () -> Unit,
    vm:          IdentityViewModel = viewModel()
) {
    val identities   by vm.identities.collectAsState()
    val activeChipId by vm.activeChipIdentityId.collectAsState()

    val entity = identities.firstOrNull { it.id == identityId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (entity == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Identity not found.") }
            return@Scaffold
        }

        val identity = remember(entity.json) {
            runCatching { Json.decodeFromString<SyntheticIdentity>(entity.json) }.getOrNull()
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            HeaderSection(entity)

            // ── MRZ ────────────────────────────────────────────────────────
            val doc = identity?.primaryDocument
            val mrz = doc?.mrzData
            if (mrz != null) {
                SectionCard("MRZ") {
                    Text(
                        text      = mrz.raw,
                        style     = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    if (mrz.raw.length > 44) {
                        // TD3 (2×44) or TD1 (3×30) — split into lines visually
                        Spacer(Modifier.height(4.dp))
                        val lineLen = if (mrz.raw.length == 88) 44 else 30
                        mrz.raw.chunked(lineLen).forEachIndexed { i, line ->
                            Text(
                                text       = "${i + 1}: $line",
                                style      = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── NFC chip ───────────────────────────────────────────────────
            val chip = doc?.nfcChip
            if (chip != null) {
                SectionCard("NFC chip") {
                    LabeledRow("State",        chip.chipState.name)
                    LabeledRow("Identity src", chip.identitySource.name)
                    LabeledRow("Face photo",   chip.facePhotoState.name)
                    chip.chipMrz?.let { chipMrz ->
                        Spacer(Modifier.height(4.dp))
                        Text("Chip MRZ:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text       = chipMrz.raw,
                            style      = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Discrepancies ──────────────────────────────────────────────
            val discrepancies = doc?.discrepancies ?: emptyList()
            if (discrepancies.isNotEmpty()) {
                SectionCard("Discrepancies (${discrepancies.size})") {
                    discrepancies.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    discrepancyLabel(d),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                discrepancyDetail(d),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── HCE control ────────────────────────────────────────────────
            if (entity.hasNfcChip) {
                val isActive = entity.id == activeChipId
                if (isActive) {
                    OutlinedButton(
                        onClick  = { vm.deactivateHce() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Unload chip") }
                } else {
                    Button(
                        onClick  = { vm.activateForHce(entity.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Load to NFC chip") }
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(entity: IdentityEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "${entity.countryCode} · ${entity.documentType}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "State: ${entity.documentState}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "NFC: ${entity.nfcChipState} · src: ${entity.chipIdentitySrc}",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            "Scenario: ${entity.scenarioId}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun discrepancyLabel(d: DocumentDiscrepancy): String = when (d) {
    is DocumentDiscrepancy.MrzNameVsVisual      -> "MRZ/VIZ"
    is DocumentDiscrepancy.MrzDobVsVisual       -> "MRZ/VIZ"
    is DocumentDiscrepancy.MrzNumberVsVisual    -> "MRZ/VIZ"
    is DocumentDiscrepancy.MrzExpiryVsVisual    -> "MRZ/VIZ"
    is DocumentDiscrepancy.ChipMrzVsPrinted     -> "Chip/MRZ"
    is DocumentDiscrepancy.ChipIdentityVsDocument -> "Chip/Doc"
    is DocumentDiscrepancy.ChipFaceVsDocument   -> "Chip/Face"
}

private fun discrepancyDetail(d: DocumentDiscrepancy): String = when (d) {
    is DocumentDiscrepancy.MrzNameVsVisual      ->
        "MRZ name \"${d.mrzValue}\" differs from visual \"${d.visualValue}\""
    is DocumentDiscrepancy.MrzDobVsVisual       ->
        "MRZ date-of-birth \"${d.mrzValue}\" differs from visual \"${d.visualValue}\""
    is DocumentDiscrepancy.MrzNumberVsVisual    ->
        "MRZ doc number \"${d.mrzValue}\" differs from visual \"${d.visualValue}\""
    is DocumentDiscrepancy.MrzExpiryVsVisual    ->
        "MRZ expiry \"${d.mrzValue}\" differs from visual \"${d.visualValue}\""
    is DocumentDiscrepancy.ChipMrzVsPrinted     ->
        "Chip MRZ differs from printed MRZ (field: ${d.field})"
    is DocumentDiscrepancy.ChipIdentityVsDocument ->
        "Chip holds identity from ${d.chipCountry} — document country is ${d.documentCountry}"
    is DocumentDiscrepancy.ChipFaceVsDocument   ->
        "Chip face: ${d.chipFaceState} — does not match document holder"
}
