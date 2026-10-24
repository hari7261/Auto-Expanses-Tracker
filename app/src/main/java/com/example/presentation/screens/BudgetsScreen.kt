package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Budget
import com.example.presentation.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: ExpenseViewModel) {
    val budgets by viewModel.budgets.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val globalBudgetVal by viewModel.globalBudget.collectAsState()

    var showEditBudgetDialog by remember { mutableStateOf<Pair<String, Double?>?>(null) } // category to its budget limit amount

    val categoriesList = listOf("Food", "Shopping", "Bills", "Travel", "Groceries", "Entertainment", "Health", "Other")

    // Date range of current month
    val calendar = Calendar.getInstance()
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val monthlySpentTotal = expenses.filter { it.date >= monthStart && !it.isCredit }.sumOf { it.amount }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card showing overall monthly budget progress
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_budget_planner_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Overall Monthly Limit",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    currencyFormatter.format(globalBudgetVal),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = { showEditBudgetDialog = Pair("All", globalBudgetVal) },
                                modifier = Modifier.testTag("edit_global_budget_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Overall Budget")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adjust")
                            }
                        }

                        val overallProgress = if (globalBudgetVal > 0) (monthlySpentTotal / globalBudgetVal).toFloat() else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Spent: ${currencyFormatter.format(monthlySpentTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${(overallProgress * 100).toInt()}% of budget spent",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overallProgress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = overallProgress.coerceAtMost(1f),
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                color = if (overallProgress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                            )
                        }

                        if (overallProgress > 0.82f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Over Limit Alert! Limit warning threshold crossed.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Category Limits label row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Category Limits",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Configure visual alerts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Setup List
            val currentMonthCategorySpent = expenses
                .filter { it.date >= monthStart && !it.isCredit }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { exp -> exp.amount } }

            items(categoriesList) { category ->
                val categorySpentVal = currentMonthCategorySpent[category] ?: 0.0
                val budgetObject = budgets.find { it.category == category }
                val targetLimit = budgetObject?.monthlyLimit

                CategoryBudgetCard(
                    categoryName = category,
                    spentAmount = categorySpentVal,
                    limitAmount = targetLimit,
                    currencyFormatter = currencyFormatter,
                    onEditClick = {
                        showEditBudgetDialog = Pair(category, targetLimit)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(44.dp))
            }
        }
    }

    // Modal budget editor dialog limits
    showEditBudgetDialog?.let { (category, currentLimit) ->
        var budgetInputStr by remember { mutableStateOf(currentLimit?.toString() ?: "") }
        var isInputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = null },
            title = { Text(if (category == "All") "Overall Monthly Budget" else "$category Category Budget") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Enter monthly spending limit constraints:", fontSize = 14.sp)
                    OutlinedTextField(
                        value = budgetInputStr,
                        onValueChange = {
                            budgetInputStr = it
                            if (isInputError) isInputError = false
                        },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Monthly Limit") },
                        isError = isInputError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_editor_field")
                    )
                    if (isInputError) {
                        Text(
                            "Please specify a valid numeric amount greater than zero.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputNum = budgetInputStr.toDoubleOrNull()
                        if (inputNum == null || inputNum <= 0) {
                            isInputError = true
                        } else {
                            if (category == "All") {
                                viewModel.updateGlobalBudget(inputNum)
                            } else {
                                viewModel.addOrUpdateBudget(Budget(category = category, monthlyLimit = inputNum, alertPercent = 0.8f))
                            }
                            showEditBudgetDialog = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_budget_editor_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("budget_adjuster_dialog")
        )
    }
}

@Composable
fun CategoryBudgetCard(
    categoryName: String,
    spentAmount: Double,
    limitAmount: Double?,
    currencyFormatter: NumberFormat,
    onEditClick: () -> Unit
) {
    val progressRatio = if (limitAmount != null && limitAmount > 0) (spentAmount / limitAmount).toFloat() else null
    val isNearLimit = progressRatio != null && progressRatio > 0.8f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_budget_card_$categoryName"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isNearLimit) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isNearLimit) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(categoryName),
                            contentDescription = categoryName,
                            tint = if (isNearLimit) Color(0xFFC62828) else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        categoryName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("modify_limit_btn_$categoryName")
                ) {
                    Icon(
                        if (limitAmount == null) Icons.Default.AddCircleOutline else Icons.Default.Edit,
                        contentDescription = "Edit Category budget limit",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        currencyFormatter.format(spentAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isNearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly Limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        limitAmount?.let { currencyFormatter.format(it) } ?: "Not Set",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (progressRatio != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = progressRatio.coerceAtMost(1f),
                        color = if (isNearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(progressRatio * 100).toInt()}% utilized",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isNearLimit) {
                            Text(
                                "Nearing limit warning!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
