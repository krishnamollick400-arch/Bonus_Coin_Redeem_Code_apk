package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM user_session WHERE userId = 'default_user' LIMIT 1")
    fun getUserSession(): Flow<UserSession?>

    @Query("SELECT * FROM user_session WHERE userId = 'default_user' LIMIT 1")
    suspend fun getUserSessionSync(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSession)

    @Update
    suspend fun updateUserSession(session: UserSession)

    @Query("SELECT * FROM transaction_history ORDER BY timestamp DESC")
    fun getTransactions(): Flow<List<TransactionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionHistory)

    @Query("SELECT * FROM game_tasks")
    fun getGameTasks(): Flow<List<GameTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameTask(task: GameTask)

    @Query("UPDATE game_tasks SET isCompleted = 1 WHERE taskId = :taskId")
    suspend fun completeTask(taskId: String)

    @Query("DELETE FROM transaction_history")
    suspend fun clearTransactions()
}
