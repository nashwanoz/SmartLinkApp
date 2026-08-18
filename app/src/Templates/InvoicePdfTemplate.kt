package com.smartlink.erp.templates

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.InvoiceItem
import com.smartlink.erp.data.local.entity.SystemSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generate Invoice PDF Template (Thermal Receipt 80mm)
 * Format:
 * - Header with business info
 * - Meta info (invoice number, date, cashier, customer)
 * - Items table
 * - Totals & Payment details
 * - Customer balance section
 */
suspend fun generateInvoicePdf(
    context: Context,
    invoice: Invoice,
    settings: SystemSettings
): File = withContext(Dispatchers.IO) {
    val pdfFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        "فاتورة-${invoice.invoiceNumber}.pdf"
    )
    
    val document = PdfDocument.Builder(
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
            .build()
    ).build()
    
    val pageInfo = PdfDocument.PageInfo.Builder(
        240, // 80mm at 72 DPI ≈ 240 pixels width
        1200, // Dynamic height
        1
    ).create()
    
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val boldPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    val monospacePaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
    }
    
    val rectPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    
    val dashedPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }
    
    val dottedPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 3f), 0f)
    }
    
    var y = 40f
    val centerX = 120f
    val margin = 20f
    
    // 1. Header Box with Rounded Border
    val headerRect = RectF(margin, y - 25, 240 - margin, y + 55)
    canvas.drawRoundRect(headerRect, 12f, 12f, rectPaint)
    
    paint.textSize = 17f
    canvas.drawText(settings.businessName ?: "شبكة خمر نت اللاسلكية", centerX, y + 10, boldPaint)
    
    if (!settings.phone.isNullOrBlank()) {
        paint.textSize = 13f
        canvas.drawText("هاتف / جوال: ${settings.phone}", centerX, y + 30, boldPaint)
    }
    
    if (!settings.address.isNullOrBlank()) {
        paint.textSize = 11f
        canvas.drawText("العنوان: ${settings.address}", centerX, y + 45, paint)
    }
    
    y += 70f
    
    // 2. Meta Info
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 5f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 20f
    
    paint.textSize = 12f
    paint.textAlign = Paint.Align.RIGHT
    
    // Time & Invoice Number
    val timeStr = formatTime(invoice.date)
    canvas.drawText("الوقت: $timeStr", 240 - margin, y, paint)
    canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", margin, y, monospacePaint)
    y += 25f
    
    // Cashier & Date
    val dateStr = formatDate(invoice.date)
    canvas.drawText("الكاشير: ${invoice.cashierName}", 240 - margin, y, paint)
    canvas.drawText("التاريخ: $dateStr", margin, y, paint)
    y += 25f
    
    // Customer Name
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("اسم العميل: ${invoice.customerName} [${invoice.customerCode}]", 240 - margin, y, boldPaint)
    y += 30f
    
    // 3. Items Table
    paint.textSize = 12f
    paint.textAlign = Paint.Align.CENTER
    
    // Table Headers
    val tableY = y
    val headerHeight = 25f
    val rowHeight = 28f
    
    // Draw header background
    canvas.drawRect(margin, tableY, 240 - margin, tableY + headerHeight, boldPaint)
    
    paint.color = Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    
    val colWidths = floatArrayOf(22f, 70f, 35f, 35f, 38f)
    val colStarts = floatArrayOf(margin, margin + 22f, margin + 92f, margin + 127f, margin + 162f)
    
    canvas.drawText("#", colStarts[0] + colWidths[0] / 2, tableY + 16, paint)
    canvas.drawText("الصنف", colStarts[1] + colWidths[1] / 2, tableY + 16, paint)
    canvas.drawText("الكمية", colStarts[2] + colWidths[2] / 2, tableY + 16, paint)
    canvas.drawText("السعر", colStarts[3] + colWidths[3] / 2, tableY + 16, paint)
    canvas.drawText("الإجمالي", colStarts[4] + colWidths[4] / 2, tableY + 16, paint)
    
    paint.color = Color.BLACK
    y = tableY + headerHeight
    
    // Draw items
    invoice.items.forEachIndexed { idx, item ->
        canvas.drawLine(margin, y, 240 - margin, y, dottedPaint)
        y += 5f
        
        paint.textSize = 13f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("${idx + 1}", colStarts[0] + colWidths[0] / 2, y + 10, boldPaint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(item.productName, colStarts[1] + colWidths[1] - 4, y + 10, boldPaint)
        
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("${item.quantity} ${item.unitName}", colStarts[2] + colWidths[2] / 2, y + 10, paint)
        
        paint.textSize = 12f
        canvas.drawText(formatCurrency(item.unitPrice), colStarts[3] + colWidths[3] / 2, y + 10, paint)
        
        paint.textSize = 14f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(formatCurrency(item.total), colStarts[4] + colWidths[4] - 4, y + 10, boldPaint)
        
        y += rowHeight - 5f
    }
    
    y += 10f
    
    // 4. Totals & Payment Details
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 10f
    
    paint.textSize = 13f
    paint.textAlign = Paint.Align.RIGHT
    
    val currencyName = settings.currency ?: "ريال يمني"
    
    // Total Invoice
    canvas.drawText("اجمالي الفاتورة:", 240 - margin - 80, y, boldPaint)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("${formatCurrency(invoice.total)} $currencyName", margin + 80, y, boldPaint)
    y += 25f
    
    // Check if cash invoice
    val isCashInvoice = invoice.paymentMethod == "CASH" ||
                       invoice.customerId == "CASH_CUSTOMER" ||
                       (invoice.paidAmount >= invoice.total && (invoice.customerId.isNullOrBlank() || invoice.customerId == "CASH_CUSTOMER"))
    
    if (!isCashInvoice && invoice.paidAmount > 0) {
        paint.textSize = 13f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("المدفوع نقداً:", 240 - margin - 80, y, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${formatCurrency(invoice.paidAmount)} $currencyName", margin + 80, y, paint)
        y += 25f
    }
    
    if (!isCashInvoice && (invoice.remainingAmount > 0 || invoice.paymentMethod == "CREDIT")) {
        paint.textSize = 13f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("المتبقي آجل:", 240 - margin - 80, y, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${formatCurrency(invoice.remainingAmount)} $currencyName", margin + 80, y, paint)
        y += 25f
    }
    
    // 5. Customer Balance Section (only if not pure cash invoice)
    if (!isCashInvoice) {
        y += 10f
        canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
        y += 5f
        canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
        y += 20f
        
        paint.textSize = 13f
        paint.textAlign = Paint.Align.RIGHT
        
        val prevBal = invoice.prevCustomerBalance
        val newBal = invoice.newCustomerBalance
        
        canvas.drawText("الرصيد السابق:", 240 - margin - 80, y, boldPaint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${formatCurrency(kotlin.math.abs(prevBal))} $currencyName", margin + 80, y, boldPaint)
        y += 25f
        
        paint.textSize = 14f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الرصيد الاجمالي:", 240 - margin - 80, y, boldPaint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${formatCurrency(kotlin.math.abs(newBal))} $currencyName", margin + 80, boldPaint)
        y += 25f
        
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        val totalWords = numberToArabicWords(kotlin.math.abs(newBal), currencyName, "فلس")
        canvas.drawText("فقط: $totalWords", centerX, y, paint)
        y += 20f
        
        canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    }
    
    document.finishPage(page)
    
    FileOutputStream(pdfFile).use { outputStream ->
        document.writeTo(outputStream)
    }
    
    document.close()
    
    pdfFile
}

/**
 * Print Invoice directly to thermal printer (80mm / 58mm)
 */
fun printInvoice(
    context: Context,
    invoice: Invoice,
    settings: SystemSettings
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    
    val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    
    printManager.print(
        "فاتورة-${invoice.invoiceNumber}",
        InvoicePrintDocumentAdapter(context, invoice, settings),
        printAttributes
    )
}

/**
 * Share Invoice PDF via WhatsApp or native share
 */
suspend fun shareInvoicePdf(
    context: Context,
    invoice: Invoice,
    settings: SystemSettings
): Boolean = withContext(Dispatchers.IO) {
    try {
        val pdfFile = generateInvoicePdf(context, invoice, settings)
        val messageText = buildInvoiceWhatsAppMessage(invoice, settings)
        
        // Share via WhatsApp
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            ))
            putExtra(android.content.Intent.EXTRA_TEXT, messageText)
            setPackage("com.whatsapp")
        }
        
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Build WhatsApp message for Invoice
 */
fun buildInvoiceWhatsAppMessage(invoice: Invoice, settings: SystemSettings): String {
    val currencyName = settings.currency ?: "ريال يمني"
    val totalDebit = invoice.total
    val totalCredit = invoice.paidAmount
    val newBalance = kotlin.math.abs(invoice.newCustomerBalance)
    val balanceStatus = if (invoice.newCustomerBalance >= 0) "عليكم" else "لكم"
    
    return """
📑 *فاتورة مبيعات - ${settings.businessName}*
👤 *العميل:* ${invoice.customerName} (كود: ${invoice.customerCode})
📅 *التاريخ:* ${formatDateTime(invoice.date)}
🧾 *رقم الفاتورة:* ${invoice.invoiceNumber}

🛒 *عدد الأصناف:* ${invoice.items.size}
💵 *إجمالي الفاتورة:* ${formatCurrency(totalDebit)} $currencyName
💰 *المدفوع:* ${formatCurrency(totalCredit)} $currencyName
⚖️ *الرصيد المتبقي:* ${formatCurrency(newBalance)} $currencyName [$balanceStatus]

شكراً لتعاملكم معنا! 🙏
    """.trimIndent()
}

// Helper functions
private fun formatCurrency(amount: Double): String {
    return String.format("%,.2f", amount)
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
    return sdf.format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}

private fun numberToArabicWords(amount: Double, currency: String, subCurrency: String): String {
    // Simplified implementation - replace with full number-to-words converter
    val wholePart = amount.toInt()
    val fractionalPart = ((amount - wholePart) * 100).toInt()
    
    return "$wholePart $currency و$fractionalPart $subCurrency"
}

// PrintDocumentAdapter for thermal printing
class InvoicePrintDocumentAdapter(
    private val context: Context,
    private val invoice: Invoice,
    private val settings: SystemSettings
) : android.print.PrintDocumentAdapter() {
    
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: android.os.CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        
        val info = PdfDocument.PageInfo.Builder(
            240, 1200, 1
        ).create()
        
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder()
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build(),
            newAttributes != oldAttributes
        )
    }
    
    override fun onWrite(
        pages: Array<out PdfDocument.PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: android.os.CancellationSignal,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        
        // Implement PDF writing logic here (similar to generateInvoicePdf)
        callback.onWriteFinished(arrayOf(0))
    }
}
