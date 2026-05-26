package com.vadimtoptunov.devdata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vadimtoptunov.devdata.db.TestSuiteEntity
import com.vadimtoptunov.devdata.ui.IdentityViewModel

/**
 * Lists all saved test suites.
 * Tapping a suite navigates to its run results history.
 */
@Composable
fun SuiteListScreen(
    onOpenResults: (suiteId: String) -> Unit = {},
    vm:            IdentityViewModel = viewModel()
) {
    val suites by vm.suites.collectAsState()

    if (suites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No suites yet.\nUse 'NFC suite' or 'State suite' on the Generate screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suites, key = { it.id }) { suite ->
            SuiteCard(
                suite    = suite,
                onDelete  = { vm.deleteSuite(suite) },
                onResults = { onOpenResults(suite.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuiteCard(
    suite:     TestSuiteEntity,
    onDelete:  () -> Unit,
    onResults: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onResults
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(suite.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    suite.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${suite.totalCount} identities · " +
                    "${suite.passCount} pass · ${suite.failCount} fail · " +
                    "created ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(suite.createdAt))}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete suite")
            }
        }
    }
}
