package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.ui.components.ModelLoaderWidget
import com.runanywhere.kotlin_starter_example.ui.theme.PrimaryDark
import com.runanywhere.sdk.public.extensions.generateStream
import kotlinx.coroutines.launch
import com.runanywhere.sdk.public.RunAnywhere
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KodentAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modelService: ModelService = viewModel()
) {

    var codeInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Kodent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
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

            // ✅ Show loader if LLM not ready
            if (!modelService.isLLMLoaded) {

                ModelLoaderWidget(
                    modelName = "SmolLM2 360M",
                    isDownloading = modelService.isLLMDownloading,
                    isLoading = modelService.isLLMLoading,
                    isLoaded = modelService.isLLMLoaded,
                    downloadProgress = modelService.llmDownloadProgress,
                    onLoadClick = { modelService.downloadAndLoadLLM() }
                )

                return@Column
            }

            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text("Paste your code here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (codeInput.isNotBlank() && !isAnalyzing) {
                        scope.launch {
                            try {
                                isAnalyzing = true
                                analysisResult = ""

                                val prompt = """
                                You are Kodent, an expert Kotlin software engineer.

                                Analyze the following Kotlin code carefully.
                                
                                If the code is correct, clearly say that it is valid and explain briefly what it does.
                                Only report errors if they truly exist.
                                Do NOT invent problems.
                                
                                Respond in this format:
                                
                                1. What the code does
                                2. Syntax errors (if any, otherwise say "None")
                                3. Logical issues (if any, otherwise say "None")
                                4. Suggested improvements (optional, only if meaningful)
                                5. Time and Space Complexity
                                
                                $codeInput
                                """.trimIndent()

                                val options = LLMGenerationOptions(
                                    temperature = 0.2f,
                                    topP = 0.8f,
                                    maxTokens = 1024,
                                    systemPrompt = null
                                )

                                RunAnywhere.generateStream(prompt, options).collect { token ->
                                    analysisResult += token
                                }

                            } catch (e: Exception) {
                                analysisResult = "Error: ${e.message}"
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Analyze Code")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Analysis Result", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = analysisResult,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }
    }
}
