package com.runanywhere.kotlin_starter_example.kodent.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.kotlin_starter_example.services.ModelType
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream
import com.runanywhere.sdk.public.extensions.transcribe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.runanywhere.kotlin_starter_example.kodent.engine.GenerationConfig
import com.runanywhere.kotlin_starter_example.kodent.engine.OutputProcessor
import com.runanywhere.kotlin_starter_example.kodent.engine.PromptEngine
import com.runanywhere.kotlin_starter_example.kodent.engine.StreamRepetitionDetector
import kotlinx.coroutines.CancellationException
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeValidator
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeHealthParser
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeHealthResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import com.runanywhere.kotlin_starter_example.kodent.engine.SecurityScanner
import com.runanywhere.kotlin_starter_example.kodent.engine.SecurityScanResult
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeHealthAnalyzer

class KodentViewModel(application: Application) : AndroidViewModel(application) {

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

    // Default to Kotlin, but allow changing
    var selectedLanguage by mutableStateOf("Kotlin")
        private set

    var tokensPerSecond by mutableStateOf(0f)
        private set

    var healthResult by mutableStateOf<CodeHealthResult?>(null)
        private set

    var securityResult by mutableStateOf<SecurityScanResult?>(null)
        private set

    fun updateLanguage(lang: String) {
        selectedLanguage = lang
    }

    private var analysisJob: Job? = null
    private val audioRecorder = AudioRecorder()

    private val repetitionDetector = StreamRepetitionDetector()

    // History persistence
    private val prefs =
        application.getSharedPreferences("kodent_history", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadHistory()
    }

    private fun codeMatchesLanguage(code: String, language: String): Boolean {
        val text = code.trim()

        return when (language) {
            "Kotlin" -> text.contains("fun ") || text.contains("val ") || text.contains("var ")
            "Java" -> text.contains("public class") || text.contains("static void")
            "Python" -> text.contains("def ") || text.contains("import ")
            "JS" -> text.contains("function ") || text.contains("const ") || text.contains("=>")
            "C++" -> text.contains("#include") || text.contains("std::")
            "Go" -> text.contains("package ") || text.contains("func ")
            else -> true
        }
    }

    private fun loadHistory() {
        try {
            val json = prefs.getString("history", null)
            if (json != null) {
                val type = object : TypeToken<List<AnalysisRecord>>() {}.type
                val loaded: List<AnalysisRecord> = gson.fromJson(json, type)
                history = loaded.takeLast(20)
            }
        } catch (e: Exception) {
            history = emptyList()
        }
    }

