package com.runanywhere.kotlin_starter_example.kodent.ui

data class AnalysisRecord(
    val code: String,
    val mode: String,
    val language: String = "Kotlin",
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)