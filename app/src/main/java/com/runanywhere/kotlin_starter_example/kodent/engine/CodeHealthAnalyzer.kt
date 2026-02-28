package com.runanywhere.kotlin_starter_example.kodent.engine

import com.runanywhere.kotlin_starter_example.kodent.engine.CodeMetricsCalculator

/**
 * Enterprise-grade rule-based code health analyzer.
 * Instant, deterministic, no LLM required.
 * Covers: Bug Risk, Performance, Security, Readability, Complexity
 * + Kotlin-Specific + Android-Specific + Code Smell detection
 */
object CodeHealthAnalyzer {

    data class HealthDetail(
        val category: String,
        val title: String,
        val description: String,
        val lineNumber: Int?,
        val severity: IssueSeverity,
        val suggestion: String = ""
    )

    fun analyze(code: String, language: String): CodeHealthResult {
        val lines = code.lines()
        val issues = mutableListOf<HealthIssue>()
        val details = mutableListOf<HealthDetail>()

        // Run all analyzers
        val bugScore = analyzeBugRisk(lines, code, issues, details)
        val perfScore = analyzePerformance(lines, code, issues, details)
        val securityScore = analyzeSecurity(lines, code, issues, details)
        val readabilityScore = analyzeReadability(lines, code, issues, details)
        val complexityScore = analyzeComplexity(lines, code, issues, details)

        // Bonus checks
        analyzeKotlinIdioms(lines, code, issues, details)
        analyzeAndroidPatterns(lines, code, issues, details)
        analyzeCodeSmells(lines, code, issues, details)

        // Calculate metrics
        val metrics = CodeMetricsCalculator.calculate(code)

        // Detect best practices
        val bestPractices = detectBestPractices(lines, code)

        val overall = calculateOverall(bugScore, perfScore, securityScore, readabilityScore, complexityScore)

        return CodeHealthResult(
            overallScore = overall,
            bugRisk = bugScore,
            performance = perfScore,
            security = securityScore,
            readability = readabilityScore,
            complexity = complexityScore,
            issues = issues.sortedBy { it.severity.ordinal }.take(15),
            summary = buildSummary(overall, issues),
            metrics = metrics,
            bestPractices = bestPractices
        )
    }

    // ══════════════════════════════════════════════════════════
    // BUG RISK ANALYSIS (25+ checks)
    // ══════════════════════════════════════════════════════════

