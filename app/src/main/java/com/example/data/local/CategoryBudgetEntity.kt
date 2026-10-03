package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val categoryId: String,
    val monthlyLimit: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
