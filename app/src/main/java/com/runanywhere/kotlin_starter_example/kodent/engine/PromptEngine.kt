package com.runanywhere.kotlin_starter_example.kodent.engine

object PromptEngine {

    fun build(
        code: String,
        mode: String,
        language: String,
        modelType: String?
    ): String {
        val trimmedCode = truncateCode(code, maxChars = 800)
        val lang = language.lowercase()
        val isDeep = modelType == "deep"

        return if (isDeep) {
            buildDeepPrompt(trimmedCode, mode, lang)
        } else {
            buildQuickPrompt(trimmedCode, mode, lang)
        }
    }

    // ──────────────────────────────────────
    // Quick Mode Prompts (360M model)
    // ──────────────────────────────────────

    private fun buildQuickPrompt(
        code: String,
        mode: String,
        lang: String
    ): String {
        return when (mode) {

            "Explain" -> {
                "Explain this " + lang + " code in 2-3 sentences:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Explanation:"
            }

            "Debug" -> {
                "Find bugs in this " + lang + " code. " +
                        "One per line. If none, say \"No bugs found.\"\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Bugs:"
            }

            "Optimize" -> {
                "Suggest 1-3 improvements for this " + lang + " code:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Improvements:"
            }

            "Complexity" -> {
                "State time and space complexity in Big-O:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Time: O("
            }

            else -> {
                "Analyze this " + lang + " code:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Analysis:"
            }
        }
    }

    // ──────────────────────────────────────
    // Deep Mode Prompts (1.5B model)
    // ──────────────────────────────────────

    private fun buildDeepPrompt(
        code: String,
        mode: String,
        lang: String
    ): String {
        return when (mode) {

            "Explain" -> {
                "Analyze this " + lang + " code step by step. " +
                        "Explain purpose, logic, and return value:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Detailed explanation:"
            }

            "Debug" -> {
                "Review this " + lang + " code for all bugs: " +
                        "null issues, type errors, logic errors, edge cases. " +
                        "List each with context:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Issues found:"
            }

            "Optimize" -> {
                "Suggest concrete improvements for this " + lang + " code. " +
                        "Show improved code where applicable:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Optimizations:"
            }

            "Complexity" -> {
                "Analyze time and space complexity. " +
                        "Examine loops, recursion, data structures. " +
                        "Provide Big-O with justification:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Complexity Analysis:\n" +
                        "Time: O("
            }

            else -> {
                "Analyze this " + lang + " code:\n" +
                        "```" + lang + "\n" +
                        code + "\n" +
                        "```\n" +
                        "Analysis:"
            }
        }
    }

    // ──────────────────────────────────────
    // Code Truncation
    // ──────────────────────────────────────

    private fun truncateCode(code: String, maxChars: Int): String {
        val trimmed = code.trim()
        if (trimmed.length <= maxChars) return trimmed

        val cutPoint = trimmed.lastIndexOf('\n', maxChars)
        val safeCut = if (cutPoint > maxChars * 0.5) {
            cutPoint
        } else {
            maxChars
        }

        return trimmed.substring(0, safeCut) + "\n// ... (truncated)"
    }
}