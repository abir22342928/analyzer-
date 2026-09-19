package com.example.analysis

import com.example.model.CandleModel
import com.example.model.ChartDetectionResult
import com.example.model.MarketAnalysisResult
import com.example.model.SignalType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TechnicalAnalysisEngine {

    fun analyzeChart(detectionResult: ChartDetectionResult): MarketAnalysisResult {
        val candles = detectionResult.candles

        if (!detectionResult.detected || candles.size < 3) {
            return MarketAnalysisResult(
                asset = detectionResult.asset,
                timeframe = detectionResult.timeframe,
                signal = SignalType.WAIT,
                score = 50,
                trend = "Unclear",
                momentum = "Insufficient Data",
                structure = "Uncertain",
                pattern = "None",
                supportResistance = "Unidentified",
                reasons = listOf(
                    "Insufficient visible candles to confirm market direction",
                    "A minimum of 3-5 clear candlestick bars are required"
                ),
                riskFactors = listOf("Visible chart data is limited or obscured")
            )
        }

        val lastCandle = candles.last()
        val prevCandle = candles[candles.size - 2]
        val prevPrevCandle = if (candles.size >= 3) candles[candles.size - 3] else null

        // 1. Candlestick Pattern Recognition
        val detectedPattern = detectCandlePattern(lastCandle, prevCandle, prevPrevCandle)

        // 2. Trend & Market Structure Analysis
        val trendAnalysis = analyzeTrendAndStructure(candles)

        // 3. Momentum Analysis
        val momentumAnalysis = analyzeMomentum(candles)

        // 4. Support and Resistance Analysis
        val srAnalysis = analyzeSupportResistance(candles)

        // 5. Multi-Factor Decision Evaluation
        var bullishPoints = 0
        var bearishPoints = 0
        val reasons = mutableListOf<String>()
        val riskFactors = mutableListOf<String>()

        // Evaluate Trend
        when (trendAnalysis.trend) {
            "Uptrend (Strong)" -> {
                bullishPoints += 3
                reasons.add("Strong Uptrend with Higher Highs & Higher Lows")
            }
            "Uptrend (Moderate)" -> {
                bullishPoints += 2
                reasons.add("Moderate Uptrend structure confirmed")
            }
            "Downtrend (Strong)" -> {
                bearishPoints += 3
                reasons.add("Strong Downtrend with Lower Highs & Lower Lows")
            }
            "Downtrend (Moderate)" -> {
                bearishPoints += 2
                reasons.add("Moderate Downtrend structure confirmed")
            }
            else -> {
                riskFactors.add("Market structure is sideways / ranging")
            }
        }

        // Evaluate Pattern
        when (detectedPattern.patternType) {
            PatternType.BULLISH_ENGULFING -> {
                bullishPoints += 3
                reasons.add("Bullish Engulfing pattern formed at latest bar")
            }
            PatternType.HAMMER -> {
                bullishPoints += 3
                reasons.add("Hammer candlestick with long lower wick rejection")
            }
            PatternType.MORNING_STAR -> {
                bullishPoints += 4
                reasons.add("Morning Star 3-bar bullish reversal pattern detected")
            }
            PatternType.STRONG_BULLISH_MOMENTUM -> {
                bullishPoints += 2
                reasons.add("Strong bullish momentum candle expansion")
            }
            PatternType.BEARISH_ENGULFING -> {
                bearishPoints += 3
                reasons.add("Bearish Engulfing pattern formed at latest bar")
            }
            PatternType.SHOOTING_STAR -> {
                bearishPoints += 3
                reasons.add("Shooting Star candlestick with strong overhead rejection")
            }
            PatternType.EVENING_STAR -> {
                bearishPoints += 4
                reasons.add("Evening Star 3-bar bearish reversal pattern detected")
            }
            PatternType.STRONG_BEARISH_MOMENTUM -> {
                bearishPoints += 2
                reasons.add("Strong bearish momentum candle expansion")
            }
            PatternType.DOJI -> {
                riskFactors.add("Doji candlestick represents market indecision")
            }
            PatternType.INSIDE_BAR -> {
                riskFactors.add("Inside bar indicates volatility compression")
            }
            else -> {}
        }

        // Evaluate Momentum
        when (momentumAnalysis.direction) {
            "Positive Continuation" -> {
                bullishPoints += 2
                reasons.add("Consecutive bullish momentum candles")
            }
            "Negative Continuation" -> {
                bearishPoints += 2
                reasons.add("Consecutive bearish momentum candles")
            }
            "Deceleration" -> {
                riskFactors.add("Candle momentum is decelerating near recent level")
            }
        }

        // Evaluate S/R
        when (srAnalysis.srState) {
            SRState.SUPPORT_BOUNCE -> {
                bullishPoints += 2
                reasons.add("Price bounced off key support level (${String.format("%.2f", srAnalysis.supportLevel)})")
            }
            SRState.RESISTANCE_REJECTION -> {
                bearishPoints += 2
                reasons.add("Price rejected key resistance level (${String.format("%.2f", srAnalysis.resistanceLevel)})")
            }
            SRState.NEAR_RESISTANCE -> {
                riskFactors.add("Warning: Price is immediately into resistance (${String.format("%.2f", srAnalysis.resistanceLevel)})")
            }
            SRState.NEAR_SUPPORT -> {
                riskFactors.add("Warning: Price is immediately into support (${String.format("%.2f", srAnalysis.supportLevel)})")
            }
            SRState.BREAKOUT_HIGH -> {
                bullishPoints += 3
                reasons.add("Breakout above previous swing high")
            }
            SRState.BREAKDOWN_LOW -> {
                bearishPoints += 3
                reasons.add("Breakdown below previous swing low")
            }
            SRState.NEUTRAL -> {}
        }

        // Indicator confirmation if found
        if (detectionResult.indicatorsFound.isNotEmpty()) {
            reasons.add("Visible indicators: ${detectionResult.indicatorsFound.joinToString(", ")}")
        }

        // Final Multi-Factor Decision & Scoring
        val scoreDiff = bullishPoints - bearishPoints
        val totalPoints = bullishPoints + bearishPoints

        val signal: SignalType
        val score: Int

        // Multi-factor confirmation rule:
        // High agreement required. If conditions are mixed or insufficient -> WAIT
        if (bullishPoints >= 4 && bullishPoints >= bearishPoints * 2 && srAnalysis.srState != SRState.NEAR_RESISTANCE) {
            signal = SignalType.POSSIBLE_UP
            val baseScore = 72
            val bonus = min(bullishPoints * 3, 20)
            val penalty = min(bearishPoints * 4, 15)
            score = (baseScore + bonus - penalty).coerceIn(68, 94)
        } else if (bearishPoints >= 4 && bearishPoints >= bullishPoints * 2 && srAnalysis.srState != SRState.NEAR_SUPPORT) {
            signal = SignalType.POSSIBLE_DOWN
            val baseScore = 72
            val bonus = min(bearishPoints * 3, 20)
            val penalty = min(bullishPoints * 4, 15)
            score = (baseScore + bonus - penalty).coerceIn(68, 94)
        } else {
            signal = SignalType.WAIT
            score = (45 + (totalPoints * 2)).coerceIn(40, 59)
            if (reasons.isEmpty()) {
                reasons.add("Market conditions do not provide sufficient directional confirmation")
                reasons.add("Conflicting trend and candle momentum signals")
            } else {
                reasons.add(0, "Multi-factor agreement threshold was not reached")
            }
        }

        if (riskFactors.isEmpty()) {
            riskFactors.add("Standard market volatility risk applies")
        }

        return MarketAnalysisResult(
            asset = detectionResult.asset,
            timeframe = detectionResult.timeframe,
            signal = signal,
            score = score,
            trend = trendAnalysis.trend,
            momentum = momentumAnalysis.description,
            structure = trendAnalysis.structure,
            pattern = detectedPattern.name,
            supportResistance = srAnalysis.summary,
            reasons = reasons,
            riskFactors = riskFactors
        )
    }

    private enum class PatternType {
        BULLISH_ENGULFING,
        BEARISH_ENGULFING,
        HAMMER,
        SHOOTING_STAR,
        MORNING_STAR,
        EVENING_STAR,
        STRONG_BULLISH_MOMENTUM,
        STRONG_BEARISH_MOMENTUM,
        DOJI,
        INSIDE_BAR,
        NORMAL_BULLISH,
        NORMAL_BEARISH
    }

    private data class PatternResult(val patternType: PatternType, val name: String)

    private fun detectCandlePattern(last: CandleModel, prev: CandleModel, prevPrev: CandleModel?): PatternResult {
        // Morning star: 3 candles
        if (prevPrev != null && !prevPrev.isBullish && prevPrev.bodyRatio > 0.4f) {
            if (prev.isDoji || prev.bodyRatio < 0.25f) {
                if (last.isBullish && last.close > (prevPrev.open + prevPrev.close) / 2f) {
                    return PatternResult(PatternType.MORNING_STAR, "Morning Star Reversal")
                }
            }
        }

        // Evening star: 3 candles
        if (prevPrev != null && prevPrev.isBullish && prevPrev.bodyRatio > 0.4f) {
            if (prev.isDoji || prev.bodyRatio < 0.25f) {
                if (!last.isBullish && last.close < (prevPrev.open + prevPrev.close) / 2f) {
                    return PatternResult(PatternType.EVENING_STAR, "Evening Star Reversal")
                }
            }
        }

        // Bullish Engulfing
        if (!prev.isBullish && last.isBullish && last.close >= prev.open && last.open <= prev.close) {
            return PatternResult(PatternType.BULLISH_ENGULFING, "Bullish Engulfing")
        }

        // Bearish Engulfing
        if (prev.isBullish && !last.isBullish && last.open >= prev.close && last.close <= prev.open) {
            return PatternResult(PatternType.BEARISH_ENGULFING, "Bearish Engulfing")
        }

        // Hammer: lower wick at least twice the body height, small upper wick, closes high
        if (last.lowerWickRatio > 0.55f && last.upperWickRatio < 0.15f) {
            return if (last.isBullish) {
                PatternResult(PatternType.HAMMER, "Bullish Hammer / Pin Bar")
            } else {
                PatternResult(PatternType.HAMMER, "Inverted Hammer Rejection")
            }
        }

        // Shooting star: upper wick at least twice the body height, small lower wick
        if (last.upperWickRatio > 0.55f && last.lowerWickRatio < 0.15f) {
            return PatternResult(PatternType.SHOOTING_STAR, "Shooting Star / Bearish Pin Bar")
        }

        // Doji
        if (last.isDoji) {
            return PatternResult(PatternType.DOJI, "Doji Indecision")
        }

        // Inside bar
        if (last.high <= prev.high && last.low >= prev.low) {
            return PatternResult(PatternType.INSIDE_BAR, "Inside Bar (Consolidation)")
        }

        // Strong momentum candles
        if (last.bodyRatio > 0.70f) {
            return if (last.isBullish) {
                PatternResult(PatternType.STRONG_BULLISH_MOMENTUM, "Strong Bullish Momentum")
            } else {
                PatternResult(PatternType.STRONG_BEARISH_MOMENTUM, "Strong Bearish Momentum")
            }
        }

        return if (last.isBullish) {
            PatternResult(PatternType.NORMAL_BULLISH, "Standard Bullish Candle")
        } else {
            PatternResult(PatternType.NORMAL_BEARISH, "Standard Bearish Candle")
        }
    }

    private data class TrendStructureResult(val trend: String, val structure: String)

    private fun analyzeTrendAndStructure(candles: List<CandleModel>): TrendStructureResult {
        if (candles.size < 3) {
            return TrendStructureResult("Sideways", "Unclear Structure")
        }

        val firstPrice = candles.first().open
        val lastPrice = candles.last().close
        val priceChange = lastPrice - firstPrice

        var higherHighs = 0
        var lowerLows = 0

        for (i in 1 until candles.size) {
            if (candles[i].high > candles[i - 1].high) higherHighs++
            if (candles[i].low < candles[i - 1].low) lowerLows++
        }

        val totalComparisons = candles.size - 1
        val hhRatio = higherHighs.toFloat() / totalComparisons
        val llRatio = lowerLows.toFloat() / totalComparisons

        return when {
            priceChange > 0 && hhRatio >= 0.6f -> TrendStructureResult(
                "Uptrend (Strong)",
                "Higher Highs + Higher Lows"
            )
            priceChange > 0 && hhRatio >= 0.45f -> TrendStructureResult(
                "Uptrend (Moderate)",
                "Higher Highs Forming"
            )
            priceChange < 0 && llRatio >= 0.6f -> TrendStructureResult(
                "Downtrend (Strong)",
                "Lower Highs + Lower Lows"
            )
            priceChange < 0 && llRatio >= 0.45f -> TrendStructureResult(
                "Downtrend (Moderate)",
                "Lower Lows Forming"
            )
            else -> TrendStructureResult(
                "Sideways / Ranging",
                "Consolidation / Range Bound"
            )
        }
    }

    private data class MomentumResult(val direction: String, val description: String)

    private fun analyzeMomentum(candles: List<CandleModel>): MomentumResult {
        val lastThree = candles.takeLast(3)
        val allBullish = lastThree.all { it.isBullish }
        val allBearish = lastThree.all { !it.isBullish }

        return when {
            allBullish -> MomentumResult("Positive Continuation", "Positive Bullish Momentum")
            allBearish -> MomentumResult("Negative Continuation", "Negative Bearish Momentum")
            lastThree.last().bodyRatio < lastThree.first().bodyRatio * 0.5f -> {
                MomentumResult("Deceleration", "Momentum Deceleration Detected")
            }
            else -> MomentumResult("Neutral", "Neutral / Balanced Momentum")
        }
    }

    private enum class SRState {
        SUPPORT_BOUNCE,
        RESISTANCE_REJECTION,
        NEAR_RESISTANCE,
        NEAR_SUPPORT,
        BREAKOUT_HIGH,
        BREAKDOWN_LOW,
        NEUTRAL
    }

    private data class SRResult(
        val srState: SRState,
        val supportLevel: Float,
        val resistanceLevel: Float,
        val summary: String
    )

    private fun analyzeSupportResistance(candles: List<CandleModel>): SRResult {
        var minLow = Float.MAX_VALUE
        var maxHigh = Float.MIN_VALUE

        // Look at previous candles excluding the latest to determine levels
        val lookback = candles.dropLast(1)
        if (lookback.isEmpty()) {
            return SRResult(SRState.NEUTRAL, 0f, 0f, "Levels Undetermined")
        }

        for (c in lookback) {
            if (c.low < minLow) minLow = c.low
            if (c.high > maxHigh) maxHigh = c.high
        }

        val last = candles.last()
        val range = (maxHigh - minLow).coerceAtLeast(0.01f)
        val distToHigh = abs(last.close - maxHigh) / range
        val distToLow = abs(last.close - minLow) / range

        val srState: SRState
        val summary: String

        when {
            last.close > maxHigh -> {
                srState = SRState.BREAKOUT_HIGH
                summary = "Breakout Above High (${String.format("%.2f", maxHigh)})"
            }
            last.close < minLow -> {
                srState = SRState.BREAKDOWN_LOW
                summary = "Breakdown Below Low (${String.format("%.2f", minLow)})"
            }
            distToLow < 0.15f && last.lowerWickRatio > 0.4f -> {
                srState = SRState.SUPPORT_BOUNCE
                summary = "Support Bounce at ${String.format("%.2f", minLow)}"
            }
            distToHigh < 0.15f && last.upperWickRatio > 0.4f -> {
                srState = SRState.RESISTANCE_REJECTION
                summary = "Resistance Rejection at ${String.format("%.2f", maxHigh)}"
            }
            distToHigh < 0.10f -> {
                srState = SRState.NEAR_RESISTANCE
                summary = "Testing Overhead Resistance (${String.format("%.2f", maxHigh)})"
            }
            distToLow < 0.10f -> {
                srState = SRState.NEAR_SUPPORT
                summary = "Testing Underlying Support (${String.format("%.2f", minLow)})"
            }
            else -> {
                srState = SRState.NEUTRAL
                summary = "Mid-Range Between ${String.format("%.2f", minLow)} - ${String.format("%.2f", maxHigh)}"
            }
        }

        return SRResult(srState, minLow, maxHigh, summary)
    }
}
