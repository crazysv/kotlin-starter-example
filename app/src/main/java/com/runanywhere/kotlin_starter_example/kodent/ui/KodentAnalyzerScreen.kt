package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.ui.components.ModelLoaderWidget
import com.runanywhere.kotlin_starter_example.ui.theme.PrimaryDark
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modelService: ModelService = viewModel()
) {
    val kodentViewModel: KodentViewModel = viewModel()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (!modelService.isLLMLoaded &&
            !modelService.isLLMDownloading &&
            !modelService.isLLMLoading
        ) {
            modelService.downloadAndLoadLLM()
        }
    }

    LaunchedEffect(kodentViewModel.analysisResult) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Kodent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (kodentViewModel.history.isNotEmpty()) {
                        IconButton(onClick = { kodentViewModel.toggleHistory() }) {
                            Icon(Icons.Rounded.History, "History")
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (kodentViewModel.showHistory) {
            // History Screen
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
            // Analyzer Screen
            AnalyzerContent(
                kodentViewModel = kodentViewModel,
                modelService = modelService,
                scrollState = scrollState,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

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

        // Mode Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Explain", "Debug", "Optimize", "Complexity").forEach { mode ->
                val modeColor = badgeColorFor(mode)

                FilterChip(
                    selected = kodentViewModel.selectedMode == mode,
                    onClick = { kodentViewModel.selectMode(mode) },
                    label = { Text(mode, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = modeColor.copy(alpha = 0.2f),
                        selectedLabelColor = modeColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = modeColor,
                        enabled = true,
                        selected = kodentViewModel.selectedMode == mode
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Code Input
        OutlinedTextField(
            value = kodentViewModel.codeInput,
            onValueChange = { kodentViewModel.updateCode(it) },
            label = { Text("Paste Kotlin code") },
            placeholder = {
                Text(
                    text = "fun example() {\n    println(\"Hello\")\n}",
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
            )
        )

        // Kotlin detection
        if (kodentViewModel.codeInput.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (kodentViewModel.codeInput.trim().length < 10)
                    "⚠️ Too short"
                else if (looksLikeKotlin(kodentViewModel.codeInput))
                    "✅ Looks like Kotlin"
                else
                    "⚠️ May not be valid Kotlin",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Analyze Button
        Button(
            onClick = { kodentViewModel.analyze() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !kodentViewModel.isAnalyzing
        ) {
            if (kodentViewModel.isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Text("Analyze Code")
            }
        }

        // Cancel Button
        if (kodentViewModel.isAnalyzing) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { kodentViewModel.cancelAnalysis() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Header
        if (kodentViewModel.analysisResult.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeColor = badgeColorFor(kodentViewModel.selectedMode)

                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = kodentViewModel.selectedMode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Analysis Result",
                    style = MaterialTheme.typography.titleSmall
                )

                if (kodentViewModel.isAnalyzing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "streaming...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (kodentViewModel.isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = badgeColorFor(kodentViewModel.selectedMode)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Result
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (kodentViewModel.analysisResult.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = if (kodentViewModel.isAnalyzing)
                            kodentViewModel.analysisResult + " ▌"
                        else
                            kodentViewModel.analysisResult,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            } else if (!kodentViewModel.isAnalyzing) {
                Text(
                    text = "Paste Kotlin code above and tap Analyze",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    history: List<AnalysisRecord>,
    onItemClick: (AnalysisRecord) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analysis History",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = onClearClick) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear history",
                    tint = AccentPink
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${history.size} analyses saved",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history.reversed().forEach { record ->
                HistoryCard(
                    record = record,
                    onClick = { onItemClick(record) }
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    record: AnalysisRecord,
    onClick: () -> Unit
) {
    val modeColor = badgeColorFor(record.mode)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = modeColor.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = modeColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = record.mode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = modeColor
                    )
                }

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.code.lines().take(3).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = record.result.take(100) + if (record.result.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun badgeColorFor(mode: String): Color {
    return when (mode) {
        "Explain" -> AccentCyan
        "Debug" -> AccentPink
        "Optimize" -> AccentGreen
        "Complexity" -> AccentOrange
        else -> AccentCyan
    }
}