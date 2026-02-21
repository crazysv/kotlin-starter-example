package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.services.ModelType
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.AccentViolet
import com.runanywhere.kotlin_starter_example.ui.theme.PrimaryDark
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentHomeScreen(
    onAnalyzeClick: () -> Unit,
    modelService: ModelService
) {
    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Kodent") }
            )
        }
    ) { padding ->

        // Cache all model states before scrollable content
        val activeModel = modelService.activeModel
        val isLoaded = modelService.isLLMLoaded
        val isDownloading = modelService.isLLMDownloading
        val isLoading = modelService.isLLMLoading
        val downloadProgress = modelService.llmDownloadProgress
        val beingPrepared = modelService.modelBeingPrepared
        val error = modelService.errorMessage

        val isQuickActive = activeModel == ModelType.QUICK
        val isDeepActive = activeModel == ModelType.DEEP
        val isQuickPreparing = beingPrepared == ModelType.QUICK && (isDownloading || isLoading)
        val isDeepPreparing = beingPrepared == ModelType.DEEP && (isDownloading || isLoading)
        val isQuickDownloading = beingPrepared == ModelType.QUICK && isDownloading
        val isDeepDownloading = beingPrepared == ModelType.DEEP && isDownloading
        val quickProgress = if (beingPrepared == ModelType.QUICK) downloadProgress else 0f
        val deepProgress = if (beingPrepared == ModelType.DEEP) downloadProgress else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // App Title
            Text(
                text = "🔍 Kodent",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-Powered Kotlin Code Analyzer",
                style = MaterialTheme.typography.titleMedium,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Fully offline • Privacy-first • On-device AI",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Model Selection Section
            Text(
                text = "Choose Your Model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Only one model loads at a time to save memory",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Model Card
            ModelCard(
                emoji = "🚀",
                title = "Quick Mode",
                modelName = "SmolLM2 360M",
                description = "Fast analysis • Basic quality • Low memory",
                specs = "360 MB download • ~400 MB RAM",
                color = AccentCyan,
                isActive = isQuickActive,
                isLoading = isQuickPreparing,
                isDownloading = isQuickDownloading,
                downloadProgress = quickProgress,
                isOtherModelLoading = isDeepPreparing,
                onLoadClick = { modelService.downloadAndLoadModel(ModelType.QUICK) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Deep Model Card
            ModelCard(
                emoji = "🧠",
                title = "Deep Mode",
                modelName = "Qwen2.5 Coder 1.5B",
                description = "Thorough analysis • High quality • Better reasoning",
                specs = "1.12 GB download • ~1.3 GB RAM",
                color = AccentViolet,
                isActive = isDeepActive,
                isLoading = isDeepPreparing,
                isDownloading = isDeepDownloading,
                downloadProgress = deepProgress,
                isOtherModelLoading = isQuickPreparing,
                onLoadClick = { modelService.downloadAndLoadModel(ModelType.DEEP) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentPink.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "❌ $error",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentPink
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Start Analyzing Button
            Button(
                onClick = onAnalyzeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isLoaded
            ) {
                Text(
                    text = if (isLoaded)
                        "Start Analyzing"
                    else
                        "Load a Model First",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Active model indicator
            if (isLoaded && activeModel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (activeModel) {
                        ModelType.QUICK -> "🚀 Quick Mode active"
                        ModelType.DEEP -> "🧠 Deep Mode active"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Cards
            Text(
                text = "Analysis Modes",
                style = MaterialTheme.typography.titleSmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                emoji = "💡",
                title = "Explain",
                description = "Understand what any Kotlin code does",
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeatureCard(
                emoji = "🐛",
                title = "Debug",
                description = "Find bugs and runtime risks",
                color = AccentPink
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeatureCard(
                emoji = "⚡",
                title = "Optimize",
                description = "Get suggestions for better code",
                color = AccentGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeatureCard(
                emoji = "📊",
                title = "Complexity",
                description = "Time and space complexity in Big-O",
                color = AccentOrange
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ModelCard(
    emoji: String,
    title: String,
    modelName: String,
    description: String,
    specs: String,
    color: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
    isLoading: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isOtherModelLoading: Boolean,
    onLoadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                color.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = if (isActive)
            androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                // Status badge
                if (isActive) {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Active ✅",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = specs,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.7f)
            )

            // Download progress
            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f),
                )
            }

            // Loading indicator
            if (isLoading && !isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading model...",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f),
                )
            }

            // Load/Switch button
            if (!isActive && !isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onLoadClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isOtherModelLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Load This Model",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 22.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}