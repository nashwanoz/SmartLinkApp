package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.SystemSettings
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.utils.Formatters
import com.example.utils.NumberToArabicWords
import com.example.utils.WhatsAppHelper
import kotlin.math.abs

data class StatementEntry(
    val id: String,
    val date: String,
    val opType: String,
    val opNumber: String,
    val description: String,
    val debit: Double, // مدين (عليه)
    val credit: Double, // دائن (له)
    val runningBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    customers: List<Customer>,
    invoices: List<Invoice>,
    bonds: List<Bond>,
    settings: SystemSettings,
    preselectedCustomerId: String? = null
) {
    val context = LocalContext.current
    var selectedCustomer by remember {
        mutableStateOf(customers.find { it.id == preselectedCustomerId } ?: customers.firstOrNull())
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }

    // Compute statement entries for selected customer
    val entries = remember(selectedCustomer, invoices, bonds) {
        val cust = selectedCustomer ?: return@remember emptyList<StatementEntry>()
        val custInvoices = invoices.filter { it.customerId == cust.id }
        val custBonds = bonds.filter { it.customerId == cust.id }

        val rawList = mutableListOf<StatementEntry>()

        custInvoices.forEach { inv ->
            rawList.add(
                StatementEntry(
                    id = inv.id,
                    date = inv.date,
                    opType = "فاتورة مبيعات",
                    opNumber = inv.invoiceNumber,
                    description = "مشتريات: " + inv.items.joinToString("، ") { "${it.productName} (${it.quantity.toInt()})" },
                    debit = inv.remainingAmount,
                    credit = inv.paidAmount,
                    runningBalance = 0.0
                )
            )
        }

        custBonds.forEach { bond ->
            if (bond.type == BondType.RECEIPT) {
                rawList.add(
                    StatementEntry(
                        id = bond.id,
                        date = bond.date,
                        opType = "سند قبض",
                        opNumber = bond.bondNumber,
                        description = bond.note.ifBlank { "سداد من الحساب نقداً" },
                        debit = 0.0,
                        credit = bond.amount,
                        runningBalance = 0.0
                    )
                )
            } else {
                rawList.add(
                    StatementEntry(
                        id = bond.id,
                        date = bond.date,
                        opType = "سند صرف",
                        opNumber = bond.bondNumber,
                        description = bond.note.ifBlank { "صرف مالي" },
                        debit = bond.amount,
                        credit = 0.0,
                        runningBalance = 0.0
                    )
                )
            }
        }

        // Sort by date ascending to calculate running balance
        rawList.sortBy { it.date }

        var running = 0.0
        rawList.map { entry ->
            running += (entry.debit - entry.credit)
            entry.copy(runningBalance = running)
        }.reversed() // Show newest on top
    }

    val totalPurchases = invoices.filter { it.customerId == selectedCustomer?.id }.sumOf { it.total }
    val totalReceipts = bonds.filter { it.customerId == selectedCustomer?.id && it.type == BondType.RECEIPT }.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize()) {
        // Customer Selector Dropdown
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} (${it.cCode}) - الرصيد: ${Formatters.formatCurrency(it.balance)}" } ?: "اختر العميل",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر العميل لعرض كشف الحساب") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                customers.forEach { cust ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val debtStatus = if (cust.balance >= 0) "عليه" else "له"
                                Text(
                                    "${Formatters.formatCurrency(abs(cust.balance))} [$debtStatus]",
                                    fontSize = 11.sp,
                                    color = if (cust.balance > 0) RoseError else EmeraldSuccess
                                )
                            }
                        },
                        onClick = {
                            selectedCustomer = cust
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Customer Summary Header Card
        selectedCustomer?.let { cust ->
            val isDebit = cust.balance > 0
            val isCredit = cust.balance < 0
            val statusText = if (isDebit) "مدين (عليه)" else if (isCredit) "دائن (له)" else "الحساب متزن"
            val statusColor = if (isDebit) RoseError else if (isCredit) MintSecondary else EmeraldSuccess
            val statusBg = if (isDebit) RoseContainer else if (isCredit) MintContainer else EmeraldContainer

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = cust.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "كود: ${cust.cCode} • هاتف: ${cust.mobile}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${Formatters.formatCurrency(abs(cust.balance))} $currencyName",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = statusColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate100)

                    // Quick Share Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val msg = WhatsAppHelper.formatCustomerStatementWhatsApp(
                                    customerName = cust.name,
                                    balance = cust.balance,
                                    invoicesCount = entries.count { it.opType.contains("فاتورة") },
                                    totalPurchases = totalPurchases,
                                    totalPayments = totalReceipts,
                                    settings = settings
                                )
                                WhatsAppHelper.sendWhatsApp(context, cust.mobile, msg)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال كشف واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val msg = WhatsAppHelper.formatCustomerStatementWhatsApp(
                                    customerName = cust.name,
                                    balance = cust.balance,
                                    invoicesCount = entries.count { it.opType.contains("فاتورة") },
                                    totalPurchases = totalPurchases,
                                    totalPayments = totalReceipts,
                                    settings = settings
                                )
                                WhatsAppHelper.shareText(context, msg, "كشف حساب ${cust.name}")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة كشف الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Ledger Statement Table Header
        Surface(
            color = Slate100,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حركات الحساب (${entries.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Text("مدين (عليه) / دائن (له)", fontSize = 10.sp, color = Slate500)
            }
        }

        // Ledger entries
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(entries) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (entry.opType.contains("قبض")) EmeraldContainer else if (entry.opType.contains("صرف")) RoseContainer else TealContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (entry.opType.contains("فاتورة")) Icons.Default.ReceiptLong else Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (entry.opType.contains("قبض")) EmeraldSuccess else if (entry.opType.contains("صرف")) RoseError else TealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${entry.opType} [${entry.opNumber}]",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = Formatters.formatDateTime(entry.date),
                                        fontSize = 9.sp,
                                        color = Slate400
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (entry.debit > 0) {
                                    Text(
                                        text = "+${Formatters.formatCurrency(entry.debit)} $currencyName (مدين)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseError
                                    )
                                }
                                if (entry.credit > 0) {
                                    Text(
                                        text = "-${Formatters.formatCurrency(entry.credit)} $currencyName (دائن)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }

                        if (entry.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.description,
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد حركات مسجلة لهذا العميل حتى الآن", color = Slate400, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
