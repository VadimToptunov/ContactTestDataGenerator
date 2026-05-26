package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.ui.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Live NFC session view — shows APDU log while the phone emulates an eMRTD chip.
 *
 * The log entries are collected from [NfcSessionLog] which is a global
 * singleton updated by [HcePassportService] via a simple SharedFlow.
 *
 * UI also lets the tester manually record a pass/fail result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcSessionScreen(
    identityId: String,
    suiteId:    String?,
    onBack:     () -> Unit,
    vm:         IdentityViewModel = viewModel()
) {
    val identities   by vm.identities.collectAsState()
    val activeChipId by vm.activeChipIdentityId.collectAsState()

    val entity   = identities.firstOrNull { it.id == identityId }
    val isActive = identityId == activeChipId

    val logEntries = remember { mutableStateListOf<ApduLogEntry>() }
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    // Observe the global APDU log from HcePassportService
    LaunchedEffect(Unit) {
        NfcSessionLog.entries.collect { entry ->
            logEntries.add(entry)
            scope.launch {
                listState.animateScrollToItem(logEntries.lastIndex.coerceAtLeast(0))
            }
        }
    }

    var showResultDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NFC session")
                        entity?.let {
                            Text(
                                "${it.countryCode} · ${it.documentType} · ${it.nfcChipState}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { logEntries.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear log")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Status bar ─────────────────────────────────────────────────
            StatusBar(isActive, entity?.nfcChipState)

            // ── APDU log ───────────────────────────────────────────────────
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (logEntries.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isActive) "Waiting for NFC reader…"
                                else "Load the chip to start emulation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(logEntries) { entry ->
                        ApduLogRow(entry)
                    }
                }
            }

            // ── Controls ───────────────────────────────────────────────────
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    OutlinedButton(
                        onClick  = { vm.deactivateHce() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Unload chip") }
                } else {
                    Button(
                        onClick  = { vm.activateForHce(identityId) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Load chip") }
                }
                if (suiteId != null) {
                    OutlinedButton(
                        onClick  = { showResultDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled  = logEntries.isNotEmpty()
                    ) { Text("Record result") }
                }
            }
        }
    }

    if (showResultDialog && suiteId != null) {
        RecordResultDialog(
            onDismiss = { showResultDialog = false },
            onConfirm = { expected, actual, passed, notes ->
                vm.recordScanResult(identityId, suiteId, expected, actual, passed, notes)
                showResultDialog = false
            }
        )
    }
}

// ── Status bar ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(isActive: Boolean, chipState: String?) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
             else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            if (isActive) "● Emulating chip" else "○ Chip not loaded",
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
        chipState?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

// ── APDU log row ───────────────────────────────────────────────────────────

@Composable
private fun ApduLogRow(entry: ApduLogEntry) {
    val color = when (entry.direction) {
        ApduDirection.IN  -> MaterialTheme.colorScheme.primary
        ApduDirection.OUT -> MaterialTheme.colorScheme.tertiary
        ApduDirection.ERR -> MaterialTheme.colorScheme.error
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = if (entry.direction == ApduDirection.IN) "→" else "←",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text  = entry.hex,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = entry.annotation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Record result dialog ────────────────────────────────────────────────────

@Composable
private fun RecordResultDialog(
    onDismiss: () -> Unit,
    onConfirm: (expected: String, actual: String, passed: Boolean, notes: String) -> Unit
) {
    var expected by remember { mutableStateOf("READABLE") }
    var actual   by remember { mutableStateOf("READABLE") }
    var passed   by remember { mutableStateOf(true) }
    var notes    by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record scan result") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = expected,
                    onValueChange = { expected = it },
                    label         = { Text("Expected outcome") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = actual,
                    onValueChange = { actual = it },
                    label         = { Text("Actual outcome") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = passed, onCheckedChange = { passed = it })
                    Text("Passed")
                }
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notes (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(expected, actual, passed, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── APDU log model ─────────────────────────────────────────────────────────

enum class ApduDirection { IN, OUT, ERR }

data class ApduLogEntry(
    val direction:  ApduDirection,
    val hex:        String,
    val annotation: String = ""
)

/**
 * Global SharedFlow that [HcePassportService] writes to and [NfcSessionScreen] reads from.
 * Kept here so the screen has a single import rather than referencing the service directly.
 */
object NfcSessionLog {
    private val _entries = kotlinx.coroutines.flow.MutableSharedFlow<ApduLogEntry>(
        extraBufferCapacity = 256
    )
    val entries: kotlinx.coroutines.flow.SharedFlow<ApduLogEntry> = _entries

    fun emit(entry: ApduLogEntry) { _entries.tryEmit(entry) }
}
