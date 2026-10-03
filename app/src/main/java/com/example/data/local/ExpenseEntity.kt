package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["timestamp"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "UPI", // UPI, Transfer, Cash, Card
    val notes: String = "",
    val isIncome: Boolean = false
)
