package com.smartlink.erp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.SystemSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object WhatsAppUtils {
    
    /**
     * إرسال رسالة نصية فقط عبر واتساب
     */
    fun sendTextOnly(context: Context, phoneNumber: String, message: String) {
        try {
            val encodedMessage = Uri.encode(message)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMessage")
            
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed - open web version
            try {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMessage")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * مشاركة ملف PDF + نص عبر واتساب
     */
    fun sharePdfWithText(context: Context, pdfFile: File, phoneNumber: String, message: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // تحقق إذا كان واتساب مثبت
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * تنسيق رسالة واتساب للفاتورة
     */
    fun formatInvoiceWhatsAppMessage(invoice: Invoice, settings: SystemSettings): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        val dateStr = dateFormat.format(Date(invoice.date))
        
        val itemsList = invoice.items.joinToString("\n") { item ->
            "• ${item.productName} × ${item.quantity} = ${String.format("%,.2f", item.total)} ${settings.currencySymbol}"
        }
        
        return """
🧾 *فاتورة مبيعات*
━━━━━━━━━━━━━━━━
📌 رقم الفاتورة: *${invoice.invoiceNumber}*
📅 التاريخ: $dateStr
👤 العميل: ${invoice.customerName}
🏪 الكاشير: ${invoice.cashierName}
━━━━━━━━━━━━━━━━

📦 *تفاصيل الأصناف:*
$itemsList

━━━━━━━━━━━━━━━━
💰 *الإجمالي:* ${String.format("%,.2f", invoice.total)} ${settings.currencySymbol}
💵 *المدفوع:* ${String.format("%,.2f", invoice.paidAmount)} ${settings.currencySymbol}
📊 *المتبقي:* ${String.format("%,.2f", invoice.remainingAmount)} ${settings.currencySymbol}

━━━━━━━━━━━━━━━━
${settings.companyName ?: "شكراً لتعاملكم معنا!"}
📞 ${settings.supportPhone ?: ""}
        """.trimIndent()
    }
    
    /**
     * نسخ نص إلى الحافظة
     */
    fun copyToClipboard(context: Context, text: String): Boolean {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Invoice", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم نسخ النص بنجاح ✓", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "فشل النسخ", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
