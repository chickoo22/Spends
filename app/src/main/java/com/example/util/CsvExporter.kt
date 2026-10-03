package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val CSV_HEADER = "ID,Date,Time,Title,Amount,Currency,Category,Payment Method,Notes"

    /**
     * Converts a list of ExpenseEntity into RFC-4180 standard CSV format.
     */
    fun generateCsvContent(expenses: List<ExpenseEntity>, currencyCode: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\r\n")

        for (item in expenses) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val timeStr = timeFormat.format(Date(item.timestamp))
            val amountStr = String.format(Locale.US, "%.2f", item.amount)

            sb.append(item.id).append(",")
            sb.append(dateStr).append(",")
            sb.append(timeStr).append(",")
            sb.append(escapeCsv(item.title)).append(",")
            sb.append(amountStr).append(",")
            sb.append(escapeCsv(currencyCode)).append(",")
            sb.append(escapeCsv(item.categoryName)).append(",")
            sb.append(escapeCsv(item.paymentMethod)).append(",")
            sb.append(escapeCsv(item.notes))
            sb.append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Escapes a single CSV value according to RFC-4180 rules.
     * Encloses the string in quotes if it contains commas, quotes, or newlines,
     * and doubles any existing double-quotes.
     */
    fun escapeCsv(value: String): String {
        val trimmed = value.trim()
        val needsQuotes = trimmed.contains(",") ||
                trimmed.contains("\"") ||
                trimmed.contains("\n") ||
                trimmed.contains("\r")

        return if (needsQuotes) {
            "\"" + trimmed.replace("\"", "\"\"") + "\""
        } else {
            trimmed
        }
    }

    /**
     * Writes the CSV string into a user-selected document Uri (Storage Access Framework).
     */
    fun writeCsvToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves CSV content to cacheDir/exports/ and returns a shareable content:// Uri via FileProvider.
     */
    fun createShareableCsvUri(context: Context, filename: String, content: String): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches the system share sheet with the generated CSV file.
     */
    fun shareCsvFile(context: Context, uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is my expense backup from SpendWise.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Expense Backup CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
