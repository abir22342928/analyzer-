package com.example.model

data class AutoTradeConfig(
    val enabled: Boolean = false,
    val tradeAmount: Double = 10.0,
    val minConfidenceScore: Int = 75,
    val executionType: String = "SIMULATED_AUTO_EXECUTION", // Live Simulation execution
    val stopLossPips: Int = 15,
    val takeProfitPips: Int = 30,
    val totalSimulatedBalance: Double = 10000.0
)
