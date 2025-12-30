package com.vadimtoptunov.contacttestdatagenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vadimtoptunov.contacttestdatagenerator.ui.theme.ContactTestDataGeneratorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactTestDataGeneratorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileHistory by viewModel.fileHistory.collectAsStateWithLifecycle()
    val isPremium by viewModel.billingManager.isPremium.collectAsStateWithLifecycle()
    val purchaseState by viewModel.billingManager.purchaseState.collectAsStateWithLifecycle()
    
    var contactCount by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var historyExpanded by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.app_title))
                        if (isPremium) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = stringResource(R.string.premium_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isPremium) {
                        IconButton(onClick = { showPremiumDialog = true }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.premium_title),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and description
            Text(
                text = stringResource(R.string.vcf_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Input field
            val maxContacts = if (isPremium) 10000 else 1000
            OutlinedTextField(
                value = contactCount,
                onValueChange = { newValue ->
                    contactCount = newValue.filter { it.isDigit() }
                    
                    // Update validation error
                    validationError = when {
                        contactCount.isEmpty() -> null
                        contactCount.toIntOrNull() == null -> stringResource(R.string.error_field_invalid)
                        contactCount.toInt() == 0 -> stringResource(R.string.error_field_zero)
                        contactCount.toInt() > maxContacts -> if (isPremium) {
                            stringResource(R.string.error_field_too_large)
                        } else {
                            stringResource(R.string.premium_limit_reached)
                        }
                        else -> null
                    }
                },
                label = { Text(stringResource(R.string.contacts_quantity)) },
                placeholder = { Text("Max: ${if (isPremium) "10,000 (Pro)" else "1,000 (Free)"}") },
                supportingText = {
                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState is UiState.Idle,
                singleLine = true,
                isError = validationError != null
            )
            
            // Generate button
            Button(
                onClick = {
                    if (contactCount.isEmpty()) {
                        validationError = stringResource(R.string.error_field_empty)
                    } else {
                        val count = contactCount.toIntOrNull()
                        if (count != null && count > 0 && count <= 10000) {
                            validationError = null
                            viewModel.startGenerating(count)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState is UiState.Idle
            ) {
                Text(stringResource(R.string.generate_btn_text))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress section
            AnimatedVisibility(
                visible = uiState is UiState.Loading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState is UiState.Loading) {
                    val loadingState = uiState as UiState.Loading
                    ProgressSection(
                        current = loadingState.current,
                        total = loadingState.total,
                        progress = loadingState.progress,
                        onCancel = { viewModel.cancelGeneration() }
                    )
                }
            }
            
            // Success section
            AnimatedVisibility(
                visible = uiState is UiState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState is UiState.Success) {
                    val successState = uiState as UiState.Success
                    SuccessSection(
                        message = successState.message,
                        onShare = { viewModel.shareVcfFile(successState.fileUri) },
                        onDismiss = { 
                            viewModel.resetState()
                            contactCount = ""
                        }
                    )
                }
            }
            
            // Error section
            AnimatedVisibility(
                visible = uiState is UiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState is UiState.Error) {
                    val errorState = uiState as UiState.Error
                    ErrorSection(
                        message = errorState.message,
                        onDismiss = { viewModel.resetState() }
                    )
                }
            }
            
            // File History Section
            if (fileHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FileHistorySection(
                    history = fileHistory,
                    expanded = historyExpanded,
                    onExpandToggle = { historyExpanded = !historyExpanded },
                    onShare = { viewModel.shareVcfFile(it.uri) },
                    onDelete = { viewModel.deleteFile(it) }
                )
            }
        }
        
        // Premium Dialog
        if (showPremiumDialog) {
            PremiumDialog(
                onDismiss = { showPremiumDialog = false },
                onPurchase = {
                    activity?.let { viewModel.purchasePremium(it) }
                    showPremiumDialog = false
                }
            )
        }
    }
}

@Composable
fun ProgressSection(
    current: Int,
    total: Int,
    progress: Int,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_generating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_btn_text))
                }
            }
            
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.progress_creating, current, total),
                    style = MaterialTheme.typography.bodyMedium
                )
    Text(
                    text = stringResource(R.string.progress_percentage, progress),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SuccessSection(
    message: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.success_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.share_btn_text))
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.done_btn_text))
                }
            }
        }
    }
}

@Composable
fun ErrorSection(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.ok_btn_text))
            }
        }
    }
}

@Composable
fun FileHistorySection(
    history: List<VcfFileInfo>,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onShare: (VcfFileInfo) -> Unit,
    onDelete: (VcfFileInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${history.size} ${if (history.size == 1) "file" else "files"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // History list
            AnimatedVisibility(visible = expanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(history) { file ->
                        FileHistoryItem(
                            file = file,
                            onShare = { onShare(file) },
                            onDelete = { onDelete(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileHistoryItem(
    file: VcfFileInfo,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_contacts, file.contactCount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${file.fileSizeFormatted} • ${file.dateFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.history_share),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.history_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
