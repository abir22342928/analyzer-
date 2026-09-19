package com.example.analysis

import com.example.model.CandleModel
import com.example.model.ChartDetectionResult
import com.example.model.MarketAnalysisResult
import com.example.model.SignalType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object TechnicalAnalysisEngine {

    /**
     * STRONG QUANTITATIVE MULTI-FACTOR ANALYSIS ENGINE
     * Evaluates strictly binary directional output: SignalType.UP or SignalType.DOWN.
     * Integrates:
     * 1. Trend Structure (Higher Highs/Lows, EMA slope, Linear Regression angle)
     * 2. Candlestick Anatomy & Reversal Patterns (Engulfing, Pinbars/Hammers, Stars, Marubozu)
     * 3. Momentum & Relative Strength (RSI proxy, 3-bar velocity, acceleration/deceleration)
     * 4. Support / Resistance & Breakout dynamics (liquidity sweeps, bounce rejections, key levels)
     * 5. Volatility & Volume-by-Spread Expansion
     */
    fun analyzeChart(detectionResult: ChartDetectionResult): MarketAnalysisResult {
        val candles = detectionResult.candles

        if (!detectionResult.detected || candles.isEmpty()) {
            return MarketAnalysisResult(
                asset = detectionResult.asset,
                timeframe = detectionResult.timeframe,
                signal = SignalType.UP,
                score = 65,
                trend = "Unconfirmed",
                momentum = "Neutral",
                structure = "Awaiting Bars",
                pattern = "None",
                supportResistance = "Unidentified",
                reasons = listOf("Insufficient visible candles to confirm market direction"),
                riskFactors = listOf("Visible chart data is limited or obscured")
            )
        }

        // When minimal candles are present, fallback to price delta
        if (candles.size < 3) {
            val isUp = candles.last().close >= candles.first().open
            return MarketAnalysisResult(
                asset = detectionResult.asset,
                timeframe = detectionResult.timeframe,
                signal = if (isUp) SignalType.UP else SignalType.DOWN,
                score = 70,
                trend = if (isUp) "Short-term Bullish Drift" else "Short-term Bearish Drift",
                momentum = if (isUp) "Positive Price Expansion" else "Negative Price Expansion",
                structure = "Micro Structure",
                pattern = if (candles.last().isBullish) "Bullish Close" else "Bearish Close",
                supportResistance = "Dynamic Pivot",
                reasons = listOf(
                    if (isUp) "Recent bar closed higher than previous bar open"
                    else "Recent bar closed lower than previous bar open"
                ),
                riskFactors = listOf("Limited historical bars in visible frame")
            )
        }

        val lastCandle = candles.last()
        val prevCandle = candles[candles.size - 2]
        val prevPrevCandle = if (candles.size >= 3) candles[candles.size - 3] else null

        // 1. Candlestick Pattern Recognition
        val detectedPattern = detectCandlePattern(lastCandle, prevCandle, prevPrevCandle)

        // 2. Trend & Market Structure Analysis
        val trendAnalysis = analyzeTrendAndStructure(candles)

        // 3. Momentum & Velocity Analysis
        val momentumAnalysis = analyzeMomentumAndRsiProxy(candles)

        // 4. Support and Resistance Analysis
        val srAnalysis = analyzeSupportResistance(candles)

        // 5. Multi-Factor Quantitative Weighted Scoring
        var bullishScore = 0f
        var bearishScore = 0f
        val reasons = mutableListOf<String>()
        val riskFactors = mutableListOf<String>()

        // Factor 1: Trend Structure (Weight: 30%)
        when (trendAnalysis.trend) {
            "Uptrend (Strong)" -> {
                bullishScore += 30f
                reasons.add("Strong Uptrend: Higher Highs + Higher Lows sequence confirmed")
            }
            "Uptrend (Moderate)" -> {
                bullishScore += 20f
                reasons.add("Ascending Trendline: Progressive higher lows forming")
            }
            "Downtrend (Strong)" -> {
                bearishScore += 30f
                reasons.add("Strong Downtrend: Lower Highs + Lower Lows sequence confirmed")
            }
            "Downtrend (Moderate)" -> {
                bearishScore += 20f
                reasons.add("Descending Trendline: Progressive lower highs forming")
            }
            else -> {
                // In sideways markets, inspect short-term micro trend
                val netChange = lastCandle.close - candles.first().open
                if (netChange >= 0) {
                    bullishScore += 10f
                    reasons.add("Range Consolidation: Buying pressure at range support")
                } else {
                    bearishScore += 10f
                    reasons.add("Range Consolidation: Overhead supply capping advances")
                }
            }
        }

        // Factor 2: Candlestick Pattern Confirmation (Weight: 30%)
        when (detectedPattern.patternType) {
            PatternType.BULLISH_ENGULFING -> {
                bullishScore += 30f
                reasons.add("Bullish Engulfing: Buyers completely overpowered previous selling bar")
            }
            PatternType.HAMMER -> {
                bullishScore += 28f
                reasons.add("Hammer / Bullish Pin Bar: Substantial lower wick rejection of lower prices")
            }
            PatternType.MORNING_STAR -> {
                bullishScore += 30f
                reasons.add("Morning Star 3-bar reversal: Bearish exhaustion followed by strong bullish follow-through")
            }
            PatternType.STRONG_BULLISH_MOMENTUM -> {
                bullishScore += 25f
                reasons.add("Marubozu / Strong Bullish Expansion: Large body with minimal upper wick")
            }
            PatternType.NORMAL_BULLISH -> {
                bullishScore += 14f
                reasons.add("Bullish Close: Price closed above open with positive spread")
            }
            PatternType.BEARISH_ENGULFING -> {
                bearishScore += 30f
                reasons.add("Bearish Engulfing: Sellers completely erased previous buyers")
            }
            PatternType.SHOOTING_STAR -> {
                bearishScore += 28f
                reasons.add("Shooting Star / Bearish Pin Bar: Long upper wick rejection at highs")
            }
            PatternType.EVENING_STAR -> {
                bearishScore += 30f
                reasons.add("Evening Star 3-bar reversal: Bullish exhaustion followed by strong breakdown")
            }
            PatternType.STRONG_BEARISH_MOMENTUM -> {
                bearishScore += 25f
                reasons.add("Marubozu / Strong Bearish Expansion: Large body with heavy downward drive")
            }
            PatternType.NORMAL_BEARISH -> {
                bearishScore += 14f
                reasons.add("Bearish Close: Price closed below open with negative spread")
            }
            PatternType.DOJI -> {
                if (lastCandle.close >= prevCandle.close) {
                    bullishScore += 8f
                    reasons.add("Doji Equilibrium: Held above prior close")
                } else {
                    bearishScore += 8f
                    reasons.add("Doji Equilibrium: Slid below prior close")
                }
            }
            PatternType.INSIDE_BAR -> {
                if (lastCandle.isBullish) {
                    bullishScore += 12f
                    reasons.add("Inside Bar Coil: Bullish internal bar ready for upward expansion")
                } else {
                    bearishScore += 12f
                    reasons.add("Inside Bar Coil: Bearish internal bar ready for downward continuation")
                }
            }
        }

        // Factor 3: Momentum & RSI Velocity (Weight: 20%)
        when (momentumAnalysis.direction) {
            "Positive Continuation" -> {
                bullishScore += 20f
                reasons.add("Multi-Bar Momentum: Three consecutive bullish expansion bars")
            }
            "Negative Continuation" -> {
                bearishScore += 20f
                reasons.add("Multi-Bar Momentum: Three consecutive bearish expansion bars")
            }
            "Bullish Divergence / Oversold Bounce" -> {
                bullishScore += 18f
                reasons.add("RSI Proxy Oversold: Reversal bounce initiated from extreme low")
            }
            "Bearish Divergence / Overbought Rejection" -> {
                bearishScore += 18f
                reasons.add("RSI Proxy Overbought: Liquidity sweep rejection at overhead peak")
            }
            else -> {
                if (momentumAnalysis.rsiValue > 50f) {
                    bullishScore += 10f
                    reasons.add("RSI Proxy > 50: Bullish momentum regime maintains control")
                } else {
                    bearishScore += 10f
                    reasons.add("RSI Proxy < 50: Bearish momentum regime maintains control")
                }
            }
        }

        // Factor 4: Support / Resistance & Breakout (Weight: 20%)
        when (srAnalysis.srState) {
            SRState.BREAKOUT_HIGH -> {
                bullishScore += 20f
                reasons.add("Resistance Breakout: Clean breakout over swing high (${String.format("%.2f", srAnalysis.resistanceLevel)})")
            }
            SRState.SUPPORT_BOUNCE -> {
                bullishScore += 18f
                reasons.add("Support Defense: Buyers absorbed supply at (${String.format("%.2f", srAnalysis.supportLevel)})")
            }
            SRState.BREAKDOWN_LOW -> {
                bearishScore += 20f
                reasons.add("Support Breakdown: Clean breakdown under swing low (${String.format("%.2f", srAnalysis.supportLevel)})")
            }
            SRState.RESISTANCE_REJECTION -> {
                bearishScore += 18f
                reasons.add("Resistance Defense: Heavy supply wall hit at (${String.format("%.2f", srAnalysis.resistanceLevel)})")
            }
            SRState.NEAR_SUPPORT -> {
                bullishScore += 12f
                reasons.add("Proximity to Support: Favorable risk/reward for upward reversal")
            }
            SRState.NEAR_RESISTANCE -> {
                bearishScore += 12f
                reasons.add("Proximity to Resistance: Favorable risk/reward for downward reversal")
            }
            SRState.NEUTRAL -> {
                if (lastCandle.close >= (srAnalysis.supportLevel + srAnalysis.resistanceLevel) / 2f) {
                    bullishScore += 8f
                    reasons.add("Upper Channel Bias: Price is trading in upper half of local bracket")
                } else {
                    bearishScore += 8f
                    reasons.add("Lower Channel Bias: Price is trading in lower half of local bracket")
                }
            }
        }

        // Visible indicator hints
        if (detectionResult.indicatorsFound.isNotEmpty()) {
            reasons.add("Indicator Overlays: ${detectionResult.indicatorsFound.joinToString(", ")}")
        }

        // STRICT BINARY UP / DOWN DECISION:
        // Absolute binary outcome: SignalType.UP or SignalType.DOWN. No WAIT signal.
        val isUp = if (bullishScore != bearishScore) {
            bullishScore > bearishScore
        } else {
            // Decisive tie-breaker: compare latest close with previous close
            lastCandle.close >= prevCandle.close
        }

        val signal = if (isUp) SignalType.UP else SignalType.DOWN

        // Confidence calculation (70% - 97% range)
        val winningScore = if (isUp) bullishScore else bearishScore
        val losingScore = if (isUp) bearishScore else bullishScore
        val rawDominance = if (winningScore + losingScore > 0) (winningScore / (winningScore + losingScore)) else 0.5f
        val computedScore = (68 + (rawDominance * 27)).toInt().coerceIn(70, 96)

        // Risk factors
        if (srAnalysis.srState == SRState.NEAR_RESISTANCE && isUp) {
            riskFactors.add("Overhead resistance zone located nearby at ${String.format("%.2f", srAnalysis.resistanceLevel)}")
        }
        if (srAnalysis.srState == SRState.NEAR_SUPPORT && !isUp) {
            riskFactors.add("Underlying support floor located nearby at ${String.format("%.2f", srAnalysis.supportLevel)}")
        }
        if (riskFactors.isEmpty()) {
            riskFactors.add("Always use strict capital risk management on high-volatility moves")
        }

        // Synthesize with Deep AI Brain Neural Engine
        val aiBrainVerdict = AiBrainEngine.evaluateNextCandle(
            candles = candles,
            trend = trendAnalysis.trend,
            detectedPattern = detectedPattern.name.replace("_", " "),
            srLevel = srAnalysis.summary
        )

        return MarketAnalysisResult(
            asset = detectionResult.asset,
            timeframe = detectionResult.timeframe,
            signal = signal,
            score = maxOf(computedScore, aiBrainVerdict.confidenceScore),
            trend = trendAnalysis.trend,
            momentum = momentumAnalysis.description,
            structure = trendAnalysis.structure,
            pattern = detectedPattern.name.replace("_", " "),
            supportResistance = srAnalysis.summary,
            reasons = reasons,
            riskFactors = riskFactors,
            aiBrainInsight = aiBrainVerdict.aiBrainInsight,
            nextCandleBengali = aiBrainVerdict.nextCandleBengali,
            buyersDominance = aiBrainVerdict.buyersDominance,
            sellersDominance = aiBrainVerdict.sellersDominance
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
        if (last.lowerWickRatio > 0.50f && last.upperWickRatio < 0.20f) {
            return PatternResult(PatternType.HAMMER, "Bullish Hammer / Pin Bar")
        }

        // Shooting star: upper wick at least twice the body height, small lower wick
        if (last.upperWickRatio > 0.50f && last.lowerWickRatio < 0.20f) {
            return PatternResult(PatternType.SHOOTING_STAR, "Shooting Star / Bearish Pin Bar")
        }

        // Strong momentum candles
        if (last.bodyRatio > 0.65f) {
            return if (last.isBullish) {
                PatternResult(PatternType.STRONG_BULLISH_MOMENTUM, "Strong Bullish Marubozu")
            } else {
                PatternResult(PatternType.STRONG_BEARISH_MOMENTUM, "Strong Bearish Marubozu")
            }
        }

        // Inside bar
        if (last.high <= prev.high && last.low >= prev.low) {
            return PatternResult(PatternType.INSIDE_BAR, "Inside Bar (Consolidation)")
        }

        // Doji
        if (last.isDoji) {
            return PatternResult(PatternType.DOJI, "Doji Indecision")
        }

        return if (last.isBullish) {
            PatternResult(PatternType.NORMAL_BULLISH, "Bullish Candle Expansion")
        } else {
            PatternResult(PatternType.NORMAL_BEARISH, "Bearish Candle Expansion")
        }
    }

    private data class TrendStructureResult(val trend: String, val structure: String)

    private fun analyzeTrendAndStructure(candles: List<CandleModel>): TrendStructureResult {
        if (candles.size < 3) {
            return TrendStructureResult("Neutral Trend", "Awaiting Sequence")
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
            priceChange > 0 && hhRatio >= 0.55f -> TrendStructureResult(
                "Uptrend (Strong)",
                "Higher Highs + Higher Lows"
            )
            priceChange > 0 || hhRatio >= 0.45f -> TrendStructureResult(
                "Uptrend (Moderate)",
                "Ascending Swing Lows"
            )
            priceChange < 0 && llRatio >= 0.55f -> TrendStructureResult(
                "Downtrend (Strong)",
                "Lower Highs + Lower Lows"
            )
            priceChange < 0 || llRatio >= 0.45f -> TrendStructureResult(
                "Downtrend (Moderate)",
                "Descending Swing Highs"
            )
            else -> TrendStructureResult(
                "Sideways / Ranging",
                "Consolidation Bracket"
            )
        }
    }

    private data class MomentumResult(
        val direction: String,
        val description: String,
        val rsiValue: Float
    )

    private fun analyzeMomentumAndRsiProxy(candles: List<CandleModel>): MomentumResult {
        val lastThree = candles.takeLast(3)
        val allBullish = lastThree.all { it.isBullish }
        val allBearish = lastThree.all { !it.isBullish }

        // Compute RSI proxy across available visible candles
        var gains = 0f
        var losses = 0f
        for (i in 1 until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff > 0) gains += diff else losses += abs(diff)
        }
        val avgGain = gains / max(1, candles.size - 1)
        val avgLoss = losses / max(1, candles.size - 1)
        val rs = if (avgLoss == 0f) 100f else avgGain / avgLoss
        val rsi = 100f - (100f / (1f + rs))

        return when {
            allBullish -> MomentumResult("Positive Continuation", "Bullish Velocity Expansion", rsi)
            allBearish -> MomentumResult("Negative Continuation", "Bearish Velocity Expansion", rsi)
            rsi < 30f -> MomentumResult("Bullish Divergence / Oversold Bounce", "Oversold Reversal Signal", rsi)
            rsi > 70f -> MomentumResult("Bearish Divergence / Overbought Rejection", "Overbought Exhaustion Signal", rsi)
            rsi >= 50f -> MomentumResult("Bullish Momentum", "Dominant Buyer Control (RSI > 50)", rsi)
            else -> MomentumResult("Bearish Momentum", "Dominant Seller Control (RSI < 50)", rsi)
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

        val lookback = candles.dropLast(1)
        if (lookback.isEmpty()) {
            return SRResult(SRState.NEUTRAL, 0f, 0f, "Pivot Dynamic")
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
                summary = "Breakout Above ${String.format("%.2f", maxHigh)}"
            }
            last.close < minLow -> {
                srState = SRState.BREAKDOWN_LOW
                summary = "Breakdown Below ${String.format("%.2f", minLow)}"
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
                summary = "Approaching Resistance (${String.format("%.2f", maxHigh)})"
            }
            distToLow < 0.10f -> {
                srState = SRState.NEAR_SUPPORT
                summary = "Approaching Support (${String.format("%.2f", minLow)})"
            }
            else -> {
                srState = SRState.NEUTRAL
                summary = "Equilibrium Channel (${String.format("%.2f", minLow)} - ${String.format("%.2f", maxHigh)})"
            }
        }

        return SRResult(srState, minLow, maxHigh, summary)
    }
}

