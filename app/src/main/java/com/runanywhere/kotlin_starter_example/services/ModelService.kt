package com.runanywhere.kotlin_starter_example.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.core.types.InferenceFramework
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.Models.ModelCategory
import com.runanywhere.sdk.public.extensions.registerModel
import com.runanywhere.sdk.public.extensions.downloadModel
import com.runanywhere.sdk.public.extensions.loadLLMModel
import com.runanywhere.sdk.public.extensions.loadSTTModel
import com.runanywhere.sdk.public.extensions.loadTTSVoice
import com.runanywhere.sdk.public.extensions.unloadLLMModel
import com.runanywhere.sdk.public.extensions.unloadSTTModel
import com.runanywhere.sdk.public.extensions.unloadTTSVoice
import com.runanywhere.sdk.public.extensions.isLLMModelLoaded
import com.runanywhere.sdk.public.extensions.isSTTModelLoaded
import com.runanywhere.sdk.public.extensions.isTTSVoiceLoaded
import com.runanywhere.sdk.public.extensions.isVoiceAgentReady
import com.runanywhere.sdk.public.extensions.availableModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream

enum class ModelType {
    QUICK, // SmolLM2-360M
    DEEP // Qwen2.5-Coder-1.5B
}

class ModelService : ViewModel() {

    // Active model tracking
    var activeModel by mutableStateOf<ModelType?>(null)
        private set

    // LLM state
    var isLLMDownloading by mutableStateOf(false)
        private set
    var isLLMLoading by mutableStateOf(false)
        private set
    var isLLMLoaded by mutableStateOf(false)
        private set
    var llmDownloadProgress by mutableStateOf(0f)
        private set

    // Track which model is being downloaded/loaded
    var modelBeingPrepared by mutableStateOf<ModelType?>(null)
        private set

    var isPreWarming by mutableStateOf(false)
        private set

    var isQuickDownloaded by mutableStateOf(false)
        private set

    var isDeepDownloaded by mutableStateOf(false)
        private set

    // STT state
    var isSTTDownloading by mutableStateOf(false)
        private set
    var isSTTLoading by mutableStateOf(false)
        private set
    var isSTTLoaded by mutableStateOf(false)
        private set
    var sttDownloadProgress by mutableStateOf(0f)
        private set

    // TTS state
    var isTTSDownloading by mutableStateOf(false)
        private set
    var isTTSLoading by mutableStateOf(false)
        private set
    var isTTSLoaded by mutableStateOf(false)
        private set
    var ttsDownloadProgress by mutableStateOf(0f)
        private set

    var isVoiceAgentReady by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set


    companion object {
        // Quick Model
        const val QUICK_MODEL_ID = "smollm2-360m-instruct-q8_0"

        // Deep Model
        const val DEEP_MODEL_ID = "qwen2.5-coder-1.5b-instruct-q4_k_m"

        // STT / TTS (unchanged)
        const val STT_MODEL_ID = "sherpa-onnx-whisper-tiny.en"
        const val TTS_MODEL_ID = "vits-piper-en_US-lessac-medium"

        fun registerDefaultModels() {
            // Quick Model - SmolLM2 360M
            RunAnywhere.registerModel(
                id = QUICK_MODEL_ID,
                name = "SmolLM2 360M Instruct Q8_0",
                url = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                framework = InferenceFramework.LLAMA_CPP,
                modality = ModelCategory.LANGUAGE,
                memoryRequirement = 400_000_000
            )

            // Deep Model - Qwen2.5 Coder 1.5B
            RunAnywhere.registerModel(
                id = DEEP_MODEL_ID,
                name = "Qwen2.5 Coder 1.5B Instruct Q4_K_M",
                url = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
                framework = InferenceFramework.LLAMA_CPP,
                modality = ModelCategory.LANGUAGE,
                memoryRequirement = 1_200_000_000
            )

            // STT Model
            RunAnywhere.registerModel(
                id = STT_MODEL_ID,
                name = "Sherpa Whisper Tiny (ONNX)",
                url = "https://github.com/RunanywhereAI/sherpa-onnx/releases/download/runanywhere-models-v1/sherpa-onnx-whisper-tiny.en.tar.gz",
                framework = InferenceFramework.ONNX,
                modality = ModelCategory.SPEECH_RECOGNITION
            )

            // TTS Model
            RunAnywhere.registerModel(
                id = TTS_MODEL_ID,
                name = "Piper TTS (US English - Medium)",
                url = "https://github.com/RunanywhereAI/sherpa-onnx/releases/download/runanywhere-models-v1/vits-piper-en_US-lessac-medium.tar.gz",
                framework = InferenceFramework.ONNX,
                modality = ModelCategory.SPEECH_SYNTHESIS
            )
        }
    }

    init {
        viewModelScope.launch {
            refreshModelState()
            checkDownloadedModels()
        }
    }

    private suspend fun refreshModelState() {
        isLLMLoaded = RunAnywhere.isLLMModelLoaded()
        isSTTLoaded = RunAnywhere.isSTTModelLoaded()
        isTTSLoaded = RunAnywhere.isTTSVoiceLoaded()
        isVoiceAgentReady = RunAnywhere.isVoiceAgentReady()
    }

    private suspend fun checkDownloadedModels() {
        isQuickDownloaded = isModelDownloaded(QUICK_MODEL_ID)
        isDeepDownloaded = isModelDownloaded(DEEP_MODEL_ID)
    }

    private suspend fun isModelDownloaded(modelId: String): Boolean {
        val models = RunAnywhere.availableModels()
        val model = models.find { it.id == modelId }
        return model?.localPath != null
    }

