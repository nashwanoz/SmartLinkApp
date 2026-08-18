package com.smartlink.erp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.InvoiceItem
import com.smartlink.erp.data.local.entity.SystemSettings
import java.text.SimpleDateFormat
import java.util.*

/**
 * Format currency number with commas
 */
fun formatCurrency(amount: Double): String {
    return String.format("%,.2f", amount)
}

/**
 * Format date & time nicely in Arabic locale (DD/MM/YYYY, HH:MM:SS am/pm)
 */
fun formatDateTime(timestamp: Long? = null): String {
    val d = timestamp?.let { Date(it) } ?: Date()
    
    val day = String.format("%02d", d.date)
    val month = String.format("%02d", d.month + 1)
    val year = d.year + 1900
    
    var hours = d.hours
    val minutes = String.format("%02d", d.minutes)
    val seconds = String.format("%02d", d.seconds)
    val ampm = if (hours >= 12) "pm" else "am"
    hours = if (hours % 12 == 0) 12 else hours % 12
    
    return "$day/$month/$year, ${String.format("%02d", hours)}:$minutes:$seconds $ampm"
}

/**
 * Generates the WhatsApp Bond message format:
 * - سند قبض / سند صرف
 * - رقم السند
 * - اسم العميل
 * - مبلغ السند
 * - الرصيد بعد السند
 */
fun formatBondWhatsAppMessage(
    bond: Bond,
    customer: Customer?,
    settings: SystemSettings
): String {
    val currencyName = settings.currency ?: "ريال يمني"
    val motionType = if (bond.type == "RECEIPT") "سند قبض" else "سند صرف"
    val newBal = bond.newCustomerBalance
    val newStatus = if (newBal >= 0) "عليه" else "له"
    val amountWords = numberToArabicWords(bond.amount, currencyName, "فلس")
    
    return """
🧾 *$motionType*
🔢 *رقم السند:* [${bond.bondNumber}]
👤 *اسم العميل:* ${bond.customerName}
💰 *مبلغ السند:* ${formatCurrency(bond.amount)} $currencyName ($amountWords)
⚖️ *الرصيد بعد السند:* ${formatCurrency(kotlin.math.abs(newBal))} $currencyName [$newStatus]
📝 *البيان:* ${bond.note ?: "سند مالي"}
📅 *التاريخ:* ${formatDateTime(bond.date)}
    """.trimIndent()
}

/**
 * Generates the EXACT WhatsApp Invoice message text & footer
 */
fun formatInvoiceWhatsAppMessage(
    invoice: Invoice,
    settings: SystemSettings
): String {
    val currencyName = settings.currency ?: "ريال يمني"
    val prevBal = invoice.prevCustomerBalance
    val newBal = invoice.newCustomerBalance
    val totalWords = numberToArabicWords(newBal, currencyName, "فلس")
    
    var itemsList = ""
    if (invoice.items.isNotEmpty()) {
        itemsList = "\n📋 *تفاصيل الفاتورة:*\n" + invoice.items.mapIndexed { idx, it ->
            "${idx + 1}. ${it.productName} - (${it.quantity} ${it.unitName}) × ${formatCurrency(it.unitPrice)} = ${formatCurrency(it.total)} $currencyName"
        }.joinToString("\n") + "\n"
    }
    
    val paidText = if (invoice.paidAmount > 0) {
        "💰 المبلغ المسدد (نقداً): ${formatCurrency(invoice.paidAmount)} $currencyName\n"
    } else {
        ""
    }
    
    return """
🧾 *فاتورة مبيعات - ${settings.businessName}*
🔢 *رقم الفاتورة:* [${invoice.invoiceNumber}]
📅 *التاريخ:* ${formatDateTime(invoice.date)}
$itemsList
👤 عميلنا المحترم : ${invoice.customerName}
💵 عليكم مبلغ سابق : ${formatCurrency(prevBal)} $currencyName
🧾 قيمة الفاتورة : ${formatCurrency(invoice.total)} $currencyName
$paidText⚖️ الرصيد الاجمالي : ${formatCurrency(newBal)} $currencyName
✍️ فقط : $totalWords
    """.trimIndent()
}

/**
 * Normalizes phone numbers: converts Arabic/Eastern numerals to English digits,
 * strips non-numeric characters, and ensures correct country code prefix.
 */
fun normalizePhoneNumber(phone: String): String {
    if (phone.isBlank()) return ""
    
    // Convert Arabic/Eastern digits (٠-٩ and ۰-۹) to standard ASCII 0-9
    val arabicToEnglish = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
    )
    
    val normalized = phone.replace("[٠-٩۰-۹]".toRegex()) { matchResult ->
        arabicToEnglish[matchResult.value[0]] ?: matchResult.value[0]
    }
    
    var clean = normalized.replace("[^0-9]".toRegex(), "")
    
    if (clean.isEmpty()) return ""
    
    // Remove leading zeros like 00967 -> 967 or 00966 -> 966
    if (clean.startsWith("00")) {
        clean = clean.substring(2)
    }
    
    // If Yemen local phone format (starts with 07... or 7... with 9 digits)
    if (clean.startsWith("07") && clean.length == 10) {
        clean = "967" + clean.substring(1)
    } else if ((clean.startsWith("70") || clean.startsWith("71") || 
                clean.startsWith("73") || clean.startsWith("77") || 
                clean.startsWith("78")) && clean.length == 9) {
        clean = "967" + clean
    } else if (clean.startsWith("0") && clean.length >= 9) {
        clean = "967" + clean.substring(1)
    }
    
    return clean
}

/**
 * Open WhatsApp directly with the phone number and formatted text
 */
fun sendToWhatsApp(context: Context, phone: String, text: String) {
    val cleanPhone = normalizePhoneNumber(phone)
    val encodedText = Uri.encode(text)
    
    // If phone number exists, send to specific phone, otherwise open WhatsApp to choose contact
    val url = if (cleanPhone.isNotEmpty()) {
        "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
    } else {
        "https://api.whatsapp.com/send?text=$encodedText"
    }
    
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: open browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

/**
 * Copy text to clipboard helper
 */
fun copyToClipboard(context: Context, text: String): Boolean {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("SmartLink ERP", text)
        clipboard.setPrimaryClip(clip)
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * Build customer statement message for WhatsApp
 */
fun buildCustomerStatementMessage(
    customer: Customer,
    settings: SystemSettings,
    netBalance: Double,
    invoices: List<Invoice>,
    bonds: List<Bond>
): String {
    val totalDebit = invoices.sumOf { it.total }
    val totalCredit = bonds.filter { it.type == "RECEIPT" }.sumOf { it.amount } +
                      invoices.sumOf { it.paidAmount }
    val currencyName = settings.currency ?: "ريال يمني"
    
    return """
📑 *كشف حساب عميل - ${settings.businessName}*
👤 *العميل:* ${customer.name} (كود: ${customer.cCode})
📅 *التاريخ:* ${formatDateTime()}

🧾 *إجمالي المسحوبات:* ${formatCurrency(totalDebit)} $currencyName
💵 *إجمالي المسدد:* ${formatCurrency(totalCredit)} $currencyName
⚖️ *الرصيد المتبقي:* ${formatCurrency(kotlin.math.abs(netBalance))} $currencyName [${if (netBalance >= 0) "عليكم" else "لكم"}]
    """.trimIndent()
}

/**
 * Build invoice WhatsApp message with PDF
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

/**
 * Build bond WhatsApp message with PDF
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