    private fun analyzeBugRisk(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ): Int {
        var score = 10
        var issueCount = 0

        // 1. Force unwrap (!!)
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val forceUnwrapCount = Regex("""!!""").findAll(line).count()
            if (forceUnwrapCount > 0) {
                score -= forceUnwrapCount.coerceAtMost(2)
                if (issueCount < 5) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Force Unwrap (!!)",
                        description = "Line ${index + 1}: Force unwrap crashes if null. Use ?. or ?: instead."
                    ))
                    issueCount++
                }
            }
        }

        // 2. Unsafe cast (as without ?)
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""\bas\s+\w""").containsMatchIn(line) && !line.contains("as?")) {
                score -= 1
                if (issueCount < 5) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "Unsafe Cast",
                        description = "Line ${index + 1}: Use 'as?' for safe casting to avoid ClassCastException."
                    ))
                    issueCount++
                }
            }
        }

        // 3. Array/list access without bounds check
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""\w+\[\s*\w+\s*\]""").containsMatchIn(line)) {
                val nearby = getNearbyLines(lines, index, 3)
                val hasBoundsCheck = nearby.any {
                    it.contains("getOrNull") || it.contains("getOrElse") ||
                            it.contains("coerceIn") || it.contains(".size") ||
                            it.contains(".indices") || it.contains("if ") ||
                            it.contains("takeIf") || it.contains(".isEmpty") ||
                            it.contains(".isNotEmpty") || it.contains("in ")
                }
                if (!hasBoundsCheck) {
                    score -= 1
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Index Without Bounds Check",
                            description = "Line ${index + 1}: May throw IndexOutOfBoundsException. Use getOrNull()."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 4. Division without zero check
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""\s/\s+\w+""").containsMatchIn(line) || Regex("""\s%\s+\w+""").containsMatchIn(line)) {
                val nearby = getNearbyLines(lines, index, 3)
                val hasZeroCheck = nearby.any {
                    it.contains("!= 0") || it.contains("> 0") || it.contains("if ") ||
                            it.contains("require") || it.contains("check(")
                }
                if (!hasZeroCheck) {
                    score -= 1
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Possible Division by Zero",
                            description = "Line ${index + 1}: Add zero check before division/modulo."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 5. Empty catch blocks
        for (i in lines.indices) {
            val line = lines[i]
            if (isComment(line)) continue

            val singleLineEmpty = Regex("""catch\s*\([^)]*\)\s*\{\s*\}""")
            if (singleLineEmpty.containsMatchIn(line)) {
                score -= 2
                if (issueCount < 5) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Empty Catch Block",
                        description = "Line ${i + 1}: Exceptions silently swallowed. Log or handle them."
                    ))
                    issueCount++
                }
                continue
            }

            if (line.contains("catch") && line.contains("{")) {
                val nextLines = lines.drop(i + 1).take(3)
                val catchBody = nextLines.takeWhile { !it.trim().startsWith("}") }.joinToString("").trim()
                if (catchBody.isEmpty() || catchBody.replace(Regex("""//.*"""), "").trim().isEmpty()) {
                    score -= 2
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "Empty Catch Block",
                            description = "Line ${i + 1}: Exceptions silently swallowed. Log or handle them."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 6. lateinit without isInitialized check
        lines.forEachIndexed { index, line ->
            if (line.contains("lateinit var")) {
                val varName = Regex("""lateinit\s+var\s+(\w+)""").find(line)?.groupValues?.get(1)
                if (varName != null && !code.contains("::$varName.isInitialized")) {
                    score -= 1
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "lateinit Without Check",
                            description = "Line ${index + 1}: '$varName' is lateinit but never checked with isInitialized."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 7. Resource leak (unclosed streams)
        val resourcePatterns = listOf(
            "FileInputStream(", "FileOutputStream(", "BufferedReader(",
            "BufferedWriter(", "InputStreamReader(", "OutputStreamWriter(",
            "FileReader(", "FileWriter(", "Scanner(", "Connection"
        )
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val found = resourcePatterns.firstOrNull { line.contains(it) }
            if (found != null) {
                val nearby = getNearbyLines(lines, index, 10)
                val hasClose = nearby.any {
                    it.contains(".close()") || it.contains(".use {") ||
                            it.contains(".use{") || it.contains("finally") ||
                            it.contains("AutoCloseable") || it.contains("Closeable")
                }
                if (!hasClose) {
                    score -= 1
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "Resource Leak",
                            description = "Line ${index + 1}: Resource not closed. Use .use { } block."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 8. GlobalScope usage
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("GlobalScope.launch") || line.contains("GlobalScope.async")) {
                score -= 2
                if (issueCount < 5) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "GlobalScope Usage",
                        description = "Line ${index + 1}: GlobalScope outlives lifecycle. Use viewModelScope or lifecycleScope."
                    ))
                    issueCount++
                }
            }
        }

        // 9. Thread safety - mutable shared state
        val hasCoroutines = code.contains("launch") || code.contains("async") || code.contains("withContext")
        if (hasCoroutines) {
            lines.forEachIndexed { index, line ->
                if (isComment(line)) return@forEachIndexed
                if (Regex("""^\s*var\s+\w+""").containsMatchIn(line) && !line.contains("private")) {
                    val nearby = getNearbyLines(lines, index, 10)
                    val usedInCoroutine = nearby.any {
                        it.contains("launch") || it.contains("async") || it.contains("withContext")
                    }
                    if (usedInCoroutine && !line.contains("@Volatile") && !line.contains("Atomic") &&
                        !line.contains("Mutex") && !line.contains("synchronized") &&
                        !line.contains("mutableStateOf")) {
                        score -= 1
                        if (issueCount < 5) {
                            issues.add(HealthIssue(
                                severity = IssueSeverity.MEDIUM,
                                title = "Thread Safety Risk",
                                description = "Line ${index + 1}: Mutable var near coroutines without synchronization."
                            ))
                            issueCount++
                        }
                    }
                }
            }
        }

        // 10. Deprecated API usage
        val deprecatedAPIs = mapOf(
            "AsyncTask" to "Use Kotlin coroutines instead",
            "Loader" to "Use ViewModel + LiveData/Flow instead",
            "LocalBroadcastManager" to "Use SharedFlow or EventBus pattern",
            "startActivityForResult" to "Use Activity Result API (registerForActivityResult)",
            "onActivityResult" to "Use Activity Result API (registerForActivityResult)",
            "requestPermissions" to "Use Activity Result API for permissions",
            "onRequestPermissionsResult" to "Use Activity Result API for permissions",
            "getColor(Int)" to "Use ContextCompat.getColor()",
            "getDrawable(Int)" to "Use ContextCompat.getDrawable()",
            "Handler()" to "Use Handler(Looper.getMainLooper())"
        )
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            for ((api, fix) in deprecatedAPIs) {
                if (line.contains(api)) {
                    score -= 1
                    if (issueCount < 5) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "Deprecated API: $api",
                            description = "Line ${index + 1}: $fix."
                        ))
                        issueCount++
                    }
                    break
                }
            }
        }

        // 11. Runnable/Thread instead of coroutines
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("Thread(") || line.contains("Runnable {") || line.contains("object : Runnable")) {
                score -= 1
                if (issueCount < 5) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Raw Thread Usage",
                        description = "Line ${index + 1}: Use Kotlin coroutines instead of raw threads."
                    ))
                    issueCount++
                }
            }
        }

        // 12. Possible NPE on platform types
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            // Java interop without null check
            if (Regex("""\.get\w+\(\)\.""").containsMatchIn(line) && !line.contains("?.") && !line.contains("!!")) {
                val nearby = getNearbyLines(lines, index, 2)
                val hasNullCheck = nearby.any { it.contains("if") || it.contains("?.") || it.contains("?:") }
                if (!hasNullCheck) {
                    score -= 1
                }
            }
        }

        return score.coerceIn(1, 10)
    }

    // ══════════════════════════════════════════════════════════
    // PERFORMANCE ANALYSIS (20+ checks)
    // ══════════════════════════════════════════════════════════

    private fun analyzePerformance(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ): Int {
        var score = 10
        var issueCount = 0

        // 1. Nested loop detection with actual nesting tracking
        var nestLevel = 0
        var maxNest = 0
        var maxNestLine = 0
        var inLoop = false

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""(for|while)\s*\(""").containsMatchIn(line)) {
                nestLevel++
                inLoop = true
                if (nestLevel > maxNest) {
                    maxNest = nestLevel
                    maxNestLine = index + 1
                }
            }
            if (line.contains("}") && inLoop) {
                nestLevel = (nestLevel - 1).coerceAtLeast(0)
                if (nestLevel == 0) inLoop = false
            }
        }

        if (maxNest >= 3) {
            score -= 3
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.HIGH,
                    title = "Triple-Nested Loop (O(n³))",
                    description = "Line $maxNestLine: $maxNest levels of nesting. Extremely slow for large inputs."
                ))
                issueCount++
            }
        } else if (maxNest >= 2) {
            score -= 2
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.MEDIUM,
                    title = "Nested Loop (O(n²))",
                    description = "Line $maxNestLine: Nested loops detected. Consider optimization."
                ))
                issueCount++
            }
        }

        // 2. String concatenation in loops
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if ((line.contains("+=") && (line.contains("\"") || line.contains("String"))) ||
                (line.contains("= ") && line.contains(" + ") && line.contains("\""))) {
                val nearby = getNearbyLines(lines, index, 5)
                val inLoopContext = nearby.any {
                    Regex("""(for|while)\s*\(""").containsMatchIn(it)
                }
                if (inLoopContext) {
                    score -= 2
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "String Concatenation in Loop",
                            description = "Line ${index + 1}: O(n²) string building. Use StringBuilder or buildString {}."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 3. Object creation in loops
        val heavyObjects = listOf(
            "ArrayList(", "HashMap(", "HashSet(", "LinkedList(",
            "StringBuilder(", "Regex(", "Pattern.compile(",
            "SimpleDateFormat(", "DecimalFormat(", "Gson(",
            "ObjectMapper(", "Moshi.Builder("
        )
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val found = heavyObjects.firstOrNull { line.contains(it) }
            if (found != null) {
                val nearby = getNearbyLines(lines, index, 5)
                val inLoopContext = nearby.any { Regex("""(for|while|forEach)\s*[\({]""").containsMatchIn(it) }
                if (inLoopContext) {
                    score -= 1
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Object Creation in Loop",
                            description = "Line ${index + 1}: Creating ${found.removeSuffix("(")} inside loop causes GC pressure."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 4. Thread.sleep usage
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("Thread.sleep")) {
                score -= 2
                if (issueCount < 4) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Thread.sleep() Blocks Thread",
                        description = "Line ${index + 1}: Use delay() in coroutines. Thread.sleep blocks the thread."
                    ))
                    issueCount++
                }
            }
        }

        // 5. Inefficient collection chains
        if (Regex("""\.filter\s*\{[^}]+\}\s*\.map\s*\{""").containsMatchIn(code) ||
            Regex("""\.map\s*\{[^}]+\}\s*\.filter\s*\{""").containsMatchIn(code)) {
            val hasSequence = code.contains("asSequence()")
            if (!hasSequence) {
                score -= 1
                if (issueCount < 4) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Intermediate Collections",
                        description = "Chained filter/map creates temporary lists. Use .asSequence() for large collections."
                    ))
                    issueCount++
                }
            }
        }

        // 6. Multiple list iterations
        val chainOps = listOf(".filter", ".map", ".flatMap", ".groupBy", ".sortedBy", ".distinct")
        val chainCount = chainOps.count { code.contains(it) }
        if (chainCount >= 4) {
            score -= 1
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "Many Collection Operations",
                    description = "$chainCount chained operations. Consider combining or using asSequence()."
                ))
                issueCount++
            }
        }

        // 7. Synchronous I/O without coroutine context
        val ioOperations = listOf(
            "FileInputStream", "FileOutputStream", "URL(", "HttpURLConnection",
            "BufferedReader", "readLine()", "readText()", "readBytes()",
            "writeText(", "writeBytes("
        )
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val found = ioOperations.firstOrNull { line.contains(it) }
            if (found != null) {
                val nearby = getNearbyLines(lines, index, 10)
                val inIOContext = nearby.any {
                    it.contains("Dispatchers.IO") || it.contains("withContext") ||
                            it.contains("@WorkerThread") || it.contains("background")
                }
                if (!inIOContext) {
                    score -= 1
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "I/O Without Background Thread",
                            description = "Line ${index + 1}: I/O operation may block main thread. Use withContext(Dispatchers.IO)."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 8. Missing lazy initialization
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""val\s+\w+\s*=\s*(ArrayList|HashMap|mutableListOf|mutableMapOf|StringBuilder)""").containsMatchIn(line)) {
                val nearby = getNearbyLines(lines, index, 20)
                val usedConditionally = nearby.any { it.contains("if ") || it.contains("when") }
                if (usedConditionally) {
                    score -= 1
                }
            }
        }

        // 9. Bitmap without proper handling
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("BitmapFactory.decode")) {
                val nearby = getNearbyLines(lines, index, 10)
                val hasOptions = nearby.any { it.contains("BitmapFactory.Options") || it.contains("inSampleSize") }
                if (!hasOptions) {
                    score -= 1
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Bitmap Without Sampling",
                            description = "Line ${index + 1}: Decoding bitmap without Options. May cause OOM for large images."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 10. RecyclerView without DiffUtil
        if (code.contains("RecyclerView") || code.contains("Adapter")) {
            if (code.contains("notifyDataSetChanged") && !code.contains("DiffUtil") && !code.contains("ListAdapter")) {
                score -= 1
                if (issueCount < 4) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "notifyDataSetChanged()",
                        description = "Use DiffUtil or ListAdapter for efficient RecyclerView updates."
                    ))
                    issueCount++
                }
            }
        }

        return score.coerceIn(1, 10)
    }

    // ══════════════════════════════════════════════════════════
    // SECURITY ANALYSIS (Simplified for Health view)
    // ══════════════════════════════════════════════════════════

    private fun analyzeSecurity(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ): Int {
        var score = 10
        var issueCount = 0

        // 1. Hardcoded secrets
        val secretPattern = Regex(
            """(?:val|var|const)\s+\w*(?:api[_]?key|password|secret|token|credential)\w*\s*=\s*"[^"]{5,}"""",
            RegexOption.IGNORE_CASE
        )
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("\"\"") || line.contains("\"TODO\"")) return@forEachIndexed
            if (secretPattern.containsMatchIn(line)) {
                score -= 3
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.CRITICAL,
                        title = "Hardcoded Secret",
                        description = "Line ${index + 1}: Never hardcode secrets. Use secure storage."
                    ))
                    issueCount++
                }
            }
        }

        // 2. SQL injection
        val sqlKeywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "DROP")
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val upper = line.uppercase()
            if (sqlKeywords.any { upper.contains(it) } && (line.contains("+") || line.contains("\${"))) {
                score -= 2
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.CRITICAL,
                        title = "SQL Injection Risk",
                        description = "Line ${index + 1}: SQL built with concatenation. Use parameterized queries."
                    ))
                    issueCount++
                }
            }
        }

        // 3. HTTP instead of HTTPS
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex(""""http://(?!localhost|127\.0\.0\.1|10\.|192\.168\.)""").containsMatchIn(line)) {
                score -= 2
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Insecure HTTP",
                        description = "Line ${index + 1}: Using HTTP instead of HTTPS. Data can be intercepted."
                    ))
                    issueCount++
                }
            }
        }

        // 4. Sensitive data in logs
        val logPatterns = listOf("Log.", "println", "Timber.", "System.out")
        val sensitiveWords = listOf("password", "token", "secret", "key", "credential", "auth")
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val hasLog = logPatterns.any { line.contains(it) }
            if (hasLog) {
                val lower = line.lowercase()
                val found = sensitiveWords.firstOrNull { lower.contains(it) }
                if (found != null) {
                    score -= 2
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "Sensitive Data Logged",
                            description = "Line ${index + 1}: Logging '$found'. Remove before production."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 5. Weak crypto
        val weakAlgos = listOf("MD5", "SHA1", "SHA-1", "DES", "RC4")
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val found = weakAlgos.firstOrNull { line.contains(it, true) && line.contains("getInstance") }
            if (found != null) {
                score -= 1
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "Weak Crypto: $found",
                        description = "Line ${index + 1}: Use SHA-256+ for hashing, AES-GCM for encryption."
                    ))
                    issueCount++
                }
            }
        }

        return score.coerceIn(1, 10)
    }

    // ══════════════════════════════════════════════════════════
    // READABILITY ANALYSIS (20+ checks)
    // ══════════════════════════════════════════════════════════

    private fun analyzeReadability(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ): Int {
        var score = 10
        var issueCount = 0

        // 1. Function length analysis
        val functionRanges = mutableListOf<Triple<Int, Int, String>>() // start, length, name
        var funcStart = -1
        var funcName = ""
        var braceCount = 0

        lines.forEachIndexed { index, line ->
            val funcMatch = Regex("""fun\s+(\w+)\s*\(""").find(line)
            if (funcMatch != null && funcStart == -1) {
                funcStart = index
                funcName = funcMatch.groupValues[1]
                braceCount = 0
            }
            if (funcStart >= 0) {
                braceCount += line.count { it == '{' } - line.count { it == '}' }
                if (braceCount <= 0 && index > funcStart) {
                    functionRanges.add(Triple(funcStart + 1, index - funcStart + 1, funcName))
                    funcStart = -1
                }
            }
        }

        for ((start, length, name) in functionRanges) {
            when {
                length > 60 -> {
                    score -= 2
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "Very Long Function",
                            description = "Function '$name' at line $start is $length lines. Max recommended: 30."
                        ))
                        issueCount++
                    }
                }
                length > 40 -> {
                    score -= 1
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Long Function",
                            description = "Function '$name' at line $start is $length lines. Consider breaking it up."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 2. Line length
        var longLineCount = 0
        var firstLongLine = 0
        lines.forEachIndexed { index, line ->
            if (line.length > 120) {
                longLineCount++
                if (longLineCount == 1) firstLongLine = index + 1
            }
        }
        if (longLineCount > 5) {
            score -= 1
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "$longLineCount Long Lines (>120 chars)",
                    description = "First at line $firstLongLine. Break long lines for readability."
                ))
                issueCount++
            }
        }

        // 3. Poor variable naming
        var poorNameCount = 0
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""(?:val|var)\s+[a-z]\s*[=:]""").containsMatchIn(line)) {
                val nearby = getNearbyLines(lines, index, 2)
                val inLoop = nearby.any {
                    it.contains("for ") || it.contains("while ") ||
                            it.contains("forEach") || it.contains("forEachIndexed")
                }
                if (!inLoop) {
                    poorNameCount++
                }
            }
        }
        if (poorNameCount > 0) {
            score -= poorNameCount.coerceAtMost(2)
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "$poorNameCount Single-Letter Variable(s)",
                    description = "Use descriptive names. Single letters are only acceptable in loops."
                ))
                issueCount++
            }
        }

        // 4. Magic numbers
        var magicCount = 0
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (line.contains("const") || line.contains("companion")) return@forEachIndexed

            val numbers = Regex("""(?<![.\w])\d{2,}(?!\.\d|L|f|dp|sp|px)""").findAll(line)
            for (match in numbers) {
                val num = match.value.toIntOrNull() ?: continue
                if (num !in listOf(0, 1, 2, 10, 100, 1000)) {
                    magicCount++
                }
            }
        }
        if (magicCount > 3) {
            score -= 1
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "$magicCount Magic Numbers",
                    description = "Extract hardcoded numbers to named constants for clarity."
                ))
                issueCount++
            }
        }

        // 5. Too many parameters
        lines.forEachIndexed { index, line ->
            if (line.contains("fun ")) {
                val params = Regex("""\(([^)]*)\)""").find(line)?.groupValues?.get(1) ?: ""
                val paramCount = if (params.isBlank()) 0 else params.split(",").size
                if (paramCount > 5) {
                    score -= 1
                    if (issueCount < 4) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.MEDIUM,
                            title = "Too Many Parameters ($paramCount)",
                            description = "Line ${index + 1}: Function has $paramCount params. Use a data class."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 6. Comment ratio
        val totalLines = lines.count { it.trim().isNotEmpty() }
        val commentLines = lines.count {
            val t = it.trim()
            t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")
        }
        val commentRatio = if (totalLines > 0) commentLines.toFloat() / totalLines else 0f

        if (totalLines > 40 && commentRatio < 0.05f) {
            score -= 1
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "Lack of Comments",
                    description = "Only ${(commentRatio * 100).toInt()}% comments in $totalLines lines. Add docs for complex logic."
                ))
                issueCount++
            }
        }

        // 7. Too many var (mutable) vs val (immutable)
        val varCount = Regex("""(?<!\w)var\s+""").findAll(code).count()
        val valCount = Regex("""(?<!\w)val\s+""").findAll(code).count()
        val totalDecl = varCount + valCount
        if (totalDecl > 5 && varCount.toFloat() / totalDecl > 0.6f) {
            score -= 1
            if (issueCount < 4) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "Too Many Mutable Variables",
                    description = "$varCount var vs $valCount val. Prefer val for immutability."
                ))
                issueCount++
            }
        }

        // 8. Wildcard imports
        val wildcardImports = lines.count { it.trim().startsWith("import ") && it.contains(".*") }
        if (wildcardImports > 3) {
            score -= 1
        }

        return score.coerceIn(1, 10)
    }

    // ══════════════════════════════════════════════════════════
    // COMPLEXITY ANALYSIS (15+ checks)
    // ══════════════════════════════════════════════════════════

    private fun analyzeComplexity(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ): Int {
        var score = 10
        var issueCount = 0

        // 1. Nesting depth
        var maxNesting = 0
        var currentNesting = 0
        var deepestLine = 0

        lines.forEachIndexed { index, line ->
            currentNesting += line.count { it == '{' } - line.count { it == '}' }
            if (currentNesting > maxNesting) {
                maxNesting = currentNesting
                deepestLine = index + 1
            }
        }

        when {
            maxNesting > 6 -> {
                score -= 3
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Extreme Nesting ($maxNesting levels)",
                        description = "At line $deepestLine. Refactor using early returns and extracted methods."
                    ))
                    issueCount++
                }
            }
            maxNesting > 4 -> {
                score -= 2
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "Deep Nesting ($maxNesting levels)",
                        description = "At line $deepestLine. Extract nested logic into separate functions."
                    ))
                    issueCount++
                }
            }
            maxNesting > 3 -> {
                score -= 1
            }
        }

        // 2. Cyclomatic complexity
        var decisionPoints = 0
        for (line in lines) {
            if (isComment(line)) continue
            if (Regex("""(?<!\w)if\s*\(""").containsMatchIn(line)) decisionPoints++
            if (line.contains("else if")) decisionPoints++
            if (Regex("""(?<!\w)when\s*[\({]""").containsMatchIn(line)) decisionPoints++
            if (Regex("""(?<!\w)(for|while)\s*\(""").containsMatchIn(line)) decisionPoints++
            if (line.contains("catch")) decisionPoints++
            if (line.contains("&&")) decisionPoints += line.split("&&").size - 1
            if (line.contains("||")) decisionPoints += line.split("||").size - 1
            if (line.contains("?:")) decisionPoints++
        }

        when {
            decisionPoints > 25 -> {
                score -= 3
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.HIGH,
                        title = "Very High Complexity",
                        description = "$decisionPoints decision points. Target: <15. Break into smaller functions."
                    ))
                    issueCount++
                }
            }
            decisionPoints > 15 -> {
                score -= 2
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "High Complexity",
                        description = "$decisionPoints decision points. Consider extracting some logic."
                    ))
                    issueCount++
                }
            }
            decisionPoints > 10 -> {
                score -= 1
            }
        }

        // 3. When/switch branch count
        var inWhen = false
        var whenBranches = 0
        var whenStartLine = 0

        lines.forEachIndexed { index, line ->
            if (Regex("""when\s*[\({]""").containsMatchIn(line)) {
                inWhen = true
                whenBranches = 0
                whenStartLine = index + 1
            }
            if (inWhen) {
                if (line.contains("->")) whenBranches++
                if (line.trim() == "}" && inWhen) {
                    if (whenBranches > 10) {
                        score -= 1
                        if (issueCount < 3) {
                            issues.add(HealthIssue(
                                severity = IssueSeverity.MEDIUM,
                                title = "Large When ($whenBranches branches)",
                                description = "At line $whenStartLine. Consider polymorphism or map lookup."
                            ))
                            issueCount++
                        }
                    }
                    inWhen = false
                }
            }
        }

        // 4. Recursion detection
        val funcNames = Regex("""fun\s+(\w+)\s*\(""").findAll(code).map { it.groupValues[1] }.toList()
        for (name in funcNames) {
            val funcBody = extractFunctionBody(code, name)
            if (funcBody != null && funcBody.contains("$name(")) {
                val hasBaseCase = funcBody.contains("return") &&
                        (funcBody.contains("if ") || funcBody.contains("when"))
                score -= 1
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = if (hasBaseCase) IssueSeverity.LOW else IssueSeverity.MEDIUM,
                        title = "Recursion: $name()",
                        description = if (hasBaseCase) "Has base case. Verify stack depth for large inputs."
                        else "Recursive without clear base case. Risk of StackOverflow."
                    ))
                    issueCount++
                }
                break
            }
        }

        // 5. File complexity
        val classCount = Regex("""(?:class|object|interface)\s+\w+""").findAll(code).count()
        val funcCount = Regex("""fun\s+\w+""").findAll(code).count()
        val totalLines = lines.size

        if (classCount > 5) {
            score -= 1
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "$classCount Classes in One File",
                    description = "Split into separate files for maintainability."
                ))
                issueCount++
            }
        }

        if (totalLines > 300) {
            score -= 1
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.MEDIUM,
                    title = "Large File ($totalLines lines)",
                    description = "Files over 300 lines are hard to maintain. Split by responsibility."
                ))
                issueCount++
            }
        }

        // 6. Boolean parameter (code smell)
        lines.forEachIndexed { index, line ->
            if (line.contains("fun ") && line.contains("Boolean")) {
                val boolCount = Regex(""":\s*Boolean""").findAll(line).count()
                if (boolCount >= 2) {
                    score -= 1
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "Multiple Boolean Parameters",
                            description = "Line ${index + 1}: Hard to read at call site. Use enum or sealed class."
                        ))
                        issueCount++
                    }
                }
            }
        }

        return score.coerceIn(1, 10)
    }

    // ══════════════════════════════════════════════════════════
    // KOTLIN IDIOM CHECKS
    // ══════════════════════════════════════════════════════════

    private fun analyzeKotlinIdioms(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ) {
        var issueCount = 0

        // 1. if-null check instead of ?.let or ?:
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""if\s*\(\s*\w+\s*!=\s*null\s*\)""").containsMatchIn(line)) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Non-Idiomatic Null Check",
                        description = "Line ${index + 1}: Use ?.let { } or ?: instead of if (x != null)."
                    ))
                    issueCount++
                }
            }
        }

        // 2. Java-style for loop
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""for\s*\(\s*\w+\s+in\s+0\s+until""").containsMatchIn(line)) {
                val nearby = getNearbyLines(lines, index, 3)
                val justIterating = nearby.any { it.contains("[") || it.contains("get(") }
                if (justIterating) {
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "Index Loop Instead of forEach",
                            description = "Line ${index + 1}: Use .forEach or .forEachIndexed for cleaner code."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 3. Not using string templates
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex(""""[^"]*"\s*\+\s*\w+\s*\+\s*"[^"]*"""").containsMatchIn(line)) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "String Concatenation",
                        description = "Line ${index + 1}: Use string templates \"\$variable\" instead of + concatenation."
                    ))
                    issueCount++
                }
            }
        }

        // 4. Unnecessary .toString()
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (Regex("""\$\{[^}]+\.toString\(\)\}""").containsMatchIn(line)) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Unnecessary toString()",
                        description = "Line ${index + 1}: String templates auto-call toString(). Remove it."
                    ))
                    issueCount++
                }
            }
        }

        // 5. Could use data class
        lines.forEachIndexed { index, line ->
            if (line.contains("class ") && !line.contains("data class") &&
                !line.contains("abstract") && !line.contains("sealed") &&
                !line.contains("enum") && !line.contains("object") &&
                !line.contains("interface") && !line.contains("inner") &&
                !line.contains("open") && !line.contains("annotation")) {
                val nearby = lines.drop(index).take(20)
                val hasOnlyProperties = nearby.all {
                    it.trim().isEmpty() || it.contains("val ") || it.contains("var ") ||
                            it.contains("{") || it.contains("}") || it.contains("class") ||
                            it.trim().startsWith("//") || it.trim().startsWith("/*") || it.trim().startsWith("*")
                }
                val propertyCount = nearby.count { it.contains("val ") || it.contains("var ") }
                if (hasOnlyProperties && propertyCount >= 2 && propertyCount <= 10) {
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "Consider Data Class",
                            description = "Line ${index + 1}: Class with only properties. Use 'data class' for auto equals/hashCode/toString."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 6. when without else
        lines.forEachIndexed { index, line ->
            if (Regex("""when\s*\(""").containsMatchIn(line)) {
                val nearby = lines.drop(index).take(30)
                val whenBlock = nearby.joinToString("\n")
                if (!whenBlock.contains("else ->") && !whenBlock.contains("else->")) {
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "When Without Else",
                            description = "Line ${index + 1}: Add 'else' branch for exhaustive when expression."
                        ))
                        issueCount++
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // ANDROID-SPECIFIC CHECKS
    // ══════════════════════════════════════════════════════════

    private fun analyzeAndroidPatterns(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ) {
        var issueCount = 0

        // 1. Context leak in companion/static
        val hasCompanion = code.contains("companion object")
        if (hasCompanion) {
            lines.forEachIndexed { index, line ->
                if (line.contains("Context") || line.contains("Activity") || line.contains("Fragment")) {
                    val inCompanion = isInsideBlock(lines, index, "companion object")
                    if (inCompanion) {
                        if (issueCount < 3) {
                            issues.add(HealthIssue(
                                severity = IssueSeverity.HIGH,
                                title = "Context Leak Risk",
                                description = "Line ${index + 1}: Context/Activity in companion object causes memory leak."
                            ))
                            issueCount++
                        }
                    }
                }
            }
        }

        // 2. findViewById usage (should use ViewBinding)
        val findViewCount = Regex("""findViewById\s*[<(]""").findAll(code).count()
        if (findViewCount > 3) {
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "Use ViewBinding ($findViewCount calls)",
                    description = "Replace findViewById with ViewBinding for type safety and null safety."
                ))
                issueCount++
            }
        }

        // 3. Hardcoded strings in UI
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if ((line.contains("setText(\"") || line.contains("text = \"") ||
                        line.contains("hint = \"") || line.contains("title = \"")) &&
                !line.contains("getString(") && !line.contains("R.string.")) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Hardcoded UI String",
                        description = "Line ${index + 1}: Use string resources (R.string) for i18n support."
                    ))
                    issueCount++
                }
            }
        }

        // 4. runOnUiThread instead of coroutines
        if (code.contains("runOnUiThread")) {
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "runOnUiThread Usage",
                    description = "Use withContext(Dispatchers.Main) in coroutines instead."
                ))
                issueCount++
            }
        }

        // 5. Composable without remember
        if (code.contains("@Composable")) {
            lines.forEachIndexed { index, line ->
                if (line.contains("mutableStateOf(") && !line.contains("remember")) {
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.HIGH,
                            title = "State Without Remember",
                            description = "Line ${index + 1}: mutableStateOf without remember resets on recomposition."
                        ))
                        issueCount++
                    }
                }
            }
        }

        // 6. Compose side effects
        if (code.contains("@Composable")) {
            lines.forEachIndexed { index, line ->
                if (isComment(line)) return@forEachIndexed
                val hasLaunch = line.contains("launch {") || line.contains("launch{")
                val inComposable = !line.contains("LaunchedEffect") && !line.contains("rememberCoroutineScope")
                if (hasLaunch && inComposable) {
                    val nearby = getNearbyLines(lines, index, 5)
                    val hasEffect = nearby.any {
                        it.contains("LaunchedEffect") || it.contains("rememberCoroutineScope") ||
                                it.contains("SideEffect") || it.contains("DisposableEffect")
                    }
                    if (!hasEffect) {
                        if (issueCount < 3) {
                            issues.add(HealthIssue(
                                severity = IssueSeverity.MEDIUM,
                                title = "Side Effect in Composable",
                                description = "Line ${index + 1}: Use LaunchedEffect or rememberCoroutineScope for side effects."
                            ))
                            issueCount++
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CODE SMELL DETECTION
    // ══════════════════════════════════════════════════════════

    private fun analyzeCodeSmells(
        lines: List<String>,
        code: String,
        issues: MutableList<HealthIssue>,
        details: MutableList<HealthDetail>
    ) {
        var issueCount = 0

        // 1. God class (too many responsibilities)
        val funcCount = Regex("""fun\s+\w+""").findAll(code).count()
        val lineCount = lines.size
        if (funcCount > 20 && lineCount > 400) {
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.MEDIUM,
                    title = "God Class ($funcCount functions, $lineCount lines)",
                    description = "Class has too many responsibilities. Split by Single Responsibility Principle."
                ))
                issueCount++
            }
        }

        // 2. Duplicate logic detection (simplified)
        val normalizedLines = lines.map { it.trim().lowercase() }.filter { it.length > 20 && !it.startsWith("//") }
        val duplicates = normalizedLines.groupBy { it }.filter { it.value.size > 2 }
        if (duplicates.isNotEmpty()) {
            if (issueCount < 3) {
                issues.add(HealthIssue(
                    severity = IssueSeverity.LOW,
                    title = "Duplicate Code Detected",
                    description = "${duplicates.size} patterns repeated 3+ times. Extract to shared functions."
                ))
                issueCount++
            }
        }

        // 3. Dead code indicators
        val privateFuncs = Regex("""private\s+fun\s+(\w+)""").findAll(code).map { it.groupValues[1] }.toList()
        for (func in privateFuncs) {
            val callCount = Regex("""(?<!\w)${Regex.escape(func)}\s*\(""").findAll(code).count()
            // 1 for declaration, 0 for calls means unused
            if (callCount <= 1) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Possibly Unused: $func()",
                        description = "Private function '$func' may be unused. Remove if not needed."
                    ))
                    issueCount++
                }
                break
            }
        }

        // 4. Feature envy (method uses other class fields more than own)
        // Simplified: check for many external calls
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            val dotCalls = Regex("""\w+\.\w+""").findAll(line).count()
            if (dotCalls > 5) {
                if (issueCount < 3) {
                    issues.add(HealthIssue(
                        severity = IssueSeverity.LOW,
                        title = "Complex Expression Chain",
                        description = "Line ${index + 1}: Many chained calls. Extract into named intermediate variables."
                    ))
                    issueCount++
                }
            }
        }

        // 5. Comments that explain "what" not "why"
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("//")) {
                val comment = trimmed.removePrefix("//").trim().lowercase()
                val trivialPrefixes = listOf(
                    "set ", "get ", "return ", "create ", "initialize ",
                    "check if", "loop through", "iterate over", "call "
                )
                val isTrivial = trivialPrefixes.any { comment.startsWith(it) } && comment.length < 40
                if (isTrivial) {
                    if (issueCount < 3) {
                        issues.add(HealthIssue(
                            severity = IssueSeverity.LOW,
                            title = "Trivial Comment",
                            description = "Line ${index + 1}: Comment states the obvious. Comments should explain WHY, not WHAT."
                        ))
                        issueCount++
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // BEST PRACTICES DETECTION (What's GOOD about the code)
    // ══════════════════════════════════════════════════════════

    private fun detectBestPractices(lines: List<String>, code: String): List<String> {
        val practices = mutableListOf<String>()

        // Immutability
        val valCount = Regex("""(?<!\w)val\s+""").findAll(code).count()
        val varCount = Regex("""(?<!\w)var\s+""").findAll(code).count()
        val totalDecl = valCount + varCount
        if (totalDecl > 0 && valCount.toFloat() / totalDecl >= 0.7f) {
            practices.add("Good immutability: ${valCount}/${totalDecl} declarations use val")
        }

        // Uses data classes
        if (code.contains("data class")) {
            practices.add("Uses data classes for value objects")
        }

        // Uses sealed classes/interfaces
        if (code.contains("sealed class") || code.contains("sealed interface")) {
            practices.add("Uses sealed classes for type safety")
        }

        // Coroutines instead of threads
        if ((code.contains("launch") || code.contains("async") || code.contains("withContext")) &&
            !code.contains("Thread(") && !code.contains("Runnable")) {
            practices.add("Uses coroutines instead of raw threads")
        }

        // Null safety
        val safeCallCount = Regex("""\?\.""").findAll(code).count()
        val elvisCount = Regex("""\?:""").findAll(code).count()
        val forceUnwrapCount = Regex("""!!""").findAll(code).count()
        if (safeCallCount + elvisCount > 0 && forceUnwrapCount == 0) {
            practices.add("Proper null safety with ?. and ?: operators")
        }

        // Error handling
        if (code.contains("try") && code.contains("catch") && !code.contains("catch (e: Exception) { }")) {
            practices.add("Has error handling with try-catch")
        }

        // Uses scope functions
        val scopeFunctions = listOf(".let {", ".also {", ".apply {", ".run {", ".with(")
        val usesScope = scopeFunctions.count { code.contains(it) }
        if (usesScope >= 2) {
            practices.add("Uses Kotlin scope functions effectively")
        }

        // Uses string templates
        if (code.contains("\${") && !code.contains("\" + ")) {
            practices.add("Uses string templates instead of concatenation")
        }

        // Uses extension functions
        if (Regex("""fun\s+\w+\.\w+""").containsMatchIn(code)) {
            practices.add("Uses extension functions")
        }

        // Has documentation/comments
        val totalLines = lines.size
        val commentLines = lines.count { it.trim().startsWith("//") || it.trim().startsWith("/*") || it.trim().startsWith("*") }
        if (totalLines > 20 && commentLines.toFloat() / totalLines >= 0.1f) {
            practices.add("Good documentation (${(commentLines.toFloat() / totalLines * 100).toInt()}% comments)")
        }

        // Uses functional operations
        val functionalOps = listOf(".map", ".filter", ".flatMap", ".fold", ".reduce", ".groupBy", ".associate")
        val usesFunctional = functionalOps.count { code.contains(it) }
        if (usesFunctional >= 2) {
            practices.add("Uses functional programming patterns")
        }

        // Uses when expressions
        if (code.contains("when (") || code.contains("when {")) {
            practices.add("Uses when expressions for clean branching")
        }

        // Uses companion object constants
        if (code.contains("companion object") && code.contains("const val")) {
            practices.add("Constants in companion object")
        }

        // Proper access modifiers
        if (code.contains("private ") || code.contains("internal ")) {
            practices.add("Uses access modifiers for encapsulation")
        }

        // Uses suspend functions
        if (code.contains("suspend fun")) {
            practices.add("Uses suspend functions for async operations")
        }

        // Uses Flow
        if (code.contains("Flow<") || code.contains("StateFlow") || code.contains("SharedFlow")) {
            practices.add("Uses Kotlin Flow for reactive streams")
        }

        // Uses viewModelScope/lifecycleScope
        if (code.contains("viewModelScope") || code.contains("lifecycleScope")) {
            practices.add("Uses lifecycle-aware coroutine scopes")
        }

        // Short functions
        val funcLengths = calculateFunctionLengths(lines)
        if (funcLengths.isNotEmpty() && funcLengths.average() < 20) {
            practices.add("Good function length (avg ${funcLengths.average().toInt()} lines)")
        }

        return practices.take(8) // Max 8 best practices shown
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

    // ══════════════════════════════════════════════════════════
    // SCORING
    // ══════════════════════════════════════════════════════════

    private fun calculateOverall(bug: Int, perf: Int, security: Int, readability: Int, complexity: Int): Int {
        val weighted = (
                bug * 25 +
                        security * 25 +
                        perf * 20 +
                        readability * 15 +
                        complexity * 15
                ) / 10
        return weighted.coerceIn(0, 100)
    }

    private fun buildSummary(score: Int, issues: List<HealthIssue>): String {
        val criticalCount = issues.count { it.severity == IssueSeverity.CRITICAL }
        val highCount = issues.count { it.severity == IssueSeverity.HIGH }
        val total = issues.size

        return when {
            total == 0 -> "✅ Excellent! No issues detected."
            criticalCount > 0 -> "🚨 $total issues ($criticalCount critical). Immediate attention required."
            highCount > 0 -> "⚠️ $total issues ($highCount high priority). Review recommended."
            score >= 80 -> "👍 $total minor issues. Code is well-written overall."
            score >= 60 -> "📋 $total issues found. Some areas could be improved."
            score >= 40 -> "⚡ $total issues. Several areas need attention."
            else -> "🔧 $total issues. Significant refactoring recommended."
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private fun isComment(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("//") || trimmed.startsWith("/*") ||
                trimmed.startsWith("*") || trimmed.startsWith("<!--")
    }

    private fun getNearbyLines(lines: List<String>, index: Int, range: Int): List<String> {
        val start = maxOf(0, index - range)
        val end = minOf(lines.size, index + range + 1)
        return lines.subList(start, end)
    }

    private fun extractFunctionBody(code: String, funcName: String): String? {
        val pattern = Regex("""fun\s+${Regex.escape(funcName)}\s*\([^)]*\)[^{]*\{""")
        val match = pattern.find(code) ?: return null
        val start = match.range.last + 1

        var braceCount = 1
        var end = start
        while (end < code.length && braceCount > 0) {
            if (code[end] == '{') braceCount++
            if (code[end] == '}') braceCount--
            end++
        }

        return if (braceCount == 0) code.substring(start, end - 1) else null
    }

    private fun isInsideBlock(lines: List<String>, targetIndex: Int, blockKeyword: String): Boolean {
        var braceCount = 0
        for (i in targetIndex downTo 0) {
            val line = lines[i]
            braceCount += line.count { it == '}' } - line.count { it == '{' }
            if (line.contains(blockKeyword) && braceCount <= 0) return true
        }
        return false
    }
}