package com.runanywhere.kotlin_starter_example.kodent.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeValidator
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.services.ModelType
import com.runanywhere.kotlin_starter_example.ui.components.ModelLoaderWidget
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.AccentViolet
import com.runanywhere.kotlin_starter_example.ui.theme.PrimaryDark
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modelService: ModelService = viewModel(),
    kodentViewModel: KodentViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // Auto-load models
    LaunchedEffect(Unit) {
        if (!modelService.isLLMLoaded && !modelService.isLLMDownloading && !modelService.isLLMLoading) {
            modelService.downloadAndLoadLLM()
        }
        if (!modelService.isSTTLoaded && !modelService.isSTTDownloading && !modelService.isSTTLoading) {
            modelService.downloadAndLoadSTT()
        }
    }

    // Auto-scroll during streaming
    LaunchedEffect(kodentViewModel.analysisResult) {
        if (kodentViewModel.isAnalyzing) {
            delay(50)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kodent", fontWeight = FontWeight.Bold)
                        Text(
                            text = when {
                                kodentViewModel.isAnalyzing ->
                                    "⏳ Analyzing... ${kodentViewModel.tokenCount} tokens"
                                modelService.activeModel == ModelType.QUICK ->
                                    "🚀 Quick Mode"
                                modelService.activeModel == ModelType.DEEP ->
                                    "🧠 Deep Mode"
                                else -> "No model loaded"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (kodentViewModel.isAnalyzing) AccentOrange else TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (kodentViewModel.showHistory) {
                            kodentViewModel.toggleHistory()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Stop button during analysis
                    if (kodentViewModel.isAnalyzing) {
                        IconButton(onClick = { kodentViewModel.cancelAnalysis() }) {
                            Icon(Icons.Rounded.Stop, "Stop", tint = AccentPink)
                        }
                    }

                    // History button
                    if (kodentViewModel.history.isNotEmpty()) {
                        IconButton(onClick = { kodentViewModel.toggleHistory() }) {
                            Icon(
                                Icons.Rounded.History,
                                "History",
                                tint = if (kodentViewModel.showHistory) AccentCyan
                                else Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (kodentViewModel.showHistory) {
            HistoryContent(
                history = kodentViewModel.history,
                onItemClick = { kodentViewModel.loadFromHistory(it) },
                onClearClick = { kodentViewModel.clearHistory() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
            AnalyzerContent(
                kodentViewModel = kodentViewModel,
                modelService = modelService,
                scrollState = scrollState,
                hasAudioPermission = hasAudioPermission,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun AnalyzerContent(
    kodentViewModel: KodentViewModel,
    modelService: ModelService,
    scrollState: androidx.compose.foundation.ScrollState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {

        // Model loader
        if (!modelService.isLLMLoaded) {
            ModelLoaderWidget(
                modelName = "SmolLM2 360M",
                isDownloading = modelService.isLLMDownloading,
                isLoading = modelService.isLLMLoading,
                isLoaded = modelService.isLLMLoaded,
                downloadProgress = modelService.llmDownloadProgress,
                onLoadClick = { modelService.downloadAndLoadLLM() }
            )
            return
        }

        // ── Error Banner ──
        kodentViewModel.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AccentPink.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "❌ " + error,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentPink,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { kodentViewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Rounded.Close, "Dismiss", tint = AccentPink)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Language Selection ──
        Text(
            text = "Language: " + kodentViewModel.selectedLanguage,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val languages = listOf(
                "Kotlin", "Java", "Python", "JS", "C++", "Swift", "Dart", "Go"
            )
            languages.forEach { lang ->
                FilterChip(
                    selected = kodentViewModel.selectedLanguage == lang,
                    onClick = { kodentViewModel.updateLanguage(lang) },
                    label = { Text(lang, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentViolet.copy(alpha = 0.2f),
                        selectedLabelColor = AccentViolet
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Mode Selection ──
        Text(
            text = "Analysis Mode",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Explain" to "💡",
                "Debug" to "🐛",
                "Optimize" to "⚡",
                "Complexity" to "📊"
            ).forEach { (mode, emoji) ->
                FilterChip(
                    selected = kodentViewModel.selectedMode == mode,
                    onClick = { kodentViewModel.selectMode(mode) },
                    label = { Text(emoji + " " + mode) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = badgeColorFor(mode).copy(alpha = 0.2f),
                        selectedLabelColor = badgeColorFor(mode)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Code Input ──
        OutlinedTextField(
            value = kodentViewModel.codeInput,
            onValueChange = { kodentViewModel.updateCode(it) },
            label = { Text("Paste " + kodentViewModel.selectedLanguage + " code") },
            placeholder = {
                Text(
                    text = "Paste code here...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TextMuted.copy(alpha = 0.4f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                if (kodentViewModel.codeInput.isNotBlank()) {
                    IconButton(onClick = { kodentViewModel.clearInput() }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear code",
                            tint = TextMuted
                        )
                    }
                }
            }
        )

        // ── Code Detection Status ──
        if (kodentViewModel.codeInput.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            val charCount = kodentViewModel.codeInput.trim().length
            val isCode = CodeValidator.looksLikeCode(kodentViewModel.codeInput)

            Text(
                text = if (isCode) {
                    "✅ Looks like code • " + charCount + " chars"
                } else {
                    "⚠️ Doesn't look like code"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isCode) AccentGreen else AccentOrange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Action Buttons ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            // Analyze button
            Button(
                onClick = {
                    keyboardController?.hide()
                    kodentViewModel.analyze(modelService.activeModel)
                },
                modifier = Modifier.weight(1f),
                enabled = !kodentViewModel.isAnalyzing
                        && !kodentViewModel.isListening
                        && CodeValidator.looksLikeCode(kodentViewModel.codeInput)
            ) {
                if (kodentViewModel.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Text("🔍 Analyze")
                }
            }

            // Voice button
            Button(
                onClick = {
                    if (!hasAudioPermission) {
                        onRequestPermission()
                    } else if (kodentViewModel.isListening) {
                        kodentViewModel.stopListeningAndProcess(modelService.activeModel)
                    } else {
                        kodentViewModel.startListening()
                    }
                },
                enabled = !kodentViewModel.isAnalyzing && modelService.isSTTLoaded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        kodentViewModel.isListening -> AccentPink
                        kodentViewModel.isTranscribing -> AccentOrange
                        else -> AccentViolet
                    }
                )
            ) {
                if (kodentViewModel.isTranscribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        if (kodentViewModel.isListening) Icons.Rounded.Stop
                        else Icons.Rounded.Mic,
                        null
                    )
                }
            }
        }

        // ── Voice Status ──
        if (kodentViewModel.isListening) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "🎤 Listening... say: explain, debug, optimize, or complexity",
                style = MaterialTheme.typography.labelSmall,
                color = AccentPink
            )
        }
        if (kodentViewModel.speechText.isNotBlank() && !kodentViewModel.isListening) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                kodentViewModel.speechText,
                style = MaterialTheme.typography.labelSmall,
                color = AccentViolet
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Result Area ──
        if (kodentViewModel.analysisResult.isNotBlank()) {

            // Result header with metrics
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Result",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Metrics chip
                if (kodentViewModel.analysisTimeMs > 0) {
                    Surface(
                        color = AccentCyan.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        val metricsText = buildString {
                            append(kodentViewModel.tokenCount)
                            append(" tokens • ")
                            append(kodentViewModel.analysisTimeMs / 1000)
                            append("s")
                            if (kodentViewModel.tokensPerSecond > 0) {
                                append(" • ")
                                append(
                                    String.format("%.1f", kodentViewModel.tokensPerSecond)
                                )
                                append(" t/s")
                            }
                        }
                        Text(
                            text = metricsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Copy button
                IconButton(onClick = {
                    clipboardManager.setText(
                        AnnotatedString(kodentViewModel.analysisResult)
                    )
                }) {
                    Icon(Icons.Rounded.ContentCopy, "Copy", tint = AccentCyan)
                }

                // Clear result button
                IconButton(onClick = { kodentViewModel.clearResult() }) {
                    Icon(Icons.Rounded.Close, "Clear", tint = TextMuted)
                }
            }

            // Progress bar during streaming
            if (kodentViewModel.isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = badgeColorFor(kodentViewModel.selectedMode)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Result content in card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = badgeColorFor(kodentViewModel.selectedMode)
                        .copy(alpha = 0.05f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = kodentViewModel.analysisResult,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}

// ── History ──

@Composable
private fun HistoryContent(
    history: List<AnalysisRecord>,
    onItemClick: (AnalysisRecord) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "History (" + history.size + ")",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClearClick) {
                Icon(Icons.Rounded.DeleteSweep, null, tint = AccentPink)
                Spacer(Modifier.width(4.dp))
                Text("Clear All", color = AccentPink)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.size) { i ->
                val record = history[history.size - 1 - i]
                HistoryCard(record) { onItemClick(record) }
            }
        }
    }
}

@Composable
private fun HistoryCard(record: AnalysisRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = badgeColorFor(record.mode).copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    Text(
                        modeEmoji(record.mode),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        record.mode,
                        color = badgeColorFor(record.mode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.language,
                        color = AccentViolet,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        .format(Date(record.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                record.code.take(80).replace("\n", " "),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

// ── Helpers ──

private fun badgeColorFor(mode: String): Color {
    return when (mode) {
        "Explain" -> AccentCyan
        "Debug" -> AccentPink
        "Optimize" -> AccentGreen
        "Complexity" -> AccentOrange
        else -> AccentCyan
    }
}

private fun modeEmoji(mode: String): String {
    return when (mode) {
        "Explain" -> "💡"
        "Debug" -> "🐛"
        "Optimize" -> "⚡"
        "Complexity" -> "📊"
        else -> "🔍"
    }
}