package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entities.Expense
import com.example.data.local.entities.NotificationSuggestion
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.ExpenseViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ExpenseViewModel = viewModel()
            val activeTheme by viewModel.themePreference.collectAsState()

            val isDarkTheme = when (activeTheme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        AppleSplashScreen(onAnimationFinished = { showSplash = false })
                    } else {
                        val biometricEnabled by viewModel.isBiometricLockEnabled.collectAsState()
                        var isAppUnlocked by remember { mutableStateOf(false) }

                        if (biometricEnabled && !isAppUnlocked) {
                            BiometricLockScreen(
                                onUnlockSuccess = { isAppUnlocked = true }
                            )
                        } else {
                            MainScreenContainer(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(onUnlockSuccess: () -> Unit) {
    var pinText by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf("Enter Security PIN to View Expenses") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Private Expenses locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                pinMessage,
                fontSize = 14.sp,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Pin entries bullets
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                (1..4).forEach { index ->
                    val filled = pinText.length >= index
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }
        }

        // Sim lock keyboard
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "Unlock")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                isError = false
                                when (key) {
                                    "Clear" -> {
                                        if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                    }
                                    "Unlock" -> {
                                        // Auto unlocked for demo or if pin equals 1234
                                        if (pinText == "1234" || pinText.isEmpty()) {
                                            onUnlockSuccess()
                                        } else {
                                            pinText = ""
                                            pinMessage = "Wrong PIN! (Default is digits Empty or 1234)"
                                            isError = true
                                        }
                                    }
                                    else -> {
                                        if (pinText.length < 4) {
                                            pinText += key
                                            if (pinText.length == 4) {
                                                if (pinText == "1234" || pinText == "0000") {
                                                    onUnlockSuccess()
                                                } else {
                                                    pinText = ""
                                                    pinMessage = "Incorrect code format. Try 1234"
                                                    isError = true
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("pin_btn_$key"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "Unlock") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (key == "Unlock") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(key, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenContainer(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val suggestions by viewModel.pendingSuggestions.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingSuggestion by remember { mutableStateOf<NotificationSuggestion?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_nav")
            ) {
                // Dashboard
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                // Transactions
                NavigationBarItem(
                    selected = currentRoute == "transactions",
                    onClick = {
                        navController.navigate("transactions") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "Transactions") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_item_transactions")
                )

                // Suggestions with Dynamic alerts Badgify tag
                NavigationBarItem(
                    selected = currentRoute == "suggestions",
                    onClick = {
                        navController.navigate("suggestions") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (suggestions.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text(suggestions.size.toString()) } }) {
                                Icon(Icons.Default.Sms, contentDescription = "Suggestions")
                            }
                        } else {
                            Icon(Icons.Default.Sms, contentDescription = "Suggestions")
                        }
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("nav_item_suggestions")
                )

                // Budgets
                NavigationBarItem(
                    selected = currentRoute == "budgets",
                    onClick = {
                        navController.navigate("budgets") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Budgets") },
                    label = { Text("Budget") },
                    modifier = Modifier.testTag("nav_item_budgets")
                )

                // Insights
                NavigationBarItem(
                    selected = currentRoute == "insights",
                    onClick = {
                        navController.navigate("insights") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Insights") },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("nav_item_insights")
                )

                // Settings
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "dashboard"
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSuggestions = { navController.navigate("suggestions") },
                        onNavigateToTransactions = { navController.navigate("transactions") },
                        onAddExpenseClick = { showAddExpenseDialog = true },
                        onEditExpenseClick = { expense -> editingExpense = expense }
                    )
                }

                composable("transactions") {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onEditExpenseClick = { expense -> editingExpense = expense }
                    )
                }

                composable("suggestions") {
                    SuggestionsScreen(
                        viewModel = viewModel,
                        onEditAndSaveClick = { suggestion -> editingSuggestion = suggestion }
                    )
                }

                composable("budgets") {
                    BudgetsScreen(viewModel = viewModel)
                }

                composable("insights") {
                    InsightsScreen(viewModel = viewModel)
                }

                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Dialog Trigger: Add / Create Manual Expense
        if (showAddExpenseDialog) {
            AddExpenseDialog(
                initialExpense = null,
                onDismissRequest = { showAddExpenseDialog = false },
                onSaveClick = { expense ->
                    viewModel.addExpense(expense)
                    showAddExpenseDialog = false
                }
            )
        }

        // Dialog Trigger: Edit Existing Manual Expense
        editingExpense?.let { expense ->
            AddExpenseDialog(
                initialExpense = expense,
                onDismissRequest = { editingExpense = null },
                onSaveClick = { updatedExpense ->
                    viewModel.updateExpense(updatedExpense)
                    editingExpense = null
                }
            )
        }

        // Dialog Trigger: Edit Suggestion BEFORE Saving
        editingSuggestion?.let { suggestion ->
            val draftExpense = Expense(
                amount = suggestion.amount,
                category = "Food",
                merchant = suggestion.merchant,
                paymentMethod = suggestion.paymentMethod,
                date = suggestion.timestamp,
                notes = "Approved from payment alert: ${suggestion.sourceApp}",
                isCredit = suggestion.isCredit,
                accountLast4 = suggestion.accountLast4
            )

            AddExpenseDialog(
                initialExpense = draftExpense,
                onDismissRequest = { editingSuggestion = null },
                onSaveClick = { finalizedExpense ->
                    viewModel.approveSuggestion(suggestion.id, finalizedExpense)
                    editingSuggestion = null
                }
            )
        }
    }
}

@Composable
fun AppleSplashScreen(onAnimationFinished: () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Smooth fade in for logo and scale up slightly
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        delay(400)
        
        // Step 2: Smooth fade in for text
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        
        // Solid presentation hold on current state
        delay(1300)
        
        // Step 3: Elegant fade out together to transition to screen
        launch {
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10)) // Solid ElegantDarkBg
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient glowing aura behind the icon
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFD0BCFF), Color.Transparent),
                            center = center,
                            radius = size.width / 2
                        ),
                        alpha = alpha.value * glowAlpha
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            // Elegant Vector Icon Container
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1F1530), Color(0xFF0F0F11))
                        )
                    )
                    .border(1.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f), RoundedCornerShape(32.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Layered padlock and shield
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF).copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
                // Small rupee emblem overlaying the lock for a premium tracker look
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF))
                        .border(1.dp, Color(0xFF1F1530), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F0F10)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "PRIVATE EXPENSES",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 5.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SECURE OFFLINE LEDGER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF909094),
                letterSpacing = 2.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .testTag("splash_subtitle")
            )
        }
    }
}
