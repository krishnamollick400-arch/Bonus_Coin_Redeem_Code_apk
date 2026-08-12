package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val userSession: Flow<UserSession?> = appDao.getUserSession()
    val transactions: Flow<List<TransactionHistory>> = appDao.getTransactions()
    val gameTasks: Flow<List<GameTask>> = appDao.getGameTasks()

    suspend fun getSessionSync(): UserSession? {
        return appDao.getUserSessionSync()
    }

    suspend fun saveSession(session: UserSession) {
        appDao.insertUserSession(session)
    }

    suspend fun updateSession(session: UserSession) {
        appDao.updateUserSession(session)
    }

    suspend fun addTransaction(transaction: TransactionHistory) {
        appDao.insertTransaction(transaction)
    }

    suspend fun completeTask(taskId: String) {
        appDao.completeTask(taskId)
    }

    suspend fun addGameTask(task: GameTask) {
        appDao.insertGameTask(task)
    }

    suspend fun clearTransactions() {
        appDao.clearTransactions()
    }
}
