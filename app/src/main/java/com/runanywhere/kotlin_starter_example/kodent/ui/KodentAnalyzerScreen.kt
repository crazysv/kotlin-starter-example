package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.style.TextOverflow
import com.runanywhere.kotlin_starter_example.services.ModelService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modelService: ModelService
) {

    var codeInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kodent",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text("Paste your code here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                maxLines = 20
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    analysisResult = "Analyzing...\n\n(LLM connection coming next)"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze Code")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (analysisResult.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        text = "Analysis Result",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = analysisResult,
                        style = TextStyle(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}