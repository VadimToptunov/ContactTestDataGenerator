package com.vadimtoptunov.contacttestdatagenerator.devtools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vadimtoptunov.generators.core.GeneratorRegistry
import com.vadimtoptunov.generators.core.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Entry point for the Developer Tools Factory: a registry-driven picker of every
 * generator in the library, plus a per-tool screen to configure and export data.
 *
 * @param isPremium         whether the user has unlocked premium categories.
 * @param onRequestUpgrade  invoked when a locked category is tapped.
 * @param onExit            leaves the Developer Tools section entirely.
 */
@Composable
fun DevToolsFlow(
    isPremium: Boolean,
    onRequestUpgrade: () -> Unit,
    onExit: () -> Unit,
) {
    var selectedTool by remember { mutableStateOf<DevTool?>(null) }

    BackHandler {
        if (selectedTool != null) selectedTool = null else onExit()
    }

    val currentTool = selectedTool
    if (currentTool == null) {
        DevToolsPickerScreen(
            isPremium = isPremium,
            onToolSelected = { tool ->
                if (tool.category.requiresPremium && !isPremium) onRequestUpgrade()
                else selectedTool = tool
            },
            onBack = onExit,
        )
    } else {
        DevToolDetailScreen(
            tool = currentTool,
            onBack = { selectedTool = null },
        )
    }
}

// ── Picker ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevToolsPickerScreen(
    isPremium: Boolean,
    onToolSelected: (DevTool) -> Unit,
    onBack: () -> Unit,
) {
    val toolsByCategory = remember { DevToolsCatalog.toolsByCategory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Tools") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            item {
                Text(
                    text = "Generate realistic test data — cards, IBANs, IPs, JWTs, " +
                        "passwords, addresses and more — then export as CSV, JSON, SQL or text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            for ((category, tools) in toolsByCategory) {
                val locked = category.requiresPremium && !isPremium
                item(key = "header_${category.name}") {
                    CategoryHeader(category = category, locked = locked)
                }
                items(items = tools, key = { it.id }) { tool ->
                    DevToolCard(
                        tool = tool,
                        locked = locked,
                        onClick = { onToolSelected(tool) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: DevToolCategory, locked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = category.emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (locked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Premium",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.height(18.dp),
            )
            Text(
                text = "Premium",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun DevToolCard(tool: DevTool, locked: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tool.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.height(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tool.supportedFormats.forEach { format ->
                    FormatChip(format)
                }
            }
        }
    }
}

@Composable
private fun FormatChip(format: OutputFormat) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = format.extension.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// ── Per-tool config + result ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevToolDetailScreen(tool: DevTool, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val generator = remember(tool.id) { GeneratorRegistry.find(tool.id) }

    var countText by remember { mutableStateOf("5") }
    var seedText by remember { mutableStateOf("") }
    var selectedFormat by remember(tool.id) { mutableStateOf(tool.supportedFormats.first()) }
    var output by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = countText,
                onValueChange = { newValue -> countText = newValue.filter { it.isDigit() }.take(5) },
                label = { Text("How many") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = seedText,
                onValueChange = { newValue -> seedText = newValue.filter { it.isDigit() }.take(18) },
                label = { Text("Seed (optional — same seed reproduces the same data)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Output format",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tool.supportedFormats.forEach { format ->
                    FilterChip(
                        selected = format == selectedFormat,
                        onClick = { selectedFormat = format },
                        label = { Text(format.label) },
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val count = countText.toIntOrNull()?.coerceIn(1, MAX_RECORDS) ?: 1
                    countText = count.toString()
                    val seed = seedText.toLongOrNull()
                    val activeGenerator = generator
                    if (activeGenerator == null) {
                        errorMessage = "This tool is unavailable."
                        return@OutlinedButton
                    }
                    isGenerating = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.Default) {
                                DevToolsExporter.generateSerialized(activeGenerator, count, selectedFormat, seed)
                            }
                        }
                        isGenerating = false
                        result
                            .onSuccess { output = it }
                            .onFailure { errorMessage = it.message ?: "Generation failed" }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(0.dp))
                    Text("  Generating…")
                } else {
                    Text("Generate")
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            output?.let { generated ->
                ResultSection(
                    output = generated,
                    onCopy = { clipboard.setText(AnnotatedString(generated)) },
                    onShare = {
                        val file = DevToolsExporter.writeToFile(context, tool.id, generated, selectedFormat)
                        DevToolsExporter.shareFile(context, file, selectedFormat)
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultSection(output: String, onCopy: () -> Unit, onShare: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Result", style = MaterialTheme.typography.labelLarge)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = output,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                Text("Copy")
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
                Text("  Share")
            }
        }
    }
}

/** Upper bound on how many records a single generation produces. */
private const val MAX_RECORDS = 10_000
