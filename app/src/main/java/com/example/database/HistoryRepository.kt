package com.example.database

import com.example.model.MarketAnalysisResult
import com.example.model.SignalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: AnalysisHistoryDao) {

    val allHistory: Flow<List<MarketAnalysisResult>> = dao.getAllHistory().map { list ->
        list.map { entity ->
            MarketAnalysisResult(
                id = entity.id,
                timestamp = entity.timestamp,
                asset = entity.asset,
                timeframe = entity.timeframe,
                signal = runCatching { SignalType.valueOf(entity.signal) }.getOrDefault(SignalType.WAIT),
                score = entity.score,
                trend = entity.trend,
                momentum = entity.momentum,
                structure = entity.structure,
                pattern = entity.pattern,
                supportResistance = entity.supportResistance,
                reasons = if (entity.reasonsString.isBlank()) emptyList() else entity.reasonsString.split("||"),
                riskFactors = if (entity.riskFactorsString.isBlank()) emptyList() else entity.riskFactorsString.split("||")
            )
        }
    }

    suspend fun saveResult(result: MarketAnalysisResult): Long {
        val entity = AnalysisHistoryEntity(
            timestamp = result.timestamp,
            asset = result.asset,
            timeframe = result.timeframe,
            signal = result.signal.name,
            score = result.score,
            trend = result.trend,
            momentum = result.momentum,
            structure = result.structure,
            pattern = result.pattern,
            supportResistance = result.supportResistance,
            reasonsString = result.reasons.joinToString("||"),
            riskFactorsString = result.riskFactors.joinToString("||")
        )
        return dao.insert(entity)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
