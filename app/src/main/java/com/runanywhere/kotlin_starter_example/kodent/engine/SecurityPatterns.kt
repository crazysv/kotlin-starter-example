package com.runanywhere.kotlin_starter_example.kodent.engine

/**
 * Centralized security vulnerability patterns.
 * Easy to add new patterns without touching scanner logic.
 */
object SecurityPatterns {

    // ══════════════════════════════════════════════════════════
    // CRITICAL PATTERNS
    // ══════════════════════════════════════════════════════════

    val HARDCODED_SECRETS = listOf(
        SecretPattern(
            regex = Regex("""(?:val|var|const)\s+\w*(?:api[_]?key|apiKey|API_KEY)\w*\s*=\s*"[^"]{8,}"""", RegexOption.IGNORE_CASE),
            title = "Hardcoded API Key",
            description = "API key embedded in source code. Can be extracted via APK decompilation."
        ),
        SecretPattern(
            regex = Regex("""(?:val|var|const)\s+\w*(?:password|passwd|pwd)\w*\s*=\s*"[^"]{3,}"""", RegexOption.IGNORE_CASE),
            title = "Hardcoded Password",
            description = "Password embedded in source code. Never hardcode credentials."
        ),
        SecretPattern(
            regex = Regex("""(?:val|var|const)\s+\w*(?:secret|token|auth|credential|private[_]?key)\w*\s*=\s*"[^"]{8,}"""", RegexOption.IGNORE_CASE),
            title = "Hardcoded Secret/Token",
            description = "Sensitive token embedded in code. Use secure storage instead."
        ),
        // Known API key formats
        SecretPattern(
            regex = Regex(""""sk-[a-zA-Z0-9]{20,}""""),
            title = "OpenAI API Key",
            description = "OpenAI API key detected. This grants access to paid API."
        ),
        SecretPattern(
            regex = Regex(""""AIza[a-zA-Z0-9_-]{35,}""""),
            title = "Google API Key",
            description = "Google Cloud API key detected. Can incur charges if leaked."
        ),
        SecretPattern(
            regex = Regex(""""ghp_[a-zA-Z0-9]{36,}""""),
            title = "GitHub Personal Access Token",
            description = "GitHub PAT detected. Grants repository access."
        ),
        SecretPattern(
            regex = Regex(""""AKIA[A-Z0-9]{16,}""""),
            title = "AWS Access Key",
            description = "AWS access key detected. Can access cloud resources."
        ),
        SecretPattern(
            regex = Regex(""""xox[bsrap]-[a-zA-Z0-9-]{10,}""""),
            title = "Slack Token",
            description = "Slack API token detected. Grants workspace access."
        ),
        SecretPattern(
            regex = Regex(""""-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----""""),
            title = "Private Key",
            description = "Private key embedded in code. Extremely sensitive."
        ),
        SecretPattern(
            regex = Regex(""""mongodb(?:\\+srv)?://[^"]+:[^"]+@[^"]+""""),
            title = "MongoDB Connection String",
            description = "Database credentials in connection string."
        ),
        SecretPattern(
            regex = Regex(""""postgres(?:ql)?://[^"]+:[^"]+@[^"]+""""),
            title = "PostgreSQL Connection String",
            description = "Database credentials in connection string."
        )
    )

    val SQL_INJECTION_PATTERNS = listOf(
        InjectionPattern(
            keywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE TABLE", "ALTER TABLE", "TRUNCATE"),
            concatIndicators = listOf("+", "\${", "\$", "StringBuilder", "buildString", "format(", "String.format"),
            title = "SQL Injection",
            description = "SQL query built with string concatenation. Use parameterized queries."
        )
    )

    val COMMAND_INJECTION_PATTERNS = listOf(
        InjectionPattern(
            keywords = listOf("Runtime.getRuntime().exec", "ProcessBuilder(", "exec(", "execSync(", "spawn("),
            concatIndicators = listOf("+", "\${", "\$"),
            title = "Command Injection",
            description = "System command built with dynamic input. Attackers could execute arbitrary commands."
        )
    )

    val PATH_TRAVERSAL_PATTERNS = listOf(
        InjectionPattern(
            keywords = listOf("File(", "FileInputStream(", "FileOutputStream(", "FileReader(", "FileWriter(", "Paths.get("),
            concatIndicators = listOf("+", "\${", "\$"),
            title = "Path Traversal Risk",
            description = "File path built with dynamic input. Attackers could access arbitrary files using ../."
        )
    )

    val LDAP_INJECTION_PATTERNS = listOf(
        InjectionPattern(
            keywords = listOf("ldap://", "ldaps://", "search(", "LdapContext", "DirContext"),
            concatIndicators = listOf("+", "\${", "\$"),
            title = "LDAP Injection Risk",
            description = "LDAP query built with dynamic input. Could leak directory information."
        )
    )

    val XPATH_INJECTION_PATTERNS = listOf(
        InjectionPattern(
            keywords = listOf("xpath", "evaluate(", "XPath", "selectNodes", "selectSingleNode"),
            concatIndicators = listOf("+", "\${", "\$"),
            title = "XPath Injection Risk",
            description = "XPath query built with dynamic input. Could extract unauthorized data."
        )
    )

    // ══════════════════════════════════════════════════════════
    // HIGH PATTERNS
    // ══════════════════════════════════════════════════════════

    val SENSITIVE_LOG_KEYWORDS = listOf(
        "password", "passwd", "pwd", "secret", "token", "api_key", "apikey",
        "api-key", "credential", "auth", "session", "cookie", "bearer",
        "authorization", "private", "ssn", "social_security", "credit_card",
        "card_number", "cvv", "pin", "otp"
    )

    val LOG_FUNCTIONS = listOf(
        Regex("""Log\.[dewivwtf]\s*\("""),
        Regex("""println\s*\("""),
        Regex("""print\s*\("""),
        Regex("""Timber\.[dewiv]\s*\("""),
        Regex("""logger\.\w+\s*\(""", RegexOption.IGNORE_CASE),
        Regex("""console\.log\s*\("""),
        Regex("""System\.out\.print""")
    )

    val SSL_BYPASS_INDICATORS = listOf(
        "TrustAllCerts", "ALLOW_ALL_HOSTNAME_VERIFIER", "trustAllCertificates",
        "setHostnameVerifier", "X509TrustManager", "disableSslVerification",
        "AcceptAllCertificates", "InsecureTrustManager", "trustAll",
        "ALLOW_ALL_HOSTNAMES", "NoopHostnameVerifier", "checkServerTrusted",
        "SSLSocketFactory.ALLOW_ALL"
    )

    val WEBVIEW_DANGEROUS = listOf(
        DangerousCall(
            pattern = "setJavaScriptEnabled(true)",
            title = "JavaScript Enabled in WebView",
            description = "JavaScript in WebView can lead to XSS if loading untrusted content."
        ),
        DangerousCall(
            pattern = "addJavascriptInterface",
            title = "JavaScript Interface Exposed",
            description = "Native methods exposed to JavaScript. Malicious web content can call app methods."
        ),
        DangerousCall(
            pattern = "setAllowFileAccess(true)",
            title = "WebView File Access Enabled",
            description = "WebView can access local files. Could leak app data to malicious pages."
        ),
        DangerousCall(
            pattern = "setAllowUniversalAccessFromFileURLs(true)",
            title = "WebView Universal File Access",
            description = "Extremely dangerous. File URLs can access any origin."
        ),
        DangerousCall(
            pattern = "setAllowFileAccessFromFileURLs(true)",
            title = "WebView Cross-File Access",
            description = "File URLs can access other file URLs. Security risk."
        )
    )

    val INSECURE_HTTP_PATTERN = Regex(""""http://(?!localhost|127\.0\.0\.1|10\.|192\.168\.|172\.(?:1[6-9]|2[0-9]|3[01])\.)""")

    // ══════════════════════════════════════════════════════════
    // MEDIUM PATTERNS
    // ══════════════════════════════════════════════════════════

    val WEAK_CRYPTO_ALGORITHMS = mapOf(
        "MD5" to "MD5 is cryptographically broken. Collisions can be generated in seconds.",
        "SHA1" to "SHA-1 is deprecated. Collision attacks are practical.",
        "SHA-1" to "SHA-1 is deprecated. Collision attacks are practical.",
        "DES" to "DES uses 56-bit keys. Can be brute-forced in hours.",
        "3DES" to "Triple DES is deprecated. Use AES instead.",
        "RC4" to "RC4 has critical biases. Broken in practice.",
        "RC2" to "RC2 is obsolete and weak.",
        "Blowfish" to "Blowfish has 64-bit blocks. Vulnerable to birthday attacks.",
        "ECB" to "ECB mode doesn't hide patterns. Never use for encryption.",
        "PKCS1Padding" to "PKCS#1 v1.5 padding is vulnerable to Bleichenbacher attacks."
    )

    val CRYPTO_CONTEXTS = listOf(
        "getInstance", "MessageDigest", "Cipher", "digest", "encrypt",
        "decrypt", "SecretKey", "KeyGenerator", "Mac."
    )

    val INSECURE_RANDOM_PATTERNS = listOf(
        Regex("""java\.util\.Random\s*\("""),
        Regex("""Random\s*\(\s*\)(?!.*Secure)"""),
        Regex("""Math\.random\s*\("""),
        Regex("""kotlin\.random\.Random(?!\.Secure)""")
    )

    val SHARED_PREFS_SENSITIVE_KEYS = listOf(
        "password", "passwd", "pwd", "token", "secret", "key", "credential",
        "session", "auth", "api_key", "apikey", "private", "bearer"
    )

    val DANGEROUS_FILE_MODES = listOf(
        "MODE_WORLD_READABLE", "MODE_WORLD_WRITEABLE", "MODE_WORLD_WRITABLE"
    )

    val HARDCODED_IP_PATTERN = Regex(""""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?::\d+)?"""")
    val LOCAL_IP_PREFIXES = listOf("127.", "0.0.0.0", "10.", "192.168.", "172.16.", "172.17.",
        "172.18.", "172.19.", "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
        "172.25.", "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.")

    // ══════════════════════════════════════════════════════════
    // LOW PATTERNS
    // ══════════════════════════════════════════════════════════

    val BROAD_EXCEPTION_PATTERNS = listOf(
        Regex("""catch\s*\(\s*\w+\s*:\s*Exception\s*\)"""),
        Regex("""catch\s*\(\s*\w+\s*:\s*Throwable\s*\)"""),
        Regex("""catch\s*\(\s*e\s*:\s*Exception\s*\)"""),
        Regex("""catch\s*\(Exception\s+\w+\)""")  // Java style
    )

    val SECURITY_TODO_PATTERN = Regex("""(?://|/\*)\s*(?:TODO|FIXME|HACK|XXX|BUG)""", RegexOption.IGNORE_CASE)
    val SECURITY_TODO_KEYWORDS = listOf(
        "security", "auth", "password", "encrypt", "decrypt", "permission",
        "token", "validate", "sanitize", "escape", "injection", "xss",
        "csrf", "ssl", "tls", "certificate", "vulnerability", "unsafe"
    )

    val INTENT_DATA_PATTERNS = listOf(
        Regex("""getStringExtra\s*\("""),
        Regex("""getIntExtra\s*\("""),
        Regex("""getLongExtra\s*\("""),
        Regex("""getBooleanExtra\s*\("""),
        Regex("""getParcelableExtra\s*\("""),
        Regex("""getSerializableExtra\s*\("""),
        Regex("""getData\s*\(\s*\)"""),
        Regex("""intent\.data""")
    )

    val VALIDATION_INDICATORS = listOf(
        "if", "?:", "?.", "require", "check", "isNullOrEmpty", "isNullOrBlank",
        "isEmpty", "isBlank", "?.let", "takeIf", "takeUnless", "when"
    )

    // ══════════════════════════════════════════════════════════
    // ANDROID SPECIFIC PATTERNS
    // ══════════════════════════════════════════════════════════

    val EXPORTED_COMPONENT_PATTERNS = listOf(
        DangerousCall(
            pattern = "exported=\"true\"",
            title = "Exported Component",
            description = "Component is accessible by other apps. Verify this is intended."
        ),
        DangerousCall(
            pattern = "android:exported=\"true\"",
            title = "Exported Component",
            description = "Component is accessible by other apps. Verify this is intended."
        )
    )

    val DANGEROUS_PERMISSIONS = listOf(
        "READ_SMS", "RECEIVE_SMS", "SEND_SMS", "READ_CALL_LOG", "WRITE_CALL_LOG",
        "READ_CONTACTS", "WRITE_CONTACTS", "RECORD_AUDIO", "CAMERA",
        "ACCESS_FINE_LOCATION", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"
    )

    val BACKUP_DANGEROUS = listOf(
        DangerousCall(
            pattern = "android:allowBackup=\"true\"",
            title = "Backup Enabled",
            description = "App data can be backed up. Sensitive data may be extracted via ADB."
        ),
        DangerousCall(
            pattern = "allowBackup=\"true\"",
            title = "Backup Enabled",
            description = "App data can be backed up. Sensitive data may be extracted via ADB."
        )
    )

    val DEBUGGABLE_PATTERN = DangerousCall(
        pattern = "android:debuggable=\"true\"",
        title = "Debuggable Build",
        description = "App is debuggable. Attackers can attach debugger and inspect runtime."
    )

    // ══════════════════════════════════════════════════════════
    // KOTLIN SPECIFIC PATTERNS
    // ══════════════════════════════════════════════════════════

    val UNSAFE_CAST_PATTERNS = listOf(
        Regex("""\bas\s+\w+(?!\?)"""),  // Unsafe cast without ?
        Regex("""\.toInt\(\)"""),        // Can throw
        Regex("""\.toLong\(\)"""),
        Regex("""\.toDouble\(\)"""),
        Regex("""!!\.""")                // Force unwrap
    )

    val NULL_UNSAFE_PATTERNS = listOf(
        Regex("""!!\s*\."""),
        Regex("""!!\s*\["""),
        Regex("""!!\s*\(""")
    )

    // ══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════════════

    data class SecretPattern(
        val regex: Regex,
        val title: String,
        val description: String
    )

    data class InjectionPattern(
        val keywords: List<String>,
        val concatIndicators: List<String>,
        val title: String,
        val description: String
    )

    data class DangerousCall(
        val pattern: String,
        val title: String,
        val description: String
    )
}