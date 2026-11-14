package com.example.data.local

import kotlinx.coroutines.flow.Flow

class AgentRunRepository(private val dao: AgentRunDao) {
    val allRuns: Flow<List<AgentRunEntity>> = dao.observeAllRuns()

    suspend fun saveRun(run: AgentRunEntity): Long = dao.insertRun(run)

    suspend fun deleteRun(runId: Long) = dao.deleteRunById(runId)

    suspend fun clearAll() = dao.clearAllRuns()
}
