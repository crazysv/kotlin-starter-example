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

        val isDeep = activeModel == ModelType.DEEP

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                isAnalyzing = true
                analysisResult = ""
                errorMessage = null
                tokenCount = 0
                tokensPerSecond = 0f
                analysisTimeMs = 0L
                repetitionDetector.reset()

                // ✅ Use PromptEngine (fixes ${'$'} bug and code fence bug)
                val prompt = PromptEngine.build(
                    code = codeInput,
                    mode = selectedMode,
                    language = selectedLanguage,
                    modelType = if (isDeep) "deep" else "quick"
                )

                // ✅ Use GenerationConfig (per-mode parameters)
                val genConfig = GenerationConfig.forMode(selectedMode)

                val options = LLMGenerationOptions(
                    temperature = if (isDeep) genConfig.temperature else genConfig.temperature * 0.8f,
                    topP = genConfig.topP,
                    maxTokens = if (isDeep) {
                        (genConfig.maxTokens * 1.5f).toInt().coerceAtMost(500)
                    } else {
                        genConfig.maxTokens
                    },
                    stopSequences = genConfig.stopSequences,
                    systemPrompt = buildSystemPrompt(isDeep)
                )

                val buffer = StringBuilder()
                var localTokenCount = 0
                val startTime = System.currentTimeMillis()
                val maxTimeMs = if (isDeep) 90_000L else 45_000L

                RunAnywhere.generateStream(prompt, options)
                    .collect { token ->
                        val elapsed = System.currentTimeMillis() - startTime

                        // ✅ Timeout protection
                        if (elapsed > maxTimeMs) {
                            buffer.append("\n\n⏱️ Time limit reached.")
                            analysisResult = buffer.toString()
                            analysisJob?.cancel()
                            return@collect
                        }

                        // ✅ Repetition detection (NEW)
                        if (repetitionDetector.shouldStop(token)) {
                            analysisResult = buffer.toString()
                            analysisJob?.cancel()
                            return@collect
                        }

                        buffer.append(token)
                        localTokenCount++

                        // ✅ Update EVERY token (was every 3 — choppy)
                        analysisResult = buffer.toString()
                        tokenCount = localTokenCount

                        // ✅ Tokens/sec tracking (NEW)
                        val elapsedSec = elapsed / 1000.0f
                        if (elapsedSec > 0.5f) {
                            tokensPerSecond = localTokenCount / elapsedSec
                        }
                    }

                // ✅ Post-process output (NEW — cleans artifacts)
                analysisResult = OutputProcessor.process(buffer.toString(), selectedMode)
                tokenCount = localTokenCount
                analysisTimeMs = System.currentTimeMillis() - startTime

                if (analysisResult.isBlank()) {
                    analysisResult = when (selectedMode) {
                        "Debug" -> "✅ No bugs found."
                        "Optimize" -> "✅ Code looks good."
                        "Complexity" -> "⚠️ Unable to determine complexity."
                        else -> "⚠️ No output. Try shorter code."
                    }
                }

                // Save to history (with language)
                history = (history + AnalysisRecord(
                    code = codeInput,
                    mode = selectedMode,
                    language = selectedLanguage,
                    result = analysisResult
                )).takeLast(20)
                saveHistory()

            } catch (e: Exception) {
                if (e is CancellationException) {
                    if (analysisResult.isNotBlank()) {
                        analysisResult = OutputProcessor.process(analysisResult, selectedMode)
                        if (!analysisResult.contains("⏱️")) {
                            analysisResult += "\n\n⚠️ Generation stopped."
                        }
                    }
                    throw e
                }
                analysisResult = ""
                errorMessage = when {
                    e.message?.contains("model", true) == true -> "Model error. Try reloading."
                    e.message?.contains(
                        "memory",
                        true
                    ) == true -> "Not enough memory. Try shorter code."

                    else -> "Analysis failed: ${e.message ?: "Unknown error"}"
                }
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun buildSystemPrompt(isDeep: Boolean): String {
        val lang = selectedLanguage  // ✅ No more ${'$'} bug

        return if (isDeep) {
            when (selectedMode) {
                "Explain" -> "You are an expert $lang developer. Explain code clearly and thoroughly. Be structured."
                "Debug" -> "You are a senior $lang reviewer. Find real bugs only. If none, say 'No bugs found.' Don't invent problems."
                "Optimize" -> "You are a senior $lang developer. Suggest concrete improvements. Show improved code if applicable."
                "Complexity" -> "You are an algorithm expert. Provide Big-O for time and space. Analyze loops and data structures."
                else -> "You are an expert $lang code analyst."
            }
        } else {
            when (selectedMode) {
                "Explain" -> "You are a $lang explainer. Explain in 2-3 sentences. Be direct."
                "Debug" -> "You are a $lang debugger. List only real bugs. If none, say 'No bugs found.'"
                "Optimize" -> "You are a $lang reviewer. Suggest max 3 improvements."
                "Complexity" -> "State time and space complexity in Big-O. One sentence explanation."
                else -> "You are a $lang code analyst."
            }
        }
    }
}
