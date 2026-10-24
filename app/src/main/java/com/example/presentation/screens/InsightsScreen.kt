package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.core.content.FileProvider
import com.example.data.local.entities.Expense
import com.example.presentation.viewmodel.ExpenseViewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class ChartBar(val label: String, val amount: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val context = LocalContext.current

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val redColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)

    var selectedFilter by remember { mutableStateOf("Monthly") } // "Weekly", "Monthly", "Yearly", "Custom"

    // Custom Date Selection State
    val initialStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var customStartDate by remember { mutableStateOf(initialStart) }
    var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    // Computes Active boundaries based on active tabs
    val dateRange = remember(selectedFilter, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        when (selectedFilter) {
            "Weekly" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            "Monthly" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            "Yearly" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -365)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            else -> { // "Custom"
                Pair(customStartDate, customEndDate)
            }
        }
    }

    val startDate = dateRange.first
    val endDate = dateRange.second

    // Apply Filter to current active subset
    val filteredExpenses = remember(expenses, startDate, endDate) {
        expenses.filter { it.date in startDate..endDate }
    }

    val totalExpense = remember(filteredExpenses) {
        filteredExpenses.filter { !it.isCredit }.sumOf { it.amount }
    }
    val totalIncome = remember(filteredExpenses) {
        filteredExpenses.filter { it.isCredit }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    // Generate Dynamic Period Chart Data
    val chartBars = remember(filteredExpenses, selectedFilter, startDate, endDate) {
        val debits = filteredExpenses.filter { !it.isCredit }
        when (selectedFilter) {
            "Weekly" -> {
                val bars = mutableListOf<ChartBar>()
                val daySdf = SimpleDateFormat("E", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                    }
                    val startOfDay = Calendar.getInstance().apply {
                        timeInMillis = dCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = dCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val sum = debits.filter { it.date in startOfDay..endOfDay }.sumOf { it.amount }
                    val label = daySdf.format(dCal.time)
                    bars.add(ChartBar(label, sum))
                }
                bars
            }
            "Monthly" -> {
                // Group by 4 key weekly chunks
                val bars = mutableListOf<ChartBar>()
                val diff = endDate - startDate
                val chunkMs = if (diff > 0) diff / 4 else 1L
                for (i in 0..3) {
                    val wStart = startDate + i * chunkMs
                    val wEnd = startDate + (i + 1) * chunkMs
                    val sum = debits.filter { 
                        if (i == 3) it.date in wStart..wEnd
                        else it.date >= wStart && it.date < wEnd
                    }.sumOf { it.amount }
                    bars.add(ChartBar("Wk ${i + 1}", sum))
                }
                bars
            }
            "Yearly" -> {
                // Return monthly chunks for the last 6 months
                val bars = mutableListOf<ChartBar>()
                val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
                for (i in 5 downTo 0) {
                    val mCal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -i)
                    }
                    val mStart = Calendar.getInstance().apply {
                        timeInMillis = mCal.timeInMillis
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val mEnd = Calendar.getInstance().apply {
                        timeInMillis = mCal.timeInMillis
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val sum = debits.filter { it.date in mStart..mEnd }.sumOf { it.amount }
                    bars.add(ChartBar(monthSdf.format(mCal.time), sum))
                }
                bars
            }
            else -> { // "Custom Date Range"
                val bars = mutableListOf<ChartBar>()
                val diff = endDate - startDate
                val chunkMs = if (diff > 0) diff / 5 else 1L
                val dateSdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                for (i in 0..4) {
                    val start = startDate + i * chunkMs
                    val end = startDate + (i + 1) * chunkMs
                    val sum = debits.filter { 
                        if (i == 4) it.date in start..end
                        else it.date >= start && it.date < end
                    }.sumOf { it.amount }
                    val label = dateSdf.format(Date(start))
                    bars.add(ChartBar(label, sum))
                }
                bars
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    // Quick Action: Export active subset immediately
                    IconButton(
                        onClick = { exportToCsvAndShare(context, filteredExpenses, startDate, endDate) },
                        modifier = Modifier.testTag("action_export_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export active transactions CSV")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Tab Selector for different dynamic ranges
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val filters = listOf("Weekly", "Monthly", "Yearly", "Custom")
                    filters.forEach { filterName ->
                        val active = selectedFilter == filterName
                        Button(
                            onClick = { selectedFilter = filterName },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            elevation = null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("filter_tab_$filterName")
                        ) {
                            Text(
                                text = filterName,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Expanded Sub-Bar showing custom bounds if "Custom" is tapped
            AnimatedVisibility(
                visible = selectedFilter == "Custom",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCustomRangeDialog = true }
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select bounds",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showCustomRangeDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Custom Dates",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Main Contents
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            "Not Enough Activity Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "We couldn't detect any logged transactions during this specified time frame.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                selectedFilter = "Monthly"
                            }
                        ) {
                            Text("Reset to Monthly")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Premium Balanced Multi-Metrics Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Net Balance Section
                            Column {
                                Text(
                                    text = "NET DISPOSABLE SAVINGS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (netBalance >= 0) "+${currencyFormatter.format(netBalance)}" else currencyFormatter.format(netBalance),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netBalance >= 0) greenColor else redColor
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Dual dynamic sub-meters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic debits
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF2D1B1B) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "debited spent".uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.onErrorContainer,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            currencyFormatter.format(totalExpense),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = redColor
                                        )
                                    }
                                }

                                // Dynamic credits
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1B2D1B) else Color(0xFFE8F5E9)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "credited / income".uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFB4EAB2) else Color(0xFF2E7D32).copy(alpha = 0.8f),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            currencyFormatter.format(totalIncome),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = greenColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hand-crafted Custom Period Bar Chart - High Fidelity presentation details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Spending Distribution Trends",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val maxVal = remember(chartBars) {
                                chartBars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartBars.forEachIndexed { index, item ->
                                    val pctHeight = (item.amount / maxVal).toFloat().coerceIn(0.04f, 1.0f)
                                    val isPeak = item.amount == chartBars.maxOfOrNull { it.amount } && item.amount > 0

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        if (item.amount > 0) {
                                            Text(
                                                text = currencyFormatter.format(item.amount),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight(pctHeight * 0.75f)
                                                .width(26.dp)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (isPeak) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Breakdown section with beautiful linear meters
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Category Volume Analysis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val categoryGroups = remember(filteredExpenses) {
                                filteredExpenses.filter { !it.isCredit }.groupBy { it.category }
                            }

                            if (categoryGroups.isEmpty()) {
                                Text(
                                    "No expense entries mapped for this period.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                categoryGroups.entries
                                    .map { Pair(it.key, it.value.sumOf { exp -> exp.amount }) }
                                    .sortedByDescending { it.second }
                                    .forEach { (catName, sumVal) ->
                                        val pct = if (totalExpense > 0) (sumVal / totalExpense).toFloat() else 0f
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = getCategoryIcon(catName),
                                                        contentDescription = catName,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Text(
                                                        catName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Text(
                                                    "${currencyFormatter.format(sumVal)} (${(pct * 100).toInt()}%)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                            }
                        }
                    }

                    // Share or download CSV card block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Export Period Data",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Download or share CSV statement for selected range.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { exportToCsvAndShare(context, filteredExpenses, startDate, endDate) },
                                modifier = Modifier.testTag("btn_export_csv_share")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Secure Share", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal Custom Range Selector Dialog (Uses Sequenced Material Date Pickers for maximum compatibility and polished layout)
    if (showCustomRangeDialog) {
        var startMillisSelected by remember { mutableStateOf(customStartDate) }
        var endMillisSelected by remember { mutableStateOf(customEndDate) }
        var activePicker by remember { mutableStateOf<String?>(null) } // "START" or "END"

        val labelStartSdf = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("Filters by Custom Dates", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Configure start and stop boundaries below to narrow down financial logs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Date Option Button
                        OutlinedButton(
                            onClick = { activePicker = "START" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("From:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    labelStartSdf.format(Date(startMillisSelected)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // End Date Option Button
                        OutlinedButton(
                            onClick = { activePicker = "END" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("To:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    labelStartSdf.format(Date(endMillisSelected)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customStartDate = startMillisSelected
                        customEndDate = endMillisSelected
                        showCustomRangeDialog = false
                    },
                    modifier = Modifier.testTag("confirm_custom_range_btn")
                ) {
                    Text("Apply Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        // Native M3 Date picker implementations
        if (activePicker == "START") {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillisSelected)
            DatePickerDialog(
                onDismissRequest = { activePicker = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                // Set to start of day
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = it
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                startMillisSelected = cal.timeInMillis
                            }
                            activePicker = null
                        }
                    ) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activePicker = null }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (activePicker == "END") {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endMillisSelected)
            DatePickerDialog(
                onDismissRequest = { activePicker = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                // Set to end of day
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = it
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                }
                                endMillisSelected = cal.timeInMillis
                            }
                            activePicker = null
                        }
                    ) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activePicker = null }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

/**
 * Handles CSV content writing, File Generation, Uri acquisition and system Share Sheet delivery.
 */
fun exportToCsvAndShare(context: Context, expenses: List<Expense>, startDate: Long, endDate: Long) {
    try {
        if (expenses.isEmpty()) {
            Toast.makeText(context, "No transactions found to export for this date range.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdfOutput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvHeader = "ID,Amount,Category,Merchant,PaymentMethod,Date,Notes,Type\n"
        val csvBody = StringBuilder().apply { append(csvHeader) }

        for (expense in expenses) {
            val formattedDate = sdfOutput.format(Date(expense.date))
            val typeStr = if (expense.isCredit) "Income" else "Expense"
            val sanitizedNotes = expense.notes.replace(",", ";").replace("\n", " ").trim()
            val sanitizedMerchant = expense.merchant.replace(",", ";").trim()
            csvBody.append("${expense.id},${expense.amount},${expense.category},\"$sanitizedMerchant\",\"${expense.paymentMethod}\",\"$formattedDate\",\"$sanitizedNotes\",$typeStr\n")
        }

        // Writes file directly inside Context Cache space mapped to our external pathways configuration
        val file = File(context.cacheDir, "Statement_Export_${System.currentTimeMillis()}.csv")
        file.writeText(csvBody.toString())

        // Mapped to ${packageName}.fileprovider
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Private Expenses Export Statement")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share/Save Financial Export"))
    } catch (e: Exception) {
        Toast.makeText(context, "CSV generation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
