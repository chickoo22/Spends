package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseEntity
import com.example.data.model.Category
import com.example.data.model.Currency
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.ExportCsvDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    allExpenses: List<ExpenseEntity>,
    currentCurrency: Currency,
    availableCategories: List<Category>,
    selectedYear: Int = 2026,
    selectedMonth: Int = 0,
    monthTitle: String = "",
    onAddExpense: (amount: Double, category: Category, timestamp: Long, paymentMethod: String, notes: String, isIncome: Boolean) -> Unit,
    onUpdateExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    formatCurrency: (Double) -> String
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableIntStateOf(0) } // 0: Newest, 1: Oldest, 2: Highest Amount, 3: Lowest Amount
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Filter and Sort
    val filteredList = remember(allExpenses, searchQuery, selectedCategoryId, sortOption) {
        val filtered = allExpenses.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true) ||
                    item.categoryName.contains(searchQuery, ignoreCase = true) ||
                    item.paymentMethod.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null || item.categoryId == selectedCategoryId
            matchesQuery && matchesCategory
        }

        when (sortOption) {
            0 -> filtered.sortedByDescending { it.timestamp }
            1 -> filtered.sortedBy { it.timestamp }
            2 -> filtered.sortedByDescending { it.amount }
            3 -> filtered.sortedBy { it.amount }
            else -> filtered.sortedByDescending { it.timestamp }
        }
    }

    // Group by Date header
    val groupedExpenses = remember(filteredList) {
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yestYear = cal.get(Calendar.YEAR)
        val yestDay = cal.get(Calendar.DAY_OF_YEAR)

        val headerFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        filteredList.groupBy { expense ->
            cal.timeInMillis = expense.timestamp
            val expYear = cal.get(Calendar.YEAR)
            val expDay = cal.get(Calendar.DAY_OF_YEAR)

            when {
                expYear == todayYear && expDay == todayDay -> "Today"
                expYear == yestYear && expDay == yestDay -> "Yesterday"
                else -> headerFormat.format(Date(expense.timestamp))
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("transactions_add_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                    Text("Add Expense", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("transactions_screen_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "All Transactions",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredList.size} entries recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export to CSV button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showExportCsvDialog = true }
                                .testTag("transactions_export_csv_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export to CSV",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Export",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Sort button with dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (sortOption) {
                                        0 -> "Newest"
                                        1 -> "Oldest"
                                        2 -> "Highest"
                                        else -> "Lowest"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)") },
                                    onClick = { sortOption = 0; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Oldest first)") },
                                    onClick = { sortOption = 1; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Amount (High to Low)") },
                                    onClick = { sortOption = 2; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Amount (Low to High)") },
                                    onClick = { sortOption = 3; showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by merchant, note, or category...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_field"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Categories Filter row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAllSelected = selectedCategoryId == null
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCategoryId = null }
                        ) {
                            Text(
                                text = "All (${allExpenses.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(availableCategories, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(cat.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedCategoryId = if (isSelected) null else cat.id
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color(cat.colorHex))
                                )
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Grouped items
            if (filteredList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try modifying your search or filter criteria",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                groupedExpenses.forEach { (dateHeader, itemsUnderDate) ->
                    item(key = "header_$dateHeader") {
                        val dateSum = itemsUnderDate.sumOf { it.amount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatCurrency(dateSum),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(itemsUnderDate, key = { it.id }) { item ->
                        ExpenseItemCard(
                            expense = item,
                            formatCurrency = formatCurrency,
                            onEdit = { expenseToEdit = it },
                            onDelete = { onDeleteExpense(it) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditExpenseDialog(
            existingExpense = null,
            currency = currentCurrency,
            availableCategories = availableCategories,
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, timestamp, paymentMethod, notes, isIncome ->
                onAddExpense(amount, category, timestamp, paymentMethod, notes, isIncome)
            }
        )
    }

    if (expenseToEdit != null) {
        AddEditExpenseDialog(
            existingExpense = expenseToEdit,
            currency = currentCurrency,
            availableCategories = availableCategories,
            onDismiss = { expenseToEdit = null },
            onSave = { amount, category, timestamp, paymentMethod, notes, isIncome ->
                val updated = expenseToEdit!!.copy(
                    title = category.name,
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.iconKey,
                    categoryColor = category.colorHex,
                    timestamp = timestamp,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    isIncome = isIncome
                )
                onUpdateExpense(updated)
                expenseToEdit = null
            },
            onDelete = {
                onDeleteExpense(it)
                expenseToEdit = null
            }
        )
    }

    if (showExportCsvDialog) {
        ExportCsvDialog(
            allExpenses = filteredList,
            currentCurrency = currentCurrency,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            monthTitle = monthTitle,
            onDismiss = { showExportCsvDialog = false },
            formatCurrency = formatCurrency
        )
    }
}
