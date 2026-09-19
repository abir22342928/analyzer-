package com.example.model

data class ChartDetectionResult(
    val detected: Boolean,
    val candles: List<CandleModel> = emptyList(),
    val asset: String = "CHART",
    val timeframe: String = "M1",
    val currentPrice: String = "",
    val indicatorsFound: List<String> = emptyList(),
    val errorMessage: String? = null
)
