package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.PrimaryDark
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentHomeScreen(
    onAnalyzeClick: () -> Unit,
    isModelLoaded: Boolean,
    onLoadModelClick: () -> Unit
) {
    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Kodent") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            // Feature Cards
            FeatureCard(
                emoji = "💡",
                title = "Explain",
                description = "Understand what any Kotlin code does in plain language",
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                emoji = "🐛",
                title = "Debug",
                description = "Find bugs, syntax errors, and runtime risks",
                color = AccentPink
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                emoji = "⚡",
                title = "Optimize",
                description = "Get suggestions for cleaner, faster Kotlin code",
                color = AccentGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureCard(
                emoji = "📊",
                title = "Complexity",
                description = "Estimate time and space complexity in Big-O notation",
                color = AccentOrange
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Model Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isModelLoaded) "✅ Model Ready" else "⏳ Model Not Loaded",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isModelLoaded) AccentGreen else TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "SmolLM2 360M • Runs fully on your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            if (isModelLoaded) {
                Button(
                    onClick = onAnalyzeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Start Analyzing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onLoadModelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Load Model",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "First time? Download is ~250MB (one-time only)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}