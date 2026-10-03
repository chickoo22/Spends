package com.example.data.model

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val categoryIconKey: String,
    val colorHex: Long,
    val totalAmount: Double,
    val percentage: Float, // 0.0 to 100.0
    val count: Int
)

data class DailySpending(
    val dayOfMonth: Int,
    val dayOfWeekLabel: String,
    val totalAmount: Double,
    val isHighest: Boolean = false
)

data class MonthlyTrendPoint(
    val year: Int,
    val month: Int,
    val label: String,
    val totalSpent: Double
)

data class MonthlyAnalytics(
    val year: Int,
    val month: Int, // 0-based, Calendar.JANUARY = 0
    val monthTitle: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val dailyAverage: Double,
    val highestDay: Int,
    val highestDayAmount: Double,
    val prevMonthTotal: Double,
    val percentChangeFromPrev: Double?,
    val categoryBreakdowns: List<CategorySpending>,
    val dailySpendings: List<DailySpending>,
    val multiMonthTrends: List<MonthlyTrendPoint>
)
