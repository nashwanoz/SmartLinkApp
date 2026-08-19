package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Invoice
import com.example.data.model.SystemSettings
import kotlin.math.abs

object WhatsAppHelper {

    fun formatInvoiceWhatsAppMessage(invoice: Invoice, settings: SystemSettings): String {
        val currencyName = settings.currency.ifBlank { "ريال يمني" }
        val prevBal = invoice.prevCustomerBalance
        val newBal = invoice.newCustomerBalance
        val totalWords = NumberToArabicWords.convert(newBal, currencyName, "فلس")

        val itemsList = if (invoice.items.isNotEmpty()) {
            "\n📋 *تفاصيل الفاتورة:*\n" + invoice.items.mapIndexed { idx, it ->
                "${idx + 1}. ${it.productName} - (${Formatters.formatCurrency(it.quantity)} ${it.unitName}) × ${Formatters.formatCurrency(it.unitPrice)} = ${Formatters.formatCurrency(it.total)} $currencyName"
            }.joinToString("\n") + "\n"
        } else ""

        val paidSection = if (invoice.paidAmount > 0) {
            "💰 المبلغ المسدد (نقداً): ${Formatters.formatCurrency(invoice.paidAmount)} $currencyName\n"
        } else ""

        val debtStatus = if (newBal >= 0) "عليه" else "له"

        return """
🧾 *فاتورة مبيعات - ${settings.businessName}*
🔢 *رقم الفاتورة:* [${invoice.invoiceNumber}]
📅 *التاريخ:* ${Formatters.formatDateTime(invoice.date)}
$itemsList
👤 عميلنا المحترم : ${invoice.customerName}
💵 عليكم مبلغ سابق : ${Formatters.formatCurrency(prevBal)} $currencyName
🧾 قيمة الفاتورة : ${Formatters.formatCurrency(invoice.total)} $currencyName
${paidSection}⚖️ الرصيد الاجمالي : ${Formatters.formatCurrency(abs(newBal))} $currencyName [$debtStatus]
✍️ فقط : $totalWords
        """.trimIndent()
    }

    fun formatBondWhatsAppMessage(bond: Bond, settings: SystemSettings): String {
        val currencyName = settings.currency.ifBlank { "ريال يمني" }
        val motionType = if (bond.type == BondType.RECEIPT) "سند قبض" else "سند صرف"
        val newBal = bond.newCustomerBalance
        val newStatus = if (newBal >= 0) "عليه" else "له"
        val amountWords = NumberToArabicWords.convert(bond.amount, currencyName, "فلس")

        return """
🧾 *$motionType*
🔢 *رقم السند:* [${bond.bondNumber}]
👤 *اسم العميل:* ${bond.customerName}
💰 *مبلغ السند:* ${Formatters.formatCurrency(bond.amount)} $currencyName ($amountWords)
⚖️ *الرصيد بعد السند:* ${Formatters.formatCurrency(abs(newBal))} $currencyName [$newStatus]
📝 *البيان:* ${bond.note.ifBlank { "سند مالي" }}
📅 *التاريخ:* ${Formatters.formatDateTime(bond.date)}
        """.trimIndent()
    }

    fun formatCustomerStatementWhatsApp(
        customerName: String,
        balance: Double,
        invoicesCount: Int,
        totalPurchases: Double,
        totalPayments: Double,
        settings: SystemSettings
    ): String {
        val currencyName = settings.currency.ifBlank { "ريال يمني" }
        val status = if (balance >= 0) "عليه" else "له"
        val balanceWords = NumberToArabicWords.convert(balance, currencyName, "فلس")

        return """
📊 *كشف حساب - ${settings.businessName}*
👤 *العميل:* $customerName
🧾 *عدد الفواتير:* $invoicesCount
🛒 *إجمالي المشتريات:* ${Formatters.formatCurrency(totalPurchases)} $currencyName
💵 *إجمالي السداد والقبض:* ${Formatters.formatCurrency(totalPayments)} $currencyName
⚖️ *الرصيد الحالي:* ${Formatters.formatCurrency(abs(balance))} $currencyName [$status]
✍️ *فقط:* $balanceWords
📅 *تاريخ التقرير:* ${Formatters.formatDateTime(null)}
        """.trimIndent()
    }

    fun sendWhatsApp(context: Context, phone: String, message: String) {
        val cleanPhone = Formatters.normalizePhoneNumber(phone)
        val encodedMessage = Uri.encode(message)
        val url = if (cleanPhone.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to standard share sheet
            shareText(context, message, "مشاركة عبر واتساب أو التطبيقات")
        }
    }

    fun shareText(context: Context, text: String, title: String = "مشاركة") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة النص", Toast.LENGTH_SHORT).show()
        }
    }
}
