package com.example.data.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Reading
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    fun exportToCsv(context: Context, readings: List<Reading>, currencySymbol: String) {
        val csvHeader = "ID,Customer,Date,Previous Reading,Current Reading,Actual Units,Billable Units,Grand Total ($currencySymbol),Days Late\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvBody = readings.joinToString("\n") { r ->
            val dateStr = dateFormat.format(Date(r.readingDate))
            "${r.id},\"${escapeCsv(r.customerName)}\",\"$dateStr\",${r.previousReading},${r.currentReading},${r.actualUnits},${r.billableUnits},${r.grandTotal},${r.daysLate}"
        }
        val csvContent = csvHeader + csvBody

        try {
            val file = File(context.cacheDir, "utility_readings_report.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray())
            }
            shareFile(context, file, "text/csv", "Export CSV Report")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToPdf(context: Context, readings: List<Reading>, currencySymbol: String) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint()
        val textPaint = android.graphics.Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val headerPaint = android.graphics.Paint().apply {
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var y = 50f
        canvas.drawText("Utility Calculator - Readings History", 50f, y, headerPaint)
        y += 25f
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 50f, y, textPaint)
        y += 35f

        // Table headers
        paint.strokeWidth = 1f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 18f

        val col1 = 50f  // Customer
        val col2 = 180f // Date
        val col3 = 270f // Readings (Prev -> Curr)
        val col4 = 370f // Billable Units
        val col5 = 470f // Total

        canvas.drawText("Customer", col1, y, textPaint)
        canvas.drawText("Date", col2, y, textPaint)
        canvas.drawText("Readings", col3, y, textPaint)
        canvas.drawText("Billable Units", col4, y, textPaint)
        canvas.drawText("Total ($currencySymbol)", col5, y, textPaint)
        y += 8f
        canvas.drawLine(50f, y, 545f, y, paint)
        y += 22f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        readings.forEachIndexed { index, r ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                canvas.drawText("Utility Calculator - Readings History (Page $pageNumber)", 50f, y, headerPaint)
                y += 30f
                canvas.drawLine(50f, y, 545f, y, paint)
                y += 22f
            }

            val dateStr = dateFormat.format(Date(r.readingDate))
            val readingText = "${r.previousReading.toInt()} -> ${r.currentReading.toInt()}"
            val billUnitsText = "${r.actualUnits.toInt()} (${r.billableUnits.toInt()} bill)"

            canvas.drawText(r.customerName.take(18), col1, y, textPaint)
            canvas.drawText(dateStr, col2, y, textPaint)
            canvas.drawText(readingText, col3, y, textPaint)
            canvas.drawText(billUnitsText, col4, y, textPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, r.grandTotal), col5, y, textPaint)

            y += 22f
        }

        pdfDocument.finishPage(page)

        try {
            val file = File(context.cacheDir, "utility_readings_report.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            shareFile(context, file, "application/pdf", "Export PDF Report")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
