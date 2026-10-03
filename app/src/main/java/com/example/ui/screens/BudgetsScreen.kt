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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.Category
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.Currency
import com.example.data.model.OverallBudgetSummary
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.OverallBudgetHeroCard
import com.example.ui.components.SetBudgetDialog

@Composable
fun BudgetsScreen(
    budgetProgressList: List<CategoryBudgetProgress>,
    overallSummary: OverallBudgetSummary,
    availableCategories: List<Category>,
    currentCurrency: Currency,
    selectedYear: Int,
    selectedMonth: Int,
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSetCategoryBudget: (categoryId: String, limit: Double) -> Unit,
    onRemoveCategoryBudget: (categoryId: String) -> Unit,
    formatCurrency: (Double) -> String
) {
    var filterMode by remember { mutableIntStateOf(0) } // 0: All, 1: Over Budget, 2: Near Limit, 3: On Track, 4: No Limit
    var selectedProgressForEdit by remember { mutableStateOf<CategoryBudgetProgress?>(null) }
    var showCategoryPickerForNewBudget by remember { mutableStateOf(false) }

    val filteredList = remember(budgetProgressList, filterMode) {
        when (filterMode) {
            1 -> budgetProgressList.filter { it.isExceeded }
            2 -> budgetProgressList.filter { it.isNearLimit }
            3 -> budgetProgressList.filter { it.hasBudget && !it.isExceeded && !it.isNearLimit }
            4 -> budgetProgressList.filter { !it.hasBudget }
            else -> budgetProgressList
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCategoryPickerForNewBudget = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("set_budget_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Savings, contentDescription = "Set Budget Limit")
                    Text("Set Limit", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("budgets_screen_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Budget Management",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Set monthly spending limits for categories and track your progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Month Selector Header
            item {
                MonthSelectorHeader(
                    currentYear = selectedYear,
                    currentMonth = selectedMonth,
                    monthTitle = monthTitle,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onResetCurrentMonth = onResetCurrentMonth
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Overall Budget Hero Card
            if (overallSummary.budgetedCategoryCount > 0) {
                item {
                    OverallBudgetHeroCard(
                        summary = overallSummary,
                        monthTitle = monthTitle,
                        formatCurrency = formatCurrency
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filterChips = listOf(
                        0 to "All (${budgetProgressList.size})",
                        1 to "Over Budget (${overallSummary.exceededCount})",
                        2 to "Near Limit (${overallSummary.warningCount})",
                        3 to "On Track (${overallSummary.onTrackCount})",
                        4 to "No Budget (${budgetProgressList.count { !it.hasBudget }})"
                    )

                    items(filterChips, key = { it.first }) { (mode, title) ->
                        val isSelected = filterMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { filterMode = mode }
                                .testTag("budget_filter_$mode")
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // List of Category Budgets
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
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No categories in this filter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Switch filter or tap '+ Set Limit' to establish category spending limits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.categoryId }) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        onEditBudget = { selectedProgressForEdit = it },
                        formatCurrency = formatCurrency,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }
        }
    }

    // Set / Edit Budget Dialog for a tapped card
    if (selectedProgressForEdit != null) {
        val cat = Category.findById(selectedProgressForEdit!!.categoryId, availableCategories)
        SetBudgetDialog(
            category = cat,
            currentProgress = selectedProgressForEdit,
            currency = currentCurrency,
            onDismiss = { selectedProgressForEdit = null },
            onSaveBudget = { catId, limit ->
                onSetCategoryBudget(catId, limit)
                selectedProgressForEdit = null
            },
            onRemoveBudget = { catId ->
                onRemoveCategoryBudget(catId)
                selectedProgressForEdit = null
            },
            formatCurrency = formatCurrency
        )
    }

    // Dialog to pick a category when clicking "+ Set Limit" FAB
    if (showCategoryPickerForNewBudget) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCategoryPickerForNewBudget = false },
            title = { Text("Choose Category to Budget") },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(availableCategories, key = { it.id }) { cat ->
                        val existing = budgetProgressList.firstOrNull { it.categoryId == cat.id }
                        val catColor = Color(cat.colorHex)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showCategoryPickerForNewBudget = false
                                    selectedProgressForEdit = existing ?: CategoryBudgetProgress(
                                        categoryId = cat.id,
                                        categoryName = cat.name,
                                        categoryIconKey = cat.iconKey,
                                        colorHex = cat.colorHex,
                                        monthlyLimit = 0.0,
                                        spentAmount = 0.0,
                                        remainingAmount = 0.0,
                                        progressPercentage = 0f,
                                        isExceeded = false,
                                        isNearLimit = false,
                                        hasBudget = false
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(catColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.getIcon(),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (existing?.hasBudget == true) {
                                        Text(
                                            text = "Current limit: ${formatCurrency(existing.monthlyLimit)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "No limit set yet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showCategoryPickerForNewBudget = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
