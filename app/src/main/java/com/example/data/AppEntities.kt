package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val userId: String = "default_user",
    val email: String? = null,
    val username: String = "Guest Gamer",
    val coinBalance: Int = 500, // Starts with some welcome coins
    val dailyStreak: Int = 0,
    val lastDailyBonusTime: Long = 0L
)

@Entity(tableName = "transaction_history")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val description: String,
    val coinAmount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_tasks")
data class GameTask(
    @PrimaryKey val taskId: String,
    val title: String,
    val reward: Int,
    val type: String, // "game", "task", "spin", "ad"
    val isCompleted: Boolean = false,
    val iconName: String,
    val category: String, // e.g., "Casual", "Action", "Quiz", "Offer"
    val description: String = ""
)
