package com.runanywhere.kotlin_starter_example.kodent.engine

/**
 * OWASP Top 10 (2021) and CWE mappings.
 * Maps vulnerability categories to industry standards.
 */
object OWASPMapping {

    // OWASP Top 10 - 2021
    const val A01 = "A01:2021 Broken Access Control"
    const val A02 = "A02:2021 Cryptographic Failures"
    const val A03 = "A03:2021 Injection"
    const val A04 = "A04:2021 Insecure Design"
    const val A05 = "A05:2021 Security Misconfiguration"
    const val A06 = "A06:2021 Vulnerable Components"
    const val A07 = "A07:2021 Auth & Identity Failures"
    const val A08 = "A08:2021 Software & Data Integrity"
    const val A09 = "A09:2021 Logging & Monitoring Failures"
    const val A10 = "A10:2021 Server-Side Request Forgery"

    // OWASP Mobile Top 10 - 2024
    const val M01 = "M01 Improper Credential Usage"
    const val M02 = "M02 Inadequate Supply Chain Security"
    const val M03 = "M03 Insecure Authentication"
    const val M04 = "M04 Insufficient Input/Output Validation"
    const val M05 = "M05 Insecure Communication"
    const val M06 = "M06 Inadequate Privacy Controls"
    const val M07 = "M07 Insufficient Binary Protections"
    const val M08 = "M08 Security Misconfiguration"
    const val M09 = "M09 Insecure Data Storage"
    const val M10 = "M10 Insufficient Cryptography"

    /**
     * Auto-enrich vulnerability with OWASP and CWE based on category + title
     */
    fun enrich(vuln: SecurityVulnerability): SecurityVulnerability {
        val mapping = getMapping(vuln.category, vuln.title)
        val (cvss, vector) = calculateCVSS(vuln)
        return vuln.copy(
            owasp = mapping.owasp,
            cwe = mapping.cwe,
            cvssScore = cvss,
            cvssVector = vector
        )
    }

    private data class Mapping(val owasp: String, val cwe: String)

