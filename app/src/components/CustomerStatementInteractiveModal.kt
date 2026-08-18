package com.smartlink.erp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.templates.generateCustomerStatementPdf
import com.smartlink.erp.utils.buildCustomerStatementMessage
import com.smartlink.erp.utils.formatCurrency
import com.smartlink.erp.utils.formatDateTime
import com.smartlink.erp.utils.numberToArabicWords
import com.smartlink.erp.utils.sendToWhatsApp
import java.util.*

data class LedgerEntry(
    val id: String,
    val date: Long,
    val ref: String,
    val desc: String,
    val type: String, // "invoice", "payment", "bond"
    val debit: Double,
    val credit: Double,
    var balance: Double
)

@Composable
fun CustomerStatementInteractiveModal(
    customer: Customer,
    invoices: List<Invoice>,
    bonds: List<Bond>,
    settings: SystemSettings,
    onClose: () -> Unit
) {
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "INVOICES", "BONDS"
    
    val customerInvoices by remember {
        derivedStateOf { invoices.filter { it.customerId == customer.id } }
    }
    
    val customerBonds by remember {
        derivedStateOf { bonds.filter { it.customerId == customer.id } }
    }
    
    // Build sorted ledger entries
    val allEntries by remember {
        derivedStateOf {
            val entries = mutableListOf<LedgerEntry>()
            
            customerInvoices.forEach { inv ->
                entries.add(
                    LedgerEntry(
                        id = "inv_${inv.id}",
                        date = inv.date,
                        ref = inv.invoiceNumber,
                        desc = "فاتورة مبيعات (${inv.items.size} أصناف)",
                        type = "invoice",
                        debit = inv.total,
                        credit = 0.0,
                        balance = 0.0
                    )
                )
                
                if (inv.paidAmount > 0) {
                    entries.add(
                        LedgerEntry(
                            id = "pay_${inv.id}",
                            date = inv.date,
                            ref = "سداد ${inv.invoiceNumber}",
                            desc = "دفعة مسددة نقداً مع الفاتورة",
                            type = "payment",
                            debit = 0.0,
                            credit = inv.paidAmount,
                            balance = 0.0
                        )
                    )
                }
            }
            
            customerBonds.forEach { b ->
                if (b.type == "RECEIPT") {
                    entries.add(
                        LedgerEntry(
                            id = "bond_${b.id}",
                            date = b.date,
                            ref = "سند #${b.bondNumber}",
                            desc = b.note ?: "سند قبض نقدي",
                            type = "bond",
                            debit = 0.0,
                            credit = b.amount,
                            balance = 0.0
                        )
                    )
                } else {
                    entries.add(
                        LedgerEntry(
                            id = "bond_${b.id}",
                            date = b.date,
                            ref = "سند #${b.bondNumber}",
                            desc = b.note ?: "سند صرف",
                            type = "bond",
                            debit = b.amount,
                            credit = 0.0,
                            balance = 0.0
                        )
                    )
                }
            }
            
            entries.sortBy { it.date }
            
            var runningBal = 0.0
            entries.forEach { e ->
                runningBal += e.debit - e.credit
                e.balance = runningBal
            }
            
            entries
        }
    }
    
    val totalDebit by remember {
        derivedStateOf { allEntries.sumOf { it.debit } }
    }
    
    val totalCredit by remember {
        derivedStateOf { allEntries.sumOf { it.credit } }
    }
    
    val netBalance by remember {
        derivedStateOf { customer.balance }
    }
    
    val currencyName by remember {
        derivedStateOf { settings.currency ?: "ريال يمني" }
    }
    
    // Filtered view
    val displayedEntries by remember {
        derivedStateOf {
            allEntries.filter { e ->
                when (filterType) {
                    "INVOICES" -> e.type == "invoice"
                    "BONDS" -> e.type == "bond" || e.type == "payment"
                    else -> true
                }
            }
        }
    }
    
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modal Header
                Surface(
                    color = Color(0xFF0F766E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF99F6E4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "كشف حساب:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = customer.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF99F6E4)
                                    )
                                    Surface(
                                        color = Color(0xFF134E4A).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "[${customer.cCode}]",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF99F6E4),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${settings.businessName} • ${formatDateTime()}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF99F6E4).copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Balance & Summary Bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryCard(
                                title = "إجمالي المسحوبات",
                                value = formatCurrency(totalDebit),
                                valueColor = Color(0xFFB91C1C),
                                modifier = Modifier.weight(1f)
                            )
                            
                            SummaryCard(
                                title = "إجمالي المسدد",
                                value = formatCurrency(totalCredit),
                                valueColor = Color(0xFF047857),
                                modifier = Modifier.weight(1f)
                            )
                            
                            SummaryCard(
                                title = "الرصيد المتبقي",
                                value = "${formatCurrency(kotlin.math.abs(netBalance))} ${settings.currencySymbol}",
                                valueColor = if (netBalance > 0) Color(0xFFB91C1C) 
                                             else if (netBalance < 0) Color(0xFF047857) 
                                             else Color(0xFF1E293B),
                                bgColor = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                                borderColor = Color(0xFFFCD34D),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ كتابة: ${numberToArabicWords(kotlin.math.abs(netBalance), currencyName, "فلس")}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (netBalance > 0) "(عليه / مدين)" 
                                     else if (netBalance < 0) "(له / دائن)" 
                                     else "(خالص)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                        
                        // Quick Filter Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterButton(
                                text = "الكل (${allEntries.size})",
                                selected = filterType == "ALL",
                                onClick = { filterType = "ALL" },
                                modifier = Modifier.weight(1f)
                            )
                            FilterButton(
                                text = "فواتير فقط",
                                selected = filterType == "INVOICES",
                                onClick = { filterType = "INVOICES" },
                                modifier = Modifier.weight(1f)
                            )
                            FilterButton(
                                text = "سندات وسداد",
                                selected = filterType == "BONDS",
                                onClick = { filterType = "BONDS" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Interactive Content Area
                if (displayedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد حركات مسجلة لهذا العميل",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    // Mobile Cards (visible on small screens)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayedEntries.reversed(), key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = entry.ref,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = entry.desc,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                        Text(
                                            text = formatDateTime(entry.date),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (entry.debit > 0) {
                                            Text(
                                                text = "+${formatCurrency(entry.debit)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFB91C1C)
                                            )
                                        } else {
                                            Text(
                                                text = "-${formatCurrency(entry.credit)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                        Text(
                                            text = "رصيد: ${formatCurrency(kotlin.math.abs(entry.balance))}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Footer Actions: Print PDF & WhatsApp
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // TODO: Generate PDF
                                // generateCustomerStatementPdf(context, customer, invoices, bonds, settings)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "طباعة أو حفظ A4 PDF 📄",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        
                        Button(
                            onClick = {
                                val msg = buildCustomerStatementMessage(customer, settings, netBalance, customerInvoices, customerBonds)
                                sendToWhatsApp(
                                    androidx.compose.ui.platform.LocalContext.current,
                                    customer.mobile ?: "",
                                    msg
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            )
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "واتساب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    bgColor: Color = Color.White,
    borderColor: Color = Color(0xFFE2E8F0),
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = valueColor
            )
        }
    }
}

@Composable
private fun FilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F766E),
                contentColor = Color.White
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF475569)
            )
        },
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
