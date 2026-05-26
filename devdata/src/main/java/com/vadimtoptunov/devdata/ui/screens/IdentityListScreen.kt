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
import com.vadimtoptunov.devdata.db.IdentityEntity
import com.vadimtoptunov.devdata.ui.IdentityViewModel

/**
 * Lists all persisted standalone identities.
 * Each row shows key metadata and buttons to view detail, start an NFC session, or delete.
 */
@Composable
fun IdentityListScreen(
    onOpenDetail: (id: String) -> Unit = {},
    onOpenNfc:    (id: String) -> Unit = {},
    vm:           IdentityViewModel = viewModel()
) {
    val identities   by vm.identities.collectAsState()
    val activeChipId by vm.activeChipIdentityId.collectAsState()

    if (identities.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No identities yet.\nUse Generate to create one.",
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
        items(identities, key = { it.id }) { entity ->
            IdentityCard(
                entity       = entity,
                isActive     = entity.id == activeChipId,
                onActivate   = { vm.activateForHce(entity.id) },
                onDeactivate = { vm.deactivateHce() },
                onDelete     = { vm.deleteIdentity(entity.id) },
                onDetail     = { onOpenDetail(entity.id) },
                onNfc        = { onOpenNfc(entity.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentityCard(
    entity:       IdentityEntity,
    isActive:     Boolean,
    onActivate:   () -> Unit,
    onDeactivate: () -> Unit,
    onDelete:     () -> Unit,
    onDetail:     () -> Unit,
    onNfc:        () -> Unit
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
        onClick  = onDetail
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${entity.countryCode} · ${entity.documentType}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        entity.documentState,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "NFC: ${entity.nfcChipState} · ${entity.chipIdentitySrc}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entity.hasNfcChip) {
                    if (isActive) {
                        OutlinedButton(
                            onClick  = onDeactivate,
                            modifier = Modifier.weight(1f)
                        ) { Text("Unload chip") }
                    } else {
                        Button(
                            onClick  = onActivate,
                            modifier = Modifier.weight(1f)
                        ) { Text("Load to NFC") }
                    }
                    OutlinedButton(
                        onClick  = onNfc,
                        modifier = Modifier.weight(1f)
                    ) { Text("Session") }
                } else {
                    Text(
                        "No NFC chip",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                    )
                }
            }

            if (isActive) {
                Text(
                    "● Active — phone is emulating this chip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