    private fun saveHistory() {
        try {
            val json = gson.toJson(history)
            prefs.edit().putString("history", json).apply()
        } catch (e: Exception) {
            // Silent fail
        }
    }

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
        selectedLanguage = record.language  // ← ADD THIS
        analysisResult = record.result
        showHistory = false
    }

    fun clearHistory() {
        history = emptyList()
        saveHistory()
        showHistory = false // <--- Add this line to auto-close
    }

    fun clearInput() {
        codeInput = ""
    }

    fun clearResult() {
        analysisResult = ""
        tokenCount = 0
        tokensPerSecond = 0f
        analysisTimeMs = 0L
        healthResult = null
        securityResult = null
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
        analysisJob = null
        isAnalyzing = false
        analysisResult = ""
        tokenCount = 0
        tokensPerSecond = 0f
        healthResult = null
        repetitionDetector.reset()
        securityResult = null
    }

    private fun runSecurityScan() {
        viewModelScope.launch {
            isAnalyzing = true
            analysisResult = ""
            errorMessage = null
            tokenCount = 0
            tokensPerSecond = 0f
            analysisTimeMs = 0L
            securityResult = null

            val startTime = System.currentTimeMillis()

            val result = withContext(Dispatchers.Default) {
                SecurityScanner.scan(codeInput, selectedLanguage)
            }

            securityResult = result
            analysisResult = result.summary
            analysisTimeMs = System.currentTimeMillis() - startTime
            tokenCount = result.vulnerabilities.size
            isAnalyzing = false

            // Save to history
            history = (history + AnalysisRecord(
                code = codeInput,
                mode = "Security",
                language = selectedLanguage,
                result = "Security Grade: ${result.grade} (${result.score}/100) • ${result.vulnerabilities.size} issues"
            )).takeLast(20)
            saveHistory()
        }
    }

    private fun runHealthAnalysis() {
        viewModelScope.launch {
            isAnalyzing = true
            analysisResult = ""
            errorMessage = null
            tokenCount = 0
            tokensPerSecond = 0f
            analysisTimeMs = 0L
            healthResult = null

            val startTime = System.currentTimeMillis()

            val result = withContext(Dispatchers.Default) {
                CodeHealthAnalyzer.analyze(codeInput, selectedLanguage)
            }

            healthResult = result
            analysisResult = result.summary
            analysisTimeMs = System.currentTimeMillis() - startTime
            tokenCount = result.issues.size
            isAnalyzing = false

            // Save to history
            history = (history + AnalysisRecord(
                code = codeInput,
                mode = "Health",
                language = selectedLanguage,
                result = "Health Score: ${result.overallScore}/100 • ${result.issues.size} issues"
            )).takeLast(20)
            saveHistory()
        }
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
            lower.contains("debug") || lower.contains("bug") || lower.contains("error") || lower.contains(
                "fix"
            ) -> "Debug"

            lower.contains("optimize") || lower.contains("improve") || lower.contains("better") || lower.contains(
                "faster"
            ) -> "Optimize"

            lower.contains("complex") || lower.contains("big o") || lower.contains("time") && lower.contains(
                "space"
            ) -> "Complexity"

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
            analysisResult = "⚠️ Code is too short to analyze."
            return
        }

        if (!CodeValidator.looksLikeCode(codeInput)) {
            analysisResult =
                "⚠️ Doesn't look like code. Please paste valid ${selectedLanguage} code."
            return
        }

        if (!codeMatchesLanguage(codeInput, selectedLanguage)) {
            analysisResult = "⚠️ The pasted code may not match selected language ($selectedLanguage)."
            return
        }

        // Security mode uses rule-based scanner — no LLM needed
        if (selectedMode == "Security") {
            runSecurityScan()
            return
        }

        // Health mode uses rule-based analyzer — no LLM needed
        if (selectedMode == "Health") {
            runHealthAnalysis()
            return
        }

        val isDeep = activeModel == ModelType.DEEP
        val useDeepForThis = if (selectedMode == "Health") false else isDeep

        analysisJob?.cancel()
        analysisJob = null

        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Main) {
                    isAnalyzing = true
                    analysisResult = ""
                    errorMessage = null
                    tokenCount = 0
                    tokensPerSecond = 0f
                    analysisTimeMs = 0L
                    healthResult = null
                }
                repetitionDetector.reset()
                delay(150)

                val prompt = PromptEngine.build(
                    code = codeInput,
                    mode = selectedMode,
                    language = selectedLanguage,
                    modelType = if (useDeepForThis) "deep" else "quick"
                )

                val genConfig = GenerationConfig.forMode(selectedMode)

                val options = LLMGenerationOptions(
                    temperature = if (selectedMode == "Health") 0.01f
                    else if (useDeepForThis) genConfig.temperature
                    else genConfig.temperature * 0.8f,
                    topP = genConfig.topP,
                    maxTokens = run {
                        val codeLength = codeInput.trim().length
                        val baseTokens = genConfig.maxTokens
                        val scaled = when {
                            codeLength > 500 -> baseTokens + 80
                            codeLength > 300 -> baseTokens + 40
                            else -> baseTokens
                        }
                        if (useDeepForThis) scaled.coerceAtMost(350)
                        else scaled.coerceAtMost(250)
                    },
                    stopSequences = genConfig.stopSequences,
                    systemPrompt = buildSystemPrompt(useDeepForThis)
                )

                val buffer = StringBuilder()
                var localTokenCount = 0
                val startTime = System.currentTimeMillis()
                val maxTimeMs = if (useDeepForThis) 180_000L else 90_000L

                try {
                    RunAnywhere.generateStream(prompt, options)
                        .flowOn(Dispatchers.IO)
                        .catch { e ->
                            if (e !is CancellationException) {
                                errorMessage = "Generation error: ${e.message}"
                            }
                        }
                        .collect { token ->
                            val elapsed = System.currentTimeMillis() - startTime

                            if (elapsed > maxTimeMs) {
                                buffer.append("\n\n⏱️ Time limit reached.")
                                analysisResult = buffer.toString()
                                analysisJob?.cancel()
                                return@collect
                            }

                            if (selectedMode != "Health" && repetitionDetector.shouldStop(token)) {
                                analysisResult = buffer.toString()
                                analysisJob?.cancel()
                                return@collect
                            }

                            buffer.append(token)
                            localTokenCount++

                            if (localTokenCount % 3 == 0 || token.contains("\n")) {
                                withContext(Dispatchers.Main) {
                                    analysisResult = buffer.toString()
                                    tokenCount = localTokenCount

                                    val elapsedSec = elapsed / 1000.0f
                                    if (elapsedSec > 1f) {
                                        tokensPerSecond = localTokenCount / elapsedSec
                                    }
                                }
                            }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage = "Stream error: ${e.message}"
                }

                withContext(Dispatchers.Main) {
                    analysisResult = OutputProcessor.process(buffer.toString(), selectedMode)
                    tokenCount = localTokenCount
                    analysisTimeMs = System.currentTimeMillis() - startTime

                    if (selectedMode == "Health") {
                        android.util.Log.d("KODENT_HEALTH", "Raw output: $analysisResult")
                        healthResult = CodeHealthParser.parseWithCode(analysisResult, codeInput)
                        android.util.Log.d("KODENT_HEALTH", "Scores: bug=${healthResult?.bugRisk} perf=${healthResult?.performance} sec=${healthResult?.security} read=${healthResult?.readability} comp=${healthResult?.complexity}")
                    }

                    if (analysisResult.isBlank()) {
                        analysisResult = when (selectedMode) {
                            "Debug" -> "✅ No bugs found."
                            "Optimize" -> "✅ Code looks good."
                            "Complexity" -> "⚠️ Unable to determine complexity."
                            else -> "⚠️ No output. Try shorter code."
                        }
                    }

                    history = (history + AnalysisRecord(
                        code = codeInput,
                        mode = selectedMode,
                        language = selectedLanguage,
                        result = analysisResult
                    )).takeLast(20)

                    isAnalyzing = false
                }
                saveHistory()

            } catch (e: CancellationException) {
                withContext(Dispatchers.Main) {
                    if (analysisResult.isNotBlank()) {
                        analysisResult = OutputProcessor.process(analysisResult, selectedMode)
                        if (!analysisResult.contains("⏱️")) {
                            analysisResult += "\n\n⚠️ Generation stopped."
                        }
                    }
                    isAnalyzing = false
                }
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    analysisResult = ""
                    errorMessage = when {
                        e.message?.contains("model", true) == true -> "Model error. Try reloading."
                        e.message?.contains("memory", true) == true -> "Not enough memory. Try shorter code."
                        else -> "Analysis failed: ${e.message ?: "Unknown error"}"
                    }
                    isAnalyzing = false
                }
            }
        }
    }

    private fun buildSystemPrompt(isDeep: Boolean): String {
        val lang = selectedLanguage

        return if (isDeep) {
            when (selectedMode) {
                "Explain" -> "You are an expert $lang developer. Explain code clearly and thoroughly. Be structured."
                "Debug" -> "You are a senior $lang reviewer. Find real bugs only. If none, say 'No bugs found.' Don't invent problems."
                "Optimize" -> "You are a senior $lang developer. Suggest concrete improvements. Show improved code if applicable."
                "Complexity" -> "You are an algorithm expert. Provide Big-O for time and space. Analyze loops and data structures."
                "Health" -> "List all problems in this code. Check for bugs, null safety, performance issues, security vulnerabilities, readability problems, complexity. One per line with specifics."
                else -> "You are an expert $lang code analyst."
            }
        } else {
            when (selectedMode) {
                "Explain" -> "You are a $lang explainer. Explain in 2-3 sentences. Be direct."
                "Debug" -> "You are a $lang debugger. List only real bugs. If none, say 'No bugs found.'"
                "Optimize" -> "You are a $lang reviewer. Suggest max 3 improvements."
                "Complexity" -> "State time and space complexity in Big-O. One sentence explanation."
                "Health" -> "List all problems in this code. Check bugs, performance, security, readability. One per line. Be specific."
                else -> "You are a $lang code analyst."
            }
        }
    }
}