package com.runanywhere.kotlin_starter_example.kodent.engine

import com.runanywhere.kotlin_starter_example.kodent.engine.CodeMetrics

data class CodeHealthResult(
    val overallScore: Int,
    val bugRisk: Int,
    val performance: Int,
    val security: Int,
    val readability: Int,
    val complexity: Int,
    val issues: List<HealthIssue>,
    val summary: String,
    val metrics: CodeMetrics? = null,
    val bestPractices: List<String> = emptyList()
)

data class HealthIssue(
    val severity: IssueSeverity,
    val title: String,
    val description: String
)

enum class IssueSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

object CodeHealthParser {

    fun parse(rawOutput: String): CodeHealthResult {
        val text = rawOutput.lowercase()
        val issues = extractIssues(rawOutput)

        // Calculate scores based on what problems were found
        val bugRisk = calculateBugScore(text, issues)
        val performance = calculatePerfScore(text, issues)
        val security = calculateSecurityScore(text, issues)
        val readability = calculateReadabilityScore(text, issues)
        val complexity = calculateComplexityScore(text, issues)

        val overallScore = calculateOverall(
            bugRisk, performance, security, readability, complexity
        )

        val summary = buildSummary(overallScore)

        return CodeHealthResult(
            overallScore = overallScore,
            bugRisk = bugRisk,
            performance = performance,
            security = security,
            readability = readability,
            complexity = complexity,
            issues = issues,
            summary = summary
        )
    }

    fun parseWithCode(rawOutput: String, originalCode: String): CodeHealthResult {
        val text = rawOutput.lowercase()
        val code = originalCode.lowercase()
        val issues = extractIssues(rawOutput)

        // Scan both AI output AND original code
        val combinedText = text + "\n" + scanCodeDirectly(code)

        val bugRisk = calculateBugScore(combinedText, issues)
        val performance = calculatePerfScore(combinedText, issues)
        val security = calculateSecurityScore(combinedText, issues)
        val readability = calculateReadabilityScore(combinedText, issues)
        val complexity = calculateComplexityScore(combinedText, issues)

        val overallScore = calculateOverall(
            bugRisk, performance, security, readability, complexity
        )

        val summary = buildSummary(overallScore)

        return CodeHealthResult(
            overallScore = overallScore,
            bugRisk = bugRisk,
            performance = performance,
            security = security,
            readability = readability,
            complexity = complexity,
            issues = issues,
            summary = summary
        )
    }

    private fun scanCodeDirectly(code: String): String {
        val findings = mutableListOf<String>()

        // Security checks
        if (code.contains("\"sk-") || code.contains("\"api") ||
            code.contains("password") || code.contains("secret")) {
            findings.add("hardcoded api key or secret found")
        }

        // Performance checks
        val forCount = Regex("for\\s*\\(").findAll(code).count()
        if (forCount >= 2) {
            findings.add("nested loop detected slow performance")
        }
        if (code.contains("result = result +") || code.contains("result +=")) {
            if (code.contains("for") || code.contains("while")) {
                findings.add("string concatenation in loop slow")
            }
        }

        // Bug checks
        if (code.contains("[") && !code.contains("coerceIn") &&
            !code.contains("getOrNull") && !code.contains("if")) {
            findings.add("possible index out of bounds crash")
        }
        if (!code.contains("?") && !code.contains("null") &&
            code.contains("val ") && code.contains(".")) {
            findings.add("no null safety checks")
        }

        // Readability checks
        val lines = code.lines()
        if (lines.size > 30) {
            findings.add("long function complex readability")
        }
        if (code.contains("var ") && code.count { it == '=' } > 5) {
            findings.add("too many mutable variables")
        }

        // Complexity checks
        val nestingDepth = code.count { it == '{' }
        if (nestingDepth > 4) {
            findings.add("deeply nested code complex")
        }

        return findings.joinToString("\n")
    }

    private fun calculateBugScore(text: String, issues: List<HealthIssue>): Int {
        var score = 10

        // Deduct for bug-related keywords
        if (text.contains("null")) score -= 2
        if (text.contains("crash")) score -= 3
        if (text.contains("exception")) score -= 2
        if (text.contains("index") && text.contains("bound")) score -= 2
        if (text.contains("error")) score -= 1
        if (text.contains("bug")) score -= 2
        if (text.contains("uninitialized")) score -= 2
        if (text.contains("undefined")) score -= 2
        if (text.contains("overflow")) score -= 2
        if (text.contains("type mismatch")) score -= 2
        if (text.contains("cast")) score -= 1
        if (text.contains("no bug") || text.contains("no issue") || text.contains("bug-free")) score += 3

        // Deduct for number of critical/high issues
        val bugIssues = issues.count {
            it.severity == IssueSeverity.CRITICAL || it.severity == IssueSeverity.HIGH
        }
        score -= bugIssues

        return score.coerceIn(1, 10)
    }

    private fun calculatePerfScore(text: String, issues: List<HealthIssue>): Int {
        var score = 8

        if (text.contains("o(n^2)") || text.contains("o(n²)")) score -= 3
        if (text.contains("o(n^3)") || text.contains("o(n³)")) score -= 4
        if (text.contains("nested loop")) score -= 2
        if (text.contains("concatenation") && text.contains("loop")) score -= 2
        if (text.contains("slow")) score -= 2
        if (text.contains("inefficient")) score -= 2
        if (text.contains("performance")) score -= 1
        if (text.contains("memory")) score -= 1
        if (text.contains("allocation")) score -= 1
        if (text.contains("optimal") || text.contains("efficient")) score += 2
        if (text.contains("o(1)") || text.contains("o(n)") || text.contains("o(log")) score += 1

        return score.coerceIn(1, 10)
    }

