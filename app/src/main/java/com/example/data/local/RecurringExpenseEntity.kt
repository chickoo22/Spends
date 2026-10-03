package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY"
    val paymentMethod: String = "UPI",
    val notes: String = "",
    val isIncome: Boolean = false,
    val lastGeneratedTimestamp: Long = System.currentTimeMillis(),
    val nextDueDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
