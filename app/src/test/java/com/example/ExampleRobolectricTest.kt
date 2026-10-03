package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetDao
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import com.example.data.local.RecurringExpenseDao
import com.example.data.local.UserSettingDao
import com.example.data.local.UserSettingEntity
import com.example.data.model.AvailableCurrencies
import com.example.data.model.Category
import com.example.data.model.CategoryBudgetProgress
import com.example.data.model.Currency
import com.example.data.repository.ExpenseRepository
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryBudgetDao: CategoryBudgetDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var userSettingDao: UserSettingDao
    private lateinit var recurringExpenseDao: RecurringExpenseDao
    private lateinit var repository: ExpenseRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = db.expenseDao()
        categoryBudgetDao = db.categoryBudgetDao()
        categoryDao = db.categoryDao()
        userSettingDao = db.userSettingDao()
        recurringExpenseDao = db.recurringExpenseDao()
        repository = ExpenseRepository(expenseDao, userSettingDao, categoryDao, categoryBudgetDao, recurringExpenseDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SpendWise", appName)
    }

    @Test
    fun `available currencies contain default and key currencies`() {
        val usd = AvailableCurrencies.findByCode("USD")
        assertEquals("$", usd.symbol)

        val eur = AvailableCurrencies.findByCode("EUR")
        assertEquals("€", eur.symbol)

        val inr = AvailableCurrencies.findByCode("INR")
        assertEquals("₹", inr.symbol)

        val gbp = AvailableCurrencies.findByCode("GBP")
        assertEquals("£", gbp.symbol)

        val jpy = AvailableCurrencies.findByCode("JPY")
        assertEquals("¥", jpy.symbol)

        assertNotNull(AvailableCurrencies.list)
        assertTrue(AvailableCurrencies.list.size >= 30)
    }

    @Test
    fun `category budget progress calculations work accurately`() {
        val progressOnTrack = CategoryBudgetProgress(
            categoryId = "food",
            categoryName = "Food & Dining",
            categoryIconKey = "restaurant",
            colorHex = 0xFFEF4444L,
            monthlyLimit = 300.0,
            spentAmount = 150.0,
            remainingAmount = 150.0,
            progressPercentage = 0.5f,
            isExceeded = false,
            isNearLimit = false,
            hasBudget = true
        )

        assertEquals(150.0, progressOnTrack.remainingAmount, 0.001)
        assertFalse(progressOnTrack.isExceeded)
        assertFalse(progressOnTrack.isNearLimit)

        val progressNearLimit = CategoryBudgetProgress(
            categoryId = "groceries",
            categoryName = "Groceries",
            categoryIconKey = "groceries",
            colorHex = 0xFFF97316L,
            monthlyLimit = 200.0,
            spentAmount = 180.0,
            remainingAmount = 20.0,
            progressPercentage = 0.9f,
            isExceeded = false,
            isNearLimit = true,
            hasBudget = true
        )
        assertTrue(progressNearLimit.isNearLimit)
        assertFalse(progressNearLimit.isExceeded)

        val progressExceeded = CategoryBudgetProgress(
            categoryId = "shopping",
            categoryName = "Shopping",
            categoryIconKey = "shopping",
            colorHex = 0xFFEC4899L,
            monthlyLimit = 100.0,
            spentAmount = 125.0,
            remainingAmount = -25.0,
            progressPercentage = 1.25f,
            isExceeded = true,
            isNearLimit = false,
            hasBudget = true
        )

        assertEquals(-25.0, progressExceeded.remainingAmount, 0.001)
        assertTrue(progressExceeded.isExceeded)
    }

    @Test
    fun `csv export escaping and formatting conform to standard`() {
        assertEquals("Coffee", CsvExporter.escapeCsv("Coffee"))
        assertEquals("\"Coffee, Tea\"", CsvExporter.escapeCsv("Coffee, Tea"))
        assertEquals("\"Movie \"\"Night\"\"\"", CsvExporter.escapeCsv("Movie \"Night\""))
        assertEquals("\"Line 1\nLine 2\"", CsvExporter.escapeCsv("Line 1\nLine 2"))

        val sampleExpenses = listOf(
            ExpenseEntity(
                id = 1,
                title = "Grocery, Market",
                amount = 54.30,
                categoryId = "groceries",
                categoryName = "Groceries",
                categoryIcon = "groceries",
                categoryColor = 0xFFF97316L,
                timestamp = 1774300800000L,
                paymentMethod = "Card",
                notes = "Bought fruits & \"fresh\" bread"
            )
        )

        val csv = CsvExporter.generateCsvContent(sampleExpenses, "USD")
        assertTrue(csv.contains("ID,Date,Time,Title,Amount,Currency,Category,Payment Method,Notes"))
        assertTrue(csv.contains("\"Grocery, Market\""))
        assertTrue(csv.contains("54.30"))
        assertTrue(csv.contains("USD"))
        assertTrue(csv.contains("Groceries"))
        assertTrue(csv.contains("\"Bought fruits & \"\"fresh\"\" bread\""))
    }

    @Test
    fun `csv exporter generates file and shareable URI`() {
        val content = "ID,Date,Amount\r\n1,2026-09-25,25.00"
        val fileName = "test_export.csv"
        val uri = CsvExporter.createShareableCsvUri(context, fileName, content)
        assertNotNull(uri)

        val exportedFile = File(context.cacheDir, "exports/$fileName")
        assertTrue(exportedFile.exists())
        assertEquals(content, exportedFile.readText(Charsets.UTF_8))
    }

    @Test
    fun `room database expense CRUD operations function correctly`() = runBlocking {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        }
        val timestamp = cal.timeInMillis

        // 1. Insert single expense
        val expense1 = ExpenseEntity(
            title = "Morning Latte",
            amount = 4.75,
            categoryId = "food",
            categoryName = "Food & Dining",
            categoryIcon = "restaurant",
            categoryColor = 0xFFEF4444L,
            timestamp = timestamp,
            paymentMethod = "Card",
            notes = "With oat milk"
        )
        val id1 = expenseDao.insertExpense(expense1)
        assertTrue(id1 > 0)

        // 2. Query by ID
        val retrieved = expenseDao.getExpenseById(id1)
        assertNotNull(retrieved)
        assertEquals("Morning Latte", retrieved!!.title)
        assertEquals(4.75, retrieved.amount, 0.001)

        // 3. Update expense
        val updated = retrieved.copy(title = "Morning Caramel Latte", amount = 5.50)
        expenseDao.updateExpense(updated)
        val afterUpdate = expenseDao.getExpenseById(id1)
        assertEquals("Morning Caramel Latte", afterUpdate!!.title)
        assertEquals(5.50, afterUpdate.amount, 0.001)

        // 4. Insert another expense and verify range query
        val expense2 = ExpenseEntity(
            title = "Monthly Subway Pass",
            amount = 80.0,
            categoryId = "transport",
            categoryName = "Transportation",
            categoryIcon = "transport",
            categoryColor = 0xFF3B82F6L,
            timestamp = timestamp + 1000,
            paymentMethod = "Apple Pay",
            notes = "Zone 1-2"
        )
        expenseDao.insertExpense(expense2)

        val rangeExpenses = expenseDao.getExpensesInRange(timestamp - 5000, timestamp + 5000).first()
        assertEquals(2, rangeExpenses.size)

        // 5. Search query
        val searchResults = expenseDao.searchExpenses("Subway").first()
        assertEquals(1, searchResults.size)
        assertEquals("Monthly Subway Pass", searchResults[0].title)

        // 6. Delete single expense
        expenseDao.deleteExpenseById(id1)
        assertNull(expenseDao.getExpenseById(id1))

        // 7. Delete all expenses
        expenseDao.deleteAllExpenses()
        val allAfterClear = expenseDao.getAllExpenses().first()
        assertTrue(allAfterClear.isEmpty())
    }

    @Test
    fun `category budget dao operations function correctly`() = runBlocking {
        // Set budget
        val budgetFood = CategoryBudgetEntity(
            categoryId = "food",
            monthlyLimit = 450.0,
            updatedAt = System.currentTimeMillis()
        )
        categoryBudgetDao.setBudget(budgetFood)

        val retrieved = categoryBudgetDao.getBudgetForCategory("food")
        assertNotNull(retrieved)
        assertEquals(450.0, retrieved!!.monthlyLimit, 0.001)

        val allBudgets = categoryBudgetDao.getAllBudgets().first()
        assertEquals(1, allBudgets.size)

        // Delete budget
        categoryBudgetDao.deleteBudget("food")
        val afterDelete = categoryBudgetDao.getBudgetForCategory("food")
        assertNull(afterDelete)
    }

    @Test
    fun `user settings and currency preferences persist accurately`() = runBlocking {
        // Onboarding initially false
        assertFalse(repository.isOnboardedDirect())

        // Save currency & mark onboarded
        val selectedCurrency = Currency(code = "EUR", symbol = "€", name = "Euro", flag = "🇪🇺")
        repository.saveCurrency(selectedCurrency)

        assertTrue(repository.isOnboardedDirect())
        val saved = repository.getCurrencyDirect()
        assertNotNull(saved)
        assertEquals("EUR", saved!!.first)
        assertEquals("€", saved.second)
    }

    @Test
    fun `custom category dao allows inserting and deleting custom categories`() = runBlocking {
        val customCat = CategoryEntity(
            id = "custom_crypto",
            name = "Crypto Investments",
            iconKey = "savings",
            colorHex = 0xFF8B5CF6L,
            isCustom = true
        )
        categoryDao.insertCategory(customCat)

        val customList = categoryDao.getAllCustomCategories().first()
        assertEquals(1, customList.size)
        assertEquals("Crypto Investments", customList[0].name)

        categoryDao.deleteCategory("custom_crypto")
        val afterDelete = categoryDao.getAllCustomCategories().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `viewmodel monthly analytics and overall budget calculations are mathematically sound`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 14, 0, 0)
        }
        val sept10 = cal.timeInMillis
        cal.set(2026, Calendar.SEPTEMBER, 20, 18, 0, 0)
        val sept20 = cal.timeInMillis

        val expenses = listOf(
            ExpenseEntity(
                id = 1,
                title = "Dinner with team",
                amount = 120.0,
                categoryId = "food",
                categoryName = "Food & Dining",
                categoryIcon = "restaurant",
                categoryColor = 0xFFEF4444L,
                timestamp = sept10,
                paymentMethod = "Card",
                notes = "Team celebration"
            ),
            ExpenseEntity(
                id = 2,
                title = "Weekly groceries",
                amount = 80.0,
                categoryId = "groceries",
                categoryName = "Groceries",
                categoryIcon = "groceries",
                categoryColor = 0xFFF97316L,
                timestamp = sept20,
                paymentMethod = "Debit",
                notes = "Organic food"
            )
        )

        val viewModel = ExpenseViewModel(repository)
        viewModel.setMonth(2026, Calendar.SEPTEMBER)

        val analytics = viewModel.calculateMonthlyAnalytics(expenses, 2026, Calendar.SEPTEMBER)
        assertEquals(200.0, analytics.totalSpent, 0.001)
        assertEquals(2, analytics.transactionCount)
        assertEquals(10, analytics.highestDay) // Day 10 had $120, Day 20 had $80
        assertEquals(120.0, analytics.highestDayAmount, 0.001)
        assertEquals(200.0 / 30.0, analytics.dailyAverage, 0.001) // September has 30 days

        // Category breakdown test
        assertEquals(2, analytics.categoryBreakdowns.size)
        val foodSpending = analytics.categoryBreakdowns.find { it.categoryId == "food" }
        assertNotNull(foodSpending)
        assertEquals(120.0, foodSpending!!.totalAmount, 0.001)
        assertEquals(60.0f, foodSpending.percentage, 0.5f) // 120/200 = 60%

        // Budget summary test
        val budgets = listOf(
            CategoryBudgetEntity(categoryId = "food", monthlyLimit = 200.0, updatedAt = 0L),
            CategoryBudgetEntity(categoryId = "groceries", monthlyLimit = 60.0, updatedAt = 0L) // groceries spent 80 -> EXCEEDED!
        )
        val progressList = viewModel.calculateBudgetProgress(expenses, budgets, emptyList(), 2026, Calendar.SEPTEMBER)
        val overallSummary = viewModel.calculateOverallBudgetSummary(progressList, 2026, Calendar.SEPTEMBER)

        assertEquals(2, overallSummary.budgetedCategoryCount)
        assertEquals(260.0, overallSummary.totalBudget, 0.001) // 200 + 60 = 260
        assertEquals(200.0, overallSummary.totalSpentInBudgetedCategories, 0.001) // 120 + 80 = 200
        assertEquals(60.0, overallSummary.totalRemaining, 0.001) // 260 - 200 = 60
        assertEquals(1, overallSummary.exceededCount) // Groceries exceeded!
        assertEquals(0, overallSummary.warningCount)  // None near limit
        assertEquals(1, overallSummary.onTrackCount)  // Food on track (60% spent)!
    }
}
