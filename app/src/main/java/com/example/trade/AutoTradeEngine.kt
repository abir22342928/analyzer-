package com.example.trade

import android.content.Context
import android.util.Log
import com.example.database.AppDatabase
import com.example.database.AutoTradeRepository
import com.example.model.AutoTradeConfig
import com.example.model.AutoTradeOrder
import com.example.model.MarketAnalysisResult
import com.example.model.OrderStatus
import com.example.model.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

object AutoTradeEngine {

    private val _config = MutableStateFlow(AutoTradeConfig())
    val config = _config.asStateFlow()

    private val _lastExecutedTrade = MutableStateFlow<AutoTradeOrder?>(null)
    val lastExecutedTrade = _lastExecutedTrade.asStateFlow()

    private var autoTradeRepository: AutoTradeRepository? = null
    private val tradeScope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context, repository: AutoTradeRepository) {
        autoTradeRepository = repository
    }

    fun updateConfig(newConfig: AutoTradeConfig) {
        _config.value = newConfig
    }

    /**
     * Called automatically when an analysis result is finalized.
     * If Auto Trade is enabled and confidence score meets the threshold,
     * triggers instant trade entry execution.
     */
    fun evaluateAndExecute(result: MarketAnalysisResult): AutoTradeOrder? {
        val currentConfig = _config.value
        if (!currentConfig.enabled) {
            return null
        }

        // Check confidence score requirement
        if (result.score < currentConfig.minConfidenceScore) {
            Log.d("AutoTradeEngine", "Score ${result.score} below threshold ${currentConfig.minConfidenceScore}")
            return null
        }

        // Generate realistic simulated price based on asset
        val basePrice = when {
            result.asset.contains("EUR", ignoreCase = true) -> 1.0850 + (Random.nextDouble(-0.0020, 0.0020))
            result.asset.contains("BTC", ignoreCase = true) -> 64200.0 + (Random.nextDouble(-200.0, 200.0))
            result.asset.contains("GBP", ignoreCase = true) -> 1.2940 + (Random.nextDouble(-0.0020, 0.0020))
            result.asset.contains("USD/JPY", ignoreCase = true) -> 154.30 + (Random.nextDouble(-0.5, 0.5))
            result.asset.contains("GOLD", ignoreCase = true) -> 2380.0 + (Random.nextDouble(-10.0, 10.0))
            else -> 100.0 + (Random.nextDouble(-1.0, 1.0))
        }

        // Simulate immediate profit/loss outcome based on strong score probability
        val isWin = Random.nextFloat() <= (result.score / 100f)
        val profitMultiplier = 0.85 // standard binary / option / CFD 85% return or pip return
        val calculatedPnl = if (isWin) {
            currentConfig.tradeAmount * profitMultiplier
        } else {
            -currentConfig.tradeAmount
        }

        val status = if (isWin) OrderStatus.CLOSED_WIN else OrderStatus.CLOSED_LOSS

        val order = AutoTradeOrder(
            timestamp = System.currentTimeMillis(),
            asset = result.asset,
            direction = result.signal,
            entryPrice = String.format("%.4f", basePrice).toDoubleOrNull() ?: basePrice,
            amount = currentConfig.tradeAmount,
            score = result.score,
            status = status,
            pnl = String.format("%.2f", calculatedPnl).toDoubleOrNull() ?: calculatedPnl,
            closedPrice = String.format(
                "%.4f",
                if (result.signal == SignalType.UP) basePrice + 0.0015 else basePrice - 0.0015
            ).toDoubleOrNull(),
            triggerReason = "${result.signal.title} (${result.score}%) • ${result.pattern} • ${result.trend}"
        )

        _lastExecutedTrade.value = order

        // Save order to persistent Room database
        tradeScope.launch {
            try {
                autoTradeRepository?.saveTrade(order)
            } catch (e: Exception) {
                Log.e("AutoTradeEngine", "Error saving auto trade: ${e.message}")
            }
        }

        return order
    }
}
