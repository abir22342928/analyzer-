package com.example.model

enum class OrderStatus(val title: String) {
    PENDING("PENDING"),
    FILLED("FILLED"),
    CLOSED_WIN("PROFIT"),
    CLOSED_LOSS("LOSS")
}

data class AutoTradeOrder(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val asset: String,
    val direction: SignalType, // UP or DOWN
    val entryPrice: Double,
    val amount: Double,
    val score: Int,
    val status: OrderStatus = OrderStatus.FILLED,
    val pnl: Double = 0.0,
    val closedPrice: Double? = null,
    val triggerReason: String = ""
)
