package com.runanywhere.kotlin_starter_example.kodent.engine

data class SecurityVulnerability(
    val severity: SecuritySeverity,
    val category: String,
    val title: String,
    val description: String,
    val lineNumber: Int?,
    val codeSnippet: String?,
    val fix: String,
    val owasp: String = "",
    val cwe: String = "",
    val confidence: Confidence = Confidence.HIGH,
    val cvssScore: Float = 0f,
    val cvssVector: String = ""
)

enum class SecuritySeverity {
    CRITICAL, HIGH, MEDIUM, LOW
}

enum class Confidence {
    HIGH, MEDIUM, LOW
}

data class SecurityScanResult(
    val grade: String,
    val score: Int,
    val vulnerabilities: List<SecurityVulnerability>,
    val summary: String,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val scanTimeMs: Long,
    val owaspCoverage: List<String> = emptyList(),
    val totalChecks: Int = 0,
    val complianceResult: ComplianceResult? = null
)

object SecurityScanner {

    fun scan(code: String, language: String): SecurityScanResult {
        val startTime = System.currentTimeMillis()
        val lines = code.lines()
        val vulnerabilities = mutableListOf<SecurityVulnerability>()
        val context = buildContext(lines)

        // ═══════════════════════════════════════════
        // CRITICAL CHECKS
        // ═══════════════════════════════════════════
        checkHardcodedSecrets(lines, vulnerabilities)
        checkSQLInjection(lines, context, vulnerabilities)
        checkCommandInjection(lines, context, vulnerabilities)
        checkPathTraversal(lines, context, vulnerabilities)
        checkInsecureDeserialization(lines, vulnerabilities)       // NEW
        checkXXE(lines, vulnerabilities)                           // NEW

        // ═══════════════════════════════════════════
        // HIGH CHECKS
        // ═══════════════════════════════════════════
        checkSensitiveLogging(lines, vulnerabilities)
        checkInsecureHTTP(lines, vulnerabilities)
        checkSSLBypass(lines, vulnerabilities)
        checkEmptyCatch(lines, vulnerabilities)
        checkWebViewIssues(lines, vulnerabilities)
        checkDebuggable(lines, vulnerabilities)
        checkOpenRedirect(lines, context, vulnerabilities)         // NEW
        checkInsecureBroadcastReceiver(lines, vulnerabilities)     // NEW
        checkInsecureContentProvider(lines, vulnerabilities)       // NEW
        checkInsecurePendingIntent(lines, vulnerabilities)         // NEW
        checkCleartextTraffic(lines, vulnerabilities)              // NEW

        // ═══════════════════════════════════════════
        // MEDIUM CHECKS
        // ═══════════════════════════════════════════
        checkWeakCrypto(lines, vulnerabilities)
        checkInsecureRandom(lines, vulnerabilities)
        checkSharedPrefsSecrets(lines, vulnerabilities)
        checkHardcodedIPs(lines, vulnerabilities)
        checkFilePermissions(lines, vulnerabilities)
        checkUnsafeCasts(lines, vulnerabilities)
        checkClipboardLeak(lines, vulnerabilities)                 // NEW
        checkExternalStorageUsage(lines, vulnerabilities)          // NEW
        checkInsecureDeeplink(lines, vulnerabilities)              // NEW
        checkWeakKeySize(lines, vulnerabilities)                   // NEW
        checkHardcodedIV(lines, vulnerabilities)                   // NEW

        // ═══════════════════════════════════════════
        // LOW CHECKS
        // ═══════════════════════════════════════════
        checkBroadExceptionCatch(lines, vulnerabilities)
        checkSecurityTODOs(lines, vulnerabilities)
        checkUnvalidatedInput(lines, context, vulnerabilities)
        checkNullUnsafe(lines, vulnerabilities)
        checkMissingProguard(lines, code, vulnerabilities)         // NEW
        checkReflectionUsage(lines, vulnerabilities)               // NEW
        checkDynamicCodeLoading(lines, vulnerabilities)            // NEW
        checkBackupEnabled(lines, vulnerabilities)                 // NEW

        // Deduplicate + enrich with OWASP/CWE
        val deduplicated = deduplicateVulnerabilities(vulnerabilities)
        val enriched = deduplicated.map { OWASPMapping.enrich(it) }
        val sorted = enriched.sortedBy { it.severity.ordinal }

        val score = calculateScore(sorted)
        val grade = scoreToGrade(score)
        val scanTime = System.currentTimeMillis() - startTime
        val owaspCoverage = OWASPMapping.getCoveredCategories(sorted)
        val complianceResult = ComplianceChecker.check(code, language)

        return SecurityScanResult(
            grade = grade,
            score = score,
            vulnerabilities = sorted,
            summary = buildSummary(sorted, score, grade),
            criticalCount = sorted.count { it.severity == SecuritySeverity.CRITICAL },
            highCount = sorted.count { it.severity == SecuritySeverity.HIGH },
            mediumCount = sorted.count { it.severity == SecuritySeverity.MEDIUM },
            lowCount = sorted.count { it.severity == SecuritySeverity.LOW },
            scanTimeMs = scanTime,
            owaspCoverage = owaspCoverage,
            totalChecks = 32,
            complianceResult = complianceResult
        )
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: INSECURE DESERIALIZATION
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureDeserialization(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val patterns = listOf(
            "ObjectInputStream(" to "Java deserialization",
            "readObject()" to "Java deserialization",
            "Serializable" to "Java Serializable interface",
            "readUnshared()" to "Java deserialization",
            "Gson().fromJson(" to "JSON deserialization",
            "ObjectMapper().readValue(" to "Jackson deserialization",
            "Moshi" to "Moshi deserialization"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for ((pattern, type) in patterns) {
                if (line.contains(pattern)) {
                    // ObjectInputStream is dangerous, others are warning
                    val isJava = type.contains("Java")

                    // Check if input comes from untrusted source
                    val nearbyLines = getNearbyLines(lines, index, 5)
                    val fromNetwork = nearbyLines.any {
                        it.contains("socket", true) || it.contains("http", true) ||
                                it.contains("input", true) || it.contains("stream", true) ||
                                it.contains("request", true) || it.contains("intent", true)
                    }

                    if (isJava && fromNetwork) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.CRITICAL,
                            category = "Injection",
                            title = "Insecure Deserialization",
                            description = "Deserializing data from untrusted source. Can lead to remote code execution.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Never deserialize untrusted data. Use JSON/Protocol Buffers instead of Java serialization.",
                            confidence = if (fromNetwork) Confidence.HIGH else Confidence.MEDIUM
                        ))
                    } else if (isJava) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.HIGH,
                            category = "Injection",
                            title = "Deserialization Usage",
                            description = "Java deserialization detected. Ensure input is from trusted source only.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Prefer JSON (Gson/Moshi) over Java ObjectInputStream.",
                            confidence = Confidence.MEDIUM
                        ))
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: XML EXTERNAL ENTITY (XXE)
    // ══════════════════════════════════════════════════════════

