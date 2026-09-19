package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val asset: String,
    val timeframe: String,
    val signal: String,
    val score: Int,
    val trend: String,
    val momentum: String,
    val structure: String,
    val pattern: String,
    val supportResistance: String,
    val reasonsString: String,
    val riskFactorsString: String
)