    fun getModelId(type: ModelType): String {
        return when (type) {
            ModelType.QUICK -> QUICK_MODEL_ID
            ModelType.DEEP -> DEEP_MODEL_ID
        }
    }

    suspend fun isModelTypeDownloaded(type: ModelType): Boolean {
        return isModelDownloaded(getModelId(type))
    }

    private suspend fun prewarmModel() {
        try {
            isPreWarming = true
            withContext(Dispatchers.IO) {
                val warmupOptions = LLMGenerationOptions(
                    temperature = 0.01f,
                    maxTokens = 1,
                    systemPrompt = "Reply with OK."
                )
                RunAnywhere.generateStream("Hi", warmupOptions)
                    .collect { /* discard */ }
            }
            isPreWarming = false
        } catch (e: Exception) {
            isPreWarming = false
        }
    }

    fun downloadAndLoadModel(type: ModelType) {
        // If already loading the same type, skip
        if ((isLLMDownloading || isLLMLoading) && modelBeingPrepared == type) return

        // If stuck from previous failed attempt, reset
        if (isLLMDownloading || isLLMLoading) {
            isLLMDownloading = false
            isLLMLoading = false
            modelBeingPrepared = null
        }

        viewModelScope.launch {
            try {
                errorMessage = null
                modelBeingPrepared = type
                val modelId = getModelId(type)

                // Step 1: Download if needed
                if (!isModelDownloaded(modelId)) {
                    isLLMDownloading = true
                    llmDownloadProgress = 0f

                    RunAnywhere.downloadModel(modelId)
                        .catch { e ->
                            errorMessage = "Download failed: ${e.message}"
                        }
                        .collect { progress ->
                            llmDownloadProgress = progress.progress
                        }

                    isLLMDownloading = false

                    // Track download state
                    when (type) {
                        ModelType.QUICK -> isQuickDownloaded = true
                        ModelType.DEEP -> isDeepDownloaded = true
                    }
                }

                // Step 2: Unload current model if one is loaded (OFF MAIN THREAD)
                if (isLLMLoaded) {
                    withContext(Dispatchers.IO) {
                        RunAnywhere.unloadLLMModel()
                    }
                    isLLMLoaded = false
                    activeModel = null
                }

                // Let memory settle after unload
                delay(200)

                // Step 3: Load new model (OFF MAIN THREAD)
                isLLMLoading = true
                withContext(Dispatchers.IO) {
                    RunAnywhere.loadLLMModel(modelId)
                }
                isLLMLoaded = true
                activeModel = type
                isLLMLoading = false

                // Step 4: Pre-warm the model
                prewarmModel()

                refreshModelState()

            } catch (e: Exception) {
                errorMessage = "Model load failed: ${e.message}"
                isLLMLoaded = false
                activeModel = null
            } finally {
                isLLMDownloading = false
                isLLMLoading = false
                modelBeingPrepared = null
            }
        }
    }

    fun downloadAndLoadLLM() {
        downloadAndLoadModel(ModelType.QUICK)
    }

    fun downloadAndLoadSTT() {
        if (isSTTDownloading || isSTTLoading) return

        viewModelScope.launch {
            try {
                errorMessage = null

                if (!isModelDownloaded(STT_MODEL_ID)) {
                    isSTTDownloading = true
                    sttDownloadProgress = 0f

                    RunAnywhere.downloadModel(STT_MODEL_ID)
                        .catch { e ->
                            errorMessage = "STT download failed: ${e.message}"
                        }
                        .collect { progress ->
                            sttDownloadProgress = progress.progress
                        }

                    isSTTDownloading = false
                }

                isSTTLoading = true
                RunAnywhere.loadSTTModel(STT_MODEL_ID)
                isSTTLoaded = true
                isSTTLoading = false

                refreshModelState()
            } catch (e: Exception) {
                errorMessage = "STT load failed: ${e.message}"
                isSTTDownloading = false
                isSTTLoading = false
            }
        }
    }

    fun downloadAndLoadTTS() {
        if (isTTSDownloading || isTTSLoading) return

        viewModelScope.launch {
            try {
                errorMessage = null

                if (!isModelDownloaded(TTS_MODEL_ID)) {
                    isTTSDownloading = true
                    ttsDownloadProgress = 0f

                    RunAnywhere.downloadModel(TTS_MODEL_ID)
                        .catch { e ->
                            errorMessage = "TTS download failed: ${e.message}"
                        }
                        .collect { progress ->
                            ttsDownloadProgress = progress.progress
                        }

                    isTTSDownloading = false
                }

                isTTSLoading = true
                RunAnywhere.loadTTSVoice(TTS_MODEL_ID)
                isTTSLoaded = true
                isTTSLoading = false

                refreshModelState()
            } catch (e: Exception) {
                errorMessage = "TTS load failed: ${e.message}"
                isTTSDownloading = false
                isTTSLoading = false
            }
        }
    }

    fun downloadAndLoadAllModels() {
        viewModelScope.launch {
            if (!isLLMLoaded) downloadAndLoadLLM()
            if (!isSTTLoaded) downloadAndLoadSTT()
            if (!isTTSLoaded) downloadAndLoadTTS()
        }
    }

    fun unloadAllModels() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RunAnywhere.unloadLLMModel()
                    RunAnywhere.unloadSTTModel()
                    RunAnywhere.unloadTTSVoice()
                }
                activeModel = null
                refreshModelState()
            } catch (e: Exception) {
                errorMessage = "Failed to unload models: ${e.message}"
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}