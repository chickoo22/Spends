package com.example.util

import com.example.data.model.Category

data class ParsedExpense(
    val title: String,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val paymentMethod: String = "UPI",
    val notes: String = ""
)

object SmartExpenseParser {
    fun parse(text: String, availableCategories: List<Category>): ParsedExpense {
        val cleanText = text.replace(",", "")
        // Match numbers like 45.50, 100, etc.
        val amountRegex = Regex("""(?:[$₹€£]?)\s*(\d+(?:\.\d{1,2})?)""")
        val matchResult = amountRegex.find(cleanText)
        val amount = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        val lower = text.lowercase()

        val matchedCat = availableCategories.find { cat ->
            when (cat.id) {
                "food" -> lower.contains("food") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("coffee") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("starbucks") || lower.contains("meal")
                "groceries" -> lower.contains("grocery") || lower.contains("supermarket") || lower.contains("vegetable") || lower.contains("fruit") || lower.contains("milk") || lower.contains("store") || lower.contains("market")
                "transport" -> lower.contains("uber") || lower.contains("taxi") || lower.contains("metro") || lower.contains("train") || lower.contains("bus") || lower.contains("fuel") || lower.contains("gas") || lower.contains("transit") || lower.contains("cab")
                "shopping" -> lower.contains("shop") || lower.contains("nike") || lower.contains("clothes") || lower.contains("amazon") || lower.contains("mall") || lower.contains("purchase") || lower.contains("bought")
                "housing" -> lower.contains("rent") || lower.contains("apartment") || lower.contains("housing")
                "utilities" -> lower.contains("electric") || lower.contains("water") || lower.contains("bill") || lower.contains("wifi") || lower.contains("internet") || lower.contains("utility") || lower.contains("electricity")
                "entertainment" -> lower.contains("movie") || lower.contains("cinema") || lower.contains("game") || lower.contains("concert") || lower.contains("show") || lower.contains("ticket")
                "health" -> lower.contains("pharmacy") || lower.contains("doctor") || lower.contains("medical") || lower.contains("cvs") || lower.contains("hospital") || lower.contains("vitamin") || lower.contains("medicine")
                "education" -> lower.contains("course") || lower.contains("book") || lower.contains("tuition") || lower.contains("class") || lower.contains("school")
                "subscriptions" -> lower.contains("netflix") || lower.contains("spotify") || lower.contains("subscription") || lower.contains("prime") || lower.contains("membership")
                else -> lower.contains(cat.name.lowercase())
            }
        } ?: availableCategories.firstOrNull() ?: Category.defaultCategories.first()

        var title = text.replace(Regex("""(?i)\b(paid|spent|rs|inr|usd|eur|gbp|to|at|for|on|via)\b"""), "").trim()
        if (amount > 0) {
            title = title.replace(amount.toString(), "").trim()
        }
        title = title.replace(Regex("""[$₹€£]"""), "").trim()
        if (title.isBlank() || title.length < 2) {
            title = matchedCat.name
        } else {
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }

        return ParsedExpense(
            title = title,
            amount = amount,
            categoryId = matchedCat.id,
            categoryName = matchedCat.name,
            categoryIcon = matchedCat.iconKey,
            categoryColor = matchedCat.colorHex,
            notes = text
        )
    }
}
