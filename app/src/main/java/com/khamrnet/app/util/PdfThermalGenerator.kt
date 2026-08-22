package com.khamrnet.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.InvoiceItem
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfThermalGenerator {

    /**
     * Generates a crystal-clear vector PDF sized exactly to 80mm or 58mm POS thermal rolls
     * and returns the local shareable File URI.
     */
    fun createInvoicePdf(
        context: Context,
        invoice: InvoiceEntity,
        settings: SystemSettingsEntity
    ): File {
        val is58mm = settings.thermalPaperWidth == "58mm"
        val pageWidth = if (is58mm) 384 else 576 // Standard thermal receipt points (72dpi - 80mm/58mm)

        // Parse items
        val itemType = object : TypeToken<List<InvoiceItem>>() {}.type
        val items: List<InvoiceItem> = try {
            Gson().fromJson(invoice.itemsJson, itemType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Calculate dynamic height based on content
        val estimatedHeight = 520 + (items.size * 32) + (if (invoice.billType == 4) 140 else 0)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, estimatedHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val boldPaint = Paint(textPaint).apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = 20f
        }
        val centerPaint = Paint(boldPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 22f
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        var y = 40f
        val rightMargin = pageWidth - 20f
        val leftMargin = 20f
        val centerX = pageWidth / 2f

        // 1. Business Header Box
        canvas.drawRoundRect(leftMargin, y - 20f, rightMargin, y + 65f, 10f, 10f, linePaint)
        canvas.drawText(settings.businessName, centerX, y + 10f, centerPaint)
        val phonePaint = Paint(centerPaint).apply { textSize = 16f; typeface = Typeface.DEFAULT }
        if (settings.phone.isNotEmpty()) canvas.drawText("هاتف: ${settings.phone}", centerX, y + 35f, phonePaint)
        if (settings.address.isNotEmpty()) canvas.drawText(settings.address, centerX, y + 55f, phonePaint)
        y += 95f

        // 2. Invoice Details
        val dateFormat = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar"))
        val formattedDate = dateFormat.format(Date(invoice.date))
        canvas.drawText("رقم الفاتورة: #${invoice.billNo.ifEmpty { invoice.invoiceNumber }}", rightMargin, y, boldPaint)
        y += 26f
        canvas.drawText("التاريخ: $formattedDate", rightMargin, y, textPaint)
        y += 26f
        canvas.drawText("العميل: ${invoice.customerName} [${invoice.customerCode}]", rightMargin, y, boldPaint)
        y += 20f

        // Divider
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 24f

        // 3. Table Headers
        boldPaint.textSize = 17f
        canvas.drawText("الصنف", rightMargin, y, boldPaint)
        canvas.drawText("الكمية", rightMargin - 180f, y, boldPaint)
        canvas.drawText("السعر", rightMargin - 280f, y, boldPaint)
        canvas.drawText("الإجمالي", leftMargin + 60f, y, boldPaint)
        y += 10f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 24f

        // 4. Items Rows
        textPaint.textSize = 16f
        for ((idx, item) in items.withIndex()) {
            canvas.drawText("${idx + 1}. ${item.productName}", rightMargin, y, textPaint)
            canvas.drawText("${item.quantity.toInt()} ${item.unitName}", rightMargin - 180f, y, textPaint)
            canvas.drawText("${item.unitPrice.toInt()}", rightMargin - 280f, y, textPaint)
            canvas.drawText("${item.total.toInt()}", leftMargin + 60f, y, textPaint)
            y += 28f
        }

        // Divider
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 28f

        // 5. Total & Tafqeet
        boldPaint.textSize = 20f
        canvas.drawText("إجمالي الفاتورة: ${invoice.total.toInt()} ${settings.currencyName}", rightMargin, y, boldPaint)
        y += 24f

        val tafqeet = ArabicNumberConverter.convertToArabicWords(invoice.total, settings.currencyName)
        textPaint.textSize = 14f
        canvas.drawText("فقط وقدره: $tafqeet لا غير", rightMargin, y, textPaint)
        y += 24f

        // Credit Details if applicable
        if (invoice.billType == 4 || invoice.paymentMethod == "CREDIT") {
            textPaint.textSize = 15f
            if (invoice.paidAmount > 0) {
                canvas.drawText("المدفوع نقداً: ${invoice.paidAmount.toInt()} ${settings.currencyName}", rightMargin, y, textPaint)
                y += 22f
            }
            canvas.drawText("المتبقي آجل: ${invoice.remainingAmount.toInt()} ${settings.currencyName}", rightMargin, y, textPaint)
            y += 22f
            canvas.drawText("الرصيد السابق: ${invoice.previousCustomerBalance.toInt()} ${settings.currencyName}", rightMargin, y, textPaint)
            y += 22f
            boldPaint.textSize = 18f
            canvas.drawText("الرصيد الإجمالي الحالي: ${invoice.newCustomerBalance.toInt()} ${settings.currencyName}", rightMargin, y, boldPaint)
            y += 22f
            val balTafqeet = ArabicNumberConverter.convertToArabicWords(invoice.newCustomerBalance, settings.currencyName)
            canvas.drawText("فقط وقدره: $balTafqeet لا غير", rightMargin, y, textPaint)
            y += 24f
        }

        // Footer
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 24f
        canvas.drawText("فاتورة مبيعات إلكترونية معتمدة", centerX, y, phonePaint)

        document.finishPage(page)

        // Save PDF to cache dir for sharing
        val fileDir = File(context.cacheDir, "invoices")
        if (!fileDir.exists()) fileDir.mkdirs()
        val pdfFile = File(fileDir, "فاتورة_${invoice.billNo.ifEmpty { invoice.invoiceNumber }}.pdf")

        val outputStream = FileOutputStream(pdfFile)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return pdfFile
    }

    /**
     * Shares invoice PDF directly to WhatsApp or any chosen app with one tap
     */
    fun shareInvoiceToWhatsApp(context: Context, invoice: InvoiceEntity, settings: SystemSettingsEntity) {
        val pdfFile = createInvoicePdf(context, invoice, settings)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "مرفق فاتورة مبيعات رقم #${invoice.billNo.ifEmpty { invoice.invoiceNumber }} - ${settings.businessName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة عبر:"))
    }
}
