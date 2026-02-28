package com.runanywhere.kotlin_starter_example.kodent.engine

data class GenConfig(
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val repeatPenalty: Float,
    val stopSequences: List<String> = listOf(
        "<|im_end|>", "<|endoftext|>", "<|end|>",
        "<|EOT|>", "\n\n\n", "User:", "Human:"
    )
)

object GenerationConfig {

    fun forMode(mode: String): GenConfig = when (mode) {
        "Explain" -> GenConfig(
            temperature = 0.2f, topP = 0.9f,
            maxTokens = 180, repeatPenalty = 1.3f
        )
        "Debug" -> GenConfig(
            temperature = 0.05f, topP = 0.85f,
            maxTokens = 160, repeatPenalty = 1.4f
        )
        "Optimize" -> GenConfig(
            temperature = 0.15f, topP = 0.9f,
            maxTokens = 200, repeatPenalty = 1.3f
        )
        "Complexity" -> GenConfig(
            temperature = 0.01f, topP = 0.8f,
            maxTokens = 60, repeatPenalty = 1.2f
        )
        "Health" -> GenConfig(
            temperature = 0.01f,
            topP = 0.9f,
            maxTokens = 150,
            repeatPenalty = 1.3f
        )
        else -> GenConfig(
            temperature = 0.1f, topP = 0.9f,
            maxTokens = 180, repeatPenalty = 1.3f
        )
    }
}