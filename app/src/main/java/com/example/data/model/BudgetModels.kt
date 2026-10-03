package com.example.data.model

data class CategoryBudgetProgress(
    val categoryId: String,
    val categoryName: String,
    val categoryIconKey: String,
    val colorHex: Long,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Float, // 0.0 to 1.0+ (can exceed 1.0)
    val isExceeded: Boolean,
    val isNearLimit: Boolean, // >= 80% and < 100%
    val hasBudget: Boolean = true
)

data class OverallBudgetSummary(
    val totalBudget: Double,
    val totalSpentInBudgetedCategories: Double,
    val totalRemaining: Double,
    val overallPercentage: Float,
    val budgetedCategoryCount: Int,
    val exceededCount: Int,
    val warningCount: Int,
    val onTrackCount: Int,
    val daysRemainingInMonth: Int,
    val dailyBudgetRemaining: Double
)
