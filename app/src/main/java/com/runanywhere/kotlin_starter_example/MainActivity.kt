package com.runanywhere.kotlin_starter_example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runanywhere.kotlin_starter_example.kodent.ui.KodentAnalyzerScreen
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.ui.screens.*
import com.runanywhere.kotlin_starter_example.ui.theme.KotlinStarterTheme
import com.runanywhere.sdk.core.onnx.ONNX
import com.runanywhere.sdk.foundation.bridge.extensions.CppBridgeModelPaths
import com.runanywhere.sdk.llm.llamacpp.LlamaCPP
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.SDKEnvironment
import com.runanywhere.sdk.storage.AndroidPlatformContext
import java.io.File
import com.runanywhere.kotlin_starter_example.kodent.ui.KodentHomeScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initializeSDK()

        setContent {
            KotlinStarterTheme {
                RunAnywhereApp()
            }
        }
    }

    private fun initializeSDK() {

        // 1️⃣ Initialize Android storage context
        AndroidPlatformContext.initialize(this)

        // 2️⃣ Initialize SDK (Development mode)
        RunAnywhere.initialize(environment = SDKEnvironment.DEVELOPMENT)

        // 3️⃣ Set model storage directory
        val runAnywhereDir = File(filesDir, "runanywhere").absolutePath
        CppBridgeModelPaths.setBaseDirectory(runAnywhereDir)

        // 4️⃣ Register inference backends
        LlamaCPP.register(priority = 100)   // LLM (GGUF)
        ONNX.register(priority = 100)       // STT / TTS (ONNX)

        // 5️⃣ Register default models
        ModelService.registerDefaultModels()
    }
}

@Composable
fun RunAnywhereApp() {

    val navController = rememberNavController()
    val modelService: ModelService = viewModel()

    NavHost(
        navController = navController,
        startDestination = "kodent_home"   // 🔥 Kodent is now default
    ) {

        // 🔹 Kodent Home
        composable("kodent_home") {
            KodentHomeScreen(
                onAnalyzeClick = {
                    navController.navigate("kodent_analyzer")
                },
                modelService = modelService
            )
        }

        // 🔥 Kodent Analyzer Screen
        composable("kodent_analyzer") {
            KodentAnalyzerScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }

        // Old demo screens (still accessible if needed)
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToSTT = { navController.navigate("stt") },
                onNavigateToTTS = { navController.navigate("tts") },
                onNavigateToVoicePipeline = { navController.navigate("voice_pipeline") },
                onNavigateToToolCalling = { navController.navigate("tool_calling") }
            )
        }

        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }

        composable("stt") {
            SpeechToTextScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }

        composable("tts") {
            TextToSpeechScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }

        composable("voice_pipeline") {
            VoicePipelineScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }

        composable("tool_calling") {
            ToolCallingScreen(
                onNavigateBack = { navController.popBackStack() },
                modelService = modelService
            )
        }
    }
}
