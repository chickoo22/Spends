package com.example.data.repository

import com.example.data.local.CategoryBudgetDao
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import com.example.data.local.RecurringExpenseDao
import com.example.data.local.RecurringExpenseEntity
import com.example.data.local.UserSettingDao
import com.example.data.local.UserSettingEntity
import com.example.data.model.Category
import com.example.data.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val userSettingDao: UserSettingDao,
    private val categoryDao: CategoryDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {
    companion object {
        const val KEY_CURRENCY_CODE = "currency_code"
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
    }

    // --- Currency & Onboarding Settings ---
    val currencyCodeFlow: Flow<String?> = userSettingDao.getSetting(KEY_CURRENCY_CODE)
    val currencySymbolFlow: Flow<String?> = userSettingDao.getSetting(KEY_CURRENCY_SYMBOL)
    val isOnboardedFlow: Flow<Boolean> = userSettingDao.getSetting(KEY_ONBOARDING_DONE).map {
        it == "true"
    }

    suspend fun saveCurrency(currency: Currency) {
        userSettingDao.setSetting(UserSettingEntity(KEY_CURRENCY_CODE, currency.code))
        userSettingDao.setSetting(UserSettingEntity(KEY_CURRENCY_SYMBOL, currency.symbol))
        userSettingDao.setSetting(UserSettingEntity(KEY_ONBOARDING_DONE, "true"))
    }

    suspend fun getCurrencyDirect(): Pair<String, String>? {
        val code = userSettingDao.getSettingDirect(KEY_CURRENCY_CODE)
        val symbol = userSettingDao.getSettingDirect(KEY_CURRENCY_SYMBOL)
        return if (code != null && symbol != null) Pair(code, symbol) else null
    }

    suspend fun isOnboardedDirect(): Boolean {
        return userSettingDao.getSettingDirect(KEY_ONBOARDING_DONE) == "true"
    }

    // --- Expenses ---
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesInRange(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesInRange(startTime, endTime)
    }

    fun searchExpenses(query: String): Flow<List<ExpenseEntity>> {
        return expenseDao.searchExpenses(query)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun clearAllExpenses() {
        expenseDao.deleteAllExpenses()
    }

    // --- Recurring Expenses ---
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAllRecurring()

    suspend fun insertRecurring(recurring: RecurringExpenseEntity): Long {
        return recurringExpenseDao.insert(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringExpenseEntity) {
        recurringExpenseDao.update(recurring)
    }

    suspend fun deleteRecurring(recurring: RecurringExpenseEntity) {
        recurringExpenseDao.delete(recurring)
    }

    suspend fun processDueRecurringExpenses() {
        val now = System.currentTimeMillis()
        val activeList = recurringExpenseDao.getActiveRecurring()
        for (item in activeList) {
            var dueTime = item.nextDueDate
            // If nextDueDate is in the future, don't generate yet
            if (dueTime > now) continue

            while (dueTime <= now) {
                val expense = ExpenseEntity(
                    title = item.title,
                    amount = item.amount,
                    categoryId = item.categoryId,
                    categoryName = item.categoryName,
                    categoryIcon = item.categoryIcon,
                    categoryColor = item.categoryColor,
                    timestamp = dueTime,
                    paymentMethod = item.paymentMethod,
                    notes = if (item.notes.isNotBlank()) "[Recurring] ${item.notes}" else "[Recurring]",
                    isIncome = item.isIncome
                )
                expenseDao.insertExpense(expense)

                val cal = Calendar.getInstance().apply { timeInMillis = dueTime }
                when (item.frequency.uppercase()) {
                    "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                    else -> cal.add(Calendar.MONTH, 1)
                }
                dueTime = cal.timeInMillis
            }

            val updated = item.copy(
                lastGeneratedTimestamp = now,
                nextDueDate = dueTime
            )
            recurringExpenseDao.update(updated)
        }
    }

    // --- Categories ---
    val customCategories: Flow<List<Category>> = categoryDao.getAllCustomCategories().map { list ->
        list.map {
            Category(
                id = it.id,
                name = it.name,
                iconKey = it.iconKey,
                colorHex = it.colorHex,
                isCustom = it.isCustom
            )
        }
    }

    suspend fun addCustomCategory(category: Category) {
        categoryDao.insertCategory(
            CategoryEntity(
                id = category.id,
                name = category.name,
                iconKey = category.iconKey,
                colorHex = category.colorHex,
                isCustom = true
            )
        )
    }

    suspend fun deleteCustomCategory(id: String) {
        categoryDao.deleteCategory(id)
    }

    // --- Category Budgets ---
    val allBudgets: Flow<List<CategoryBudgetEntity>> = categoryBudgetDao.getAllBudgets()

    suspend fun setCategoryBudget(categoryId: String, monthlyLimit: Double) {
        categoryBudgetDao.setBudget(
            CategoryBudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCategoryBudget(categoryId: String) {
        categoryBudgetDao.deleteBudget(categoryId)
    }

    suspend fun clearAllBudgets() {
        categoryBudgetDao.deleteAllBudgets()
    }

    suspend fun seedStarterExpensesIfEmpty() {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val starter = mutableListOf<ExpenseEntity>()

        fun timeForDay(day: Int, hour: Int = 12): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth)
            val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            c.set(Calendar.DAY_OF_MONTH, if (day > maxDay) maxDay else day)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            return c.timeInMillis
        }

        fun timeForPrevMonth(day: Int): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, currentYear)
            c.set(Calendar.MONTH, currentMonth)
            c.add(Calendar.MONTH, -1)
            val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            c.set(Calendar.DAY_OF_MONTH, if (day > maxDay) maxDay else day)
            return c.timeInMillis
        }

        starter.addAll(
            listOf(
                ExpenseEntity(title = "Monthly Apartment Rent", amount = 850.00, categoryId = "housing", categoryName = "Housing & Rent", categoryIcon = "housing", categoryColor = 0xFF6366F1L, timestamp = timeForDay(1, 9), paymentMethod = "Bank Transfer", notes = "October Rent"),
                ExpenseEntity(title = "Whole Foods Groceries", amount = 84.50, categoryId = "groceries", categoryName = "Groceries", categoryIcon = "groceries", categoryColor = 0xFFF97316L, timestamp = timeForDay(3, 15), paymentMethod = "Card", notes = "Weekly vegetables & fruits"),
                ExpenseEntity(title = "Electric & Water Bill", amount = 95.20, categoryId = "utilities", categoryName = "Utilities", categoryIcon = "utilities", categoryColor = 0xFF06B6D4L, timestamp = timeForDay(5, 10), paymentMethod = "UPI", notes = "Monthly utilities"),
                ExpenseEntity(title = "Team Lunch at Bistro", amount = 42.00, categoryId = "food", categoryName = "Food & Dining", categoryIcon = "restaurant", categoryColor = 0xFFEF4444L, timestamp = timeForDay(7, 13), paymentMethod = "Card", notes = "Client lunch meeting"),
                ExpenseEntity(title = "Uber Ride to Office", amount = 18.50, categoryId = "transport", categoryName = "Transportation", categoryIcon = "transport", categoryColor = 0xFF3B82F6L, timestamp = timeForDay(9, 8), paymentMethod = "UPI", notes = "Morning commute"),
                ExpenseEntity(title = "Spotify & Netflix", amount = 24.98, categoryId = "subscriptions", categoryName = "Subscriptions", categoryIcon = "subscriptions", categoryColor = 0xFF8B5CF6L, timestamp = timeForDay(10, 12), paymentMethod = "Card", notes = "Monthly streaming"),
                ExpenseEntity(title = "Cinema & Popcorn", amount = 35.00, categoryId = "entertainment", categoryName = "Entertainment", categoryIcon = "entertainment", categoryColor = 0xFFF59E0BL, timestamp = timeForDay(12, 19), paymentMethod = "Card", notes = "Movie night"),
                ExpenseEntity(title = "Supermarket Restock", amount = 64.80, categoryId = "groceries", categoryName = "Groceries", categoryIcon = "groceries", categoryColor = 0xFFF97316L, timestamp = timeForDay(14), paymentMethod = "Card", notes = "Produce and dairy"),
                ExpenseEntity(title = "Pharmacy & Vitamins", amount = 28.40, categoryId = "health", categoryName = "Health & Medical", categoryIcon = "health", categoryColor = 0xFF10B981L, timestamp = timeForDay(16), paymentMethod = "Cash", notes = "CVS prescription"),
                ExpenseEntity(title = "New Running Shoes", amount = 110.00, categoryId = "shopping", categoryName = "Shopping", categoryIcon = "shopping", categoryColor = 0xFFEC4899L, timestamp = timeForDay(18), paymentMethod = "Card", notes = "Nike store sale"),
                ExpenseEntity(title = "Coffee & Bagels", amount = 12.50, categoryId = "food", categoryName = "Food & Dining", categoryIcon = "restaurant", categoryColor = 0xFFEF4444L, timestamp = timeForDay(20), paymentMethod = "UPI", notes = "Morning cafe stop"),
                ExpenseEntity(title = "Metro Transit Pass", amount = 30.00, categoryId = "transport", categoryName = "Transportation", categoryIcon = "transport", categoryColor = 0xFF3B82F6L, timestamp = timeForDay(22), paymentMethod = "Card", notes = "Weekly transit pass"),
                ExpenseEntity(title = "Online Course", amount = 49.00, categoryId = "education", categoryName = "Education", categoryIcon = "education", categoryColor = 0xFF06B6D4L, timestamp = timeForDay(24), paymentMethod = "Card", notes = "Kotlin Android Masterclass"),
                ExpenseEntity(title = "Last Month Rent", amount = 850.00, categoryId = "housing", categoryName = "Housing & Rent", categoryIcon = "housing", categoryColor = 0xFF6366F1L, timestamp = timeForPrevMonth(1), paymentMethod = "Bank Transfer", notes = "Previous rent"),
                ExpenseEntity(title = "Grocery Supplies", amount = 195.40, categoryId = "groceries", categoryName = "Groceries", categoryIcon = "groceries", categoryColor = 0xFFF97316L, timestamp = timeForPrevMonth(10), paymentMethod = "Card", notes = "Costco bulk"),
                ExpenseEntity(title = "Car Service & Oil", amount = 160.00, categoryId = "transport", categoryName = "Transportation", categoryIcon = "transport", categoryColor = 0xFF3B82F6L, timestamp = timeForPrevMonth(15), paymentMethod = "Card", notes = "Oil change"),
                ExpenseEntity(title = "Dining Out", amount = 88.00, categoryId = "food", categoryName = "Food & Dining", categoryIcon = "restaurant", categoryColor = 0xFFEF4444L, timestamp = timeForPrevMonth(20), paymentMethod = "Card", notes = "Birthday dinner")
            )
        )

        expenseDao.insertExpenses(starter)

        val starterBudgets = listOf(
            CategoryBudgetEntity("groceries", 250.00),
            CategoryBudgetEntity("food", 150.00),
            CategoryBudgetEntity("transport", 100.00),
            CategoryBudgetEntity("housing", 900.00),
            CategoryBudgetEntity("utilities", 120.00),
            CategoryBudgetEntity("entertainment", 60.00),
            CategoryBudgetEntity("shopping", 100.00)
        )
        categoryBudgetDao.insertBudgets(starterBudgets)
    }
}
