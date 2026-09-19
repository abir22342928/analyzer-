package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_trades")
data class AutoTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val asset: String,
    val direction: String, // "UP" or "DOWN"
    val entryPrice: Double,
    val amount: Double,
    val score: Int,
    val status: String,
    val pnl: Double,
    val closedPrice: Double?,
    val triggerReason: String
)
