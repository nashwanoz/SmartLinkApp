package com.smartlink.erp.templates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.SystemSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generate Bond PDF Template (Thermal Receipt 80mm)
 * Format:
 * - سند قبض / سند صرف
 * - رقم السند
 * - اسم العميل
 * - مبلغ السند
 * - الرصيد بعد السند
 */
suspend fun generateBondPdf(
    context: Context,
    bond: Bond,
    settings: SystemSettings
): File = withContext(Dispatchers.IO) {
    val pdfFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        "سند-${bond.bondNumber}.pdf"
    )
    
    val document = PdfDocument.Builder(
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
            .build()
    ).build()
    
    val pageInfo = PdfDocument.PageInfo.Builder(
        240, // 80mm at 72 DPI ≈ 240 pixels width
        800, // Dynamic height
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
        strokeWidth = 2f
    }
    
    val dashedPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }
    
    var y = 40f
    val centerX = 120f
    val margin = 20f
    
    // 1. Header Box with Business Name
    canvas.drawRect(margin, y - 20, 240 - margin, y + 50, rectPaint)
    
    paint.textSize = 17f
    canvas.drawText(settings.businessName ?: "محل تجاري", centerX, y + 10, boldPaint)
    
    y += 45f
    canvas.drawLine(margin, y, 240 - margin, y, rectPaint)
    
    paint.textSize = 14f
    val bondTitle = if (bond.type == "RECEIPT") "سند قبض نقداً" else "سند صرف"
    canvas.drawText(bondTitle, centerX, y + 20, boldPaint)
    
    y += 50f
    
    // 2. Meta Info
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 5f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 20f
    
    paint.textSize = 12f
    paint.textAlign = Paint.Align.RIGHT
    
    // Bond Number & Type
    canvas.drawText("نوع السند: $bondTitle", 240 - margin, y, paint)
    canvas.drawText("رقم السند: ${bond.bondNumber}", margin, y, monospacePaint)
    y += 25f
    
    // Time & Date
    val timeStr = formatTime(bond.date)
    val dateStr = formatDate(bond.date)
    canvas.drawText("الوقت: $timeStr", 240 - margin, y, paint)
    canvas.drawText("التاريخ: $dateStr", margin, y, paint)
    y += 25f
    
    // Customer Name & Code
    canvas.drawText("اسم العميل: ${bond.customerName}", 240 - margin, y, paint)
    canvas.drawText("[${bond.customerCode}]", margin, y, paint)
    y += 25f
    
    // Note
    if (!bond.note.isNullOrBlank()) {
        canvas.drawText("البيان: ${bond.note}", 240 - margin, y, paint)
        y += 25f
    }
    
    y += 10f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 5f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 20f
    
    // 3. Bond Amount Box
    canvas.drawRect(margin, y - 10, 240 - margin, y + 70, rectPaint)
    
    paint.textSize = 12f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("مبلغ السند", centerX, y + 15, boldPaint)
    
    paint.textSize = 18f
    val amountStr = formatCurrency(bond.amount)
    val currencyName = settings.currency ?: "ريال يمني"
    canvas.drawText("$amountStr $currencyName", centerX, y + 45, boldPaint)
    
    paint.textSize = 11f
    val amountWords = numberToArabicWords(bond.amount, currencyName, "فلس")
    canvas.drawText("فقط: $amountWords", centerX, y + 65, paint)
    
    y += 80f
    
    // 4. Balance Details
    y += 10f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 5f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 25f
    
    paint.textSize = 12f
    paint.textAlign = Paint.Align.RIGHT
    
    val prevBalance = kotlin.math.abs(bond.prevCustomerBalance)
    val newBalance = kotlin.math.abs(bond.newCustomerBalance)
    val prevStatus = if (bond.prevCustomerBalance >= 0) "عليه" else "له"
    val newStatus = if (bond.newCustomerBalance >= 0) "عليه" else "له"
    
    canvas.drawText("الرصيد السابق: ${formatCurrency(prevBalance)} $currencyName ($prevStatus)", 240 - margin, y, paint)
    y += 30f
    
    paint.textSize = 14f
    canvas.drawText("الرصيد بعد السند: ${formatCurrency(newBalance)} $currencyName ($newStatus)", centerX, y, boldPaint)
    y += 25f
    
    paint.textSize = 11f
    val newBalWords = numberToArabicWords(newBalance, currencyName, "فلس")
    canvas.drawText("فقط: $newBalWords", centerX, y, paint)
    
    y += 35f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 5f
    canvas.drawLine(margin, y, 240 - margin, y, dashedPaint)
    y += 30f
    
    // 5. Signatures area
    paint.textSize = 11f
    paint.textAlign = Paint.Align.CENTER
    
    // Left: Customer Signature
    canvas.drawText("توقيع المستلم", margin + 60, y, boldPaint)
    canvas.drawLine(margin + 20, y + 10, margin + 100, y + 10, dashedPaint)
    
    // Right: Cashier Signature
    canvas.drawText("المحاسب / الكاشير", 240 - margin - 60, y, boldPaint)
    canvas.drawText(bond.cashierName ?: "", 240 - margin - 60, y + 25, boldPaint)
    
    document.finishPage(page)
    
    FileOutputStream(pdfFile).use { outputStream ->
        document.writeTo(outputStream)
    }
    
    document.close()
    
    pdfFile
}

/**
 * Print Bond directly to thermal printer (80mm / 58mm)
 */
fun printBond(
    context: Context,
    bond: Bond,
    settings: SystemSettings
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    
    val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    
    printManager.print(
        "سند-${bond.bondNumber}",
        BondPrintDocumentAdapter(context, bond, settings),
        printAttributes
    )
}

/**
 * Share Bond PDF via WhatsApp or native share
 */
suspend fun shareBondPdf(
    context: Context,
    bond: Bond,
    settings: SystemSettings
): Boolean = withContext(Dispatchers.IO) {
    try {
        val pdfFile = generateBondPdf(context, bond, settings)
        val messageText = buildBondWhatsAppMessage(bond, settings)
        
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
 * Build WhatsApp message for Bond
 */
fun buildBondWhatsAppMessage(bond: Bond, settings: SystemSettings): String {
    val currencyName = settings.currency ?: "ريال يمني"
    val bondTitle = if (bond.type == "RECEIPT") "سند قبض" else "سند صرف"
    val totalDebit = bond.amount
    val totalCredit = if (bond.type == "RECEIPT") bond.amount else 0.0
    val newBalance = kotlin.math.abs(bond.newCustomerBalance)
    val balanceStatus = if (bond.newCustomerBalance >= 0) "عليكم" else "لكم"
    
    return """
📑 *$bondTitle - ${settings.businessName}*
👤 *العميل:* ${bond.customerName} (كود: ${bond.customerCode})
📅 *التاريخ:* ${formatDateTime(bond.date)}

💵 *مبلغ السند:* ${formatCurrency(bond.amount)} $currencyName
🧾 *الرصيد السابق:* ${formatCurrency(kotlin.math.abs(bond.prevCustomerBalance))} $currencyName
⚖️ *الرصيد بعد السند:* ${formatCurrency(newBalance)} $currencyName [$balanceStatus]

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
class BondPrintDocumentAdapter(
    private val context: Context,
    private val bond: Bond,
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
            240, 800, 1
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
        
        // Implement PDF writing logic here (similar to generateBondPdf)
        callback.onWriteFinished(arrayOf(0))
    }
}
