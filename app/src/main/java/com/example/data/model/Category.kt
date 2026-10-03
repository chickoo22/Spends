package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val isCustom: Boolean = false
) {
    val color: Color get() = Color(colorHex)

    fun getIcon(): ImageVector = getIconForKey(iconKey)

    companion object {
        fun getIconForKey(key: String): ImageVector {
            return when (key) {
                "restaurant" -> Icons.Default.Restaurant
                "groceries" -> Icons.Default.ShoppingCart
                "transport" -> Icons.Default.DirectionsCar
                "housing" -> Icons.Default.Home
                "utilities" -> Icons.Default.Bolt
                "entertainment" -> Icons.Default.Movie
                "shopping" -> Icons.Default.ShoppingBag
                "health" -> Icons.Default.LocalHospital
                "education" -> Icons.Default.School
                "travel" -> Icons.Default.Flight
                "subscriptions" -> Icons.Default.Smartphone
                "wellness" -> Icons.Default.Spa
                "finance" -> Icons.Default.AccountBalance
                else -> Icons.Default.MoreHoriz
            }
        }

        val defaultCategories = listOf(
            Category("food", "Food & Dining", "restaurant", 0xFFEF4444L),
            Category("groceries", "Groceries", "groceries", 0xFFF97316L),
            Category("transport", "Transportation", "transport", 0xFF3B82F6L),
            Category("housing", "Housing & Rent", "housing", 0xFF6366F1L),
            Category("utilities", "Bills & Utilities", "utilities", 0xFFEAB308L),
            Category("entertainment", "Entertainment", "entertainment", 0xFFA855F7L),
            Category("shopping", "Shopping", "shopping", 0xFFEC4899L),
            Category("health", "Health & Medical", "health", 0xFF10B981L),
            Category("education", "Education", "education", 0xFF06B6D4L),
            Category("travel", "Travel & Vacation", "travel", 0xFF14B8A6L),
            Category("subscriptions", "Subscriptions", "subscriptions", 0xFF8B5CF6L),
            Category("other", "Other", "other", 0xFF64748BL)
        )

        fun findById(id: String, customCategories: List<Category> = emptyList()): Category {
            return (defaultCategories + customCategories).firstOrNull { it.id == id }
                ?: defaultCategories.last()
        }
    }
}
