package com.runanywhere.kotlin_starter_example.kodent.engine

data class CodeMetrics(
    val totalLines: Int,
    val codeLines: Int,
    val commentLines: Int,
    val blankLines: Int,
    val functionCount: Int,
    val classCount: Int,
    val avgFunctionLength: Int,
    val maxFunctionLength: Int,
    val maxNestingDepth: Int,
    val cyclomaticComplexity: Int,
    val valCount: Int,
    val varCount: Int,
    val commentPercentage: Int,
    val importCount: Int,
    val longestLine: Int,
    val todoCount: Int
)

object CodeMetricsCalculator {

    fun calculate(code: String): CodeMetrics {
        val lines = code.lines()

        val totalLines = lines.size
        val blankLines = lines.count { it.trim().isEmpty() }
        val commentLines = countCommentLines(lines)
        val codeLines = totalLines - blankLines - commentLines

        val functionCount = Regex("""fun\s+\w+""").findAll(code).count()
        val classCount = Regex("""(?:class|object|interface|enum)\s+\w+""").findAll(code).count()

        val functionLengths = calculateFunctionLengths(lines)
        val avgFunctionLength = if (functionLengths.isNotEmpty()) {
            functionLengths.average().toInt()
        } else 0
        val maxFunctionLength = functionLengths.maxOrNull() ?: 0

        val maxNestingDepth = calculateMaxNesting(lines)
        val cyclomaticComplexity = calculateCyclomaticComplexity(lines)

        val valCount = Regex("""(?<!\w)val\s+""").findAll(code).count()
        val varCount = Regex("""(?<!\w)var\s+""").findAll(code).count()

        val commentPercentage = if (totalLines > 0) {
            (commentLines * 100) / totalLines
        } else 0

        val importCount = lines.count { it.trim().startsWith("import ") }
        val longestLine = lines.maxOfOrNull { it.length } ?: 0

        val todoCount = lines.count { line ->
            val trimmed = line.trim().lowercase()
            trimmed.contains("todo") || trimmed.contains("fixme") || trimmed.contains("hack")
        }

        return CodeMetrics(
            totalLines = totalLines,
            codeLines = codeLines,
            commentLines = commentLines,
            blankLines = blankLines,
            functionCount = functionCount,
            classCount = classCount,
            avgFunctionLength = avgFunctionLength,
            maxFunctionLength = maxFunctionLength,
            maxNestingDepth = maxNestingDepth,
            cyclomaticComplexity = cyclomaticComplexity,
            valCount = valCount,
            varCount = varCount,
            commentPercentage = commentPercentage,
            importCount = importCount,
            longestLine = longestLine,
            todoCount = todoCount
        )
    }

    private fun countCommentLines(lines: List<String>): Int {
        var count = 0
        var inBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                inBlock -> {
                    count++
                    if (trimmed.contains("*/")) inBlock = false
                }
                trimmed.startsWith("//") -> count++
                trimmed.startsWith("/*") -> {
                    count++
                    inBlock = !trimmed.contains("*/")
                }
                trimmed.startsWith("*") -> count++
            }
        }
        return count
    }

    private fun calculateFunctionLengths(lines: List<String>): List<Int> {
        val lengths = mutableListOf<Int>()
        var funcStart = -1
        var braceCount = 0

        lines.forEachIndexed { index, line ->
            if (Regex("""fun\s+\w+\s*\(""").containsMatchIn(line) && funcStart == -1) {
                funcStart = index
                braceCount = 0
            }
            if (funcStart >= 0) {
                braceCount += line.count { it == '{' } - line.count { it == '}' }
                if (braceCount <= 0 && index > funcStart) {
                    lengths.add(index - funcStart + 1)
                    funcStart = -1
                }
            }
        }
        return lengths
    }

    private fun calculateMaxNesting(lines: List<String>): Int {
        var maxNesting = 0
        var currentNesting = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue
            currentNesting += line.count { it == '{' } - line.count { it == '}' }
            if (currentNesting > maxNesting) maxNesting = currentNesting
        }
        return maxNesting
    }

    private fun calculateCyclomaticComplexity(lines: List<String>): Int {
        var complexity = 1 // Base complexity

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue

            if (Regex("""(?<!\w)if\s*\(""").containsMatchIn(line)) complexity++
            if (line.contains("else if")) complexity++
            if (Regex("""(?<!\w)when\s*[\({]""").containsMatchIn(line)) complexity++
            if (Regex("""(?<!\w)(for|while)\s*\(""").containsMatchIn(line)) complexity++
            if (line.contains("catch")) complexity++
            complexity += line.split("&&").size - 1
            complexity += line.split("||").size - 1
            if (line.contains("?:")) complexity++
            if (line.contains("->") && !line.contains("fun")) complexity++ // when branches
        }
        return complexity
    }
}