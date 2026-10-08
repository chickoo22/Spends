package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExpenseEntity
import com.example.data.local.RecurringExpenseEntity
import com.example.data.model.AvailableCurrencies
import com.example.data.model.Category
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.CategorySpending
import com.example.data.model.Currency
import com.example.data.model.DailySpending
import com.example.data.model.MonthlyAnalytics
import com.example.data.model.MonthlyTrendPoint
import com.example.data.model.OverallBudgetSummary
import com.example.data.repository.ExpenseRepository
import com.example.util.ParsedExpense
import com.example.util.SmartExpenseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    // Onboarding and Currency
    private val _isOnboarded = MutableStateFlow<Boolean?>(null)
    val isOnboarded: StateFlow<Boolean?> = _isOnboarded.asStateFlow()

    private val _currentCurrency = MutableStateFlow(AvailableCurrencies.default)
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    // Custom categories
    val customCategories: StateFlow<List<Category>> = repository.customCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Selector: Defaults to current month & year
    private val calendarInstance = Calendar.getInstance()
    val selectedYear = MutableStateFlow(calendarInstance.get(Calendar.YEAR))
    val selectedMonth = MutableStateFlow(calendarInstance.get(Calendar.MONTH)) // 0 to 11

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)

    // All expenses stream from Room
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budgets stream from Room
    val allBudgets = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recurring expenses stream from Room
    val allRecurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // WhatsApp shared expense parsing state
    private val _sharedParsedExpense = MutableStateFlow<ParsedExpense?>(null)
    val sharedParsedExpense: StateFlow<ParsedExpense?> = _sharedParsedExpense.asStateFlow()

    fun onWhatsAppTextShared(text: String) {
        val categories = Category.defaultCategories + customCategories.value
        val parsed = SmartExpenseParser.parse(text, categories)
        _sharedParsedExpense.value = parsed
    }

    fun dismissSharedExpense() {
        _sharedParsedExpense.value = null
    }

    // Filtered expenses based on search and category
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        selectedYear,
        selectedMonth,
        searchQuery,
        selectedCategoryFilter
    ) { expenses, year, month, query, categoryFilter ->
        val cal = Calendar.getInstance()
        expenses.filter { item ->
            cal.timeInMillis = item.timestamp
            val matchesMonth = cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true)
            val matchesCategory = categoryFilter == null || item.categoryId == categoryFilter
            matchesMonth && matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Analytics (charts, breakdown, totals)
    val monthlyAnalytics: StateFlow<MonthlyAnalytics> = combine(
        allExpenses,
        selectedYear,
        selectedMonth
    ) { expenses, year, month ->
        calculateMonthlyAnalytics(expenses, year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        calculateMonthlyAnalytics(emptyList(), calendarInstance.get(Calendar.YEAR), calendarInstance.get(Calendar.MONTH))
    )

    // Category Budgets Progress for selected month
    val categoryBudgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(
        allExpenses,
        allBudgets,
        customCategories,
        selectedYear,
        selectedMonth
    ) { expenses, budgets, customCats, year, month ->
        calculateBudgetProgress(expenses, budgets, customCats, year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Budget Summary
    val overallBudgetSummary: StateFlow<OverallBudgetSummary> = combine(
        categoryBudgetProgressList,
        selectedYear,
        selectedMonth
    ) { budgetList, year, month ->
        calculateOverallBudgetSummary(budgetList, year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        calculateOverallBudgetSummary(emptyList(), calendarInstance.get(Calendar.YEAR), calendarInstance.get(Calendar.MONTH))
    )

    init {
        viewModelScope.launch {
            try {
                repository.processDueRecurringExpenses()
            } catch (e: Exception) {
                // Ignore or log
            }

            // Check onboarding state
            val onboarded = repository.isOnboardedDirect()
            _isOnboarded.value = onboarded

            // Load saved currency
            val savedCurrencyPair = repository.getCurrencyDirect()
            if (savedCurrencyPair != null) {
                _currentCurrency.value = AvailableCurrencies.findByCode(savedCurrencyPair.first)
            }
        }
    }

    fun completeOnboardingWithCurrency(currency: Currency, seedDemoData: Boolean = true) {
        viewModelScope.launch {
            repository.saveCurrency(currency)
            _currentCurrency.value = currency
            _isOnboarded.value = true
            if (seedDemoData) {
                repository.seedStarterExpensesIfEmpty()
            }
        }
    }

    fun updateCurrency(currency: Currency) {
        viewModelScope.launch {
            repository.saveCurrency(currency)
            _currentCurrency.value = currency
        }
    }

    fun nextMonth() {
        if (selectedMonth.value == Calendar.DECEMBER) {
            selectedMonth.value = Calendar.JANUARY
            selectedYear.value += 1
        } else {
            selectedMonth.value += 1
        }
    }

    fun prevMonth() {
        if (selectedMonth.value == Calendar.JANUARY) {
            selectedMonth.value = Calendar.DECEMBER
            selectedYear.value -= 1
        } else {
            selectedMonth.value -= 1
        }
    }

    fun setMonth(year: Int, month: Int) {
        selectedYear.value = year
        selectedMonth.value = month
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance()
        selectedYear.value = now.get(Calendar.YEAR)
        selectedMonth.value = now.get(Calendar.MONTH)
    }

    // Expense/Income CRUD
    fun addExpense(
        title: String,
        amount: Double,
        category: Category,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "UPI",
        notes: String = "",
        isIncome: Boolean = false
    ) {
        viewModelScope.launch {
            val entity = ExpenseEntity(
                title = title.ifBlank { category.name },
                amount = amount,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.iconKey,
                categoryColor = category.colorHex,
                timestamp = timestamp,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                isIncome = isIncome
            )
            repository.insertExpense(entity)
        }
    }

    fun addExpense(
        amount: Double,
        category: Category,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "UPI",
        notes: String = "",
        isIncome: Boolean = false
    ) {
        addExpense(category.name, amount, category, timestamp, paymentMethod, notes, isIncome)
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    // Recurring CRUD
    fun addRecurringExpense(
        title: String,
        amount: Double,
        category: Category,
        frequency: String,
        paymentMethod: String,
        notes: String,
        isIncome: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = RecurringExpenseEntity(
                title = title.ifBlank { category.name },
                amount = amount,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.iconKey,
                categoryColor = category.colorHex,
                frequency = frequency,
                paymentMethod = paymentMethod,
                notes = notes,
                isIncome = isIncome,
                lastGeneratedTimestamp = now,
                nextDueDate = now,
                isActive = true
            )
            repository.insertRecurring(entity)
            repository.processDueRecurringExpenses()
        }
    }

    fun updateRecurringExpense(recurring: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurring(recurring)
        }
    }

    fun deleteRecurringExpense(recurring: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    // Budget CRUD
    fun setCategoryBudget(categoryId: String, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.setCategoryBudget(categoryId, monthlyLimit)
        }
    }

    fun removeCategoryBudget(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(categoryId)
        }
    }

    fun addCustomCategory(name: String, iconKey: String, colorHex: Long) {
        viewModelScope.launch {
            val id = "custom_" + System.currentTimeMillis()
            val category = Category(id = id, name = name, iconKey = iconKey, colorHex = colorHex, isCustom = true)
            repository.addCustomCategory(category)
        }
    }

    fun seedDemoTransactions() {
        viewModelScope.launch {
            repository.seedStarterExpensesIfEmpty()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllExpenses()
            repository.clearAllBudgets()
        }
    }

    fun formatCurrency(amount: Double): String {
        val currency = _currentCurrency.value
        val formatter = DecimalFormat("#,##0.00")
        val formattedNumber = formatter.format(amount)
        return when (currency.code) {
            "USD", "CAD", "AUD", "SGD", "NZD", "MXN", "CLP", "COP", "ARS" -> "${currency.symbol}$formattedNumber"
            "EUR" -> "$formattedNumber ${currency.symbol}"
            "GBP", "INR", "JPY", "CNY", "KRW", "ILS", "RUB", "BRL", "TRY" -> "${currency.symbol}$formattedNumber"
            else -> "${currency.symbol} $formattedNumber"
        }
    }

    internal fun calculateBudgetProgress(
        allExpenses: List<ExpenseEntity>,
        budgets: List<com.example.data.local.CategoryBudgetEntity>,
        customCategories: List<Category>,
        year: Int,
        month: Int
    ): List<CategoryBudgetProgress> {
        val cal = Calendar.getInstance()
        val allAvailableCategories = (Category.defaultCategories + customCategories).distinctBy { it.id }

        // Filter expenses for selected month
        val currentMonthExpenses = allExpenses.filter { item ->
            cal.timeInMillis = item.timestamp
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }

        val spentByCategory = currentMonthExpenses.groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        val budgetMap = budgets.associateBy { it.categoryId }

        return allAvailableCategories.map { cat ->
            val budget = budgetMap[cat.id]
            val hasBudget = budget != null
            val limit = budget?.monthlyLimit ?: 0.0
            val spent = spentByCategory[cat.id] ?: 0.0
            val remaining = if (hasBudget) limit - spent else 0.0
            val pct = if (hasBudget && limit > 0) (spent / limit).toFloat() else 0f
            val isExceeded = hasBudget && spent > limit
            val isNearLimit = hasBudget && !isExceeded && pct >= 0.80f

            CategoryBudgetProgress(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryIconKey = cat.iconKey,
                colorHex = cat.colorHex,
                monthlyLimit = limit,
                spentAmount = spent,
                remainingAmount = remaining,
                progressPercentage = pct,
                isExceeded = isExceeded,
                isNearLimit = isNearLimit,
                hasBudget = hasBudget
            )
        }.sortedWith(
            compareByDescending<CategoryBudgetProgress> { it.hasBudget }
                .thenByDescending { it.isExceeded }
                .thenByDescending { it.progressPercentage }
        )
    }

    internal fun calculateOverallBudgetSummary(
        budgetList: List<CategoryBudgetProgress>,
        year: Int,
        month: Int
    ): OverallBudgetSummary {
        val budgeted = budgetList.filter { it.hasBudget }
        val totalBudget = budgeted.sumOf { it.monthlyLimit }
        val totalSpent = budgeted.sumOf { it.spentAmount }
        val totalRemaining = totalBudget - totalSpent
        val overallPct = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f

        val exceededCount = budgeted.count { it.isExceeded }
        val warningCount = budgeted.count { it.isNearLimit }
        val onTrackCount = budgeted.count { !it.isExceeded && !it.isNearLimit }

        // Days remaining calculation
        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH)
        val nowDay = cal.get(Calendar.DAY_OF_MONTH)

        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val daysRemaining = when {
            year < nowYear || (year == nowYear && month < nowMonth) -> 0
            year == nowYear && month == nowMonth -> (daysInMonth - nowDay + 1).coerceAtLeast(1)
            else -> daysInMonth
        }

        val dailyBudgetRemaining = if (daysRemaining > 0 && totalRemaining > 0) {
            totalRemaining / daysRemaining
        } else 0.0

        return OverallBudgetSummary(
            totalBudget = totalBudget,
            totalSpentInBudgetedCategories = totalSpent,
            totalRemaining = totalRemaining,
            overallPercentage = overallPct,
            budgetedCategoryCount = budgeted.size,
            exceededCount = exceededCount,
            warningCount = warningCount,
            onTrackCount = onTrackCount,
            daysRemainingInMonth = daysRemaining,
            dailyBudgetRemaining = dailyBudgetRemaining
        )
    }

    internal fun calculateMonthlyAnalytics(
        allExpenses: List<ExpenseEntity>,
        year: Int,
        month: Int
    ): MonthlyAnalytics {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthTitle = monthFormat.format(cal.time)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Filter for current month
        val currentMonthExpenses = allExpenses.filter { item ->
            cal.timeInMillis = item.timestamp
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }

        // Filter for previous month
        val prevCal = Calendar.getInstance()
        prevCal.set(Calendar.YEAR, year)
        prevCal.set(Calendar.MONTH, month)
        prevCal.add(Calendar.MONTH, -1)
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        val prevMonthExpenses = allExpenses.filter { item ->
            cal.timeInMillis = item.timestamp
            cal.get(Calendar.YEAR) == prevYear && cal.get(Calendar.MONTH) == prevMonth
        }

        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        val prevMonthTotal = prevMonthExpenses.sumOf { it.amount }

        val percentChange: Double? = if (prevMonthTotal > 0.0) {
            ((totalSpent - prevMonthTotal) / prevMonthTotal) * 100.0
        } else null

        val transactionCount = currentMonthExpenses.size
        val dailyAverage = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

        // Daily spending map
        val dailyMap = mutableMapOf<Int, Double>()
        for (d in 1..daysInMonth) {
            dailyMap[d] = 0.0
        }
        for (item in currentMonthExpenses) {
            cal.timeInMillis = item.timestamp
            val d = cal.get(Calendar.DAY_OF_MONTH)
            dailyMap[d] = (dailyMap[d] ?: 0.0) + item.amount
        }

        var highestDay = 1
        var highestAmount = 0.0
        dailyMap.forEach { (d, amt) ->
            if (amt > highestAmount) {
                highestAmount = amt
                highestDay = d
            }
        }

        val dayOfWeekFormat = SimpleDateFormat("EE", Locale.getDefault())
        val dailySpendings = (1..daysInMonth).map { day ->
            cal.set(Calendar.DAY_OF_MONTH, day)
            DailySpending(
                dayOfMonth = day,
                dayOfWeekLabel = dayOfWeekFormat.format(cal.time),
                totalAmount = dailyMap[day] ?: 0.0,
                isHighest = (day == highestDay && highestAmount > 0.0)
            )
        }

        // Category breakdown
        val categoryGroup = currentMonthExpenses.groupBy { it.categoryId }
        val categoryBreakdowns = categoryGroup.map { (catId, items) ->
            val catTotal = items.sumOf { it.amount }
            val pct = if (totalSpent > 0.0) ((catTotal / totalSpent) * 100.0).toFloat() else 0f
            val first = items.first()
            CategorySpending(
                categoryId = catId,
                categoryName = first.categoryName,
                categoryIconKey = first.categoryIcon,
                colorHex = first.categoryColor,
                totalAmount = catTotal,
                percentage = pct,
                count = items.size
            )
        }.sortedByDescending { it.totalAmount }

        // Multi-month trend (last 6 months up to selected month)
        val trends = mutableListOf<MonthlyTrendPoint>()
        val trendCal = Calendar.getInstance()
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        for (i in 5 downTo 0) {
            trendCal.set(Calendar.YEAR, year)
            trendCal.set(Calendar.MONTH, month)
            trendCal.set(Calendar.DAY_OF_MONTH, 1)
            trendCal.add(Calendar.MONTH, -i)

            val tYear = trendCal.get(Calendar.YEAR)
            val tMonth = trendCal.get(Calendar.MONTH)
            val label = shortMonthFormat.format(trendCal.time)

            val mTotal = allExpenses.filter { item ->
                cal.timeInMillis = item.timestamp
                cal.get(Calendar.YEAR) == tYear && cal.get(Calendar.MONTH) == tMonth
            }.sumOf { it.amount }

            trends.add(
                MonthlyTrendPoint(
                    year = tYear,
                    month = tMonth,
                    label = label,
                    totalSpent = mTotal
                )
            )
        }

        return MonthlyAnalytics(
            year = year,
            month = month,
            monthTitle = monthTitle,
            totalSpent = totalSpent,
            transactionCount = transactionCount,
            dailyAverage = dailyAverage,
            highestDay = highestDay,
            highestDayAmount = highestAmount,
            prevMonthTotal = prevMonthTotal,
            percentChangeFromPrev = percentChange,
            categoryBreakdowns = categoryBreakdowns,
            dailySpendings = dailySpendings,
            multiMonthTrends = trends
        )
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
