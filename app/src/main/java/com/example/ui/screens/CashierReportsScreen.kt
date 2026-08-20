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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.theme.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Invoice
import com.example.data.model.PaymentMethod
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
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
import com.example.utils.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierReportsScreen(
    users: List<User>,
    invoices: List<Invoice>,
    bonds: List<Bond>,
    settings: SystemSettings,
    currentUser: User?
) {
    val context = LocalContext.current
    var selectedUser by remember { mutableStateOf<User?>(currentUser ?: users.firstOrNull()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }

    // Computations for selected user
    val targetInvoices = remember(selectedUser, invoices) {
        if (selectedUser == null) invoices
        else invoices.filter { it.cashierId == selectedUser?.id }
    }

    val targetBonds = remember(selectedUser, bonds) {
        if (selectedUser == null) bonds
        else bonds.filter { it.cashierId == selectedUser?.id }
    }

    val totalSales = targetInvoices.sumOf { it.total }
    val cashSales = targetInvoices.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.paidAmount }
    val creditSales = targetInvoices.sumOf { it.remainingAmount }
    val partialPaid = targetInvoices.filter { it.paymentMethod == PaymentMethod.PARTIAL }.sumOf { it.paidAmount }

    val totalCashFromSales = cashSales + partialPaid
    val totalReceiptsBonds = targetBonds.filter { it.type == BondType.RECEIPT }.sumOf { it.amount }
    val totalPaymentsBonds = targetBonds.filter { it.type == BondType.PAYMENT }.sumOf { it.amount }

    // Net Cash in Drawer for Cashier
    val netCashInDrawer = totalCashFromSales + totalReceiptsBonds - totalPaymentsBonds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        // User Selector
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedUser?.let { "${it.name} (${it.userCode}) - ${it.assignedBranch}" } ?: "جميع المستخدمين",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر الكاشير / نقطة البيع") },
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
                users.forEach { u ->
                    DropdownMenuItem(
                        text = { Text("${u.name} (كود: ${u.userCode})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            selectedUser = u
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Net Cash in Drawer Hero Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "صافي النقدية المتوفرة في الدرج (الكاش الصافي):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${Formatters.formatCurrency(netCashInDrawer)} $currencyName",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val reportText = buildString {
                            appendLine("📊 *تقرير إقفال وردية الكاشير*")
                            appendLine("المحل: ${settings.businessName}")
                            appendLine("الكاشير: ${selectedUser?.name ?: "الكل"} (${selectedUser?.userCode ?: ""})")
                            appendLine("التاريخ: ${Formatters.formatDateTime(Formatters.currentIsoDate())}")
                            appendLine("━━━━━━━━━━━━━━━")
                            appendLine("💰 إجمالي المبيعات: ${Formatters.formatCurrency(totalSales)} $currencyName")
                            appendLine("💵 مبيعات نقداً: ${Formatters.formatCurrency(totalCashFromSales)} $currencyName")
                            appendLine("📑 مبيعات آجلة: ${Formatters.formatCurrency(creditSales)} $currencyName")
                            appendLine("📥 سندات قبض نقدية: +${Formatters.formatCurrency(totalReceiptsBonds)} $currencyName")
                            appendLine("📤 سندات صرف نقدية: -${Formatters.formatCurrency(totalPaymentsBonds)} $currencyName")
                            appendLine("━━━━━━━━━━━━━━━")
                            appendLine("🎯 *صافي النقدية في الدرج:* ${Formatters.formatCurrency(netCashInDrawer)} $currencyName")
                        }
                        WhatsAppHelper.shareText(context, reportText, "مشاركة تقرير الإقفال")
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة تقرير الإقفال عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Breakdown items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "تفصيل حركات الوردية للكاشير:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Slate800
                )
            }

            item {
                ReportRowItem("إجمالي المبيعات الكلية", "${Formatters.formatCurrency(totalSales)} $currencyName", "${targetInvoices.size} فاتورة", TealPrimary)
            }
            item {
                ReportRowItem("المحصل نقداً من الفواتير", "${Formatters.formatCurrency(totalCashFromSales)} $currencyName", "نقدية محصلة", EmeraldSuccess)
            }
            item {
                ReportRowItem("المتبقي آجل (ديون مسجلة)", "${Formatters.formatCurrency(creditSales)} $currencyName", "ذمم مدينة", RoseError)
            }
            item {
                ReportRowItem("مقبوضات بسندات قبض", "+${Formatters.formatCurrency(totalReceiptsBonds)} $currencyName", "${targetBonds.count { it.type == BondType.RECEIPT }} سند", MintSecondary)
            }
            item {
                ReportRowItem("مصروفات بسندات صرف", "-${Formatters.formatCurrency(totalPaymentsBonds)} $currencyName", "${targetBonds.count { it.type == BondType.PAYMENT }} سند", AmberWarning)
            }
        }
    }
}

@Composable
private fun ReportRowItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                Text(subtitle, fontSize = 10.sp, color = Slate500)
            }
            Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, color = color)
        }
    }
}
