package com.runanywhere.kotlin_starter_example.kodent.engine

object OutputProcessor {

    fun process(raw: String, mode: String): String {
        var result = raw.trim()
        result = removeArtifacts(result)
        result = cutAtRepetition(result)
        result = trimIncompleteSentence(result)
        result = when (mode) {
            "Debug" -> polishDebug(result)
            "Complexity" -> polishComplexity(result)
            else -> result
        }
        return result.trim()
    }

    private fun removeArtifacts(text: String): String {
        return text
            .replace("<|im_start|>", "")
            .replace("<|im_end|>", "")
            .replace("<|endoftext|>", "")
            .replace("<|end|>", "")
            .replace("<|EOT|>", "")
            .replace(Regex("```\\w*\\s*$"), "")
            .replace(Regex("^```\\w*\\s*\\n"), "")
            .replace(Regex("(?i)^(assistant|system|user)\\s*:?\\s*"), "")
            .trim()
    }

    private fun cutAtRepetition(text: String): String {
        val lines = text.split("\n")
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()

        for (line in lines) {
            val normalized = line.trim().lowercase()
            if (normalized.length < 5) { result.add(line); continue }
            if (normalized in seen) break
            val nearDup = seen.any {
                normalized.length > 15 && it.length > 15 &&
                        normalized.take(25) == it.take(25)
            }
            if (nearDup) break
            seen.add(normalized)
            result.add(line)
        }
        return result.joinToString("\n")
    }

    private fun trimIncompleteSentence(text: String): String {
        if (text.isEmpty()) return text
        if (text.last() in setOf('.', '!', '?', ')', ']', '}', ':')) return text
        val lastEnd = text.lastIndexOfAny(charArrayOf('.', '!', '?', ':'))
        return if (lastEnd > text.length * 0.4) text.substring(0, lastEnd + 1) else text
    }

    private fun polishDebug(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("no bug") || lower.contains("no issue") ||
                    lower.contains("looks correct") || text.isBlank() ->
                "✅ No bugs detected.\n\n${text.ifBlank { "The code appears correct." }}"
            else -> "🐛 Issues found:\n\n$text"
        }
    }

    private fun polishComplexity(text: String): String {
        return "📊 $text"
    }
}

class StreamRepetitionDetector {
    private val recentTokens = ArrayDeque<String>()
    private val trigramCounts = mutableMapOf<String, Int>()

    fun shouldStop(token: String): Boolean {
        recentTokens.addLast(token)
        if (recentTokens.size > 60) recentTokens.removeFirst()

        if (recentTokens.size >= 3) {
            val trigram = recentTokens.toList().takeLast(3).joinToString("").trim()
            if (trigram.length >= 5) {
                val count = trigramCounts.getOrDefault(trigram, 0) + 1
                trigramCounts[trigram] = count
                if (count >= 3) return true
            }
        }

        if (recentTokens.size >= 5) {
            val last5 = recentTokens.toList().takeLast(5)
            if (last5.all { it.trim() == last5.first().trim() && it.isNotBlank() })
                return true
        }

        return false
    }

    fun reset() {
        recentTokens.clear()
        trigramCounts.clear()
    }
}