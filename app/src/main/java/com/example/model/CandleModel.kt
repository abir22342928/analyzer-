package com.example.model

data class CandleModel(
    val index: Int,
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
    val isBullish: Boolean = close >= open,
    val pattern: String? = null
) {
    val range: Float get() = (high - low).coerceAtLeast(0.0001f)
    val bodyHeight: Float get() = kotlin.math.abs(close - open)
    val bodyRatio: Float get() = (bodyHeight / range).coerceIn(0f, 1f)
    val upperWick: Float get() = (high - kotlin.math.max(open, close)).coerceAtLeast(0f)
    val lowerWick: Float get() = (kotlin.math.min(open, close) - low).coerceAtLeast(0f)
    val upperWickRatio: Float get() = (upperWick / range).coerceIn(0f, 1f)
    val lowerWickRatio: Float get() = (lowerWick / range).coerceIn(0f, 1f)
    val isDoji: Boolean get() = bodyRatio < 0.12f
}
