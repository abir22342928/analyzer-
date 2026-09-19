package com.example.model

data class MarketAnalysisResult(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val asset: String = "EUR/USD",
    val timeframe: String = "M1",
    val signal: SignalType,
    val score: Int,
    val trend: String,
    val momentum: String,
    val structure: String,
    val pattern: String,
    val supportResistance: String,
    val reasons: List<String>,
    val riskFactors: List<String> = emptyList(),
    val disclaimer: String = "Algorithmic analysis only. No signal is guaranteed. Trading involves substantial financial risk. The application does not automatically execute trades."
)
