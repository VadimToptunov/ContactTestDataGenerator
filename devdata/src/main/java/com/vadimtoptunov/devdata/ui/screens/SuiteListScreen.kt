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
 * Lists all saved test suites (NFC suites, state suites, etc.).
 */
@Composable
fun SuiteListScreen(vm: IdentityViewModel = viewModel()) {
    val suites by vm.suites.collectAsState()

    if (suites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No suites yet.\nUse 'NFC suite' or 'State suite' on the Generate screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suites, key = { it.id }) { suite ->
            SuiteCard(suite = suite, onDelete = { vm.deleteSuite(suite) })
        }
    }
}

@Composable
private fun SuiteCard(suite: TestSuiteEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(suite.name, style = MaterialTheme.typography.titleSmall)
                Text(suite.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${suite.totalCount} identities · created ${
                    java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(suite.createdAt))
                }",
                    style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete suite")
            }
        }
    }
}
