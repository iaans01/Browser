package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRunDao {
    @Query("SELECT * FROM agent_runs ORDER BY timestampMs DESC")
    fun observeAllRuns(): Flow<List<AgentRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: AgentRunEntity): Long

    @Query("DELETE FROM agent_runs WHERE id = :runId")
    suspend fun deleteRunById(runId: Long)

    @Query("DELETE FROM agent_runs")
    suspend fun clearAllRuns()
}
