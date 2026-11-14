package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AgentRunEntity::class], version = 1, exportSchema = false)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentRunDao(): AgentRunDao

    companion object {
        @Volatile
        private var instance: AgentDatabase? = null

        fun getInstance(context: Context): AgentDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgentDatabase::class.java,
                    "murshed_agent.db"
                ).build().also { instance = it }
            }
        }
    }
}