    private fun checkXXE(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val xmlParsers = listOf(
            "DocumentBuilderFactory", "SAXParserFactory", "XMLInputFactory",
            "TransformerFactory", "SchemaFactory", "XMLReader",
            "SAXBuilder", "SAXReader"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val found = xmlParsers.firstOrNull { line.contains(it) }
            if (found != null) {
                // Check if external entities are disabled
                val nearbyLines = getNearbyLines(lines, index, 10)
                val nearbyText = nearbyLines.joinToString("\n")
                val hasProtection = nearbyText.contains("FEATURE_SECURE_PROCESSING") ||
                        nearbyText.contains("DISALLOW_DOCTYPE") ||
                        nearbyText.contains("setFeature") ||
                        nearbyText.contains("external-general-entities", true) ||
                        nearbyText.contains("setExpandEntityReferences(false)")

                if (!hasProtection) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.CRITICAL,
                        category = "Injection",
                        title = "XML External Entity (XXE)",
                        description = "XML parser '$found' without entity protection. Attackers can read files or perform SSRF.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Disable external entities: factory.setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true)",
                        confidence = Confidence.HIGH
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: OPEN REDIRECT
    // ══════════════════════════════════════════════════════════

    private fun checkOpenRedirect(
        lines: List<String>,
        context: CodeContext,
        out: MutableList<SecurityVulnerability>
    ) {
        val redirectPatterns = listOf(
            "startActivity(" to "Activity launch",
            "Intent(Intent.ACTION_VIEW" to "View intent",
            "Uri.parse(" to "URI parsing",
            "CustomTabsIntent" to "Custom tab",
            "openBrowser(" to "Browser open",
            "loadUrl(" to "WebView URL load"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for ((pattern, type) in redirectPatterns) {
                if (line.contains(pattern)) {
                    val hasConcat = line.contains("+") || line.contains("\${") || line.contains("\$")
                    val usesUserInput = context.userInputVariables.any { line.contains(it) } ||
                            context.functionParameters.any { line.contains(it) }

                    if (hasConcat || usesUserInput) {
                        val nearbyLines = getNearbyLines(lines, index, 3)
                        val hasValidation = nearbyLines.any {
                            it.contains("startsWith(") || it.contains("contains(") ||
                                    it.contains("allowlist") || it.contains("whitelist") ||
                                    it.contains("isValidUrl") || it.contains("Uri.parse")
                        }

                        if (!hasValidation) {
                            out.add(SecurityVulnerability(
                                severity = SecuritySeverity.HIGH,
                                category = "Injection",
                                title = "Open Redirect Risk",
                                description = "$type with dynamic URL. Attackers can redirect users to malicious sites.",
                                lineNumber = index + 1,
                                codeSnippet = sanitizeSnippet(line),
                                fix = "Validate URLs against an allowlist of trusted domains.",
                                confidence = if (usesUserInput) Confidence.HIGH else Confidence.MEDIUM
                            ))
                        }
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: INSECURE BROADCAST RECEIVER
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureBroadcastReceiver(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("registerReceiver(") || line.contains("registerReceiver (")) {
                val nearbyLines = getNearbyLines(lines, index, 5)
                val nearbyText = nearbyLines.joinToString("\n")

                val hasPermission = nearbyText.contains("permission") ||
                        nearbyText.contains("RECEIVER_NOT_EXPORTED") ||
                        nearbyText.contains("Context.RECEIVER_NOT_EXPORTED") ||
                        nearbyText.contains("LocalBroadcastManager")

                if (!hasPermission) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.HIGH,
                        category = "Android Component",
                        title = "Insecure Broadcast Receiver",
                        description = "BroadcastReceiver registered without permission. Any app can send broadcasts to it.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Use RECEIVER_NOT_EXPORTED flag.\n\n${FixSnippets.BROADCAST_FIX}",
                        confidence = Confidence.HIGH
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: INSECURE CONTENT PROVIDER
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureContentProvider(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val fullCode = lines.joinToString("\n")

        if (fullCode.contains("ContentProvider") || fullCode.contains("content://")) {
            lines.forEachIndexed { index, line ->
                if (line.contains("exported=\"true\"") || line.contains("exported = true")) {
                    val nearbyLines = getNearbyLines(lines, index, 5)
                    val hasPermission = nearbyLines.any {
                        it.contains("permission") || it.contains("readPermission") ||
                                it.contains("writePermission") || it.contains("grantUriPermissions")
                    }

                    if (!hasPermission) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.HIGH,
                            category = "Android Component",
                            title = "Unprotected Content Provider",
                            description = "Content provider exported without permissions. Any app can read/write data.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Add readPermission/writePermission or set exported=\"false\".",
                            confidence = Confidence.HIGH
                        ))
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: INSECURE PENDING INTENT
    // ══════════════════════════════════════════════════════════

    private fun checkInsecurePendingIntent(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("PendingIntent.get") || line.contains("PendingIntent.send")) {
                val hasImmutableFlag = line.contains("FLAG_IMMUTABLE") ||
                        line.contains("FLAG_NO_CREATE")

                val hasImplicitIntent = line.contains("Intent()") ||
                        !line.contains("ComponentName") && !line.contains("setClass") &&
                        !line.contains("setComponent")

                if (!hasImmutableFlag) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.HIGH,
                        category = "Android Component",
                        title = "Mutable PendingIntent",
                        description = "PendingIntent without FLAG_IMMUTABLE. Malicious apps can modify the intent.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Add PendingIntent.FLAG_IMMUTABLE.\n\n${FixSnippets.PENDING_INTENT_FIX}",
                        confidence = Confidence.HIGH
                    ))
                }

                if (hasImplicitIntent) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.HIGH,
                        category = "Android Component",
                        title = "Implicit PendingIntent",
                        description = "PendingIntent with implicit intent. Other apps can intercept it.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Add PendingIntent.FLAG_IMMUTABLE.\n\n${FixSnippets.PENDING_INTENT_FIX}",
                        confidence = Confidence.MEDIUM
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: CLEARTEXT TRAFFIC
    // ══════════════════════════════════════════════════════════

    private fun checkCleartextTraffic(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (line.contains("cleartextTrafficPermitted=\"true\"") ||
                line.contains("usesCleartextTraffic=\"true\"") ||
                line.contains("android:usesCleartextTraffic=\"true\"")) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.HIGH,
                    category = "Transport",
                    title = "Cleartext Traffic Allowed",
                    description = "App allows unencrypted HTTP traffic. All data can be intercepted.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Set usesCleartextTraffic=\"false\" and use network_security_config.xml for exceptions.",
                    confidence = Confidence.HIGH
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: CLIPBOARD DATA LEAK
    // ══════════════════════════════════════════════════════════

    private fun checkClipboardLeak(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("ClipboardManager") || line.contains("setPrimaryClip") ||
                line.contains("ClipData.newPlainText")) {
                val nearbyLines = getNearbyLines(lines, index, 5)
                val lower = nearbyLines.joinToString(" ").lowercase()
                val isSensitive = lower.contains("password") || lower.contains("token") ||
                        lower.contains("secret") || lower.contains("key") ||
                        lower.contains("credential") || lower.contains("otp")

                if (isSensitive) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Data Leak",
                        title = "Sensitive Data on Clipboard",
                        description = "Copying sensitive data to clipboard. Other apps can read clipboard content.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Avoid copying sensitive data. If needed, clear clipboard after timeout.",
                        confidence = Confidence.MEDIUM
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: EXTERNAL STORAGE
    // ══════════════════════════════════════════════════════════

    private fun checkExternalStorageUsage(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val patterns = listOf(
            "getExternalFilesDir" to "External app directory",
            "getExternalStorageDirectory" to "External storage root",
            "Environment.getExternalStorageDirectory" to "External storage root",
            "getExternalCacheDir" to "External cache",
            "EXTERNAL_STORAGE" to "External storage constant"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for ((pattern, type) in patterns) {
                if (line.contains(pattern)) {
                    val nearbyLines = getNearbyLines(lines, index, 5)
                    val lower = nearbyLines.joinToString(" ").lowercase()
                    val storesSensitive = lower.contains("password") || lower.contains("token") ||
                            lower.contains("key") || lower.contains("secret") ||
                            lower.contains("credential") || lower.contains("user")

                    if (storesSensitive) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.MEDIUM,
                            category = "Storage",
                            title = "Sensitive Data on External Storage",
                            description = "$type used near sensitive data. External storage is readable by all apps.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Store sensitive data in internal storage or EncryptedFile.",
                            confidence = Confidence.MEDIUM
                        ))
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: INSECURE DEEPLINK
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureDeeplink(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("intent.data") || line.contains("intent?.data") ||
                line.contains("getData()")) {
                val nearbyLines = getNearbyLines(lines, index, 5)
                val nearbyText = nearbyLines.joinToString(" ")

                val hasValidation = nearbyText.contains("host") || nearbyText.contains("scheme") ||
                        nearbyText.contains("authority") || nearbyText.contains("if") ||
                        nearbyText.contains("when") || nearbyText.contains("require") ||
                        nearbyText.contains("check(")

                if (!hasValidation) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Android Component",
                        title = "Unvalidated Deep Link",
                        description = "Deep link data used without validating scheme/host. Malicious apps can trigger with crafted URIs.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Validate URI scheme and host.\n\n${FixSnippets.DEEPLINK_VALIDATION}",
                        confidence = Confidence.MEDIUM
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: WEAK KEY SIZE
    // ══════════════════════════════════════════════════════════

    private fun checkWeakKeySize(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("KeyGenerator") || line.contains("KeyPairGenerator") ||
                line.contains("initialize(")) {
                // Check for small key sizes
                val keySizes = Regex("""initialize\s*\(\s*(\d+)""").find(line)
                if (keySizes != null) {
                    val size = keySizes.groupValues[1].toIntOrNull() ?: 0
                    val isRSA = line.contains("RSA") || getNearbyLines(lines, index, 3).any { it.contains("RSA") }
                    val isAES = line.contains("AES") || getNearbyLines(lines, index, 3).any { it.contains("AES") }

                    val isWeak = when {
                        isRSA && size < 2048 -> true
                        isAES && size < 128 -> true
                        !isRSA && !isAES && size < 128 -> true
                        else -> false
                    }

                    if (isWeak) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.MEDIUM,
                            category = "Cryptography",
                            title = "Weak Key Size ($size-bit)",
                            description = "${size}-bit key is too small. RSA needs 2048+, AES needs 128+.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Use RSA-2048+ or AES-256 for adequate security.",
                            confidence = Confidence.HIGH
                        ))
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: HARDCODED IV/NONCE
    // ══════════════════════════════════════════════════════════

    private fun checkHardcodedIV(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (line.contains("IvParameterSpec(") || line.contains("GCMParameterSpec(")) {
                val hasHardcodedBytes = line.contains("byteArrayOf(") ||
                        line.contains("ByteArray(") ||
                        Regex(""""[^"]{8,}"""").containsMatchIn(line)

                if (hasHardcodedBytes) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Cryptography",
                        title = "Hardcoded IV/Nonce",
                        description = "Initialization vector is hardcoded. Reusing IVs breaks encryption security.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Generate random IV per encryption.\n\n${FixSnippets.AES_ENCRYPTION}",
                        confidence = Confidence.HIGH
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: MISSING PROGUARD/R8
    // ══════════════════════════════════════════════════════════

    private fun checkMissingProguard(lines: List<String>, code: String, out: MutableList<SecurityVulnerability>) {
        if (code.contains("minifyEnabled false") || code.contains("minifyEnabled=false")) {
            lines.forEachIndexed { index, line ->
                if (line.contains("minifyEnabled false") || line.contains("minifyEnabled=false")) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.LOW,
                        category = "Binary Protection",
                        title = "Code Obfuscation Disabled",
                        description = "ProGuard/R8 is disabled. App code can be easily reverse-engineered.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Enable minifyEnabled=true for release builds with proper ProGuard rules.",
                        confidence = Confidence.HIGH
                    ))
                    return
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: REFLECTION USAGE
    // ══════════════════════════════════════════════════════════

    private fun checkReflectionUsage(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val reflectionPatterns = listOf(
            "Class.forName(" to "Dynamic class loading via reflection",
            ".getDeclaredMethod(" to "Reflective method access",
            ".getDeclaredField(" to "Reflective field access",
            "::class.java" to "Kotlin reflection",
            ".setAccessible(true)" to "Bypassing access control via reflection"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for ((pattern, desc) in reflectionPatterns) {
                if (line.contains(pattern)) {
                    val severity = if (pattern == ".setAccessible(true)")
                        SecuritySeverity.MEDIUM else SecuritySeverity.LOW

                    out.add(SecurityVulnerability(
                        severity = severity,
                        category = "Binary Protection",
                        title = "Reflection Usage",
                        description = "$desc. Reflection can bypass security controls and is fragile.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Avoid reflection when possible. If needed, validate inputs and handle errors.",
                        confidence = Confidence.MEDIUM
                    ))
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: DYNAMIC CODE LOADING
    // ══════════════════════════════════════════════════════════

    private fun checkDynamicCodeLoading(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val patterns = listOf(
            "DexClassLoader(" to "Loading external DEX code",
            "PathClassLoader(" to "Loading external classes",
            "loadLibrary(" to "Loading native library",
            "System.load(" to "Loading native code from path",
            "InMemoryDexClassLoader(" to "Loading DEX from memory"
        )

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for ((pattern, desc) in patterns) {
                if (line.contains(pattern)) {
                    val hasConcat = line.contains("+") || line.contains("\${")

                    out.add(SecurityVulnerability(
                        severity = if (hasConcat) SecuritySeverity.HIGH else SecuritySeverity.LOW,
                        category = "Binary Protection",
                        title = "Dynamic Code Loading",
                        description = "$desc. Loaded code may be tampered with or malicious.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Verify integrity of loaded code. Use signature verification.",
                        confidence = if (hasConcat) Confidence.HIGH else Confidence.MEDIUM
                    ))
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: BACKUP ENABLED
    // ══════════════════════════════════════════════════════════

    private fun checkBackupEnabled(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            for (danger in SecurityPatterns.BACKUP_DANGEROUS) {
                if (line.contains(danger.pattern)) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.LOW,
                        category = "Configuration",
                        title = danger.title,
                        description = danger.description,
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Set allowBackup=\"false\" or use BackupRules to exclude sensitive data.",
                        confidence = Confidence.HIGH
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CONTEXT BUILDING (Simplified Data Flow)
    // ══════════════════════════════════════════════════════════

    private data class CodeContext(
        val userInputVariables: Set<String>,       // Variables that come from user input
        val functionParameters: Set<String>,        // Function parameters (potential user input)
        val databaseVariables: Set<String>,         // Variables used in DB operations
        val fileVariables: Set<String>,             // Variables used in file operations
        val networkVariables: Set<String>           // Variables used in network calls
    )

    private fun buildContext(lines: List<String>): CodeContext {
        val userInputVars = mutableSetOf<String>()
        val funcParams = mutableSetOf<String>()
        val dbVars = mutableSetOf<String>()
        val fileVars = mutableSetOf<String>()
        val networkVars = mutableSetOf<String>()

        val fullCode = lines.joinToString("\n")

        // Extract function parameters
        val paramPattern = Regex("""fun\s+\w+\s*\(([^)]*)\)""")
        paramPattern.findAll(fullCode).forEach { match ->
            val params = match.groupValues[1]
            val paramNames = Regex("""(\w+)\s*:""").findAll(params)
            paramNames.forEach { funcParams.add(it.groupValues[1]) }
        }

        // Track user input sources
        val inputPatterns = listOf(
            Regex("""val\s+(\w+)\s*=.*(?:getStringExtra|getIntExtra|getData|readLine|Scanner)"""),
            Regex("""val\s+(\w+)\s*=.*(?:request\.|req\.|params\[|query\[)"""),
            Regex("""val\s+(\w+)\s*=.*(?:getText|text\.toString|editText)""", RegexOption.IGNORE_CASE),
            Regex("""(\w+)\s*=\s*(?:intent|bundle)\??\.""")
        )

        inputPatterns.forEach { pattern ->
            pattern.findAll(fullCode).forEach { match ->
                userInputVars.add(match.groupValues[1])
            }
        }

        // Track database usage
        val dbPattern = Regex("""(?:query|rawQuery|execSQL|execute|insert|update|delete)\s*\([^)]*(\w+)""")
        dbPattern.findAll(fullCode).forEach { match ->
            dbVars.add(match.groupValues[1])
        }

        // Track file operations
        val filePattern = Regex("""(?:File|FileInputStream|FileOutputStream|FileReader|FileWriter)\s*\([^)]*(\w+)""")
        filePattern.findAll(fullCode).forEach { match ->
            fileVars.add(match.groupValues[1])
        }

        return CodeContext(
            userInputVariables = userInputVars,
            functionParameters = funcParams,
            databaseVariables = dbVars,
            fileVariables = fileVars,
            networkVariables = networkVars
        )
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: HARDCODED SECRETS
    // ══════════════════════════════════════════════════════════

    private fun checkHardcodedSecrets(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed
            if (isTestOrMock(line)) return@forEachIndexed

            for (pattern in SecurityPatterns.HARDCODED_SECRETS) {
                if (pattern.regex.containsMatchIn(line)) {
                    // Skip empty strings and placeholders
                    if (line.contains("\"\"") ||
                        line.contains("\"TODO\"") ||
                        line.contains("\"null\"") ||
                        line.contains("\"example\"") ||
                        line.contains("\"your-") ||
                        line.contains("\"<") ||
                        line.contains("\"{")) {
                        continue
                    }

                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.CRITICAL,
                        category = "Secrets",
                        title = pattern.title,
                        description = pattern.description,
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Use BuildConfig or Android Keystore.\n\n${FixSnippets.BUILDCONFIG_SECRET}"
                    ))
                    break // One match per line for secrets
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: SQL INJECTION (Context-Aware)
    // ══════════════════════════════════════════════════════════

    private fun checkSQLInjection(
        lines: List<String>,
        context: CodeContext,
        out: MutableList<SecurityVulnerability>
    ) {
        val sqlKeywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE TABLE", "ALTER", "TRUNCATE")

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val upper = line.uppercase()
            val hasSql = sqlKeywords.any { upper.contains(it) }
            if (!hasSql) return@forEachIndexed

            // Check for concatenation with variables
            val hasConcat = line.contains("+") || line.contains("\${") || line.contains("\$")
            val hasStringBuild = line.contains("StringBuilder") || line.contains("buildString") || line.contains("format(")

            if (hasConcat || hasStringBuild) {
                // Extra severity if the variable is from user input
                val usesUserInput = context.userInputVariables.any { line.contains(it) } ||
                        context.functionParameters.any { line.contains(it) }

                val description = if (usesUserInput) {
                    "SQL query built with USER INPUT via string concatenation. HIGH RISK of SQL injection."
                } else {
                    "SQL query built with string concatenation. Use parameterized queries instead."
                }

                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.CRITICAL,
                    category = "Injection",
                    title = "SQL Injection Risk",
                    description = description,
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Use parameterized queries.\n\n${FixSnippets.PARAMETERIZED_QUERY}"
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: COMMAND INJECTION
    // ══════════════════════════════════════════════════════════

    private fun checkCommandInjection(
        lines: List<String>,
        context: CodeContext,
        out: MutableList<SecurityVulnerability>
    ) {
        val cmdPatterns = listOf("Runtime.getRuntime().exec", "ProcessBuilder(", "exec(", "Process")

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val hasCmd = cmdPatterns.any { line.contains(it) }
            if (!hasCmd) return@forEachIndexed

            val hasConcat = line.contains("+") || line.contains("\${") || line.contains("\$")
            val usesUserInput = context.userInputVariables.any { line.contains(it) } ||
                    context.functionParameters.any { line.contains(it) }

            if (hasConcat || usesUserInput) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.CRITICAL,
                    category = "Injection",
                    title = "Command Injection Risk",
                    description = "System command built with dynamic input. Attackers could execute arbitrary OS commands.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Never concatenate user input into commands. Use allowlists for arguments."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRITICAL: PATH TRAVERSAL
    // ══════════════════════════════════════════════════════════

    private fun checkPathTraversal(
        lines: List<String>,
        context: CodeContext,
        out: MutableList<SecurityVulnerability>
    ) {
        val filePatterns = listOf("File(", "FileInputStream(", "FileOutputStream(", "FileReader(", "FileWriter(", "Paths.get(")

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val hasFileOp = filePatterns.any { line.contains(it) }
            if (!hasFileOp) return@forEachIndexed

            val hasConcat = line.contains("+") || line.contains("\${") || line.contains("\$")
            val usesUserInput = context.userInputVariables.any { line.contains(it) } ||
                    context.functionParameters.any { line.contains(it) }

            if (hasConcat || usesUserInput) {
                // Check if there's path validation nearby
                val nearbyLines = getNearbyLines(lines, index, 3)
                val hasValidation = nearbyLines.any {
                    it.contains("canonicalPath") || it.contains("normalize") ||
                            it.contains("..") || it.contains("contains(") ||
                            it.contains("startsWith(")
                }

                if (!hasValidation) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.CRITICAL,
                        category = "Injection",
                        title = "Path Traversal Risk",
                        description = "File path built with dynamic input without validation. Attackers could access files using ../.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Validate paths: file.canonicalPath.startsWith(baseDir.canonicalPath)"
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: SENSITIVE LOGGING
    // ══════════════════════════════════════════════════════════

    private fun checkSensitiveLogging(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val hasLog = SecurityPatterns.LOG_FUNCTIONS.any { it.containsMatchIn(line) }
            if (!hasLog) return@forEachIndexed

            val lower = line.lowercase()
            val found = SecurityPatterns.SENSITIVE_LOG_KEYWORDS.firstOrNull { lower.contains(it) }

            if (found != null) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.HIGH,
                    category = "Data Leak",
                    title = "Sensitive Data in Logs",
                    description = "Logging '$found' data. Logs can be read by other apps or extracted via ADB.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Remove sensitive data from logs. Use Timber with a release tree that strips logs."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: INSECURE HTTP
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureHTTP(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (SecurityPatterns.INSECURE_HTTP_PATTERN.containsMatchIn(line)) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.HIGH,
                    category = "Transport",
                    title = "Insecure HTTP Connection",
                    description = "Using HTTP instead of HTTPS. Data transmitted in plaintext can be intercepted.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Use HTTPS. Configure network_security_config.xml for exceptions if needed."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: SSL BYPASS
    // ══════════════════════════════════════════════════════════

    private fun checkSSLBypass(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val found = SecurityPatterns.SSL_BYPASS_INDICATORS.firstOrNull {
                line.contains(it, ignoreCase = true)
            }

            if (found != null) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.HIGH,
                    category = "Transport",
                    title = "SSL/TLS Validation Bypass",
                    description = "Certificate validation appears disabled. Enables man-in-the-middle attacks.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Use default SSL validation. Never trust all certificates in production."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: EMPTY CATCH
    // ══════════════════════════════════════════════════════════

    private fun checkEmptyCatch(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val singleLineEmpty = Regex("""catch\s*\([^)]*\)\s*\{\s*\}""")

        for (i in lines.indices) {
            val line = lines[i]
            if (isComment(line)) continue

            // Single-line empty catch
            if (singleLineEmpty.containsMatchIn(line)) {
                out.add(buildEmptyCatchVuln(i))
                continue
            }

            // Multi-line empty catch
            if (line.contains("catch") && line.contains("{")) {
                val closeIndex = findClosingBrace(lines, i)
                if (closeIndex != null && closeIndex > i) {
                    val catchBody = lines.subList(i + 1, closeIndex).joinToString("").trim()
                    val isEmpty = catchBody.isEmpty() ||
                            catchBody.all { it.isWhitespace() } ||
                            catchBody.replace(Regex("""//.*"""), "").trim().isEmpty()

                    if (isEmpty) {
                        out.add(buildEmptyCatchVuln(i))
                    }
                }
            }
        }
    }

    private fun buildEmptyCatchVuln(lineIndex: Int) = SecurityVulnerability(
        severity = SecuritySeverity.HIGH,
        category = "Error Handling",
        title = "Empty Catch Block",
        description = "Exceptions silently swallowed. Security-critical errors will go unnoticed.",
        lineNumber = lineIndex + 1,
        codeSnippet = null,
        fix = "Log the exception: Log.e(TAG, \"Error\", e) or handle appropriately."
    )

    // ══════════════════════════════════════════════════════════
    // HIGH: WEBVIEW ISSUES
    // ══════════════════════════════════════════════════════════

    private fun checkWebViewIssues(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for (danger in SecurityPatterns.WEBVIEW_DANGEROUS) {
                if (line.contains(danger.pattern)) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.HIGH,
                        category = "WebView",
                        title = danger.title,
                        description = danger.description,
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Only enable if necessary. Validate all loaded URLs against allowlist."
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIGH: DEBUGGABLE
    // ══════════════════════════════════════════════════════════

    private fun checkDebuggable(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (line.contains(SecurityPatterns.DEBUGGABLE_PATTERN.pattern)) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.HIGH,
                    category = "Configuration",
                    title = SecurityPatterns.DEBUGGABLE_PATTERN.title,
                    description = SecurityPatterns.DEBUGGABLE_PATTERN.description,
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Set android:debuggable=\"false\" or remove (defaults to false in release)."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: WEAK CRYPTO
    // ══════════════════════════════════════════════════════════

    private fun checkWeakCrypto(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val hasCryptoContext = SecurityPatterns.CRYPTO_CONTEXTS.any { line.contains(it) }
            if (!hasCryptoContext) return@forEachIndexed

            for ((algo, reason) in SecurityPatterns.WEAK_CRYPTO_ALGORITHMS) {
                if (line.contains(algo, ignoreCase = true)) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Cryptography",
                        title = "Weak Cryptographic Algorithm: $algo",
                        description = reason,
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Use SHA-256+ for hashing, AES-GCM for encryption.\n\n${FixSnippets.AES_ENCRYPTION}"
                    ))
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: INSECURE RANDOM
    // ══════════════════════════════════════════════════════════

    private fun checkInsecureRandom(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            // Skip if SecureRandom is used
            if (line.contains("SecureRandom")) return@forEachIndexed

            for (pattern in SecurityPatterns.INSECURE_RANDOM_PATTERNS) {
                if (pattern.containsMatchIn(line)) {
                    // Check if used in security context
                    val nearbyLines = getNearbyLines(lines, index, 5)
                    val inSecurityContext = nearbyLines.any { nearby ->
                        nearby.contains("token", true) || nearby.contains("key", true) ||
                                nearby.contains("password", true) || nearby.contains("secret", true) ||
                                nearby.contains("nonce", true) || nearby.contains("salt", true) ||
                                nearby.contains("iv", true) || nearby.contains("otp", true)
                    }

                    if (inSecurityContext) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.MEDIUM,
                            category = "Cryptography",
                            title = "Insecure Random Number Generator",
                            description = "java.util.Random is predictable. Not suitable for security-sensitive operations.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Use java.security.SecureRandom.\n\n${FixSnippets.SECURE_RANDOM}"
                        ))
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: SHAREDPREFS SECRETS
    // ══════════════════════════════════════════════════════════

    private fun checkSharedPrefsSecrets(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val prefsPattern = Regex("""\.(?:putString|putInt|putLong|putBoolean)\s*\(""")

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (prefsPattern.containsMatchIn(line)) {
                val lower = line.lowercase()
                val found = SecurityPatterns.SHARED_PREFS_SENSITIVE_KEYS.firstOrNull {
                    lower.contains("\"$it") || lower.contains("_$it")
                }

                if (found != null) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Storage",
                        title = "Sensitive Data in SharedPreferences",
                        description = "'$found' stored in SharedPreferences — saved as plaintext XML on device.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Use EncryptedSharedPreferences.\n\n${FixSnippets.SECURE_STORAGE}"
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: HARDCODED IPS
    // ══════════════════════════════════════════════════════════

    private fun checkHardcodedIPs(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val match = SecurityPatterns.HARDCODED_IP_PATTERN.find(line)
            if (match != null) {
                val ip = match.groupValues[1]
                val isLocal = SecurityPatterns.LOCAL_IP_PREFIXES.any { ip.startsWith(it) }

                if (!isLocal) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.MEDIUM,
                        category = "Configuration",
                        title = "Hardcoded IP Address",
                        description = "IP '$ip' is hardcoded. Exposes infrastructure and complicates updates.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Use BuildConfig, config files, or DNS for server addresses."
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: FILE PERMISSIONS
    // ══════════════════════════════════════════════════════════

    private fun checkFilePermissions(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            val found = SecurityPatterns.DANGEROUS_FILE_MODES.firstOrNull { line.contains(it) }
            if (found != null) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.MEDIUM,
                    category = "Storage",
                    title = "World-Accessible File",
                    description = "File created with $found. Any app on device can access this file.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Use MODE_PRIVATE. Share files via FileProvider if needed."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // MEDIUM: UNSAFE CASTS
    // ══════════════════════════════════════════════════════════

    private fun checkUnsafeCasts(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        val forceUnwrap = Regex("""!!\s*[.\[(]""")

        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            if (forceUnwrap.containsMatchIn(line)) {
                out.add(SecurityVulnerability(
                    severity = SecuritySeverity.MEDIUM,
                    category = "Null Safety",
                    title = "Force Unwrap (!!)",
                    description = "Force unwrap can crash at runtime if value is null.",
                    lineNumber = index + 1,
                    codeSnippet = sanitizeSnippet(line),
                    fix = "Use safe calls (?.), elvis (?:), or explicit null checks."
                ))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: BROAD EXCEPTION
    // ══════════════════════════════════════════════════════════

    private fun checkBroadExceptionCatch(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            for (pattern in SecurityPatterns.BROAD_EXCEPTION_PATTERNS) {
                if (pattern.containsMatchIn(line)) {
                    // Check if it's not empty (empty catch is HIGH, already caught)
                    val closeIndex = findClosingBrace(lines, index)
                    if (closeIndex != null && closeIndex > index) {
                        val catchBody = lines.subList(index + 1, closeIndex).joinToString("").trim()
                        if (catchBody.isNotEmpty()) {
                            out.add(SecurityVulnerability(
                                severity = SecuritySeverity.LOW,
                                category = "Error Handling",
                                title = "Overly Broad Exception Catch",
                                description = "Catching generic Exception may hide security-critical errors.",
                                lineNumber = index + 1,
                                codeSnippet = sanitizeSnippet(line),
                                fix = "Catch specific exceptions. Handle SecurityException separately."
                            ))
                        }
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: SECURITY TODOS
    // ══════════════════════════════════════════════════════════

    private fun checkSecurityTODOs(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        lines.forEachIndexed { index, line ->
            if (SecurityPatterns.SECURITY_TODO_PATTERN.containsMatchIn(line)) {
                val lower = line.lowercase()
                val found = SecurityPatterns.SECURITY_TODO_KEYWORDS.firstOrNull { lower.contains(it) }

                if (found != null) {
                    out.add(SecurityVulnerability(
                        severity = SecuritySeverity.LOW,
                        category = "Incomplete",
                        title = "Security TODO/FIXME",
                        description = "Unresolved TODO related to '$found'. Security tasks should not ship incomplete.",
                        lineNumber = index + 1,
                        codeSnippet = sanitizeSnippet(line),
                        fix = "Resolve security-related TODOs before production release."
                    ))
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: UNVALIDATED INPUT
    // ══════════════════════════════════════════════════════════

    private fun checkUnvalidatedInput(
        lines: List<String>,
        context: CodeContext,
        out: MutableList<SecurityVulnerability>
    ) {
        lines.forEachIndexed { index, line ->
            if (isComment(line)) return@forEachIndexed

            for (pattern in SecurityPatterns.INTENT_DATA_PATTERNS) {
                if (pattern.containsMatchIn(line)) {
                    val nearbyLines = getNearbyLines(lines, index, 3)
                    val nearbyText = nearbyLines.joinToString(" ")

                    val hasValidation = SecurityPatterns.VALIDATION_INDICATORS.any {
                        nearbyText.contains(it)
                    }

                    if (!hasValidation) {
                        out.add(SecurityVulnerability(
                            severity = SecuritySeverity.LOW,
                            category = "Input Validation",
                            title = "Unvalidated External Input",
                            description = "External input (Intent/Bundle) used without validation.",
                            lineNumber = index + 1,
                            codeSnippet = sanitizeSnippet(line),
                            fix = "Validate all external inputs.\n\n${FixSnippets.SAFE_NULL}"
                        ))
                    }
                    break
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // LOW: NULL UNSAFE
    // ══════════════════════════════════════════════════════════

    private fun checkNullUnsafe(lines: List<String>, out: MutableList<SecurityVulnerability>) {
        // This is covered by checkUnsafeCasts, but we can add more patterns here
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private fun isComment(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("//") || trimmed.startsWith("/*") ||
                trimmed.startsWith("*") || trimmed.startsWith("<!--")
    }

    private fun isTestOrMock(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("test") || lower.contains("mock") ||
                lower.contains("fake") || lower.contains("stub") ||
                lower.contains("example") || lower.contains("sample")
    }

    private fun sanitizeSnippet(line: String): String {
        // Don't expose actual secrets in the UI
        var sanitized = line.trim().take(100)

        // Mask potential secrets
        sanitized = sanitized.replace(Regex("""(["'])[A-Za-z0-9_\-]{20,}\1""")) {
            "${it.value.first()}***REDACTED***${it.value.last()}"
        }

        return sanitized
    }

    private fun getNearbyLines(lines: List<String>, index: Int, range: Int): List<String> {
        val start = maxOf(0, index - range)
        val end = minOf(lines.size, index + range + 1)
        return lines.subList(start, end)
    }

    private fun findClosingBrace(lines: List<String>, startIndex: Int): Int? {
        var braceCount = 0
        var foundOpen = false

        for (i in startIndex until minOf(lines.size, startIndex + 20)) {
            for (char in lines[i]) {
                if (char == '{') {
                    braceCount++
                    foundOpen = true
                } else if (char == '}') {
                    braceCount--
                    if (foundOpen && braceCount == 0) {
                        return i
                    }
                }
            }
        }
        return null
    }

    private fun deduplicateVulnerabilities(vulns: List<SecurityVulnerability>): List<SecurityVulnerability> {
        val seen = mutableSetOf<String>()
        return vulns.filter { v ->
            val key = "${v.lineNumber}-${v.title}"
            if (key in seen) false
            else {
                seen.add(key)
                true
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // SCORING
    // ══════════════════════════════════════════════════════════

    private fun calculateScore(vulns: List<SecurityVulnerability>): Int {
        if (vulns.isEmpty()) return 100

        val criticalCount = vulns.count { it.severity == SecuritySeverity.CRITICAL }
        val highCount = vulns.count { it.severity == SecuritySeverity.HIGH }
        val mediumCount = vulns.count { it.severity == SecuritySeverity.MEDIUM }
        val lowCount = vulns.count { it.severity == SecuritySeverity.LOW }

        var deductions = 0

        // Critical: First costs 12, diminishing returns
        if (criticalCount > 0) {
            deductions += 12 + ((criticalCount - 1).coerceAtMost(4) * 6)
        }

        // High: First costs 8, diminishing returns
        if (highCount > 0) {
            deductions += 8 + ((highCount - 1).coerceAtMost(4) * 4)
        }

        // Medium: First costs 4, diminishing returns
        if (mediumCount > 0) {
            deductions += 4 + ((mediumCount - 1).coerceAtMost(4) * 2)
        }

        // Low: Each costs 1, max 5
        deductions += lowCount.coerceAtMost(5)

        val score = 100 - deductions

        return when {
            criticalCount >= 3 -> score.coerceIn(5, 25)
            criticalCount >= 1 -> score.coerceIn(15, 50)
            highCount >= 3 -> score.coerceIn(25, 60)
            highCount >= 1 -> score.coerceIn(35, 75)
            else -> score.coerceIn(50, 100)
        }
    }

    private fun scoreToGrade(score: Int): String = when {
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 50 -> "C"
        score >= 30 -> "D"
        else -> "F"
    }

    private fun buildSummary(vulns: List<SecurityVulnerability>, score: Int, grade: String): String {
        if (vulns.isEmpty()) return "✅ No security vulnerabilities detected."

        val critical = vulns.count { it.severity == SecuritySeverity.CRITICAL }
        val high = vulns.count { it.severity == SecuritySeverity.HIGH }
        val total = vulns.size

        return when {
            critical > 0 -> "🚨 $total issues ($critical critical). Immediate action required."
            high > 0 -> "⚠️ $total issues ($high high severity). Review recommended."
            score >= 70 -> "💡 $total minor issues. Code is mostly secure."
            else -> "📋 $total issues found. Several areas need improvement."
        }
    }

    object FixSnippets {
        val SECURE_STORAGE = """
            |// Use EncryptedSharedPreferences:
            |val prefs = EncryptedSharedPreferences.create(
            |    context, "secure_prefs",
            |    MasterKey.Builder(context)
            |        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            |        .build(),
            |    PrefKeyEncryptionScheme.AES256_SIV,
            |    PrefValueEncryptionScheme.AES256_GCM
            |)
        """.trimMargin()

        val PARAMETERIZED_QUERY = """
            |// Use parameterized query:
            |val cursor = db.query(
            |    "users",
            |    arrayOf("id", "name"),
            |    "id = ?",
            |    arrayOf(userId),
            |    null, null, null
            |)
        """.trimMargin()

        val SECURE_RANDOM = """
            |// Use SecureRandom:
            |val random = java.security.SecureRandom()
            |val token = ByteArray(32)
            |random.nextBytes(token)
        """.trimMargin()

        val SAFE_NULL = """
            |// Safe null handling:
            |val value = intent.getStringExtra("key") ?: ""
            |// or
            |val value = intent.getStringExtra("key")?.let {
            |    // use it safely
            |} ?: run {
            |    // handle null
            |}
        """.trimMargin()

        val PENDING_INTENT_FIX = """
            |// Secure PendingIntent:
            |val intent = Intent(context, MyActivity::class.java)
            |val pendingIntent = PendingIntent.getActivity(
            |    context, 0, intent,
            |    PendingIntent.FLAG_IMMUTABLE
            |)
        """.trimMargin()

        val AES_ENCRYPTION = """
            |// Proper AES-GCM encryption:
            |val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            |val iv = ByteArray(12)
            |SecureRandom().nextBytes(iv)
            |cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        """.trimMargin()

        val BUILDCONFIG_SECRET = """
            |// Use BuildConfig for secrets:
            |// In build.gradle:
            |// buildConfigField "String", "API_KEY", "\"${'$'}{project.properties['API_KEY']}\""
            |// In gradle.properties:
            |// API_KEY=your_key_here
            |// In code:
            |val apiKey = BuildConfig.API_KEY
        """.trimMargin()

        val BROADCAST_FIX = """
            |// Secure broadcast receiver:
            |if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            |    registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            |} else {
            |    LocalBroadcastManager.getInstance(this)
            |        .registerReceiver(receiver, filter)
            |}
        """.trimMargin()

        val DEEPLINK_VALIDATION = """
            |// Validate deep link:
            |val uri = intent.data ?: return
            |if (uri.scheme != "https" || uri.host != "myapp.com") {
            |    return // reject untrusted URIs
            |}
            |val path = uri.path ?: return
            |when {
            |    path.startsWith("/user/") -> handleUser(uri)
            |    else -> return // unknown path
            |}
        """.trimMargin()
    }
}