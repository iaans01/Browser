package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goal: String,
    val startUrl: String,
    val finalUrl: String,
    val status: String,
    val stepCount: Int,
    val recoveryCount: Int,
    val finalSummary: String,
    val stepsJson: String,
    val timestampMs: Long = System.currentTimeMillis()
)
