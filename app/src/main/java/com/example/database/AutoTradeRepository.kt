package com.example.database

import com.example.model.AutoTradeOrder
import com.example.model.OrderStatus
import com.example.model.SignalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutoTradeRepository(private val dao: AutoTradeDao) {

    val allTrades: Flow<List<AutoTradeOrder>> = dao.getAllTrades().map { list ->
        list.map { entity ->
            AutoTradeOrder(
                id = entity.id,
                timestamp = entity.timestamp,
                asset = entity.asset,
                direction = if (entity.direction == "DOWN") SignalType.DOWN else SignalType.UP,
                entryPrice = entity.entryPrice,
                amount = entity.amount,
                score = entity.score,
                status = runCatching { OrderStatus.valueOf(entity.status) }.getOrDefault(OrderStatus.FILLED),
                pnl = entity.pnl,
                closedPrice = entity.closedPrice,
                triggerReason = entity.triggerReason
            )
        }
    }

    suspend fun saveTrade(trade: AutoTradeOrder): Long {
        val entity = AutoTradeEntity(
            timestamp = trade.timestamp,
            asset = trade.asset,
            direction = trade.direction.name,
            entryPrice = trade.entryPrice,
            amount = trade.amount,
            score = trade.score,
            status = trade.status.name,
            pnl = trade.pnl,
            closedPrice = trade.closedPrice,
            triggerReason = trade.triggerReason
        )
        return dao.insertTrade(entity)
    }

    suspend fun deleteTradeById(id: Long) {
        dao.deleteTradeById(id)
    }

    suspend fun clearAllTrades() {
        dao.clearAllTrades()
    }
}