    private fun getMapping(category: String, title: String): Mapping {
        val lowerTitle = title.lowercase()
        val lowerCat = category.lowercase()

        return when {
            // Secrets
            lowerCat == "secrets" -> when {
                lowerTitle.contains("api key") -> Mapping("$A02 | $M01", "CWE-798")
                lowerTitle.contains("password") -> Mapping("$A07 | $M01", "CWE-259")
                lowerTitle.contains("token") || lowerTitle.contains("secret") -> Mapping("$A02 | $M01", "CWE-798")
                lowerTitle.contains("private key") -> Mapping("$A02 | $M01", "CWE-321")
                lowerTitle.contains("connection string") -> Mapping("$A02 | $M09", "CWE-798")
                else -> Mapping(A02, "CWE-798")
            }

            // Injection
            lowerCat == "injection" -> when {
                lowerTitle.contains("sql") -> Mapping("$A03 | $M04", "CWE-89")
                lowerTitle.contains("command") -> Mapping("$A03 | $M04", "CWE-78")
                lowerTitle.contains("path") -> Mapping("$A01 | $M04", "CWE-22")
                lowerTitle.contains("ldap") -> Mapping(A03, "CWE-90")
                lowerTitle.contains("xpath") -> Mapping(A03, "CWE-643")
                lowerTitle.contains("xxe") || lowerTitle.contains("xml") -> Mapping(A05, "CWE-611")
                lowerTitle.contains("deserialization") -> Mapping(A08, "CWE-502")
                lowerTitle.contains("header") -> Mapping(A03, "CWE-113")
                lowerTitle.contains("open redirect") -> Mapping(A01, "CWE-601")
                else -> Mapping(A03, "CWE-74")
            }

            // Data Leak
            lowerCat == "data leak" -> when {
                lowerTitle.contains("log") -> Mapping("$A09 | $M09", "CWE-532")
                lowerTitle.contains("clipboard") -> Mapping("$M09 | $M06", "CWE-200")
                else -> Mapping(A09, "CWE-200")
            }

            // Transport
            lowerCat == "transport" -> when {
                lowerTitle.contains("http") -> Mapping("$A02 | $M05", "CWE-319")
                lowerTitle.contains("ssl") || lowerTitle.contains("tls") -> Mapping("$A02 | $M05", "CWE-295")
                lowerTitle.contains("certificate") -> Mapping("$A02 | $M05", "CWE-295")
                else -> Mapping(A02, "CWE-319")
            }

            // Error Handling
            lowerCat == "error handling" -> when {
                lowerTitle.contains("empty catch") -> Mapping(A05, "CWE-390")
                lowerTitle.contains("broad") -> Mapping(A05, "CWE-396")
                else -> Mapping(A05, "CWE-755")
            }

            // WebView
            lowerCat == "webview" -> when {
                lowerTitle.contains("javascript") && lowerTitle.contains("interface") -> Mapping("$A05 | $M08", "CWE-749")
                lowerTitle.contains("javascript") -> Mapping("$A05 | $M08", "CWE-79")
                lowerTitle.contains("file access") -> Mapping("$A01 | $M08", "CWE-200")
                else -> Mapping(A05, "CWE-749")
            }

            // Cryptography
            lowerCat == "cryptography" -> when {
                lowerTitle.contains("weak") -> Mapping("$A02 | $M10", "CWE-327")
                lowerTitle.contains("random") -> Mapping("$A02 | $M10", "CWE-330")
                lowerTitle.contains("iv") || lowerTitle.contains("nonce") -> Mapping("$A02 | $M10", "CWE-329")
                lowerTitle.contains("key size") || lowerTitle.contains("key length") -> Mapping("$A02 | $M10", "CWE-326")
                else -> Mapping(A02, "CWE-327")
            }

            // Storage
            lowerCat == "storage" -> when {
                lowerTitle.contains("sharedpreferences") || lowerTitle.contains("shared pref") -> Mapping("$A02 | $M09", "CWE-312")
                lowerTitle.contains("world") -> Mapping("$A01 | $M09", "CWE-276")
                lowerTitle.contains("external storage") -> Mapping("$A01 | $M09", "CWE-922")
                lowerTitle.contains("backup") -> Mapping("$A05 | $M09", "CWE-312")
                else -> Mapping(M09, "CWE-312")
            }

            // Configuration
            lowerCat == "configuration" -> when {
                lowerTitle.contains("debuggable") -> Mapping("$A05 | $M08", "CWE-489")
                lowerTitle.contains("ip") -> Mapping(A05, "CWE-200")
                lowerTitle.contains("exported") -> Mapping("$A01 | $M08", "CWE-926")
                lowerTitle.contains("backup") -> Mapping("$A05 | $M08", "CWE-312")
                else -> Mapping(A05, "CWE-16")
            }

            // Input Validation
            lowerCat == "input validation" -> Mapping("$A03 | $M04", "CWE-20")

            // Null Safety
            lowerCat == "null safety" -> Mapping(A04, "CWE-476")

            // Incomplete
            lowerCat == "incomplete" -> Mapping(A04, "CWE-1164")

            // Android Components
            lowerCat == "android component" -> when {
                lowerTitle.contains("broadcast") -> Mapping("$A01 | $M08", "CWE-925")
                lowerTitle.contains("content provider") -> Mapping("$A01 | $M08", "CWE-926")
                lowerTitle.contains("pending intent") -> Mapping("$A01 | $M08", "CWE-927")
                lowerTitle.contains("deeplink") -> Mapping("$A01 | $M04", "CWE-939")
                else -> Mapping(M08, "CWE-926")
            }

            // Binary Protection
            lowerCat == "binary protection" -> when {
                lowerTitle.contains("obfuscation") || lowerTitle.contains("proguard") -> Mapping("$A05 | $M07", "CWE-656")
                lowerTitle.contains("root") || lowerTitle.contains("jailbreak") -> Mapping(M07, "CWE-919")
                lowerTitle.contains("dynamic") || lowerTitle.contains("reflection") -> Mapping(M07, "CWE-470")
                else -> Mapping(M07, "CWE-693")
            }

            // Privacy
            lowerCat == "privacy" -> Mapping("$A01 | $M06", "CWE-359")

            // Default
            else -> Mapping(A04, "CWE-710")
        }
    }

    /**
     * Get list of OWASP categories covered in a scan
     */
    fun getCoveredCategories(vulns: List<SecurityVulnerability>): List<String> {
        val categories = mutableSetOf<String>()
        for (vuln in vulns) {
            if (vuln.owasp.isNotBlank()) {
                // Extract individual OWASP IDs
                val ids = vuln.owasp.split("|").map { it.trim() }
                categories.addAll(ids)
            }
        }
        return categories.sorted()
    }

