package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GameTask
import com.example.data.TransactionHistory
import com.example.data.UserSession
import com.example.ui.AppTab
import com.example.ui.RewardsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: RewardsViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val showLogin by viewModel.showLoginScreen.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val gameTasks by viewModel.gameTasks.collectAsState()
    val selectedGameId by viewModel.selectedMiniGame.collectAsState()
    val earnedNotification by viewModel.earnedCoinsNotification.collectAsState()

    val context = LocalContext.current

    // Trigger toast notification when coins are earned
    LaunchedEffect(earnedNotification) {
        earnedNotification?.let { (desc, coins) ->
            if (coins > 0) {
                Toast.makeText(context, "🎉 $desc: +$coins Coins!", Toast.LENGTH_SHORT).show()
            } else if (desc.isNotEmpty()) {
                Toast.makeText(context, desc, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearEarnedCoinsNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showLogin && selectedGameId == null) {
                BottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            when {
                showLogin -> {
                    LoginScreen(
                        onSignIn = { email, username -> viewModel.handleSignIn(email, username) },
                        onSkip = { viewModel.skipLoginAsGuest() }
                    )
                }
                selectedGameId != null -> {
                    GameOverlay(
                        gameId = selectedGameId!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectMiniGame(null) }
                    )
                }
                else -> {
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(durationMillis = 300),
                        label = "tab_crossfade"
                    ) { tab ->
                        when (tab) {
                            AppTab.Home -> HomeScreen(viewModel = viewModel, gameTasks = gameTasks, userSession = userSession)
                            AppTab.Games -> GamesTabScreen(viewModel = viewModel, gameTasks = gameTasks)
                            AppTab.Tasks -> TasksTabScreen(viewModel = viewModel, gameTasks = gameTasks)
                            AppTab.Rewards -> RewardsTabScreen(viewModel = viewModel, userSession = userSession)
                            AppTab.Profile -> ProfileTabScreen(viewModel = viewModel, userSession = userSession, transactions = transactions)
                        }
                    }
                }
            }
        }
    }

    val showDailyRewardOnLaunch by viewModel.showDailyRewardOnLaunch.collectAsState()
    if (showDailyRewardOnLaunch && !showLogin && selectedGameId == null) {
        DailyRewardLaunchDialog(
            viewModel = viewModel,
            userSession = userSession,
            onDismiss = { viewModel.dismissDailyRewardDialog() }
        )
    }
}

