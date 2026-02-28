package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.kotlin_starter_example.kodent.engine.SecurityScanResult
import com.runanywhere.kotlin_starter_example.kodent.engine.SecuritySeverity
import com.runanywhere.kotlin_starter_example.kodent.engine.SecurityVulnerability
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import com.runanywhere.kotlin_starter_example.kodent.engine.Confidence
import com.runanywhere.kotlin_starter_example.ui.theme.AccentViolet
import com.runanywhere.kotlin_starter_example.kodent.engine.OWASPMapping
import com.runanywhere.kotlin_starter_example.kodent.engine.ComplianceResult
import com.runanywhere.kotlin_starter_example.kodent.engine.ComplianceFramework
import com.runanywhere.kotlin_starter_example.kodent.engine.ComplianceIssue

@Composable
fun SecurityScannerView(
    scanResult: SecurityScanResult,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(scanResult) {
        animationPlayed = true
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Grade Header ──
        SecurityGradeHeader(
            grade = scanResult.grade,
            score = scanResult.score,
            animate = animationPlayed
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Severity Summary ──
        if (scanResult.vulnerabilities.isNotEmpty()) {
            SeveritySummaryRow(scanResult)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Vulnerability Cards ──
        if (scanResult.vulnerabilities.isNotEmpty()) {
            scanResult.vulnerabilities.forEachIndexed { index, vuln ->
                VulnerabilityCard(vulnerability = vuln)
                if (index < scanResult.vulnerabilities.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AccentGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛡️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No Vulnerabilities Found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your code looks secure!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }


        // ── Summary ──
        if (scanResult.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                scanResult.summary,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Compliance Section ──
        if (scanResult.complianceResult != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ComplianceSection(complianceResult = scanResult.complianceResult!!)
        }

        // ── Scan Stats ──
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "⏱️ ${scanResult.scanTimeMs}ms • ${scanResult.totalChecks} checks",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
            if (scanResult.owaspCoverage.isNotEmpty()) {
                Text(
                    "OWASP: ${scanResult.owaspCoverage.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentViolet,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Grade Header ──

@Composable
private fun SecurityGradeHeader(grade: String, score: Int, animate: Boolean) {
    val animatedScore by animateFloatAsState(
        targetValue = if (animate) score.toFloat() else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "secScore"
    )

    val gradeColor = gradeToColor(grade)
    val gradeLabel = when (grade) {
        "A" -> "Secure"
        "B" -> "Good"
        "C" -> "Fair"
        "D" -> "Weak"
        "F" -> "Critical"
        else -> ""
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = gradeColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Lock + Score
            Row(verticalAlignment = Alignment.Bottom) {
                Text("🔒 ", fontSize = 24.sp)
                Text(
                    text = animatedScore.toInt().toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp
                    ),
                    color = gradeColor
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Right: Grade badge + bar
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = gradeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grade $grade • $gradeLabel",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(gradeColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedScore / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(gradeColor)
                    )
                }
            }
        }
    }
}

// ── Severity Summary Row ──

@Composable
private fun SeveritySummaryRow(scanResult: SecurityScanResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (scanResult.criticalCount > 0) {
            SeverityChip("🔴", "Critical", scanResult.criticalCount, AccentPink,
                Modifier.weight(1f))
        }
        if (scanResult.highCount > 0) {
            SeverityChip("🟠", "High", scanResult.highCount, AccentOrange,
                Modifier.weight(1f))
        }
        if (scanResult.mediumCount > 0) {
            SeverityChip("🟡", "Medium", scanResult.mediumCount, Color(0xFFFFD600),
                Modifier.weight(1f))
        }
        if (scanResult.lowCount > 0) {
            SeverityChip("🔵", "Low", scanResult.lowCount, AccentCyan,
                Modifier.weight(1f))
        }
    }
}

@Composable
private fun SeverityChip(
    dot: String,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(dot, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.width(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

// ── Vulnerability Card ──

@Composable
private fun VulnerabilityCard(vulnerability: SecurityVulnerability) {
    val color = when (vulnerability.severity) {
        SecuritySeverity.CRITICAL -> AccentPink
        SecuritySeverity.HIGH -> AccentOrange
        SecuritySeverity.MEDIUM -> Color(0xFFFFD600)
        SecuritySeverity.LOW -> AccentCyan
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Severity + Category + Confidence + Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Severity badge
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            vulnerability.severity.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontSize = 8.sp
                        )
                    }

                    // Category badge
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            vulnerability.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 8.sp
                        )
                    }

                    // Confidence badge
                    val confidenceColor = when (vulnerability.confidence) {
                        Confidence.HIGH -> AccentGreen
                        Confidence.MEDIUM -> AccentOrange
                        Confidence.LOW -> TextMuted
                    }
                    Surface(
                        color = confidenceColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            vulnerability.confidence.name,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = confidenceColor,
                            fontSize = 7.sp
                        )
                    }
                }

                if (vulnerability.lineNumber != null) {
                    Text(
                        "Line ${vulnerability.lineNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                vulnerability.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                vulnerability.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 16.sp
            )

            // Code snippet
            if (!vulnerability.codeSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        vulnerability.codeSnippet,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Fix
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text("💡 ", fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                Text(
                    vulnerability.fix,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen,
                    lineHeight = 16.sp
                )
            }

            // CVSS Score badge
            if (vulnerability.cvssScore > 0) {
                val cvssColor = when {
                    vulnerability.cvssScore >= 9.0f -> AccentPink
                    vulnerability.cvssScore >= 7.0f -> AccentOrange
                    vulnerability.cvssScore >= 4.0f -> Color(0xFFFFD600)
                    else -> AccentCyan
                }
                Surface(
                    color = cvssColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "CVSS %.1f".format(vulnerability.cvssScore),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = cvssColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // OWASP + CWE badges
            if (vulnerability.owasp.isNotBlank() || vulnerability.cwe.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    if (vulnerability.cwe.isNotBlank()) {
                        Surface(
                            color = AccentViolet.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                vulnerability.cwe,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentViolet,
                                fontSize = 8.sp
                            )
                        }
                    }
                    if (vulnerability.owasp.isNotBlank()) {
                        // Split multiple OWASP categories
                        vulnerability.owasp.split("|").forEach { owaspId ->
                            Surface(
                                color = AccentCyan.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    owaspId.trim(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyan,
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ──

private fun gradeToColor(grade: String): Color = when (grade) {
    "A" -> AccentGreen
    "B" -> AccentCyan
    "C" -> AccentOrange
    "D" -> AccentOrange
    "F" -> AccentPink
    else -> AccentCyan
}

@Composable
private fun ComplianceSection(complianceResult: ComplianceResult) {
    if (complianceResult.issues.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = AccentGreen.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "✅ No compliance issues detected",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGreen
            )
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = AccentOrange.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📋 Compliance",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = AccentOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${complianceResult.issues.size} issues",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Framework status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val gdprIssues = complianceResult.issues.count { it.framework == ComplianceFramework.GDPR }
                val hipaaIssues = complianceResult.issues.count { it.framework == ComplianceFramework.HIPAA }
                val pciIssues = complianceResult.issues.count { it.framework == ComplianceFramework.PCI_DSS }

                if (gdprIssues > 0 || complianceResult.issues.any { it.framework == ComplianceFramework.GDPR }) {
                    ComplianceBadge("GDPR", gdprIssues == 0)
                }
                if (hipaaIssues > 0) {
                    ComplianceBadge("HIPAA", false)
                }
                if (pciIssues > 0) {
                    ComplianceBadge("PCI DSS", false)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Issues
            complianceResult.issues.take(5).forEachIndexed { index, issue ->
                ComplianceIssueRow(issue)
                if (index < minOf(complianceResult.issues.size - 1, 4)) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (complianceResult.issues.size > 5) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "... and ${complianceResult.issues.size - 5} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ComplianceBadge(name: String, compliant: Boolean) {
    Surface(
        color = if (compliant) AccentGreen.copy(alpha = 0.15f) else AccentPink.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = (if (compliant) "✅ " else "❌ ") + name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (compliant) AccentGreen else AccentPink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ComplianceIssueRow(issue: ComplianceIssue) {
    val frameworkColor = when (issue.framework) {
        ComplianceFramework.GDPR -> AccentCyan
        ComplianceFramework.HIPAA -> AccentPink
        ComplianceFramework.PCI_DSS -> AccentOrange
        ComplianceFramework.SOC2 -> AccentViolet
        ComplianceFramework.COPPA -> AccentGreen
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            color = frameworkColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp)
        ) {
            Text(
                issue.framework.name.replace("_", " "),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                color = frameworkColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                issue.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = frameworkColor,
                fontSize = 11.sp
            )
            Text(
                issue.article,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}