package com.vadimtoptunov.contacttestdatagenerator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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
    val fieldSettings by viewModel.settingsRepository.settings.collectAsStateWithLifecycle()
    val templates by viewModel.templateRepository.templates.collectAsStateWithLifecycle()
    val batchState by viewModel.batchState.collectAsStateWithLifecycle()
    val batchJobs by viewModel.batchProcessor.currentBatch.collectAsStateWithLifecycle()
    
    var contactCount by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var historyExpanded by remember { mutableStateOf(false) }
    var templatesExpanded by remember { mutableStateOf(false) }
    var batchExpanded by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showAddBatchJobDialog by remember { mutableStateOf(false) }
    var batchJobsList by remember { mutableStateOf<List<BatchJob>>(emptyList()) }
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    // File picker for template import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importTemplate(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, context.getString(R.string.templates_imported), Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "${context.getString(R.string.templates_import_error)}: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

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
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
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
                        if (count != null && count > 0 && count <= maxContacts) {
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
            
            // Save Template Button
            OutlinedButton(
                onClick = { showSaveTemplateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState is UiState.Idle && contactCount.isNotEmpty() && validationError == null
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.templates_save))
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
            
            // Templates Section
            Spacer(modifier = Modifier.height(8.dp))
            TemplatesSection(
                templates = templates,
                expanded = templatesExpanded,
                onExpandToggle = { templatesExpanded = !templatesExpanded },
                onLoad = { template ->
                    viewModel.loadTemplate(template)
                    contactCount = template.contactCount.toString()
                    Toast.makeText(context, "Template \"${template.name}\" loaded", Toast.LENGTH_SHORT).show()
                },
                onDelete = { viewModel.deleteTemplate(it.id) },
                onExport = { template ->
                    viewModel.exportTemplate(
                        template = template,
                        onSuccess = { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                type = "application/json"
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Template"))
                        },
                        onError = { error ->
                            Toast.makeText(context, "${context.getString(R.string.templates_export_error)}: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onImport = { importLauncher.launch("application/json") }
            )
            
            // Batch Processing Section
            Spacer(modifier = Modifier.height(8.dp))
            BatchProcessingSection(
                jobs = batchJobsList,
                batchState = batchState,
                expanded = batchExpanded,
                onExpandToggle = { batchExpanded = !batchExpanded },
                onAddJob = { showAddBatchJobDialog = true },
                onRemoveJob = { job -> batchJobsList = batchJobsList.filter { it.id != job.id } },
                onStartBatch = {
                    if (batchJobsList.isNotEmpty()) {
                        viewModel.startBatchProcessing(batchJobsList)
                    }
                },
                onCancelBatch = { viewModel.cancelBatch() },
                onClearBatch = {
                    viewModel.resetBatchState()
                    batchJobsList = emptyList()
                },
                templates = templates,
                onCreateFromTemplate = { template ->
                    val newJob = BatchJob(
                        name = template.name,
                        contactCount = template.contactCount,
                        fieldSettings = template.fieldSettings
                    )
                    batchJobsList = batchJobsList + newJob
                }
            )
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
        
        // Settings Dialog
        if (showSettingsDialog) {
            FieldSettingsDialog(
                settings = fieldSettings,
                onDismiss = { showSettingsDialog = false },
                onSave = { newSettings ->
                    viewModel.settingsRepository.updateSettings(newSettings)
                    showSettingsDialog = false
                }
            )
        }
        
        // Save Template Dialog
        if (showSaveTemplateDialog) {
            SaveTemplateDialog(
                defaultCount = contactCount.toIntOrNull() ?: 100,
                onDismiss = { showSaveTemplateDialog = false },
                onSave = { name, count ->
                    viewModel.saveTemplate(name, count)
                    Toast.makeText(context, "Template \"$name\" saved", Toast.LENGTH_SHORT).show()
                    showSaveTemplateDialog = false
                }
            )
        }
        
        // Add Batch Job Dialog
        if (showAddBatchJobDialog) {
            AddBatchJobDialog(
                onDismiss = { showAddBatchJobDialog = false },
                onAdd = { name, count, settings ->
                    val newJob = BatchJob(
                        name = name,
                        contactCount = count,
                        fieldSettings = settings
                    )
                    batchJobsList = batchJobsList + newJob
                    showAddBatchJobDialog = false
                },
                currentSettings = fieldSettings
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

@Composable
fun FieldSettingsDialog(
    settings: ContactFieldSettings,
    onDismiss: () -> Unit,
    onSave: (ContactFieldSettings) -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings) }
    val atLeastOneSelected = tempSettings.includeName || tempSettings.includePhone || 
                              tempSettings.includeEmail || tempSettings.includeCompany || 
                              tempSettings.includeJobTitle
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(32.dp))
        },
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
                    text = stringResource(R.string.settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_field_name))
                    Switch(checked = tempSettings.includeName, onCheckedChange = { tempSettings = tempSettings.copy(includeName = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_field_phone))
                    Switch(checked = tempSettings.includePhone, onCheckedChange = { tempSettings = tempSettings.copy(includePhone = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_field_email))
                    Switch(checked = tempSettings.includeEmail, onCheckedChange = { tempSettings = tempSettings.copy(includeEmail = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_field_company))
                    Switch(checked = tempSettings.includeCompany, onCheckedChange = { tempSettings = tempSettings.copy(includeCompany = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_field_job_title))
                    Switch(checked = tempSettings.includeJobTitle, onCheckedChange = { tempSettings = tempSettings.copy(includeJobTitle = it) })
                }
                
                if (!atLeastOneSelected) {
                    Text(text = stringResource(R.string.settings_at_least_one), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempSettings) }, enabled = atLeastOneSelected) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SaveTemplateDialog(
    defaultCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, count: Int) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(32.dp)) },
        title = { Text(stringResource(R.string.templates_save_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.templates_save_dialog_description), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { 
                        templateName = it
                        isError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.templates_save_dialog_hint)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (templateName.isNotBlank()) {
                        onSave(templateName, defaultCount)
                    } else {
                        isError = true
                    }
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesSection(
    templates: List<ContactTemplate>,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onLoad: (ContactTemplate) -> Unit,
    onDelete: (ContactTemplate) -> Unit,
    onExport: (ContactTemplate) -> Unit,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = onImport) {
                        Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.templates_import))
                    }
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            if (expanded) {
                if (templates.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.templates_empty), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.templates_empty_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(templates) { template ->
                            TemplateItem(template = template, onLoad = onLoad, onDelete = onDelete, onExport = onExport)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateItem(
    template: ContactTemplate,
    onLoad: (ContactTemplate) -> Unit,
    onDelete: (ContactTemplate) -> Unit,
    onExport: (ContactTemplate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(template.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${template.contactCount} contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = { onLoad(template) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.templates_load), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onExport(template) }) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.templates_export), tint = MaterialTheme.colorScheme.tertiary)
            }
            IconButton(onClick = { onDelete(template) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.templates_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddBatchJobDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, count: Int, settings: ContactFieldSettings) -> Unit,
    currentSettings: ContactFieldSettings
) {
    var jobName by remember { mutableStateOf("") }
    var jobCount by remember { mutableStateOf("100") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        title = { Text(stringResource(R.string.batch_add_job)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jobName,
                    onValueChange = { jobName = it },
                    label = { Text(stringResource(R.string.batch_job_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jobCount,
                    onValueChange = { jobCount = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.batch_job_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = jobCount.toIntOrNull()
                    if (jobName.isNotBlank() && count != null && count > 0) {
                        onAdd(jobName, count, currentSettings)
                    }
                },
                enabled = jobName.isNotBlank() && (jobCount.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProcessingSection(
    jobs: List<BatchJob>,
    batchState: BatchState,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onAddJob: () -> Unit,
    onRemoveJob: (BatchJob) -> Unit,
    onStartBatch: () -> Unit,
    onCancelBatch: () -> Unit,
    onClearBatch: () -> Unit,
    templates: List<ContactTemplate>,
    onCreateFromTemplate: (ContactTemplate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(stringResource(R.string.batch_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (jobs.isNotEmpty()) {
                        Badge { Text("${jobs.size}") }
                    }
                }
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onAddJob, modifier = Modifier.weight(1f), enabled = batchState !is BatchState.Processing) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.batch_add_job))
                    }
                    if (batchState is BatchState.Processing) {
                        Button(onClick = onCancelBatch, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.batch_cancel))
                        }
                    } else if (jobs.isNotEmpty()) {
                        Button(onClick = onStartBatch, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.batch_start))
                        }
                    }
                }
                
                when (batchState) {
                    is BatchState.Processing -> {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.batch_processing), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.batch_job_progress, batchState.currentJobIndex + 1, batchState.totalJobs))
                                LinearProgressIndicator(progress = { batchState.overallProgress / 100f }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    is BatchState.Completed -> {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.batch_completed), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.batch_success_count, batchState.successCount))
                                if (batchState.failedCount > 0) {
                                    Text(stringResource(R.string.batch_failed_count, batchState.failedCount), color = MaterialTheme.colorScheme.error)
                                }
                                Button(onClick = onClearBatch, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.batch_clear))
                                }
                            }
                        }
                    }
                    else -> {}
                }
                
                if (jobs.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(jobs) { job ->
                            BatchJobItem(job = job, onRemove = onRemoveJob, canRemove = batchState !is BatchState.Processing)
                            HorizontalDivider()
                        }
                    }
                } else if (batchState is BatchState.Idle) {
                    Text(text = stringResource(R.string.batch_empty), modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun BatchJobItem(job: BatchJob, onRemove: (BatchJob) -> Unit, canRemove: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${job.contactCount} contacts", style = MaterialTheme.typography.bodySmall)
            if (job.status == BatchJobStatus.RUNNING) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Text("${job.progress}%", style = MaterialTheme.typography.bodySmall)
                }
            } else if (job.status == BatchJobStatus.COMPLETED) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            } else if (job.status == BatchJobStatus.FAILED) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
        if (canRemove) {
            IconButton(onClick = { onRemove(job) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
