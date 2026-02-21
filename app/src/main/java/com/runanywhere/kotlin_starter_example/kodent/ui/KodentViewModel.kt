package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.kotlin_starter_example.services.ModelType
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KodentViewModel : ViewModel() {

    var codeInput by mutableStateOf("")
        private set
    var analysisResult by mutableStateOf("")
        private set
    var isAnalyzing by mutableStateOf(false)
        private set
    var selectedMode by mutableStateOf("Explain")
        private set
    var history by mutableStateOf(listOf<AnalysisRecord>())
        private set
    var showHistory by mutableStateOf(false)
        private set
    var tokenCount by mutableStateOf(0)
        private set
    var analysisTimeMs by mutableStateOf(0L)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isListening by mutableStateOf(false)
        private set
    var isTranscribing by mutableStateOf(false)
        private set
    var speechText by mutableStateOf("")
        private set

    private var analysisJob: Job? = null
    private val audioRecorder = AudioRecorder()

    fun updateCode(newCode: String) {
        codeInput = newCode
    }

    fun selectMode(mode: String) {
        selectedMode = mode
    }

    fun toggleHistory() {
        showHistory = !showHistory
    }

    fun loadFromHistory(record: AnalysisRecord) {
        codeInput = record.code
        selectedMode = record.mode
        analysisResult = record.result
        showHistory = false
    }

    fun clearHistory() {
        history = emptyList()
    }

    fun clearInput() {
        codeInput = ""
    }

    fun clearResult() {
        analysisResult = ""
    }

    fun clearError() {
        errorMessage = null
    }

    fun retry() {
        errorMessage = null
        analyze()
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
    }

    fun startListening() {
        if (isListening || isTranscribing) return

        viewModelScope.launch {
            try {
                val started = withContext(Dispatchers.IO) {
                    audioRecorder.startRecording()
                }
                if (started) {
                    isListening = true
                    speechText = ""
                    errorMessage = null
                } else {
                    errorMessage = "Failed to start recording"
                }
            } catch (e: Exception) {
                errorMessage = "Recording failed: ${e.message}"
            }
        }
    }

    fun stopListeningAndProcess(activeModel: ModelType? = null) {
        if (!isListening) return

        isListening = false
        isTranscribing = true

        viewModelScope.launch {
            try {
                val audioData = withContext(Dispatchers.IO) {
                    audioRecorder.stopRecording()
                }

                if (audioData.isEmpty()) {
                    errorMessage = "No audio recorded"
                    isTranscribing = false
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    RunAnywhere.transcribe(audioData)
                }

                isTranscribing = false

                if (result.isNotBlank()) {
                    speechText = result
                    detectModeFromSpeech(result, activeModel)
                } else {
                    errorMessage = "Didn't catch that. Try again."
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                isTranscribing = false
                errorMessage = "Transcription failed: ${e.message}"
            }
        }
    }

    private fun detectModeFromSpeech(speech: String, activeModel: ModelType? = null) {
        val lower = speech.lowercase().trim()

        val detectedMode = when {
            lower.contains("explain") || lower.contains("what does") || lower.contains("what is") -> "Explain"
            lower.contains("debug") || lower.contains("bug") || lower.contains("error") || lower.contains("fix") -> "Debug"
            lower.contains("optimize") || lower.contains("improve") || lower.contains("better") || lower.contains("faster") -> "Optimize"
            lower.contains("complex") || lower.contains("big o") || lower.contains("time") && lower.contains("space") -> "Complexity"
            lower.contains("analyze") || lower.contains("check") || lower.contains("review") -> "Explain"
            else -> null
        }

        if (detectedMode != null) {
            selectedMode = detectedMode
            speechText = "🎤 \"$speech\" → $detectedMode mode"
            analyze(activeModel)
        } else {
            speechText = "🎤 \"$speech\""
            errorMessage = "Say: explain, debug, optimize, or complexity"
        }
    }

    fun analyze(activeModel: ModelType? = null) {
        if (codeInput.isBlank() || isAnalyzing) return

        if (codeInput.trim().length < 10) {
            analysisResult = "Input does not appear to be valid Kotlin code."
            return
        }

        if (!looksLikeKotlin(codeInput)) {
            analysisResult = "Invalid Kotlin code. Please provide valid Kotlin syntax."
            return
        }

        val isDeep = activeModel == ModelType.DEEP

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                isAnalyzing = true
                analysisResult = ""
                errorMessage = null
                tokenCount = 0
                analysisTimeMs = 0L

                val userPrompt = buildUserPrompt(isDeep)
                val options = buildOptions(isDeep)

                val buffer = StringBuilder()
                var localTokenCount = 0
                val startTime = System.currentTimeMillis()
                val maxTimeMs = if (isDeep) 120_000L else 60_000L

                RunAnywhere.generateStream(userPrompt, options)
                    .collect { token ->
                        buffer.append(token)
                        localTokenCount++

                        if (localTokenCount % 3 == 0 || token.contains("\n")) {
                            analysisResult = buffer.toString()
                            tokenCount = localTokenCount
                        }

                        // Safety timeout
                        if (System.currentTimeMillis() - startTime > maxTimeMs) {
                            analysisResult = buffer.toString()
                            return@collect
                        }
                    }

                // Final flush
                analysisResult = buffer.toString()
                tokenCount = localTokenCount
                analysisTimeMs = System.currentTimeMillis() - startTime

                // Handle empty response
                if (analysisResult.isBlank()) {
                    analysisResult = when (selectedMode) {
                        "Debug" -> "No bugs found. The code looks correct."
                        "Optimize" -> "Code looks good. No major improvements needed."
                        "Complexity" -> "Unable to determine complexity for this code."
                        else -> "No output generated. Try rephrasing or shorter code."
                    }
                }

                // Save to history
                history = (history + AnalysisRecord(
                    code = codeInput,
                    mode = selectedMode,
                    result = analysisResult
                )).takeLast(20)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    analysisResult += "\n\n⚠️ Analysis cancelled."
                    throw e
                }
                analysisResult = ""
                errorMessage = when {
                    e.message?.contains("model", ignoreCase = true) == true ->
                        "Model error. Try reloading the model."
                    e.message?.contains("memory", ignoreCase = true) == true ->
                        "Not enough memory. Try shorter code."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Analysis timed out. Try shorter code."
                    e.message?.contains("load", ignoreCase = true) == true ->
                        "Model not loaded. Go back and load the model."
                    else ->
                        "Analysis failed: ${e.message ?: "Unknown error"}"
                }
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun buildUserPrompt(isDeep: Boolean): String {
        return if (isDeep) {
            when (selectedMode) {
                "Explain" -> """
                    Analyze this Kotlin code and explain what it does step by step:
                    ```kotlin
                    $codeInput
                    ```
                    Explain the purpose, how it works, and what it returns.
                """.trimIndent()

                "Debug" -> """
                    Carefully analyze this Kotlin code for bugs:
                    ```kotlin
                    $codeInput
                    ```
                    Check for: null safety issues, type errors, logic errors, edge cases, and runtime exceptions.
                    If no bugs exist, say "No bugs found."
                """.trimIndent()

                "Optimize" -> """
                    Review this Kotlin code and suggest improvements:
                    ```kotlin
                    $codeInput
                    ```
                    Consider: Kotlin idioms, performance, readability, and best practices.
                    Show the improved code if applicable.
                """.trimIndent()

                "Complexity" -> """
                    Analyze the time and space complexity of this Kotlin code:
                    ```kotlin
                    $codeInput
                    ```
                    Provide Big-O notation with detailed justification.
                """.trimIndent()

                else -> codeInput
            }
        } else {
            when (selectedMode) {
                "Explain" -> "Analyze this Kotlin code:\n```kotlin\n$codeInput\n```\nWhat does this code do?"
                "Debug" -> "Find bugs in this Kotlin code:\n```kotlin\n$codeInput\n```"
                "Optimize" -> "Suggest improvements for this Kotlin code:\n```kotlin\n$codeInput\n```"
                "Complexity" -> "What is the time and space complexity of this code?\n```kotlin\n$codeInput\n```"
                else -> codeInput
            }
        }
    }

    private fun buildOptions(isDeep: Boolean): LLMGenerationOptions {
        return if (isDeep) {
            LLMGenerationOptions(
                temperature = 0.15f,
                topP = 0.9f,
                maxTokens = run {
                    val codeLines = codeInput.lines().size
                    val baseTokens = when (selectedMode) {
                        "Explain" -> 120
                        "Debug" -> 150
                        "Optimize" -> 150
                        "Complexity" -> 100
                        else -> 150
                    }
                    val scaled = baseTokens + (codeLines / 5) * 30
                    scaled.coerceAtMost(500)
                },
                stopSequences = listOf(
                    "<|im_end|>",
                    "<|endoftext|>",
                    "<|end|>",
                    "<|EOT|>",
                    "\n\n\n",
                    "User:",
                    "Human:"
                ),
                systemPrompt = when (selectedMode) {
                    "Explain" -> """
                        You are an expert Kotlin developer and teacher.
                        Explain the code clearly and thoroughly.
                        Mention key Kotlin features used (null safety, extensions, coroutines, etc.).
                        Be structured and educational.
                    """.trimIndent()

                    "Debug" -> """
                        You are a senior Kotlin code reviewer.
                        Find all bugs including: null pointer risks, type mismatches, logic errors, edge cases, concurrency issues.
                        If no bugs exist, say only: No bugs found.
                        Do not invent problems. Be precise. Use bullet points.
                    """.trimIndent()

                    "Optimize" -> """
                        You are a senior Kotlin developer.
                        Suggest concrete improvements with code examples.
                        Consider: idiomatic Kotlin, performance, readability, scope functions, extension functions.
                        If code is already optimal, say: Code looks good.
                    """.trimIndent()

                    "Complexity" -> """
                        You are an algorithm analysis expert.
                        Provide time and space complexity in Big-O notation.
                        Analyze each loop, recursion, and data structure used.
                        Format: Time: O(?), Space: O(?).
                        Explain your reasoning step by step.
                    """.trimIndent()

                    else -> "You are an expert Kotlin code analyst."
                }
            )
        } else {
            LLMGenerationOptions(
                temperature = 0.1f,
                topP = 0.9f,
                maxTokens = run {
                    val codeLines = codeInput.lines().size
                    val baseTokens = when (selectedMode) {
                        "Explain" -> 80
                        "Debug" -> 100
                        "Optimize" -> 120
                        "Complexity" -> 60
                        else -> 100
                    }
                    val scaled = baseTokens + (codeLines / 5) * 20
                    scaled.coerceAtMost(300)
                },
                stopSequences = listOf(
                    "<|im_end|>",
                    "<|endoftext|>",
                    "\n\n\n",
                    "User:",
                    "Human:"
                ),
                systemPrompt = when (selectedMode) {
                    "Explain" -> """
                        You are a Kotlin code explainer.
                        Explain what the code does in 2-3 short sentences.
                        Do not mention errors or improvements.
                        Be direct and concise.
                    """.trimIndent()

                    "Debug" -> """
                        You are a Kotlin debugger.
                        List only real bugs found in the code.
                        If no bugs exist, say only: No bugs found.
                        Do not invent problems. Use bullet points.
                    """.trimIndent()

                    "Optimize" -> """
                        You are a Kotlin code reviewer.
                        Suggest maximum 3 improvements.
                        If code is already good, say: Code looks good.
                        Do not repeat the original code.
                    """.trimIndent()

                    "Complexity" -> """
                        You are an algorithm analyst.
                        State time and space complexity in Big-O notation.
                        Format: Time: O(?), Space: O(?).
                        Add one sentence explanation. Nothing else.
                    """.trimIndent()

                    else -> "You are a Kotlin code analyst."
                }
            )
        }
    }
}

fun looksLikeKotlin(code: String): Boolean {
    val trimmed = code.trim()
    if (trimmed.length < 5) return false

    val kotlinSignals = listOf(
        "fun ", "val ", "var ",
        "data class ", "sealed class ", "sealed interface ",
        "object ", "companion object",
        "suspend fun", "override fun",
        "when (", "when {",
        "println(", "print(",
        "listOf(", "mapOf(", "mutableListOf(",
        "?.","!!",
        "it.", "it ->",
        ": Int", ": String", ": Boolean", ": Long", ": Double", ": Float", ": Unit", ": Any",
        ".forEach", ".map(", ".filter(", ".let {", ".apply {", ".also {",
        "package ", "import "
    )

    val antiSignals = listOf(
        "#include",
        "public static void main",
        "System.out.println",
        "def ",
        "function ",
        "const ",
        "let ",
        "console.log",
        "printf(",
        "cout <<",
        "cin >>",
        "<!DOCTYPE",
        "<html",
        "SELECT ", "FROM ", "INSERT INTO"
    )

    val antiCount = antiSignals.count { trimmed.contains(it, ignoreCase = true) }
    if (antiCount >= 1) return false

    val kotlinCount = kotlinSignals.count { trimmed.contains(it) }

    return kotlinCount >= 2
}