// ============== AUTHENTICATION PAGE ==============
@Composable
fun LoginScreen(
    onSignIn: (String, String) -> Unit,
    onSkip: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Big Branding Section
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB300),
                            Color(0xFFE65100)
                        )
                    )
                )
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_bonus_coin),
                contentDescription = "Bonus Coin Redeem Code Logo",
                modifier = Modifier
                    .size(100.dp)
                    .animateContentSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bonus Coin Redeem Code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Play games, claim rewards, and redeem vouchers!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignUp) "Create New Account" else "Secure Gamer Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text("Email/Gmail Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    isError = emailError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSignUp) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Gamer Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        // Validate inputs
                        if (email.isBlank() || !email.contains("@")) {
                            emailError = "Please enter a valid email address"
                            return@Button
                        }
                        if (password.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }

                        onSignIn(email, if (isSignUp) username else email.substringBefore("@"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF57C00)
                    )
                ) {
                    Text(
                        text = if (isSignUp) "Sign Up & Get 150 Coins" else "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { isSignUp = !isSignUp }
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? Log In" else "New Gamer? Create an Account",
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            Text(
                text = " OR ",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secure Google Sign-In Option
        OutlinedButton(
            onClick = {
                // Simulate quick Google authentication
                onSignIn("gamer.google@gmail.com", "Gamer Pro")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("google_login_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Simple representation of Google logo
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Gmail / Google",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onSkip
        ) {
            Text(
                text = "Continue as Guest Gamer",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ============== BOTTOM NAVIGATION BAR ==============
@Composable
fun BottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val items = listOf(
            NavigationItem(AppTab.Home, Icons.Default.Home, Icons.Outlined.Home, "Home"),
            NavigationItem(AppTab.Games, Icons.Default.SportsEsports, Icons.Outlined.SportsEsports, "Games"),
            NavigationItem(AppTab.Tasks, Icons.Default.Assignment, Icons.Outlined.Assignment, "Tasks"),
            NavigationItem(AppTab.Rewards, Icons.Default.CardGiftcard, Icons.Outlined.CardGiftcard, "Rewards"),
            NavigationItem(AppTab.Profile, Icons.Default.Person, Icons.Outlined.Person, "Profile")
        )

        items.forEach { item ->
            val selected = currentTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFFFE0B2)
                )
            )
        }
    }
}

data class NavigationItem(
    val tab: AppTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

// ============== HOME SCREEN ==============
@Composable
fun HomeScreen(
    viewModel: RewardsViewModel,
    gameTasks: List<GameTask>,
    userSession: UserSession?
) {
    val coroutineScope = rememberCoroutineScope()
    var showSpinWheelDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Welcome, ${userSession?.username ?: "Gamer"}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Bonus Coin Redeem Code App",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // Small streak visual
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECE0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userSession?.dailyStreak ?: 0} Days",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        // Prominent Balance Card with Glossy Image Logo
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF9100),
                                    Color(0xFFFF3D00)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    // Floating subtle decorative circles
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = 120.dp.toPx(),
                            center = Offset(size.width - 20.dp.toPx(), 20.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = 60.dp.toPx(),
                            center = Offset(30.dp.toPx(), size.height - 10.dp.toPx())
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TOTAL COIN BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%,d", userSession?.coinBalance ?: 0),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Orange Coins",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1,000 Coins = ₹10 Indian Redeem Code",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Original floating orange logo representation
                        var isPulsed by remember { mutableStateOf(false) }
                        val scaleFactor by animateFloatAsState(
                            targetValue = if (isPulsed) 1.08f else 0.96f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "coin_pulsing"
                        )
                        LaunchedEffect(Unit) {
                            isPulsed = true
                        }

                        Image(
                            painter = painterResource(id = R.drawable.img_bonus_coin),
                            contentDescription = "Original Orange Coin Logo",
                            modifier = Modifier
                                .size(90.dp)
                                .scale(scaleFactor)
                                .shadow(8.dp, CircleShape)
                        )
                    }
                }
            }
        }

        // Full Interactive 7-Day Daily Check-in & Streak Component
        item {
            DailyCheckInStreakCard(
                viewModel = viewModel,
                userSession = userSession
            )
        }

        // Quick Ads and Game shortcuts Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.startAdSimulation() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                border = BorderStroke(1.dp, Color(0xFF81D4FA))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Watch Ads",
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Watch Sponsor Videos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF01579B)
                            )
                            Text(
                                text = "Earn +25 Coins instantly by playing brief promo ad",
                                fontSize = 11.sp,
                                color = Color(0xFF0277BD)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF0288D1)
                    )
                }
            }
        }

        // Live Lucky Spin Wheel Dashboard Card
        item {
            DashboardSpinWheelSection(viewModel = viewModel)
        }

        // Top Earners Leaderboard Section
        item {
            TopEarnersSection(
                userSession = userSession
            )
        }

        // Recommended Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 Hot Arcade Games",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { viewModel.selectTab(AppTab.Games) }
                ) {
                    Text("See All", color = Color(0xFFE65100))
                }
            }
        }

        // Recommended items
        val games = gameTasks.filter { it.type == "game" }.take(2)
        items(games) { game ->
            GameOrTaskCard(task = game, onAction = { viewModel.selectMiniGame(game.taskId) })
        }

        // Promo Banner Section - Interactive Referral System
        item {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            val context = androidx.compose.ui.platform.LocalContext.current
            val referralStatus by viewModel.referralStatusMessage.collectAsState()
            var inviteCodeInput by remember { mutableStateOf("") }
            val myReferralCode = "GIFT-${(userSession?.username ?: "GAMER").uppercase().replace(" ", "")}-77"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referral_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFFFFCC80))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎁", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Refer & Earn Bonus",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Share with buddies to earn coins!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Share Your Code Panel
                    Text(
                        text = "YOUR UNIQUE INVITATION CODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = myReferralCode,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(myReferralCode))
                                Toast.makeText(context, "Referral Code Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Referral Code",
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFFFECE0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Redeem Friend's Code Panel
                    Text(
                        text = "REDEEM FRIEND'S INVITE CODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it },
                            placeholder = { Text("Enter invite code...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("referral_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF8F00),
                                unfocusedBorderColor = Color(0xFFFFCC80)
                            )
                        )

                        Button(
                            onClick = {
                                if (inviteCodeInput.isNotBlank()) {
                                    viewModel.submitReferralCode(inviteCodeInput)
                                }
                            },
                            enabled = inviteCodeInput.isNotBlank(),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("referral_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF8F00)
                            )
                        ) {
                            Text("REDEEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (referralStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = referralStatus!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (referralStatus!!.startsWith("Error")) Color.Red else Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                viewModel.clearReferralStatus()
                                if (!referralStatus!!.startsWith("Error")) {
                                    inviteCodeInput = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                        }
                    }
                }
            }
        }
    }

    // Interactive Spin Wheel Dialog
    if (showSpinWheelDialog) {
        SpinWheelDialog(
            viewModel = viewModel,
            onDismiss = { showSpinWheelDialog = false }
        )
    }

    // Ad Countdown Dialog
    val showAd by viewModel.showAdDialog.collectAsState()
    val adTimer by viewModel.adTimerSeconds.collectAsState()
    if (showAd) {
        Dialog(onDismissRequest = { /* Force watch to end */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { adTimer / 5f },
                        color = Color(0xFFFF9100),
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Playing Gaming Ad...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rewarding in $adTimer seconds",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ADVERTISEMENT SPONSOR",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ============== COMPONENT: GAME / TASK CARD ==============
@Composable
fun GameOrTaskCard(
    task: GameTask,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (task.type == "game") Color(0xFFFFF3E0) else Color(0xFFECEFF1)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (task.iconName) {
                        "sports_esports" -> Icons.Default.SportsEsports
                        "functions" -> Icons.Default.Calculate
                        "grid_view" -> Icons.Default.GridView
                        "poll" -> Icons.Default.Poll
                        "layers" -> Icons.Default.Layers
                        "send" -> Icons.Default.Send
                        "star" -> Icons.Default.Star
                        else -> Icons.Default.Extension
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (task.type == "game") Color(0xFFFF8F00) else Color(0xFF607D8B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // Coin prize indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${task.reward}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFFF57C00)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============== DIALOG: SPIN WHEEL ==============
@Composable
fun SpinWheelDialog(
    viewModel: RewardsViewModel,
    onDismiss: () -> Unit
) {
    val isSpinning by viewModel.isSpinning.collectAsState()
    val spinResultCoins by viewModel.spinResultCoins.collectAsState()

    var rotationDegree by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lucky Spin Wheel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Graphic Rotating Spin Wheel representation
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val angleState by animateFloatAsState(
                        targetValue = rotationDegree,
                        animationSpec = if (isSpinning) {
                            tween(2000, easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f))
                        } else {
                            snap()
                        },
                        label = "spin_angle_anim"
                    )

                    // Draw static outer rim
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFE65100),
                            style = Stroke(width = 8.dp.toPx())
                        )
                        // Rim lights
                        val numLights = 12
                        for (i in 0 until numLights) {
                            val lightAngle = (i * 360f / numLights) * PI / 180f
                            val radius = (size.width / 2) - 4.dp.toPx()
                            val lightX = (size.width / 2) + radius * cos(lightAngle).toFloat()
                            val lightY = (size.height / 2) + radius * sin(lightAngle).toFloat()
                            drawCircle(
                                color = if (i % 2 == 0) Color.White else Color(0xFFFFD54F),
                                radius = 4.dp.toPx(),
                                center = Offset(lightX, lightY)
                            )
                        }
                    }

                    // Rotating Inner Sections of Wheel
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .rotate(angleState)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val values = listOf("100", "5", "50", "10", "75", "25")
                            val colors = listOf(
                                Color(0xFFFFAB00), Color(0xFFFFE082),
                                Color(0xFFFF6D00), Color(0xFFFFCC80),
                                Color(0xFFD50000), Color(0xFFFF8A80)
                            )
                            val sweepAngle = 360f / values.size

                            for (i in values.indices) {
                                val startAngle = i * sweepAngle
                                drawArc(
                                    color = colors[i],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true
                                )
                            }
                        }

                        // Labels placed inside sections
                        val values = listOf("100", "5", "50", "10", "75", "25")
                        values.forEachIndexed { i, valStr ->
                            val drawAngle = (i * 60 + 30)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(drawAngle.toFloat())
                            ) {
                                Text(
                                    text = "$valStr",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 28.dp)
                                )
                            }
                        }
                    }

                    // Static Indicator Pointer at top center
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
                            .background(Color(0xFFE65100), CircleShape)
                            .padding(4.dp)
                    )

                    // Central pin core
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .shadow(2.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (!isSpinning) {
                            rotationDegree += 360f * 5 + Random.nextInt(360)
                            viewModel.startSpinWheel()
                        }
                    },
                    enabled = !isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("spin_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF57C00)
                    )
                ) {
                    Text(
                        text = if (isSpinning) "Spinning Wheel..." else "SPIN NOW",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (spinResultCoins != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🎉 Congratulations!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "You won $spinResultCoins free Orange Coins!",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ============== GAMES TAB SCREEN ==============
@Composable
fun GamesTabScreen(
    viewModel: RewardsViewModel,
    gameTasks: List<GameTask>
) {
    val games = gameTasks.filter { it.type == "game" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Play Games & Earn Coins",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Launch an arcade game below, play for fun, and coins are added instantly to your balance!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(games) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectMiniGame(game.taskId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = when (game.taskId) {
                                            "game_coin_smasher" -> listOf(Color(0xFFFF9100), Color(0xFFFFB300))
                                            "game_math_quiz" -> listOf(Color(0xFF2979FF), Color(0xFF00B0FF))
                                            else -> listOf(Color(0xFF00E676), Color(0xFF1DE9B6))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (game.iconName) {
                                "sports_esports" -> Icons.Default.SportsEsports
                                "functions" -> Icons.Default.Calculate
                                else -> Icons.Default.GridView
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = game.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = game.category,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "+${game.reward}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = game.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.selectMiniGame(game.taskId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF8F00)
                                )
                            ) {
                                Text("Play Game Now", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============== TASKS TAB SCREEN ==============
@Composable
fun TasksTabScreen(
    viewModel: RewardsViewModel,
    gameTasks: List<GameTask>
) {
    val tasks = gameTasks.filter { it.type == "task" }

    var activeScratchTask by remember { mutableStateOf<GameTask?>(null) }
    var activeSurveyTask by remember { mutableStateOf<GameTask?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Earn Easy Bonus Coins",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Complete user friendly feedback surveys, rate the app or complete golden cards to get big bundles!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks) { task ->
                GameOrTaskCard(
                    task = task,
                    onAction = {
                        when (task.taskId) {
                            "task_scratch_win" -> activeScratchTask = task
                            "task_survey" -> activeSurveyTask = task
                            "task_social_follow" -> {
                                viewModel.completeGameOrTask(task.taskId, task.reward, "Telegram Follow")
                            }
                            "task_app_review" -> {
                                viewModel.completeGameOrTask(task.taskId, task.reward, "App Store Feedback")
                            }
                        }
                    }
                )
            }
        }
    }

    // Scratch Card dialog overlay
    if (activeScratchTask != null) {
        ScratchCardDialog(
            task = activeScratchTask!!,
            viewModel = viewModel,
            onDismiss = { activeScratchTask = null }
        )
    }

    // Survey questions dialog overlay
    if (activeSurveyTask != null) {
        SurveyDialog(
            task = activeSurveyTask!!,
            viewModel = viewModel,
            onDismiss = { activeSurveyTask = null }
        )
    }
}

// ============== COMPONENT: SURVEY DIALOG ==============
@Composable
fun SurveyDialog(
    task: GameTask,
    viewModel: RewardsViewModel,
    onDismiss: () -> Unit
) {
    var questionIndex by remember { mutableStateOf(0) }
    val questions = listOf(
        "What is your favourite mobile gaming genre?" to listOf("Casual/Arcade", "Action/RPG", "Puzzles/Quiz", "Multiplayer Shooter"),
        "How many hours a week do you play mobile games?" to listOf("1-3 Hours", "4-7 Hours", "8-15 Hours", "15+ Hours"),
        "Do you prefer playing games solo or with friends?" to listOf("Solo Gamer", "Multiplayer Lobbies", "Cooperative Campaign", "Competitive PvP"),
        "Which reward voucher brand do you target most?" to listOf("Google Play INR Code", "Paytm Cashout", "Amazon Pay Card", "UPI Transfer"),
        "Rate your experience with Bonus Coin Redeem Code!" to listOf("Awesome UI", "Extremely fun", "Generous coin payout", "All of the above!")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gamers Survey",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { (questionIndex + 1) / questions.size.toFloat() },
                    color = Color(0xFFFF8F00),
                    trackColor = Color(0xFFFFE0B2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                val (questionText, choices) = questions[questionIndex]

                Text(
                    text = "Question ${questionIndex + 1} of ${questions.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = questionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                choices.forEach { choice ->
                    OutlinedButton(
                        onClick = {
                            if (questionIndex < questions.size - 1) {
                                questionIndex += 1
                            } else {
                                viewModel.completeGameOrTask(task.taskId, task.reward, "Gamer Survey Complete")
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = choice,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ============== COMPONENT: SCRATCH CARD DIALOG ==============
@Composable
fun ScratchCardDialog(
    task: GameTask,
    viewModel: RewardsViewModel,
    onDismiss: () -> Unit
) {
    var isScratched by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Golden Scratch Card",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap or rub to scratch off the golden layout and reveal matching orange coins!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Scratch Area representation
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clickable { isScratched = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isScratched) {
                        // Covered Golden Layout
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFA000))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "TAP TO SCRATCH",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Scratched revealed prize
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE8F5E9)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "MATCH FOUND!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "+50",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color(0xFFF57C00)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (isScratched) {
                            viewModel.completeGameOrTask(task.taskId, task.reward, "Scratch Card Prize Claimed")
                            onDismiss()
                        } else {
                            isScratched = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScratched) Color(0xFF4CAF50) else Color(0xFFFF8F00)
                    )
                ) {
                    Text(
                        text = if (isScratched) "CLAIM COINS NOW" else "SCRATCH FOR ME",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============== REWARDS TAB SCREEN (REDEEM) ==============
@Composable
fun RewardsTabScreen(
    viewModel: RewardsViewModel,
    userSession: UserSession?
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val redemptionCode by viewModel.redemptionCode.collectAsState()

    val rewardOptions = listOf(
        RewardOption("Google Play India", 1000, "₹10 Code", "GP-INR-10", R.drawable.img_bonus_coin, "Google Play Store INR Promo Code"),
        RewardOption("Google Play India", 4500, "₹50 Code", "GP-INR-50", R.drawable.img_bonus_coin, "Google Play Store INR Promo Code"),
        RewardOption("Paytm Cash", 4500, "₹50 Cash", "PAYTM-50", R.drawable.img_bonus_coin, "Direct Paytm Cash Transfer"),
        RewardOption("Paytm Cash", 8500, "₹100 Cash", "PAYTM-100", R.drawable.img_bonus_coin, "Direct Paytm Cash Transfer"),
        RewardOption("Amazon Pay Gift Card", 8500, "₹100 Card", "AMZ-INR-100", R.drawable.img_bonus_coin, "Amazon India Shopping Gift Card"),
        RewardOption("UPI Instant Transfer", 20000, "₹250 Transfer", "UPI-INR-250", R.drawable.img_bonus_coin, "Direct Bank UPI Transfer")
    )

    var selectedOption by remember { mutableStateOf<RewardOption?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Google Play", "Paytm", "Amazon", "UPI")

    val filteredOptions = if (selectedCategory == "All") {
        rewardOptions
    } else {
        rewardOptions.filter { it.brand.contains(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Redeem Vouchers",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Convert your collected orange coins into authentic Indian digital game play redeem codes, Paytm cash, or UPI bank transfers!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Horizontal Category Filter List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFFFF8F00) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFFF8F00) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (filteredOptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No rewards available in this category.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            // Balanced Grid list
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredOptions) { option ->
                    val canAfford = (userSession?.coinBalance ?: 0) >= option.coinsRequired
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (canAfford) Color(0xFFFFB300) else Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Voucher Header Logo Brand
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = when {
                                            option.brand.contains("Google") -> listOf(Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
                                            option.brand.contains("Amazon") -> listOf(Color(0xFF232F3E), Color(0xFFFF9900))
                                            option.brand.contains("Paytm") -> listOf(Color(0xFF00B9F5), Color(0xFF002E6E))
                                            option.brand.contains("UPI") -> listOf(Color(0xFF097939), Color(0xFF308C39))
                                            else -> listOf(Color(0xFF003087), Color(0xFF0079C1))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    option.brand.contains("Google") -> "GPlay"
                                    option.brand.contains("Amazon") -> "AMZN"
                                    option.brand.contains("Paytm") -> "Paytm"
                                    option.brand.contains("UPI") -> "UPI"
                                    else -> "PayPal"
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = option.brand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = option.valueText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (canAfford) Color(0xFFFFF3E0) else Color(0xFFECEFF1)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%,d", option.coinsRequired),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = if (canAfford) Color(0xFFE65100) else Color(0xFF78909C)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = if (canAfford) Color(0xFFFFB300) else Color(0xFFB0BEC5),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Confirmation Dialog
    if (selectedOption != null) {
        val option = selectedOption!!
        val hasCoins = (userSession?.coinBalance ?: 0) >= option.coinsRequired

        Dialog(onDismissRequest = { selectedOption = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Confirm Redemption",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Are you sure you want to redeem?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = option.description,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Denomination: ${option.valueText}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cost: ${String.format("%,d", option.coinsRequired)} Coins",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedOption = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CANCEL")
                        }

                        Button(
                            onClick = {
                                viewModel.redeemReward(option.brand + " " + option.valueText, option.coinsRequired)
                                selectedOption = null
                            },
                            enabled = hasCoins,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF8F00)
                            )
                        ) {
                            Text("CONFIRM REDEEM", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Success Code Dialog
    if (redemptionCode != null) {
        Dialog(onDismissRequest = { viewModel.dismissRedemption() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Voucher Redeemed Successfully!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Bonus Coin Redeem Code is ready to copy. Redeem it directly on the Google Play / brand store app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Code Container Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = redemptionCode!!,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.testTag("redeem_code_output")
                            )

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(redemptionCode!!))
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = Color(0xFF2E7D32))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.dismissRedemption() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("GREAT, THANKS!", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

data class RewardOption(
    val brand: String,
    val coinsRequired: Int,
    val valueText: String,
    val codeKey: String,
    val iconRes: Int,
    val description: String
)

// ============== PROFILE TAB SCREEN ==============
@Composable
fun ProfileTabScreen(
    viewModel: RewardsViewModel,
    userSession: UserSession?,
    transactions: List<TransactionHistory>
) {
    var editUsername by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userSession) {
        userSession?.let {
            editUsername = it.username
            editEmail = it.email ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gamer Profile",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main User Info Layout Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF5722))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Gamer Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userSession?.username ?: "Gamer Pro",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userSession?.email ?: "Guest Mode Account",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%,d", userSession?.coinBalance ?: 0),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Coins Balance",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${userSession?.dailyStreak ?: 0} Days",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Active Streak",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lvl ${((userSession?.coinBalance ?: 0) / 1000 + 1).coerceIn(1, 100)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Gamer Level",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Profile", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.handleSignOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset & Logout", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction History header
        Text(
            text = "📋 Coin Transaction Log",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Coin Transactions History List
        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No coins logged yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(transactions) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = log.description,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                Text(
                                    text = formatter.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isPositive = log.coinAmount >= 0
                                Text(
                                    text = if (isPositive) "+${log.coinAmount}" else "${log.coinAmount}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Edit Dialog
    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Profile Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL")
                        }

                        Button(
                            onClick = {
                                viewModel.handleSignIn(editEmail, editUsername)
                                showEditDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
                        ) {
                            Text("SAVE", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============== COMPONENT: MINI GAME OVERLAY ==============
@Composable
fun GameOverlay(
    gameId: String,
    viewModel: RewardsViewModel,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (gameId) {
            "game_coin_smasher" -> CoinSmasherGame(viewModel = viewModel, onBack = onBack)
            "game_math_quiz" -> MathSpeedQuizGame(viewModel = viewModel, onBack = onBack)
            "game_memory_match" -> MemoryMatchGame(viewModel = viewModel, onBack = onBack)
            else -> {
                // Fallback
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Unknown Arcade game type", color = Color.White)
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        }
    }
}

// ============== MINI GAME 1: COIN SMASHER ==============
@Composable
fun CoinSmasherGame(
    viewModel: RewardsViewModel,
    onBack: () -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var gameTimeRemaining by remember { mutableStateOf(15) }
    var isPlaying by remember { mutableStateOf(false) }

    // Coordinates for the interactive target coin
    var coinX by remember { mutableStateOf(100f) }
    var coinY by remember { mutableStateOf(100f) }

    val coroutineScope = rememberCoroutineScope()

    // Game loop timer
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            score = 0
            gameTimeRemaining = 15
            while (gameTimeRemaining > 0) {
                delay(1000)
                gameTimeRemaining -= 1
            }
            isPlaying = false
            // Completed! Reward coin score (capped up to 15 coins)
            viewModel.completeGameOrTask("game_coin_smasher", score.coerceAtMost(15), "Orange Coin Smasher Score")
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        if (!isPlaying) {
            // Splash layout before play
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Orange Coin Smasher!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A hyper-casual tapping game! Smash as many orange coins as you can before the 15 seconds timer runs out.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isPlaying = true
                        coinX = Random.nextInt(50, (containerWidth - 100).toInt().coerceAtLeast(100)).toFloat()
                        coinY = Random.nextInt(150, (containerHeight - 150).toInt().coerceAtLeast(200)).toFloat()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
                ) {
                    Text("START TAPPING GAME", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onBack) {
                    Text("Back to Arcade", color = Color.White.copy(alpha = 0.6f))
                }
            }
        } else {
            // Active playing mode
            Box(modifier = Modifier.fillMaxSize()) {
                // Header HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score: $score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Time Left: ${gameTimeRemaining}s",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gameTimeRemaining < 5) Color.Red else Color.White
                    )
                }

                // Falling Orange Coin to smash (represented beautifully as our custom drawable)
                Box(
                    modifier = Modifier
                        .offset(x = coinX.dp, y = coinY.dp)
                        .size(70.dp)
                        .shadow(4.dp, CircleShape)
                        .clickable {
                            score += 1
                            // Move coin randomly
                            coinX = Random.nextInt(20, (containerWidth - 90).toInt().coerceAtLeast(40)).toFloat()
                            coinY = Random.nextInt(100, (containerHeight - 120).toInt().coerceAtLeast(150)).toFloat()
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_bonus_coin),
                        contentDescription = "Smash target orange coin",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ============== MINI GAME 2: MATH SPEED QUIZ ==============
@Composable
fun MathSpeedQuizGame(
    viewModel: RewardsViewModel,
    onBack: () -> Unit
) {
    var num1 by remember { mutableStateOf(0) }
    var num2 by remember { mutableStateOf(0) }
    var operator by remember { mutableStateOf("+") }
    var correctAnswer by remember { mutableStateOf(0) }
    var choices by remember { mutableStateOf<List<Int>>(emptyList()) }

    var quizScore by remember { mutableStateOf(0) }
    var quizTimeRemaining by remember { mutableStateOf(20) }
    var isPlaying by remember { mutableStateOf(false) }

    fun generateQuestion() {
        num1 = Random.nextInt(2, 12)
        num2 = Random.nextInt(2, 12)
        val opType = Random.nextInt(3)
        if (opType == 0) {
            operator = "+"
            correctAnswer = num1 + num2
        } else if (opType == 1) {
            operator = "-"
            correctAnswer = num1.coerceAtLeast(num2) - num1.coerceAtMost(num2)
            // Ensure first is bigger
            val temp = num1.coerceAtLeast(num2)
            num2 = num1.coerceAtMost(num2)
            num1 = temp
        } else {
            operator = "x"
            correctAnswer = num1 * num2
        }

        val dummy1 = correctAnswer + Random.nextInt(1, 5)
        val dummy2 = (correctAnswer - Random.nextInt(1, 5)).coerceAtLeast(0)
        choices = listOf(correctAnswer, dummy1, dummy2).shuffled()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            quizScore = 0
            quizTimeRemaining = 20
            generateQuestion()
            while (quizTimeRemaining > 0) {
                delay(1000)
                quizTimeRemaining -= 1
            }
            isPlaying = false
            viewModel.completeGameOrTask("game_math_quiz", 25, "Math Speed Quiz Completion")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = Color(0xFF00B0FF),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Math Speed Quiz!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Think fast! Solve elementary math equations correctly under time constraints. Full completion adds 25 Orange Coins!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { isPlaying = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
            ) {
                Text("START MATH SPEED QUIZ", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("Back to Arcade", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            // Playing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Correct: $quizScore", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Timer: ${quizTimeRemaining}s", color = if (quizTimeRemaining < 5) Color.Red else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Solve the Equation",
                fontSize = 13.sp,
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$num1 $operator $num2 = ?",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            choices.forEach { choice ->
                Button(
                    onClick = {
                        if (choice == correctAnswer) {
                            quizScore += 1
                        }
                        generateQuestion()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1C0A))
                ) {
                    Text(
                        text = "$choice",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============== MINI GAME 3: MEMORY MATCH ==============
@Composable
fun MemoryMatchGame(
    viewModel: RewardsViewModel,
    onBack: () -> Unit
) {
    val symbols = listOf("🎮", "⭐", "🚀", "👾", "🔥", "💎")
    var cardStates by remember { mutableStateOf<List<MemoryCard>>(emptyList()) }
    var firstSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var secondSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var moves by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    fun setupBoard() {
        val doubled = (symbols + symbols).shuffled()
        cardStates = doubled.map { MemoryCard(it) }
        moves = 0
        firstSelectedIndex = null
        secondSelectedIndex = null
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            setupBoard()
        }
    }

    // Checking matches on flip
    LaunchedEffect(secondSelectedIndex) {
        if (firstSelectedIndex != null && secondSelectedIndex != null) {
            val card1 = cardStates[firstSelectedIndex!!]
            val card2 = cardStates[secondSelectedIndex!!]
            delay(800)
            if (card1.value == card2.value) {
                // Match
                cardStates = cardStates.mapIndexed { index, memoryCard ->
                    if (index == firstSelectedIndex || index == secondSelectedIndex) {
                        memoryCard.copy(isMatched = true)
                    } else {
                        memoryCard
                    }
                }
            } else {
                // Mismatch, Flip back
                cardStates = cardStates.mapIndexed { index, memoryCard ->
                    if (index == firstSelectedIndex || index == secondSelectedIndex) {
                        memoryCard.copy(isFaceUp = false)
                    } else {
                        memoryCard
                    }
                }
            }
            firstSelectedIndex = null
            secondSelectedIndex = null
            moves += 1

            // Check win
            if (cardStates.all { it.isMatched }) {
                delay(400)
                isPlaying = false
                viewModel.completeGameOrTask("game_memory_match", 35, "Memory Match Grid Cleared")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Memory Orange Match!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Flip cards and find all matching pairs! Focus your brain to complete inside minimum steps. Wins 35 Orange Coins!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { isPlaying = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
            ) {
                Text("START MEMORY BOARD", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("Back to Arcade", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            // Active Board
            Text("Moves: $moves", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                items(cardStates.size) { index ->
                    val card = cardStates[index]
                    val revealed = card.isFaceUp || card.isMatched

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (!revealed && secondSelectedIndex == null) {
                                    cardStates = cardStates.mapIndexed { idx, mc ->
                                        if (idx == index) mc.copy(isFaceUp = true) else mc
                                    }
                                    if (firstSelectedIndex == null) {
                                        firstSelectedIndex = index
                                    } else {
                                        secondSelectedIndex = index
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (revealed) Color(0xFFFFF3E0) else Color(0xFFFF8F00)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (revealed) {
                                Text(text = card.value, fontSize = 24.sp)
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_bonus_coin),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = { isPlaying = false },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Exit Game")
            }
        }
    }
}

data class MemoryCard(
    val value: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

// ============== TOP EARNERS LEADERBOARD ==============
@Composable
fun TopEarnersSection(
    userSession: UserSession?
) {
    var selectedTimeframe by remember { mutableStateOf("Weekly") }
    var showFullLeaderboard by remember { mutableStateOf(false) }

    val currentUserBalance = userSession?.coinBalance ?: 500
    val currentUsername = userSession?.username ?: "Guest Gamer"

    // Base mock players
    val weeklyBaseList = listOf(
        Pair("LegendGamer", 4200),
        Pair("CodeRedeemer", 3500),
        Pair("CoinCollector", 2900),
        Pair("SpinQueen", 2100),
        Pair("ArcadePro", 1200)
    )

    val allTimeBaseList = listOf(
        Pair("LegendGamer", 12450),
        Pair("CodeRedeemer", 8900),
        Pair("CoinCollector", 5600),
        Pair("SpinQueen", 3250),
        Pair("ArcadePro", 1800)
    )

    val baseList = if (selectedTimeframe == "Weekly") weeklyBaseList else allTimeBaseList

    // Combine and sort
    val combinedList = (baseList + Pair(currentUsername, currentUserBalance))
        .distinctBy { it.first } // Ensure user doesn't double-register if using matching name
        .sortedByDescending { it.second }

    val userRank = combinedList.indexOfFirst { it.first == currentUsername } + 1
    val displayedList = combinedList.take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_earners_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFECE0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with trophy icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🏆 Top Earners Ranks",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live player standings",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Timeframe switches
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("Weekly", "All-Time").forEach { timeframe ->
                        val active = selectedTimeframe == timeframe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFFFFE0B2) else Color.Transparent)
                                .clickable { selectedTimeframe = timeframe }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeframe,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-header motivational banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎯", fontSize = 16.sp)
                    val motivationalText = if (userRank == 1) {
                        "Absolute Champion! You're dominating the leaderboard!"
                    } else {
                        val nextPlayer = combinedList.getOrNull(userRank - 2)
                        if (nextPlayer != null) {
                            val diff = nextPlayer.second - currentUserBalance
                            "You are Rank #$userRank. Earn $diff more Coins to beat ${nextPlayer.first}!"
                        } else {
                            "Keep playing arcade games to rank up higher!"
                        }
                    }
                    Text(
                        text = motivationalText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard Items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedList.forEachIndexed { idx, player ->
                    val isPlayerCurrentUser = player.first == currentUsername
                    val rank = idx + 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPlayerCurrentUser) Color(0xFFFFECE0) else Color.Transparent
                            )
                            .border(
                                width = if (isPlayerCurrentUser) 1.dp else 0.dp,
                                color = if (isPlayerCurrentUser) Color(0xFFFFB300) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (rank) {
                                            1 -> Color(0xFFFFD54F) // Gold
                                            2 -> Color(0xFFCFD8DC) // Silver
                                            3 -> Color(0xFFFFCC80) // Bronze
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rank) {
                                        1 -> "🥇"
                                        2 -> "🥈"
                                        3 -> "🥉"
                                        else -> "$rank"
                                    },
                                    fontSize = if (rank <= 3) 14.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rank > 3) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Avatar emoji
                            Text(
                                text = when (rank) {
                                    1 -> "👑"
                                    2 -> "⚡"
                                    3 -> "👾"
                                    4 -> "🎮"
                                    5 -> "🚀"
                                    else -> "👤"
                                },
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Username
                            Text(
                                text = if (isPlayerCurrentUser) "$currentUsername (You)" else player.first,
                                fontSize = 13.sp,
                                fontWeight = if (isPlayerCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPlayerCurrentUser) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Score
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = String.format("%,d", player.second),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPlayerCurrentUser) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show full list action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullLeaderboard = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Global Leaderboard",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showFullLeaderboard) {
        FullLeaderboardDialog(
            userSession = userSession,
            selectedTimeframe = selectedTimeframe,
            weeklyBaseList = weeklyBaseList,
            allTimeBaseList = allTimeBaseList,
            onDismiss = { showFullLeaderboard = false }
        )
    }
}

// ============== DIALOG: GLOBAL LEADERBOARD ==============
@Composable
fun FullLeaderboardDialog(
    userSession: UserSession?,
    selectedTimeframe: String,
    weeklyBaseList: List<Pair<String, Int>>,
    allTimeBaseList: List<Pair<String, Int>>,
    onDismiss: () -> Unit
) {
    val currentUserBalance = userSession?.coinBalance ?: 500
    val currentUsername = userSession?.username ?: "Guest Gamer"

    // Construct full 10 player rankings
    val additionalWeekly = listOf(
        Pair("LuckyVibe", 950),
        Pair("BonusMaster", 800),
        Pair("OrangeNinja", 650),
        Pair("RetroKing", 450),
        Pair("GamerDot", 300)
    )

    val additionalAllTime = listOf(
        Pair("LuckyVibe", 1500),
        Pair("BonusMaster", 1250),
        Pair("OrangeNinja", 980),
        Pair("RetroKing", 750),
        Pair("GamerDot", 500)
    )

    val baseList = if (selectedTimeframe == "Weekly") {
        weeklyBaseList + additionalWeekly
    } else {
        allTimeBaseList + additionalAllTime
    }

    // Combine and sort
    val fullList = (baseList + Pair(currentUsername, currentUserBalance))
        .distinctBy { it.first }
        .sortedByDescending { it.second }

    val userRank = fullList.indexOfFirst { it.first == currentUsername } + 1

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Global Leaderboard",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Top 10 Gamers • $selectedTimeframe",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Leaderboard
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(fullList.take(10)) { idx, player ->
                            val isPlayerCurrentUser = player.first == currentUsername
                            val rank = idx + 1

                            // Tier assessment based on score
                            val tier = when {
                                player.second >= 10000 -> "Grandmaster 👑"
                                player.second >= 5000 -> "Diamond 💎"
                                player.second >= 2500 -> "Elite 🌟"
                                player.second >= 1000 -> "Gold 🥇"
                                else -> "Bronze 🥉"
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayerCurrentUser) Color(0xFFFFECE0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isPlayerCurrentUser) BorderStroke(1.dp, Color(0xFFFFB300)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Rank badge
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (rank) {
                                                        1 -> Color(0xFFFFD54F)
                                                        2 -> Color(0xFFCFD8DC)
                                                        3 -> Color(0xFFFFCC80)
                                                        else -> Color.Transparent
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (rank) {
                                                    1 -> "🥇"
                                                    2 -> "🥈"
                                                    3 -> "🥉"
                                                    else -> "#$rank"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = player.first,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isPlayerCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isPlayerCurrentUser) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isPlayerCurrentUser) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE65100))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("YOU", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = tier,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = String.format("%,d", player.second),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPlayerCurrentUser) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer showing current User rank card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF8F00)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOUR POSITION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$currentUsername",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rank #$userRank",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = String.format("%,d Coins", currentUserBalance),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "👑", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============== COMPONENT: DASHBOARD SPIN WHEEL ==============
@Composable
fun DashboardSpinWheelSection(
    viewModel: RewardsViewModel
) {
    val isSpinning by viewModel.isSpinning.collectAsState()
    val spinResultCoins by viewModel.spinResultCoins.collectAsState()

    var rotationDegree by remember { mutableStateOf(0f) }
    val angleState by animateFloatAsState(
        targetValue = rotationDegree,
        animationSpec = if (isSpinning) {
            tween(durationMillis = 2000, easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f))
        } else {
            snap()
        },
        label = "dashboard_spin_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_spin_wheel_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lucky Spin Wheel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Spin daily for free bonus coins!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FREE SPIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spin Wheel Canvas with Arrow Pointer
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Static outer rim
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFE65100),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    // Rim lights
                    val numLights = 12
                    for (i in 0 until numLights) {
                        val lightAngle = (i * 360f / numLights) * Math.PI / 180f
                        val radius = (size.width / 2) - 3.dp.toPx()
                        val lightX = (size.width / 2) + radius * Math.cos(lightAngle).toFloat()
                        val lightY = (size.height / 2) + radius * Math.sin(lightAngle).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) Color.White else Color(0xFFFFD54F),
                            radius = 3.dp.toPx(),
                            center = Offset(lightX, lightY)
                        )
                    }
                }

                // Rotating Inner Sections of Wheel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .rotate(angleState)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val values = listOf("100", "5", "50", "10", "75", "25")
                        val colors = listOf(
                            Color(0xFFFFAB00), Color(0xFFFFE082),
                            Color(0xFFFF6D00), Color(0xFFFFCC80),
                            Color(0xFFD50000), Color(0xFFFF8A80)
                        )
                        val sweepAngle = 360f / values.size

                        for (i in values.indices) {
                            val startAngle = i * sweepAngle
                            drawArc(
                                color = colors[i],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                        }
                    }

                    // Labels placed inside sections
                    val values = listOf("100", "5", "50", "10", "75", "25")
                    values.forEachIndexed { i, valStr ->
                        val drawAngle = (i * 60 + 30)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(drawAngle.toFloat())
                        ) {
                            Text(
                                text = valStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 22.dp)
                            )
                        }
                    }
                }

                // Static Indicator Pointer at top center
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                        .background(Color(0xFFE65100), CircleShape)
                        .padding(2.dp)
                )

                // Central pin core
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, CircleShape)
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    if (!isSpinning) {
                        rotationDegree += 360f * 5 + Random.nextInt(360)
                        viewModel.startSpinWheel()
                    }
                },
                enabled = !isSpinning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dashboard_spin_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF8F00)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isSpinning) "SPINNING WHEEL..." else "TAP TO SPIN WHEEL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Results reveal message with animated entry
            if (spinResultCoins != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎉", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Lucky Drop Win!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "Successfully earned +$spinResultCoins coins",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.dismissSpinResult() }) {
                            Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }
    }
}

// ============== COMPONENT: 7-DAY STREAK DAILY CHECK-IN ==============
@Composable
fun DailyCheckInStreakCard(
    viewModel: RewardsViewModel,
    userSession: UserSession?
) {
    val currentStreak = userSession?.dailyStreak ?: 0
    val lastClaimTime = userSession?.lastDailyBonusTime ?: 0L
    val currentTime = System.currentTimeMillis()
    val difference = currentTime - lastClaimTime
    val dayInMillis = 24 * 60 * 60 * 1000L
    val canClaimToday = lastClaimTime == 0L || difference >= dayInMillis

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_check_in_streak_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFECE0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Check-in Streak",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Daily Check-in Streak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Claim consecutive days for multipliers!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Fire Streak Counter Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFE0B2))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Fire",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$currentStreak Days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7 Days Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in 1..7) {
                    // Decide check-in day state
                    val isClaimed = day <= currentStreak && !canClaimToday
                    val isCurrentClaimable = (day == currentStreak + 1 && canClaimToday) || (currentStreak == 0 && day == 1 && canClaimToday)
                    val isLocked = day > currentStreak + 1 || (day == currentStreak + 1 && !canClaimToday)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Day $day",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentClaimable) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isClaimed -> Color(0xFFFFE0B2) // Checked-off background
                                        isCurrentClaimable -> Color(0xFFFFF3E0) // Glowing ready day
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                                .border(
                                    width = if (isCurrentClaimable) 2.dp else 1.dp,
                                    color = when {
                                        isClaimed -> Color(0xFFFFB300)
                                        isCurrentClaimable -> Color(0xFFE65100)
                                        else -> Color.Transparent
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isClaimed -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Claimed",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                isCurrentClaimable -> {
                                    Text(
                                        text = "+${50 + (day * 10).coerceAtMost(50)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                                else -> {
                                    // Future Day Icon
                                    Icon(
                                        imageVector = Icons.Default.Redeem,
                                        contentDescription = "Locked Bonus",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = { viewModel.claimDailyBonus() },
                enabled = canClaimToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("claim_daily_bonus_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF8F00),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (canClaimToday) Icons.Default.Star else Icons.Default.LockClock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    val buttonText = if (canClaimToday) {
                        "CLAIM TODAY'S REWARD (+${50 + (currentStreak * 10).coerceAtMost(50)} Coins)"
                    } else {
                        val hoursRemaining = ((dayInMillis - difference) / (1000 * 60 * 60)).coerceAtLeast(0)
                        val minsRemaining = (((dayInMillis - difference) % (1000 * 60 * 60)) / (1000 * 60)).coerceAtLeast(0)
                        "CLAIMED TODAY • NEXT IN ${hoursRemaining}h ${minsRemaining}m"
                    }
                    Text(
                        text = buttonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canClaimToday) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ============== DIALOG: DAILY REWARD LAUNCH POPUP ==============
@Composable
fun DailyRewardLaunchDialog(
    viewModel: RewardsViewModel,
    userSession: UserSession?,
    onDismiss: () -> Unit
) {
    val currentStreak = userSession?.dailyStreak ?: 0
    val expectedReward = 50 + ((currentStreak + 1) * 10).coerceAtMost(50)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("daily_reward_launch_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(2.dp, Color(0xFFFFB300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "🎁 Daily Reward Ready!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE65100)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Open your daily gift chest to claim bonus coins!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Elegant Gift Box Illustration Layout
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFE0B2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👑",
                            fontSize = 24.sp,
                            modifier = Modifier.offset(y = 4.dp)
                        )
                        Text(
                            text = "🎁",
                            fontSize = 54.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Streak & Potential Reward Info Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Streak Multiplier",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$currentStreak Days Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD84315)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ready Value",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+$expectedReward Coins",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = {
                        viewModel.claimDailyBonus()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_open_reward_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8F00)
                    )
                ) {
                    Text(
                        text = "OPEN MY GIFT CHEST",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DISMISS FOR NOW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

