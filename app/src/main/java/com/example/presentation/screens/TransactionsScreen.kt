package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Expense
import com.example.presentation.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onEditExpenseClick: (Expense) -> Unit
) {
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPayment by viewModel.selectedPaymentFilter.collectAsState()
    val startDateFilter by viewModel.startDateFilter.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf<Expense?>(null) }

    val categoriesList = listOf("Food", "Shopping", "Bills", "Travel", "Groceries", "Entertainment", "Health", "Other")
    val paymentsList = listOf("UPI", "Card", "Cash", "Netbanking", "ATM")

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                actions = {
                    if (searchQuery.isNotEmpty() || selectedCategory != null || selectedPayment != null || startDateFilter != null) {
                        IconButton(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier.testTag("clear_all_filters_btn")
                        ) {
                            Icon(Icons.Default.FilterListOff, contentDescription = "Clear Filters")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by merchant or note...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_transactions_input")
            )

            // Dynamic filter options row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Category filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("All Categories") },
                        modifier = Modifier.testTag("filter_cat_all")
                    )
                    categoriesList.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setCategoryFilter(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("filter_cat_$cat")
                        )
                    }
                }

                // Payment Method filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPayment == null,
                        onClick = { viewModel.setPaymentFilter(null) },
                        label = { Text("All Payments") },
                        modifier = Modifier.testTag("filter_pay_all")
                    )
                    paymentsList.forEach { p ->
                        FilterChip(
                            selected = selectedPayment == p,
                            onClick = { viewModel.setPaymentFilter(if (selectedPayment == p) null else p) },
                            label = { Text(p) },
                            modifier = Modifier.testTag("filter_pay_$p")
                        )
                    }
                }

                // Date Time Quick filter options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeDateFilter = when (startDateFilter) {
                        null -> 0
                        else -> {
                            val todayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis
                            val thisWeekStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                            }.timeInMillis
                            val thisMonthStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                            }.timeInMillis

                            when (startDateFilter) {
                                todayStart -> 1
                                thisWeekStart -> 2
                                thisMonthStart -> 3
                                else -> 0
                            }
                        }
                    }

                    FilterChip(
                        selected = activeDateFilter == 0,
                        onClick = { viewModel.setDateRangeFilter(null, null) },
                        label = { Text("All Time") },
                        modifier = Modifier.testTag("filter_date_all")
                    )

                    FilterChip(
                        selected = activeDateFilter == 1,
                        onClick = {
                            val target = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            viewModel.setDateRangeFilter(target, null)
                        },
                        label = { Text("Today") },
                        modifier = Modifier.testTag("filter_date_today")
                    )

                    FilterChip(
                        selected = activeDateFilter == 2,
                        onClick = {
                            val target = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            viewModel.setDateRangeFilter(target, null)
                        },
                        label = { Text("This Week") },
                        modifier = Modifier.testTag("filter_date_week")
                    )

                    FilterChip(
                        selected = activeDateFilter == 3,
                        onClick = {
                            val target = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            viewModel.setDateRangeFilter(target, null)
                        },
                        label = { Text("This Month") },
                        modifier = Modifier.testTag("filter_date_month")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lazy List View for Results content
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "Empty State",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No matching transactions found.",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        filteredExpenses,
                        key = { it.id }
                    ) { expense ->
                        // Swipe to dismiss or long click standard click
                        TransactionItemWithLongPress(
                            expense = expense,
                            onClick = { onEditExpenseClick(expense) },
                            onLongClick = { showDeleteConfirmDialog = expense },
                            currencyFormatter = currencyFormatter
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // Modal Confirmation Dialog
    showDeleteConfirmDialog?.let { expense ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this expense of ${currencyFormatter.format(expense.amount)} spent at ${expense.merchant}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("delete_confirm_ok_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_txn_dialog")
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemWithLongPress(
    expense: Expense,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("expense_item_full_card_${expense.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (expense.isCredit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category, expense.isCredit),
                        contentDescription = "Category Symbol",
                        tint = if (expense.isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        expense.merchant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            expense.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            expense.paymentMethod,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (!expense.notes.isNullOrBlank()) {
                            Text(
                                "•",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                expense.notes,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (expense.isCredit) "+" else "-"}${currencyFormatter.format(expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (expense.isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    sdf.format(Date(expense.date)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
