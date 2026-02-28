package com.runanywhere.kotlin_starter_example.kodent.engine

data class ComplianceIssue(
    val framework: ComplianceFramework,
    val article: String,
    val title: String,
    val description: String,
    val lineNumber: Int?,
    val fix: String
)

enum class ComplianceFramework {
    GDPR, HIPAA, PCI_DSS, SOC2, COPPA
}

data class ComplianceResult(
    val issues: List<ComplianceIssue>,
    val gdprCompliant: Boolean,
    val hipaaCompliant: Boolean,
    val pciCompliant: Boolean,
    val summary: String
)

object ComplianceChecker {

    fun check(code: String, language: String): ComplianceResult {
        val lines = code.lines()
        val issues = mutableListOf<ComplianceIssue>()

        checkGDPR(lines, issues)
        checkHIPAA(lines, issues)
        checkPCIDSS(lines, issues)
        checkSOC2(lines, issues)
        checkCOPPA(lines, issues)

        val gdprIssues = issues.count { it.framework == ComplianceFramework.GDPR }
        val hipaaIssues = issues.count { it.framework == ComplianceFramework.HIPAA }
        val pciIssues = issues.count { it.framework == ComplianceFramework.PCI_DSS }

        return ComplianceResult(
            issues = issues,
            gdprCompliant = gdprIssues == 0,
            hipaaCompliant = hipaaIssues == 0,
            pciCompliant = pciIssues == 0,
            summary = buildSummary(issues)
        )
    }

    // ══════════════════════════════════════════════════════════
    // GDPR (General Data Protection Regulation)
    // ══════════════════════════════════════════════════════════

