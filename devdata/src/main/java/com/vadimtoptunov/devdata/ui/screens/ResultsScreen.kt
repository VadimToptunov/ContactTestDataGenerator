package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.db.RunResultEntity
import com.vadimtoptunov.devdata.ui.IdentityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays all [RunResultEntity] records for a test suite.
 *
 * Shows pass/fail rate and a per-identity result list with timestamps,
 * expected vs actual outcomes, and tester notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    suiteId:  String,
    onBack:   () -> Unit,
    vm:       IdentityViewModel = viewModel()
) {
    val suites  by vm.suites.collectAsState()
    val suite   = suites.firstOrNull { it.id == suiteId }

    val results by vm.observeResults(suiteId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(suite?.name ?: "Results")
                        Text(
                            "${results.size} runs · ${suite?.passCount ?: 0} pass · ${suite?.failCount ?: 0} fail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (results.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No results yet.\nRun an NFC session and record a result.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Summary bar ────────────────────────────────────────────────
            suite?.let { s ->
                val total = (s.passCount + s.failCount).coerceAtLeast(1)
                val ratio = s.passCount.toFloat() / total
                LinearProgressIndicator(
                    progress    = { ratio },
                    modifier    = Modifier.fillMaxWidth(),
                    color       = MaterialTheme.colorScheme.primary,
                    trackColor  = MaterialTheme.colorScheme.errorContainer
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pass: ${s.passCount}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Fail: ${s.failCount}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            // ── Result list ────────────────────────────────────────────────
            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { result ->
                    ResultCard(result)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: RunResultEntity) {
    val passed = result.passed

    val containerColor = when (passed) {
        true  -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null  -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = when (passed) {
                        true  -> "PASS"
                        false -> "FAIL"
                        null  -> "PENDING"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = when (passed) {
                        true  -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null  -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text  = formatDate(result.runAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ResultRow("Expected", result.expectedOutcome)
            result.actualOutcome?.let { ResultRow("Actual", it) }

            if (result.notes.isNotBlank()) {
                Text(
                    result.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date(ms))
