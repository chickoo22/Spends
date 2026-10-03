package com.example.data.model

data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String = ""
)

object AvailableCurrencies {
    val list = listOf(
        Currency("USD", "$", "United States Dollar", "🇺🇸"),
        Currency("EUR", "€", "Euro (European Union)", "🇪🇺"),
        Currency("GBP", "£", "British Pound", "🇬🇧"),
        Currency("INR", "₹", "Indian Rupee", "🇮🇳"),
        Currency("JPY", "¥", "Japanese Yen", "🇯🇵"),
        Currency("CAD", "$", "Canadian Dollar", "🇨🇦"),
        Currency("AUD", "$", "Australian Dollar", "🇦🇺"),
        Currency("CHF", "Fr", "Swiss Franc", "🇨🇭"),
        Currency("CNY", "¥", "Chinese Yuan", "🇨🇳"),
        Currency("SGD", "$", "Singapore Dollar", "🇸🇬"),
        Currency("AED", "د.إ", "UAE Dirham", "🇦🇪"),
        Currency("SAR", "﷼", "Saudi Riyal", "🇸🇦"),
        Currency("BRL", "R$", "Brazilian Real", "🇧🇷"),
        Currency("MXN", "$", "Mexican Peso", "🇲🇽"),
        Currency("KRW", "₩", "South Korean Won", "🇰🇷"),
        Currency("SEK", "kr", "Swedish Krona", "🇸🇪"),
        Currency("NOK", "kr", "Norwegian Krone", "🇳🇴"),
        Currency("DKK", "kr", "Danish Krone", "🇩🇰"),
        Currency("PLN", "zł", "Polish Zloty", "🇵🇱"),
        Currency("NZD", "$", "New Zealand Dollar", "🇳🇿"),
        Currency("TRY", "₺", "Turkish Lira", "🇹🇷"),
        Currency("RUB", "₽", "Russian Ruble", "🇷🇺"),
        Currency("IDR", "Rp", "Indonesian Rupiah", "🇮🇩"),
        Currency("MYR", "RM", "Malaysian Ringgit", "🇲🇾"),
        Currency("PHP", "₱", "Philippine Peso", "🇵🇭"),
        Currency("THB", "฿", "Thai Baht", "🇹🇭"),
        Currency("VND", "₫", "Vietnamese Dong", "🇻🇳"),
        Currency("ZAR", "R", "South African Rand", "🇿🇦"),
        Currency("EGP", "E£", "Egyptian Pound", "🇪🇬"),
        Currency("NGN", "₦", "Nigerian Naira", "🇳🇬"),
        Currency("PKR", "₨", "Pakistani Rupee", "🇵🇰"),
        Currency("ILS", "₪", "Israeli New Shekel", "🇮🇱"),
        Currency("CLP", "$", "Chilean Peso", "🇨🇱"),
        Currency("COP", "$", "Colombian Peso", "🇨🇴"),
        Currency("ARS", "$", "Argentine Peso", "🇦🇷")
    )

    val default = Currency("USD", "$", "United States Dollar", "🇺🇸")

    fun findByCode(code: String): Currency {
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: default
    }
}