    private fun checkGDPR(lines: List<String>, issues: MutableList<ComplianceIssue>) {
        val personalDataKeywords = listOf(
            "email", "phone", "address", "name", "dateOfBirth", "date_of_birth",
            "dob", "ssn", "social_security", "nationality", "gender", "age",
            "location", "latitude", "longitude", "ip_address", "ipAddress",
            "device_id", "deviceId", "advertising_id", "advertisingId"
        )

        val fullCode = lines.joinToString("\n").lowercase()

        // Check: Personal data logged
        lines.forEachIndexed { index, line ->
            if (isLogStatement(line)) {
                val lower = line.lowercase()
                val found = personalDataKeywords.firstOrNull { lower.contains(it) }
                if (found != null) {
                    issues.add(ComplianceIssue(
                        framework = ComplianceFramework.GDPR,
                        article = "Art. 5(1)(f) - Integrity & Confidentiality",
                        title = "Personal Data Logged",
                        description = "Personal data field '$found' found in log statement. Logs are not encrypted.",
                        lineNumber = index + 1,
                        fix = "Remove personal data from logs. Use anonymized identifiers."
                    ))
                }
            }
        }

        // Check: Personal data stored without encryption
        lines.forEachIndexed { index, line ->
            if (line.contains("putString") || line.contains("putInt") || line.contains("putLong")) {
                val lower = line.lowercase()
                val found = personalDataKeywords.firstOrNull { lower.contains(it) }
                if (found != null) {
                    val nearbyLines = getNearbyLines(lines, index, 5)
                    val isEncrypted = nearbyLines.any {
                        it.contains("EncryptedSharedPreferences") || it.contains("encrypt", true)
                    }
                    if (!isEncrypted) {
                        issues.add(ComplianceIssue(
                            framework = ComplianceFramework.GDPR,
                            article = "Art. 32 - Security of Processing",
                            title = "Unencrypted Personal Data Storage",
                            description = "Personal data '$found' stored without encryption.",
                            lineNumber = index + 1,
                            fix = "Use EncryptedSharedPreferences or encrypt data before storage."
                        ))
                    }
                }
            }
        }

        // Check: Personal data sent over HTTP
        if (fullCode.contains("http://") && personalDataKeywords.any { fullCode.contains(it) }) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.GDPR,
                article = "Art. 32 - Security of Processing",
                title = "Personal Data Over Unencrypted Channel",
                description = "Code contains personal data fields and insecure HTTP. Data may be transmitted unencrypted.",
                lineNumber = null,
                fix = "Use HTTPS for all connections handling personal data."
            ))
        }

        // Check: No data deletion mechanism
        val hasPersonalData = personalDataKeywords.any { fullCode.contains(it) }
        val hasDeletion = fullCode.contains("delete") || fullCode.contains("remove") ||
                fullCode.contains("clear") || fullCode.contains("erase") ||
                fullCode.contains("purge") || fullCode.contains("wipe")

        if (hasPersonalData && !hasDeletion) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.GDPR,
                article = "Art. 17 - Right to Erasure",
                title = "No Data Deletion Mechanism",
                description = "Code processes personal data but has no visible deletion/erasure functionality.",
                lineNumber = null,
                fix = "Implement data deletion for all personal data. Users have the right to erasure."
            ))
        }

        // Check: No consent mechanism
        if (hasPersonalData) {
            val hasConsent = fullCode.contains("consent") || fullCode.contains("agree") ||
                    fullCode.contains("opt_in") || fullCode.contains("optin") ||
                    fullCode.contains("permission") || fullCode.contains("gdpr")

            if (!hasConsent) {
                issues.add(ComplianceIssue(
                    framework = ComplianceFramework.GDPR,
                    article = "Art. 6/7 - Lawful Basis & Consent",
                    title = "No Consent Mechanism",
                    description = "Code processes personal data without visible consent/opt-in mechanism.",
                    lineNumber = null,
                    fix = "Implement consent collection before processing personal data."
                ))
            }
        }

        // Check: Third-party data sharing
        val thirdPartySDKs = listOf(
            "Firebase", "Analytics", "facebook", "google", "amplitude",
            "mixpanel", "segment", "crashlytics", "appsflyer", "adjust"
        )
        val usesThirdParty = thirdPartySDKs.any { fullCode.contains(it, true) }

        if (hasPersonalData && usesThirdParty) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.GDPR,
                article = "Art. 28 - Processor Requirements",
                title = "Third-Party Data Processing",
                description = "Personal data may be shared with third-party SDKs without proper data processing agreements.",
                lineNumber = null,
                fix = "Ensure Data Processing Agreements (DPAs) with all third parties. Document data flows."
            ))
        }
    }

    // ══════════════════════════════════════════════════════════
    // HIPAA (Health Insurance Portability and Accountability Act)
    // ══════════════════════════════════════════════════════════

    private fun checkHIPAA(lines: List<String>, issues: MutableList<ComplianceIssue>) {
        val phiKeywords = listOf(
            "patient", "diagnosis", "medical", "health", "prescription",
            "medication", "treatment", "insurance", "claim", "provider",
            "hospital", "doctor", "nurse", "symptom", "condition",
            "blood", "heart_rate", "heartRate", "blood_pressure", "bloodPressure",
            "weight", "height", "bmi", "allergy", "vaccine", "immunization"
        )

        val fullCode = lines.joinToString("\n").lowercase()
        val hasPHI = phiKeywords.any { fullCode.contains(it) }

        if (!hasPHI) return

        // Check: PHI in logs
        lines.forEachIndexed { index, line ->
            if (isLogStatement(line)) {
                val lower = line.lowercase()
                val found = phiKeywords.firstOrNull { lower.contains(it) }
                if (found != null) {
                    issues.add(ComplianceIssue(
                        framework = ComplianceFramework.HIPAA,
                        article = "§164.312(a)(1) - Access Control",
                        title = "PHI in Logs",
                        description = "Protected health information '$found' found in log statement.",
                        lineNumber = index + 1,
                        fix = "Never log PHI. Use de-identified data for debugging."
                    ))
                }
            }
        }

        // Check: PHI without encryption
        if (!fullCode.contains("encrypt") && !fullCode.contains("cipher") &&
            !fullCode.contains("aes") && !fullCode.contains("EncryptedSharedPreferences")) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.HIPAA,
                article = "§164.312(a)(2)(iv) - Encryption",
                title = "PHI Without Encryption",
                description = "Code handles health data but no encryption mechanism is visible.",
                lineNumber = null,
                fix = "All PHI must be encrypted at rest and in transit. Use AES-256 encryption."
            ))
        }

        // Check: PHI over HTTP
        if (fullCode.contains("http://")) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.HIPAA,
                article = "§164.312(e)(1) - Transmission Security",
                title = "PHI Over Insecure Channel",
                description = "Health data may be transmitted over unencrypted HTTP.",
                lineNumber = null,
                fix = "Use HTTPS/TLS for all PHI transmission. Implement certificate pinning."
            ))
        }

        // Check: No audit trail
        val hasAuditLog = fullCode.contains("audit") || fullCode.contains("access_log") ||
                fullCode.contains("accessLog") || fullCode.contains("activity_log")
        if (!hasAuditLog) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.HIPAA,
                article = "§164.312(b) - Audit Controls",
                title = "No Audit Trail",
                description = "No audit logging for PHI access. HIPAA requires tracking who accesses health data.",
                lineNumber = null,
                fix = "Implement audit logging for all PHI read/write/delete operations."
            ))
        }
    }

    // ══════════════════════════════════════════════════════════
    // PCI DSS (Payment Card Industry Data Security Standard)
    // ══════════════════════════════════════════════════════════

    private fun checkPCIDSS(lines: List<String>, issues: MutableList<ComplianceIssue>) {
        val cardDataKeywords = listOf(
            "card_number", "cardNumber", "credit_card", "creditCard",
            "cvv", "cvc", "card_verification", "expiry", "expiration",
            "pan", "primary_account", "cardholder", "card_holder",
            "stripe", "payment", "billing", "merchant"
        )

        val fullCode = lines.joinToString("\n").lowercase()
        val hasCardData = cardDataKeywords.any { fullCode.contains(it) }

        if (!hasCardData) return

        // Check: Card data logged
        lines.forEachIndexed { index, line ->
            if (isLogStatement(line)) {
                val lower = line.lowercase()
                val found = cardDataKeywords.firstOrNull { lower.contains(it) }
                if (found != null) {
                    issues.add(ComplianceIssue(
                        framework = ComplianceFramework.PCI_DSS,
                        article = "Req. 3.4 - Render PAN Unreadable",
                        title = "Card Data in Logs",
                        description = "Payment card data '$found' found in logs. PCI DSS prohibits logging card details.",
                        lineNumber = index + 1,
                        fix = "Never log card numbers, CVV, or expiry dates. Mask if needed: ****1234."
                    ))
                }
            }
        }

        // Check: Card data stored locally
        if ((fullCode.contains("putstring") || fullCode.contains("sharedpreferences") ||
                    fullCode.contains("sqlite") || fullCode.contains("room")) &&
            cardDataKeywords.any { fullCode.contains(it) }) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.PCI_DSS,
                article = "Req. 3.1 - Minimize Data Storage",
                title = "Card Data Stored Locally",
                description = "Payment card data appears to be stored on device. Minimize card data retention.",
                lineNumber = null,
                fix = "Never store full card numbers or CVV. Use tokenization via payment processor."
            ))
        }

        // Check: Card data over HTTP
        if (fullCode.contains("http://")) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.PCI_DSS,
                article = "Req. 4.1 - Encrypt Transmission",
                title = "Card Data Over Insecure Channel",
                description = "Payment data may be sent over HTTP. PCI DSS requires encrypted transmission.",
                lineNumber = null,
                fix = "Use TLS 1.2+ for all payment data transmission."
            ))
        }
    }

    // ══════════════════════════════════════════════════════════
    // SOC 2 (Service Organization Control)
    // ══════════════════════════════════════════════════════════

    private fun checkSOC2(lines: List<String>, issues: MutableList<ComplianceIssue>) {
        val fullCode = lines.joinToString("\n").lowercase()

        // Check: No error handling
        val hasTryCatch = fullCode.contains("try") && fullCode.contains("catch")
        val hasErrorHandling = fullCode.contains("onerror") || fullCode.contains("on_error") ||
                fullCode.contains("handleerror") || fullCode.contains("handle_error")

        if (!hasTryCatch && !hasErrorHandling && lines.size > 20) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.SOC2,
                article = "CC7.2 - System Monitoring",
                title = "Insufficient Error Handling",
                description = "No error handling visible. SOC 2 requires proper error detection and monitoring.",
                lineNumber = null,
                fix = "Implement try-catch blocks. Log errors for monitoring."
            ))
        }

        // Check: Hardcoded credentials (SOC 2 perspective)
        if (Regex("""(?:password|secret|key|token)\s*=\s*"[^"]{5,}"""", RegexOption.IGNORE_CASE).containsMatchIn(fullCode)) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.SOC2,
                article = "CC6.1 - Logical Access Security",
                title = "Hardcoded Credentials",
                description = "Credentials in source code. SOC 2 requires proper credential management.",
                lineNumber = null,
                fix = "Use a secrets manager or encrypted configuration. Rotate credentials regularly."
            ))
        }
    }

    // ══════════════════════════════════════════════════════════
    // COPPA (Children's Online Privacy Protection Act)
    // ══════════════════════════════════════════════════════════

    private fun checkCOPPA(lines: List<String>, issues: MutableList<ComplianceIssue>) {
        val fullCode = lines.joinToString("\n").lowercase()

        val childIndicators = listOf(
            "child", "kid", "minor", "underage", "under_13", "under13",
            "parental", "parent_consent", "age_gate", "age_check",
            "young", "teen", "student", "school"
        )

        val hasChildContext = childIndicators.any { fullCode.contains(it) }
        if (!hasChildContext) return

        // Check: Data collection without age gate
        val hasAgeCheck = fullCode.contains("age") && (fullCode.contains("check") ||
                fullCode.contains("verify") || fullCode.contains("gate") ||
                fullCode.contains(">=") || fullCode.contains("> 12") || fullCode.contains("> 13"))

        if (!hasAgeCheck) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.COPPA,
                article = "16 CFR §312.3 - Parental Consent",
                title = "No Age Verification",
                description = "Code targets children but has no age verification mechanism.",
                lineNumber = null,
                fix = "Implement age gate before collecting any data from users under 13."
            ))
        }

        // Check: Tracking in child context
        val hasTracking = fullCode.contains("analytics") || fullCode.contains("track") ||
                fullCode.contains("advertising") || fullCode.contains("ad_id")
        if (hasTracking) {
            issues.add(ComplianceIssue(
                framework = ComplianceFramework.COPPA,
                article = "16 CFR §312.5 - Data Collection",
                title = "Tracking in Child Context",
                description = "Analytics/tracking detected in code that references children.",
                lineNumber = null,
                fix = "Disable tracking and advertising for users under 13. No behavioral advertising."
            ))
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private fun isLogStatement(line: String): Boolean {
        return line.contains("Log.") || line.contains("println") ||
                line.contains("Timber.") || line.contains("logger.") ||
                line.contains("System.out")
    }

    private fun getNearbyLines(lines: List<String>, index: Int, range: Int): List<String> {
        val start = maxOf(0, index - range)
        val end = minOf(lines.size, index + range + 1)
        return lines.subList(start, end)
    }

    private fun buildSummary(issues: List<ComplianceIssue>): String {
        if (issues.isEmpty()) return "✅ No compliance issues detected."

        val frameworks = issues.map { it.framework }.distinct()
        val frameworkNames = frameworks.joinToString(", ") {
            when (it) {
                ComplianceFramework.GDPR -> "GDPR"
                ComplianceFramework.HIPAA -> "HIPAA"
                ComplianceFramework.PCI_DSS -> "PCI DSS"
                ComplianceFramework.SOC2 -> "SOC 2"
                ComplianceFramework.COPPA -> "COPPA"
            }
        }

        return "⚠️ ${issues.size} compliance issues across $frameworkNames."
    }
}