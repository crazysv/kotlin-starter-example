package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    private var analysisJob: Job? = null

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

    fun cancelAnalysis() {
        analysisJob?.cancel()
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

    fun analyze() {
        if (codeInput.isBlank() || isAnalyzing) return

        if (codeInput.trim().length < 10) {
            analysisResult = "Input does not appear to be valid Kotlin code."
            return
        }

        if (!looksLikeKotlin(codeInput)) {
            analysisResult = "Invalid Kotlin code. Please provide valid Kotlin syntax."
            return
        }

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                isAnalyzing = true
                analysisResult = ""
                errorMessage = null
                tokenCount = 0
                analysisTimeMs = 0L

                val userPrompt = when (selectedMode) {
                    "Explain" -> "Analyze this Kotlin code:\n```kotlin\n$codeInput\n```\nWhat does this code do?"
                    "Debug" -> "Find bugs in this Kotlin code:\n```kotlin\n$codeInput\n```"
                    "Optimize" -> "Suggest improvements for this Kotlin code:\n```kotlin\n$codeInput\n```"
                    "Complexity" -> "What is the time and space complexity of this code?\n```kotlin\n$codeInput\n```"
                    else -> codeInput
                }

                val options = LLMGenerationOptions(
                    temperature = 0.1f,
                    topP = 0.9f,
                    maxTokens = when (selectedMode) {
                        "Explain" -> 200
                        "Debug" -> 250
                        "Optimize" -> 300
                        "Complexity" -> 150
                        else -> 250
                    },
                    stopSequences = listOf(
                        "<|im_end|>",
                        "<|endoftext|>",
                        "\n\n\n",
                        "User:",
                        "Human:",
                        "```\n\n"
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

                val buffer = StringBuilder()
                var localTokenCount = 0
                val startTime = System.currentTimeMillis()

                tokenCount = 0
                analysisTimeMs = 0L

                RunAnywhere.generateStream(userPrompt, options)
                    .collect { token ->
                        buffer.append(token)
                        localTokenCount++

                        if (localTokenCount % 3 == 0 || token.contains("\n")) {
                            analysisResult = buffer.toString()
                            tokenCount = localTokenCount
                        }
                    }

                // Final flush
                analysisResult = buffer.toString()
                tokenCount = localTokenCount
                analysisTimeMs = System.currentTimeMillis() - startTime

                // Save to history after successful analysis
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