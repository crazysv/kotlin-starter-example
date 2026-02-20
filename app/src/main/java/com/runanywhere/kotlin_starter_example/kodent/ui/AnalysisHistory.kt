package com.runanywhere.kotlin_starter_example.kodent.ui

data class AnalysisRecord(
    val code: String,
    val mode: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)