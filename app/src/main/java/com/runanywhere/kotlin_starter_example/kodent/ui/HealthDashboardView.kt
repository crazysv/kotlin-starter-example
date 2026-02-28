package com.runanywhere.kotlin_starter_example.kodent.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeHealthResult
import com.runanywhere.kotlin_starter_example.kodent.engine.CodeMetrics
import com.runanywhere.kotlin_starter_example.kodent.engine.HealthFixSnippets
import com.runanywhere.kotlin_starter_example.kodent.engine.HealthIssue
import com.runanywhere.kotlin_starter_example.kodent.engine.IssueSeverity
import com.runanywhere.kotlin_starter_example.ui.theme.AccentCyan
import com.runanywhere.kotlin_starter_example.ui.theme.AccentGreen
import com.runanywhere.kotlin_starter_example.ui.theme.AccentOrange
import com.runanywhere.kotlin_starter_example.ui.theme.AccentPink
import com.runanywhere.kotlin_starter_example.ui.theme.AccentViolet
import com.runanywhere.kotlin_starter_example.ui.theme.TextMuted

@Composable
fun HealthDashboardView(
    healthResult: CodeHealthResult,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(healthResult) { animationPlayed = true }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Score Header
        ScoreHeader(score = healthResult.overallScore, animate = animationPlayed)
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Dimension Scores Grid
        DimensionScoresGrid(healthResult, animationPlayed)
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Severity Summary
        if (healthResult.issues.isNotEmpty()) {
            SeveritySummaryRow(healthResult.issues)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Best Practices
        if (healthResult.bestPractices.isNotEmpty()) {
            BestPracticesSection(healthResult.bestPractices)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. Issues
        IssuesSection(healthResult.issues)
        Spacer(modifier = Modifier.height(12.dp))

        // 6. Code Metrics
        if (healthResult.metrics != null) {
            MetricsSection(healthResult.metrics!!)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 7. Summary
        if (healthResult.summary.isNotBlank()) {
            Text(
                healthResult.summary,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 8. Footer
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "⚡ Instant analysis • Rule-based • Zero hallucination",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════
// 1. SCORE HEADER
// ═══════════════════════════════════════

@Composable
private fun ScoreHeader(score: Int, animate: Boolean) {
    val animatedScore by animateFloatAsState(
        targetValue = if (animate) score.toFloat() else 0f,
        animationSpec = tween(durationMillis = 800), label = "score"
    )
    val color = overallScoreColor(score)
    val label = when {
        score >= 80 -> "Excellent"; score >= 60 -> "Good"
        score >= 40 -> "Needs Work"; else -> "Poor"
    }
    val emoji = when {
        score >= 80 -> "🏆"; score >= 60 -> "👍"
        score >= 40 -> "⚠️"; else -> "🔧"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$emoji ", fontSize = 24.sp)
                Text(
                    animatedScore.toInt().toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold, fontSize = 42.sp
                    ), color = color
                )
                Text("/100", style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(120.dp).height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.15f))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedScore / 100f)
                        .fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 2. DIMENSION SCORES
// ═══════════════════════════════════════

@Composable
private fun DimensionScoresGrid(result: CodeHealthResult, animate: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AccentViolet.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactScoreItem("🐛", "Bug Risk", result.bugRisk, animate, Modifier.weight(1f))
                CompactScoreItem("⚡", "Perf", result.performance, animate, Modifier.weight(1f))
                CompactScoreItem("🔒", "Security", result.security, animate, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactScoreItem("📖", "Read", result.readability, animate, Modifier.weight(1f))
                CompactScoreItem("🧩", "Complex", result.complexity, animate, Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactScoreItem(emoji: String, label: String, score: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (animate) score / 10f else 0f,
        animationSpec = tween(durationMillis = 600), label = "progress"
    )
    val color = dimensionScoreColor(score)

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("$emoji $label", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
            Text("$score/10", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = color, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.12f))) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()
                .clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

// ═══════════════════════════════════════
// 3. SEVERITY SUMMARY
// ═══════════════════════════════════════

@Composable
private fun SeveritySummaryRow(issues: List<HealthIssue>) {
    val criticalCount = issues.count { it.severity == IssueSeverity.CRITICAL }
    val highCount = issues.count { it.severity == IssueSeverity.HIGH }
    val mediumCount = issues.count { it.severity == IssueSeverity.MEDIUM }
    val lowCount = issues.count { it.severity == IssueSeverity.LOW }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (criticalCount > 0) SeverityChip("🔴", "Critical", criticalCount, AccentPink, Modifier.weight(1f))
        if (highCount > 0) SeverityChip("🟠", "High", highCount, AccentOrange, Modifier.weight(1f))
        if (mediumCount > 0) SeverityChip("🟡", "Medium", mediumCount, Color(0xFFFFD600), Modifier.weight(1f))
        if (lowCount > 0) SeverityChip("🔵", "Low", lowCount, AccentCyan, Modifier.weight(1f))
    }
}

@Composable
private fun SeverityChip(dot: String, label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Text(dot, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            Text("$count", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
        }
    }
}

// ═══════════════════════════════════════
// 4. BEST PRACTICES
// ═══════════════════════════════════════

@Composable
private fun BestPracticesSection(practices: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("✅ Best Practices", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = AccentGreen)
                Surface(color = AccentGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text("${practices.size} found", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, color = AccentGreen,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            practices.forEach { practice ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top) {
                    Text("✓", color = AccentGreen, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(practice, style = MaterialTheme.typography.bodySmall,
                        color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 5. ISSUES
// ═══════════════════════════════════════

@Composable
private fun IssuesSection(issues: List<HealthIssue>) {
    if (issues.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AccentPink.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Issues Found", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                    Surface(color = AccentPink.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text(issues.size.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = AccentPink,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                issues.forEachIndexed { index, issue ->
                    DetailedIssueRow(issue = issue)
                    if (index < issues.size - 1) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Issues Found!", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = AccentGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your code looks great!", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun DetailedIssueRow(issue: HealthIssue) {
    val severityColor = when (issue.severity) {
        IssueSeverity.CRITICAL -> AccentPink
        IssueSeverity.HIGH -> AccentOrange
        IssueSeverity.MEDIUM -> Color(0xFFFFD600)
        IssueSeverity.LOW -> AccentCyan
    }
    val severityDot = when (issue.severity) {
        IssueSeverity.CRITICAL -> "🔴"; IssueSeverity.HIGH -> "🟠"
        IssueSeverity.MEDIUM -> "🟡"; IssueSeverity.LOW -> "🔵"
    }

    val fixSnippet = HealthFixSnippets.getFixFor(issue.title)
    var showFix by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(severityDot, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(issue.title, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = severityColor,
                            modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)) {
                            Text(issue.severity.name,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = severityColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(issue.description, style = MaterialTheme.typography.bodySmall,
                        color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            // Fix snippet toggle
            if (fixSnippet != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { showFix = !showFix },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (showFix) "💡 Hide Fix ▲" else "💡 Show Fix ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen, fontSize = 10.sp
                    )
                }

                if (showFix) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(fixSnippet, modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, lineHeight = 14.sp
                            ), color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 6. CODE METRICS
// ═══════════════════════════════════════

@Composable
private fun MetricsSection(metrics: CodeMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("📊 Code Metrics", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = AccentCyan)
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Lines
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem("Lines", metrics.totalLines.toString(), Modifier.weight(1f))
                MetricItem("Code", metrics.codeLines.toString(), Modifier.weight(1f))
                MetricItem("Comments", "${metrics.commentPercentage}%", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Structure
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem("Functions", metrics.functionCount.toString(), Modifier.weight(1f))
                MetricItem("Classes", metrics.classCount.toString(), Modifier.weight(1f))
                MetricItem("Imports", metrics.importCount.toString(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Quality indicators
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem("Avg Func", "${metrics.avgFunctionLength}L", Modifier.weight(1f))
                MetricItem("Max Nest", metrics.maxNestingDepth.toString(), Modifier.weight(1f))
                MetricItem("val/var", "${metrics.valCount}/${metrics.varCount}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Complexity
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricItem("Cyclomatic", metrics.cyclomaticComplexity.toString(), Modifier.weight(1f))
                MetricItem("Longest", "${metrics.longestLine}ch", Modifier.weight(1f))
                MetricItem("TODOs", metrics.todoCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = TextMuted, fontSize = 9.sp)
        }
    }
}

// ═══════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════

private fun overallScoreColor(score: Int): Color = when {
    score >= 80 -> AccentGreen; score >= 60 -> AccentCyan
    score >= 40 -> AccentOrange; else -> AccentPink
}

private fun dimensionScoreColor(score: Int): Color = when {
    score >= 8 -> AccentGreen; score >= 6 -> AccentCyan
    score >= 4 -> AccentOrange; else -> AccentPink
}