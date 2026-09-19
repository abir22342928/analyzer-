package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoTradeDao {
    @Query("SELECT * FROM auto_trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<AutoTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(entity: AutoTradeEntity): Long

    @Update
    suspend fun updateTrade(entity: AutoTradeEntity)

    @Query("DELETE FROM auto_trades WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("DELETE FROM auto_trades")
    suspend fun clearAllTrades()
}
