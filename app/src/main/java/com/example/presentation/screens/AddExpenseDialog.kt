package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.Expense
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    initialExpense: Expense? = null, // If provided, acts as an edit dialog
    onDismissRequest: () -> Unit,
    onSaveClick: (Expense) -> Unit
) {
    var amountStr by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(initialExpense?.merchant ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Food") }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: "UPI") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var isCredit by remember { mutableStateOf(initialExpense?.isCredit ?: false) }

    var amountError by remember { mutableStateOf(false) }
    var merchantError by remember { mutableStateOf(false) }

    val categoriesList = listOf("Food", "Shopping", "Bills", "Travel", "Groceries", "Entertainment", "Health", "Other")
    val paymentsList = listOf("UPI", "Card", "Cash", "Netbanking", "ATM")

    var expandedPayment by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .testTag("add_expense_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialExpense == null) "Add Transaction" else "Edit Transaction",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("add_expense_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Window")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type Selection Tab (Expense vs. Refund)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = !isCredit,
                            onClick = { isCredit = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Expense (Debit)")
                        }
                        SegmentedButton(
                            selected = isCredit,
                            onClick = { isCredit = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Income / Refund")
                        }
                    }

                    // Amount Text Input
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            if (amountError) amountError = false
                        },
                        label = { Text("Amount") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input")
                    )
                    if (amountError) {
                        Text(
                            "Please enter a valid amount greater than 0.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Merchant / Payee Text Input
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = {
                            merchant = it
                            if (merchantError) merchantError = false
                        },
                        label = { Text(if (isCredit) "Source / Sender" else "Merchant / Recipient") },
                        placeholder = { Text(if (isCredit) "Salary, Friend, Cash" else "Zomato, Amazon, Uber") },
                        isError = merchantError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_merchant_input")
                    )
                    if (merchantError) {
                        Text(
                            "Source/Merchant name cannot be empty.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Category section with chips layout
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Select Category",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoriesList.forEach { catName ->
                                FilterChip(
                                    selected = category == catName,
                                    onClick = { category = catName },
                                    label = { Text(catName) },
                                    modifier = Modifier.testTag("category_chip_$catName")
                                )
                            }
                        }
                    }

                    // Payment Method selector
                    ExposedDropdownMenuBox(
                        expanded = expandedPayment,
                        onExpandedChange = { expandedPayment = it }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayment) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("payment_method_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPayment,
                            onDismissRequest = { expandedPayment = false }
                        ) {
                            paymentsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        paymentMethod = option
                                        expandedPayment = false
                                    },
                                    modifier = Modifier.testTag("payment_option_$option")
                                )
                            }
                        }
                    }

                    // Additional Notes Notes text input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Add any extra details...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_notes_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("cancel_expense_btn")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull()
                            val isAmtValid = amt != null && amt > 0.0
                            val isMerchValid = merchant.trim().isNotEmpty()

                            amountError = !isAmtValid
                            merchantError = !isMerchValid

                            if (isAmtValid && isMerchValid) {
                                val expenseEntity = Expense(
                                    id = initialExpense?.id ?: 0,
                                    amount = amt!!,
                                    category = category,
                                    merchant = merchant.trim(),
                                    paymentMethod = paymentMethod,
                                    date = initialExpense?.date ?: System.currentTimeMillis(),
                                    notes = notes.trim(),
                                    isCredit = isCredit,
                                    accountLast4 = initialExpense?.accountLast4
                                )
                                onSaveClick(expenseEntity)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_expense_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
