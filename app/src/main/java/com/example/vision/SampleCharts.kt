package com.example.vision

import com.example.model.CandleModel

object SampleCharts {

    enum class Scenario(val title: String, val description: String) {
        BULLISH_BREAKOUT(
            "Bullish Breakout & Momentum",
            "Higher Highs, strong green candles bouncing off support with long lower wicks."
        ),
        BEARISH_REJECTION(
            "Bearish Resistance Rejection",
            "Lower Highs, shooting star / inverted hammer at resistance followed by strong red candles."
        ),
        MORNING_STAR_REVERSAL(
            "Morning Star Bullish Reversal",
            "Downtrend exhausted at support, Doji rejection followed by strong bullish engulfing candle."
        ),
        RANGING_CONSOLIDATION(
            "Consolidation & Unclear Range",
            "Equal highs and lows, alternating small Dojis, conflicting momentum."
        )
    }

    fun getCandlesForScenario(scenario: Scenario): List<CandleModel> {
        return when (scenario) {
            Scenario.BULLISH_BREAKOUT -> listOf(
                CandleModel(0, 100.0f, 101.5f, 102.0f, 99.5f, true),
                CandleModel(1, 101.5f, 101.0f, 102.2f, 100.5f, false),
                CandleModel(2, 101.0f, 103.0f, 103.5f, 100.8f, true),
                CandleModel(3, 103.0f, 102.5f, 103.8f, 102.0f, false),
                CandleModel(4, 102.5f, 105.0f, 105.5f, 102.2f, true),
                CandleModel(5, 105.0f, 104.8f, 105.4f, 103.5f, false), // Pullback retest
                CandleModel(6, 104.8f, 105.2f, 105.5f, 103.8f, true), // Hammer at support
                CandleModel(7, 105.2f, 108.0f, 108.5f, 105.0f, true)  // Strong breakout
            )
            Scenario.BEARISH_REJECTION -> listOf(
                CandleModel(0, 110.0f, 108.5f, 110.5f, 107.8f, false),
                CandleModel(1, 108.5f, 109.0f, 109.5f, 108.0f, true),
                CandleModel(2, 109.0f, 107.0f, 109.2f, 106.5f, false),
                CandleModel(3, 107.0f, 107.8f, 108.2f, 106.8f, true),
                CandleModel(4, 107.8f, 108.0f, 109.5f, 107.5f, true), // Shooting star
                CandleModel(5, 107.9f, 105.5f, 108.0f, 105.0f, false), // Bearish engulfing
                CandleModel(6, 105.5f, 103.0f, 105.8f, 102.5f, false)  // Momentum down
            )
            Scenario.MORNING_STAR_REVERSAL -> listOf(
                CandleModel(0, 120.0f, 116.0f, 120.5f, 115.5f, false),
                CandleModel(1, 116.0f, 112.5f, 116.5f, 112.0f, false),
                CandleModel(2, 112.5f, 112.7f, 113.2f, 111.0f, true), // Doji/Hammer at major support
                CandleModel(3, 112.7f, 117.5f, 118.0f, 112.5f, true), // Strong bullish confirmation
                CandleModel(4, 117.5f, 121.0f, 121.5f, 117.0f, true)  // Follow through
            )
            Scenario.RANGING_CONSOLIDATION -> listOf(
                CandleModel(0, 100.0f, 100.8f, 101.5f, 99.5f, true),
                CandleModel(1, 100.8f, 99.8f, 101.2f, 99.4f, false),
                CandleModel(2, 99.8f, 100.5f, 101.4f, 99.6f, true),
                CandleModel(3, 100.5f, 100.2f, 101.1f, 99.7f, false), // Doji
                CandleModel(4, 100.2f, 100.4f, 101.3f, 99.5f, true)   // Doji inside range
            )
        }
    }
}