    /**
     * CVSS v3.1 Base Score estimation.
     * Simplified scoring based on vulnerability characteristics.
     * Real CVSS requires network/complexity/privilege analysis.
     */
    fun calculateCVSS(vuln: SecurityVulnerability): Pair<Float, String> {
        // CVSS v3.1 Base Score Components
        // AV: Attack Vector    (N=Network, A=Adjacent, L=Local, P=Physical)
        // AC: Attack Complexity (L=Low, H=High)
        // PR: Privileges Required (N=None, L=Low, H=High)
        // UI: User Interaction (N=None, R=Required)
        // S:  Scope            (U=Unchanged, C=Changed)
        // C:  Confidentiality  (H=High, L=Low, N=None)
        // I:  Integrity        (H=High, L=Low, N=None)
        // A:  Availability     (H=High, L=Low, N=None)

        val lowerTitle = vuln.title.lowercase()
        val lowerCat = vuln.category.lowercase()

        return when {
            // CRITICAL: Remote Code Execution vectors
            lowerTitle.contains("sql injection") ->
                9.8f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"

            lowerTitle.contains("command injection") ->
                9.8f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"

            lowerTitle.contains("deserialization") && vuln.severity == SecuritySeverity.CRITICAL ->
                9.8f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"

            lowerTitle.contains("xxe") || lowerTitle.contains("xml external") ->
                9.1f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N"

            // CRITICAL: Secret exposure
            lowerTitle.contains("api key") || lowerTitle.contains("openai") ||
                    lowerTitle.contains("aws") || lowerTitle.contains("github") ->
                8.6f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:N/A:N"

            lowerTitle.contains("private key") ->
                9.1f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N"

            lowerTitle.contains("password") && lowerCat == "secrets" ->
                7.5f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("connection string") ->
                8.6f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:N/A:N"

            lowerTitle.contains("secret") || lowerTitle.contains("token") ->
                7.5f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            // CRITICAL: Path traversal
            lowerTitle.contains("path traversal") ->
                7.5f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            // HIGH: Transport security
            lowerTitle.contains("ssl") || lowerTitle.contains("tls") ->
                7.4f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:N"

            lowerTitle.contains("cleartext") ->
                7.5f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("http") && lowerCat == "transport" ->
                5.9f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"

            // HIGH: Android components
            lowerTitle.contains("pending intent") ->
                7.8f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N"

            lowerTitle.contains("broadcast") ->
                6.5f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("content provider") ->
                6.5f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("javascript") && lowerCat == "webview" ->
                6.1f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"

            lowerTitle.contains("open redirect") ->
                6.1f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"

            // HIGH: Data leaks
            lowerTitle.contains("log") && lowerCat.contains("leak") ->
                5.5f to "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("empty catch") ->
                5.3f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H"

            lowerTitle.contains("debuggable") ->
                5.5f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            // MEDIUM: Crypto weaknesses
            lowerTitle.contains("weak crypto") || lowerTitle.contains("md5") ||
                    lowerTitle.contains("sha1") || lowerTitle.contains("des") ->
                5.3f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("random") && lowerCat == "cryptography" ->
                5.3f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("key size") ->
                5.3f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("hardcoded iv") || lowerTitle.contains("nonce") ->
                5.3f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"

            // MEDIUM: Storage
            lowerTitle.contains("sharedpreferences") || lowerTitle.contains("shared pref") ->
                4.4f to "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("external storage") ->
                4.7f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:L/I:L/A:N"

            lowerTitle.contains("world") && lowerCat == "storage" ->
                5.5f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"

            lowerTitle.contains("clipboard") ->
                4.3f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:L/I:N/A:N"

            // MEDIUM: Other
            lowerTitle.contains("hardcoded ip") ->
                3.7f to "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N"

            lowerTitle.contains("deeplink") ->
                5.3f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N"

            lowerTitle.contains("force unwrap") || lowerTitle.contains("unsafe cast") ->
                3.7f to "CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:L"

            // LOW
            lowerTitle.contains("broad exception") || lowerTitle.contains("overly broad") ->
                3.1f to "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:N/I:N/A:L"

            lowerTitle.contains("todo") || lowerTitle.contains("fixme") ->
                2.0f to "CVSS:3.1/AV:L/AC:H/PR:H/UI:N/S:U/C:L/I:N/A:N"

            lowerTitle.contains("unvalidated") ->
                3.5f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:L/I:N/A:N"

            lowerTitle.contains("reflection") ->
                3.1f to "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:L/I:L/A:N"

            lowerTitle.contains("dynamic code") ->
                4.7f to "CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:L/I:H/A:N"

            lowerTitle.contains("obfuscation") || lowerTitle.contains("proguard") ->
                2.0f to "CVSS:3.1/AV:L/AC:H/PR:H/UI:N/S:U/C:L/I:N/A:N"

            lowerTitle.contains("backup") ->
                2.4f to "CVSS:3.1/AV:L/AC:L/PR:H/UI:N/S:U/C:L/I:N/A:N"

            // Default based on severity
            else -> when (vuln.severity) {
                SecuritySeverity.CRITICAL -> 9.0f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"
                SecuritySeverity.HIGH -> 7.0f to "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"
                SecuritySeverity.MEDIUM -> 5.0f to "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:L/I:L/A:N"
                SecuritySeverity.LOW -> 3.0f to "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:L/I:N/A:N"
            }
        }
    }
}