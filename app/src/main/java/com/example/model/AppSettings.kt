package com.example.model

data class AppSettings(
    val floatingBubble: Boolean = true,
    val autoAnalysis: Boolean = false,
    val analysisIntervalSeconds: Int = 30,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val showScore: Boolean = true,
    val showReasons: Boolean = true,
    val darkTheme: Boolean = true
)
