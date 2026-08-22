package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class LedgerEntry(
    val id: String,
    val date: String,
    val reference: String,
    val description: String,
    val debitAccount: String,
    val creditAccount: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingLedgerScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    customers: List<CustomerEntity>,
    currentUserName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencyName.ifEmpty { "YER" }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    var selectedTab by remember { mutableStateOf("ALL") }

    // Generate accounting double-entry records from real invoices & bonds
    val ledgerEntries = remember(invoices, bonds) {
        val list = mutableListOf<LedgerEntry>()

        // 1. Invoices entries
        invoices.filter { !it.isCancelled }.forEach { inv ->
            val dateStr = sdf.format(Date(inv.date))
            // Cash part
            if (inv.paidAmount > 0) {
                list.add(
                    LedgerEntry(
                        id = "inv_cash_${inv.id}",
                        date = dateStr,
                        reference = "فاتورة #${inv.invoiceNumber}",
                        description = "مبيعات نقدية - ${inv.customerName}",
                        debitAccount = "1101 - الصندوق والنقدية",
                        creditAccount = "4101 - إيراد المبيعات",
                        amount = inv.paidAmount
                    )
                )
            }
            // Credit part
            if (inv.remainingAmount > 0) {
                list.add(
                    LedgerEntry(
                        id = "inv_credit_${inv.id}",
                        date = dateStr,
                        reference = "فاتورة #${inv.invoiceNumber}",
                        description = "مبيعات آجلة على الحساب - ${inv.customerName}",
                        debitAccount = "1201 - ذمم العملاء (${inv.customerName})",
                        creditAccount = "4101 - إيراد المبيعات",
                        amount = inv.remainingAmount
                    )
                )
            }
        }

        // 2. Bonds entries
        bonds.forEach { bond ->
            val dateStr = sdf.format(Date(bond.date))
            if (bond.type == "RECEIPT") {
                list.add(
                    LedgerEntry(
                        id = "bond_rec_${bond.id}",
                        date = dateStr,
                        reference = "سند قبض #${bond.bondNumber}",
                        description = "تحصيل دفعة نقدية من ${bond.customerName} - ${bond.notes}",
                        debitAccount = "1101 - الصندوق والنقدية",
                        creditAccount = "1201 - ذمم العملاء (${bond.customerName})",
                        amount = bond.amount
                    )
                )
            } else {
                list.add(
                    LedgerEntry(
                        id = "bond_pay_${bond.id}",
                        date = dateStr,
                        reference = "سند صرف #${bond.bondNumber}",
                        description = "صرف نقدي لـ ${bond.customerName} - ${bond.notes}",
                        debitAccount = "5101 - المصاريف والمدفوعات",
                        creditAccount = "1101 - الصندوق والنقدية",
                        amount = bond.amount
                    )
                )
            }
        }

        list.sortedByDescending { it.date }
    }

    val totalDebitCredit = remember(ledgerEntries) {
        ledgerEntries.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "دفتر اليومية والقيود المحاسبية",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "نظام القيد المزدوج التلقائي للحركات المالية",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إجمالي حركة القيود (المدين والدائن)", fontSize = 12.sp, color = Color(0xFFCCFBF1))
                            Text(
                                "${numberFormat.format(totalDebitCredit)} $currency",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text("عدد القيود المحاسبية: ${ledgerEntries.size}", fontSize = 11.sp, color = Color(0xFF99F6E4))
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            // Entries List
            if (ledgerEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد قيود محاسبية مسجلة حتى الآن", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                items(ledgerEntries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        entry.reference,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        entry.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Text(
                                    "${numberFormat.format(entry.amount)} $currency",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F766E)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("من حـ / (المدين)", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                    Text(entry.debitAccount, fontSize = 11.sp, color = Color(0xFF334155))
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("إلى حـ / (الدائن)", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                    Text(entry.creditAccount, fontSize = 11.sp, color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