    private fun calculateSecurityScore(text: String, issues: List<HealthIssue>): Int {
        var score = 8

        if (text.contains("hardcoded") || text.contains("hard-coded") || text.contains("hard coded")) score -= 4
        if (text.contains("api key") || text.contains("apikey") || text.contains("api_key")) score -= 3
        if (text.contains("password")) score -= 3
        if (text.contains("secret")) score -= 3
        if (text.contains("injection")) score -= 3
        if (text.contains("sql")) score -= 2
        if (text.contains("vulnerability") || text.contains("vulnerable")) score -= 2
        if (text.contains("no validation") || text.contains("without validation")) score -= 2
        if (text.contains("unsafe")) score -= 2
        if (text.contains("plaintext") || text.contains("plain text")) score -= 2
        if (text.contains("expose") || text.contains("leak")) score -= 2
        if (text.contains("secure") && !text.contains("insecure") && !text.contains("not secure")) score += 2

        return score.coerceIn(1, 10)
    }

    private fun calculateReadabilityScore(text: String, issues: List<HealthIssue>): Int {
        var score = 7

        if (text.contains("naming")) score -= 1
        if (text.contains("confusing")) score -= 2
        if (text.contains("unclear")) score -= 2
        if (text.contains("complex") && !text.contains("complexity")) score -= 1
        if (text.contains("long method") || text.contains("long function")) score -= 1
        if (text.contains("comment")) score -= 1
        if (text.contains("magic number")) score -= 1
        if (text.contains("readable") || text.contains("clean") || text.contains("well")) score += 2

        // Fewer issues = better readability
        if (issues.size <= 1) score += 1
        if (issues.size >= 4) score -= 1

        return score.coerceIn(1, 10)
    }

    private fun calculateComplexityScore(text: String, issues: List<HealthIssue>): Int {
        var score = 7

        if (text.contains("nested")) score -= 2
        if (text.contains("recursive") || text.contains("recursion")) score -= 1
        if (text.contains("too complex") || text.contains("very complex")) score -= 2
        if (text.contains("deeply nested")) score -= 2
        if (text.contains("multiple loop")) score -= 1
        if (text.contains("simple") || text.contains("straightforward")) score += 2
        if (text.contains("short") || text.contains("concise")) score += 1

        return score.coerceIn(1, 10)
    }

    private fun extractIssues(rawOutput: String): List<HealthIssue> {
        val issues = mutableListOf<HealthIssue>()
        val lines = rawOutput.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val isIssueLine = trimmed.startsWith("-") ||
                    trimmed.startsWith("•") ||
                    trimmed.startsWith("*") ||
                    trimmed.matches(Regex("^\\d+[.)]\\s+.*"))

            if (isIssueLine) {
                val content = trimmed
                    .removePrefix("-")
                    .removePrefix("•")
                    .removePrefix("*")
                    .replace(Regex("^\\d+[.)]\\s*"), "")
                    .trim()

                if (content.length > 5) {
                    val severity = detectSeverity(content)
                    issues.add(
                        HealthIssue(
                            severity = severity,
                            title = generateTitle(content),
                            description = content
                        )
                    )
                }
            }
        }

        return issues.take(5)
    }

    private fun detectSeverity(text: String): IssueSeverity {
        val lower = text.lowercase()
        return when {
            lower.contains("critical") || lower.contains("crash") ||
                    lower.contains("injection") || lower.contains("hardcoded") ||
                    lower.contains("hard-coded") || lower.contains("leak") ||
                    lower.contains("password") || lower.contains("secret") -> IssueSeverity.CRITICAL

            lower.contains("null") || lower.contains("exception") ||
                    lower.contains("unsafe") || lower.contains("security") ||
                    lower.contains("vulnerability") || lower.contains("index") -> IssueSeverity.HIGH

            lower.contains("improve") || lower.contains("should") ||
                    lower.contains("consider") || lower.contains("naming") ||
                    lower.contains("performance") || lower.contains("slow") -> IssueSeverity.MEDIUM

            else -> IssueSeverity.LOW
        }
    }

    private fun generateTitle(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.contains("null") -> "Null Safety"
            lower.contains("hardcoded") || lower.contains("hard-coded") -> "Hardcoded Value"
            lower.contains("api key") || lower.contains("secret") || lower.contains("password") -> "Exposed Secret"
            lower.contains("injection") -> "Injection Risk"
            lower.contains("validation") || lower.contains("validate") -> "Missing Validation"
            lower.contains("loop") || lower.contains("nested") -> "Performance"
            lower.contains("slow") || lower.contains("inefficient") -> "Performance"
            lower.contains("concatenat") -> "String Performance"
            lower.contains("memory") || lower.contains("leak") -> "Memory Issue"
            lower.contains("error") || lower.contains("exception") -> "Error Handling"
            lower.contains("naming") || lower.contains("name") -> "Naming"
            lower.contains("index") || lower.contains("bound") -> "Index Safety"
            lower.contains("type") -> "Type Safety"
            lower.contains("complex") -> "Complexity"
            lower.contains("security") -> "Security"
            lower.contains("readab") -> "Readability"
            else -> "Issue Found"
        }
    }

    private fun buildSummary(score: Int): String {
        return when {
            score >= 80 -> "Code is well-written with minor improvements possible."
            score >= 60 -> "Code is decent but has some areas to improve."
            score >= 40 -> "Code needs attention in several areas."
            else -> "Code has significant issues that should be addressed."
        }
    }

    private fun calculateOverall(
        bug: Int, perf: Int, security: Int, readability: Int, complexity: Int
    ): Int {
        val weighted = (bug * 25 + security * 25 + perf * 20 +
                readability * 15 + complexity * 15) / 10
        return weighted.coerceIn(0, 100)
    }
}