package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

enum class AppTab {
    Home, Games, Tasks, Rewards, Profile
}

class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.appDao())

    // UI state for navigation
    private val _currentTab = MutableStateFlow(AppTab.Home)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Observable flows from database
    val userSession: StateFlow<UserSession?> = repository.userSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionHistory>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameTasks: StateFlow<List<GameTask>> = repository.gameTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication overlay / Screen State
    private val _showLoginScreen = MutableStateFlow(true)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    // Interactive mini-game / action states
    private val _selectedMiniGame = MutableStateFlow<String?>(null) // ID of game being played
    val selectedMiniGame: StateFlow<String?> = _selectedMiniGame.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _spinResultCoins = MutableStateFlow<Int?>(null)
    val spinResultCoins: StateFlow<Int?> = _spinResultCoins.asStateFlow()

    // Simulated Ad States
    private val _adTimerSeconds = MutableStateFlow(0)
    val adTimerSeconds: StateFlow<Int> = _adTimerSeconds.asStateFlow()

    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog: StateFlow<Boolean> = _showAdDialog.asStateFlow()

    // Notification / Dialog States
    private val _earnedCoinsNotification = MutableStateFlow<Pair<String, Int>?>(null) // Description, Coins
    val earnedCoinsNotification: StateFlow<Pair<String, Int>?> = _earnedCoinsNotification.asStateFlow()

    private val _redemptionCode = MutableStateFlow<String?>(null)
    val redemptionCode: StateFlow<String?> = _redemptionCode.asStateFlow()

    private val _showDailyRewardOnLaunch = MutableStateFlow(false)
    val showDailyRewardOnLaunch: StateFlow<Boolean> = _showDailyRewardOnLaunch.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if user session exists, if not initialize default
            val currentSession = repository.getSessionSync()
            if (currentSession == null) {
                repository.saveSession(UserSession())
                _showDailyRewardOnLaunch.value = true
            } else {
                if (currentSession.email != null) {
                    // If they previously logged in with an email, hide login screen
                    _showLoginScreen.value = false
                }
                
                // Check if user is eligible for daily reward popup (24 hours passed)
                val currentTime = System.currentTimeMillis()
                val difference = currentTime - currentSession.lastDailyBonusTime
                val dayInMillis = 24 * 60 * 60 * 1000L
                if (currentSession.lastDailyBonusTime == 0L || difference >= dayInMillis) {
                    _showDailyRewardOnLaunch.value = true
                }
            }

            // Prepopulate tasks if empty
            repository.gameTasks.first().let { tasks ->
                if (tasks.isEmpty()) {
                    populateDefaultTasks()
                }
            }
        }
    }

    fun dismissDailyRewardDialog() {
        _showDailyRewardOnLaunch.value = false
    }

    private suspend fun populateDefaultTasks() {
        val defaultTasks = listOf(
            GameTask("game_coin_smasher", "Orange Coin Smasher", 15, "game", false, "sports_esports", "Casual", "Tap falling orange coins as fast as you can inside 15 seconds to grab rewards!"),
            GameTask("game_math_quiz", "Math Speed Quiz", 25, "game", false, "functions", "Quiz", "Test your brain! Solve quick math equations correctly under time pressure."),
            GameTask("game_memory_match", "Memory Orange Match", 35, "game", false, "grid_view", "Casual", "Flip and match matching game-coin pairs to boost memory and earn coins!"),
            GameTask("task_survey", "Premium Gamers Survey", 120, "task", false, "poll", "Offer", "Answer 5 simple gaming preference questions for an instant bonus."),
            GameTask("task_scratch_win", "Golden Scratch & Win", 50, "task", false, "layers", "Offer", "Scratch the golden layout card to match 3 identical coins and win huge!"),
            GameTask("task_social_follow", "Follow Telegram Channel", 45, "task", false, "send", "Social", "Join our official community channel for tips, updates, and promo codes."),
            GameTask("task_app_review", "Rate & Review 5 Stars", 150, "task", false, "star", "Offer", "Submit a 5-star review on Google Play to support our developer team.")
        )
        for (task in defaultTasks) {
            repository.addGameTask(task)
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setLoginRequired(required: Boolean) {
        _showLoginScreen.value = required
    }

    fun skipLoginAsGuest() {
        _showLoginScreen.value = false
    }

    fun handleSignIn(email: String, username: String) {
        viewModelScope.launch {
            val existing = repository.getSessionSync() ?: UserSession()
            val newSession = existing.copy(
                email = email,
                username = if (username.isBlank()) "Gamer_${Random.nextInt(1000, 9999)}" else username,
                coinBalance = existing.coinBalance + 150 // Welcome bonus!
            )
            repository.saveSession(newSession)
            repository.addTransaction(
                TransactionHistory(
                    description = "Account Created Welcome Bonus",
                    coinAmount = 150
                )
            )
            _showLoginScreen.value = false
            _earnedCoinsNotification.value = "Sign In Reward" to 150
        }
    }

    fun handleSignOut() {
        viewModelScope.launch {
            // Reset database to initial state
            repository.saveSession(UserSession())
            repository.clearTransactions()
            _showLoginScreen.value = true
            _currentTab.value = AppTab.Home
        }
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            val session = repository.getSessionSync() ?: return@launch
            val currentTime = System.currentTimeMillis()
            val difference = currentTime - session.lastDailyBonusTime
            val dayInMillis = 24 * 60 * 60 * 1000L

            // Let user claim if 24 hours have passed or if they are claiming for the first time
            if (session.lastDailyBonusTime == 0L || difference >= dayInMillis) {
                val newStreak = if (difference < 2 * dayInMillis) session.dailyStreak + 1 else 1
                val bonusCoins = 50 + (newStreak * 10).coerceAtMost(50) // Streak multiplier up to +50 coins

                val updated = session.copy(
                    coinBalance = session.coinBalance + bonusCoins,
                    dailyStreak = newStreak,
                    lastDailyBonusTime = currentTime
                )
                repository.saveSession(updated)
                repository.addTransaction(
                    TransactionHistory(
                        description = "Day $newStreak Daily Check-in Bonus",
                        coinAmount = bonusCoins
                    )
                )
                _earnedCoinsNotification.value = "Daily Bonus Claimed" to bonusCoins
            } else {
                // Calculate hours remaining
                val hoursRemaining = ((dayInMillis - difference) / (1000 * 60 * 60)) + 1
                _earnedCoinsNotification.value = "Come back in $hoursRemaining hrs for next Daily Bonus!" to 0
            }
        }
    }

    fun startSpinWheel() {
        if (_isSpinning.value) return
        _isSpinning.value = true
        _spinResultCoins.value = null

        viewModelScope.launch {
            // Simulate 2 seconds spinning rotation
            kotlinx.coroutines.delay(2000)
            val options = listOf(5, 10, 25, 50, 75, 100)
            val randomIndex = Random.nextInt(options.size)
            val wonCoins = options[randomIndex]

            val session = repository.getSessionSync() ?: return@launch
            val updated = session.copy(coinBalance = session.coinBalance + wonCoins)
            repository.saveSession(updated)
            repository.addTransaction(
                TransactionHistory(
                    description = "Spin & Win Wheel Lucky Drop",
                    coinAmount = wonCoins
                )
            )

            _spinResultCoins.value = wonCoins
            _isSpinning.value = false
            _earnedCoinsNotification.value = "Lucky Wheel Spin" to wonCoins
        }
    }

    fun dismissSpinResult() {
        _spinResultCoins.value = null
    }

    fun selectMiniGame(gameId: String?) {
        _selectedMiniGame.value = gameId
    }

    fun completeGameOrTask(id: String, earnedCoins: Int, actionName: String) {
        viewModelScope.launch {
            val session = repository.getSessionSync() ?: return@launch
            val updated = session.copy(coinBalance = session.coinBalance + earnedCoins)
            repository.saveSession(updated)
            repository.addTransaction(
                TransactionHistory(
                    description = "$actionName Reward",
                    coinAmount = earnedCoins
                )
            )
            repository.completeTask(id)
            _selectedMiniGame.value = null
            _earnedCoinsNotification.value = actionName to earnedCoins
        }
    }

    fun startAdSimulation() {
        _adTimerSeconds.value = 5
        _showAdDialog.value = true
        viewModelScope.launch {
            while (_adTimerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _adTimerSeconds.value -= 1
            }
            // Ad finished! Reward coins
            _showAdDialog.value = false
            val session = repository.getSessionSync() ?: return@launch
            val rewardCoins = 25
            val updated = session.copy(coinBalance = session.coinBalance + rewardCoins)
            repository.saveSession(updated)
            repository.addTransaction(
                TransactionHistory(
                    description = "Rewarded Video Ad Views",
                    coinAmount = rewardCoins
                )
            )
            _earnedCoinsNotification.value = "Watch Ad Reward" to rewardCoins
        }
    }

    fun dismissAdDialog() {
        _showAdDialog.value = false
    }

    fun redeemReward(rewardOption: String, coinsRequired: Int) {
        viewModelScope.launch {
            val session = repository.getSessionSync() ?: return@launch
            if (session.coinBalance >= coinsRequired) {
                val updated = session.copy(coinBalance = session.coinBalance - coinsRequired)
                repository.saveSession(updated)
                repository.addTransaction(
                    TransactionHistory(
                        description = "Redeemed $rewardOption Gift Voucher",
                        coinAmount = -coinsRequired
                    )
                )

                // Generate random claim code
                val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val part1 = (1..4).map { characters[Random.nextInt(characters.length)] }.joinToString("")
                val part2 = (1..4).map { characters[Random.nextInt(characters.length)] }.joinToString("")
                val part3 = (1..4).map { characters[Random.nextInt(characters.length)] }.joinToString("")
                val code = "BONUS-$part1-$part2-$part3"

                _redemptionCode.value = code
            } else {
                _earnedCoinsNotification.value = "Insufficient Balance! Play more games to earn coins." to 0
            }
        }
    }

    fun dismissRedemption() {
        _redemptionCode.value = null
    }

    private val _referralStatusMessage = MutableStateFlow<String?>(null)
    val referralStatusMessage: StateFlow<String?> = _referralStatusMessage.asStateFlow()

    fun submitReferralCode(code: String) {
        viewModelScope.launch {
            val trimmed = code.trim().uppercase()
            if (trimmed.length < 5) {
                _referralStatusMessage.value = "Error: Invalid referral code format."
                return@launch
            }
            val existing = repository.getSessionSync() ?: UserSession()
            val bonus = 500
            val updated = existing.copy(coinBalance = existing.coinBalance + bonus)
            repository.saveSession(updated)
            repository.addTransaction(
                TransactionHistory(
                    description = "Friend Referral Bonus Applied",
                    coinAmount = bonus
                )
            )
            _referralStatusMessage.value = "Referral code applied! You received +$bonus Coins."
            _earnedCoinsNotification.value = "Referral Bonus Received" to bonus
        }
    }

    fun clearReferralStatus() {
        _referralStatusMessage.value = null
    }

    fun clearEarnedCoinsNotification() {
        _earnedCoinsNotification.value = null
    }
}
