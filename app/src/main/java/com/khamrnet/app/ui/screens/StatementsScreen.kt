package com.khamrnet.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class StatementTransaction(
    val date: String,
    val type: String, // "INVOICE" or "BOND"
    val reference: String,
    val note: String,
    val debit: Double, // مدين (عليه)
    val credit: Double, // دائن (سدد)
    val runningBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    settings: SystemSettingsEntity,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    products: List<ProductEntity>,
    currentUserName: String,
    currentUserRole: String = "ADMIN",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val df = remember { DecimalFormat("#,##0") }

    var activeTab by remember { mutableStateOf("customer") } // "customer" or "cashier"
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showCustomerDropdown by remember { mutableStateOf(false) }

    // Date filters
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
    }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, customerSearchQuery) {
        if (customerSearchQuery.isEmpty()) customers
        else customers.filter {
            it.name.contains(customerSearchQuery, ignoreCase = true) ||
            it.phone.contains(customerSearchQuery) ||
            it.code.contains(customerSearchQuery)
        }
    }

    val selectedCustomer = remember(customers, selectedCustomerId) {
        customers.find { it.id == selectedCustomerId } ?: customers.firstOrNull()
    }

    // Transactions calculation
    val transactions: List<StatementTransaction> = remember(selectedCustomer, invoices, bonds, startDate, endDate) {
        if (selectedCustomer == null) emptyList()
        else {
            val list = mutableListOf<StatementTransaction>()
            val custInvoices = invoices.filter { !it.isCancelled && it.customerId == selectedCustomer.id }
            val custBonds = bonds.filter { it.customerId == selectedCustomer.id }

            for (inv in custInvoices) {
                list.add(
                    StatementTransaction(
                        date = inv.date,
                        type = "INVOICE",
                        reference = "فاتورة #${inv.invoiceNumber.ifEmpty { inv.id.takeLast(4) }}",
                        note = if (inv.remainingAmount > 0) "فاتورة آجلة" else "فاتورة نقدية",
                        debit = inv.total,
                        credit = inv.paidAmount,
                        runningBalance = 0.0
                    )
                )
            }

            for (b in custBonds) {
                val isReceipt = b.type == "RECEIPT" || b.bondType == "RECEIPT"
                list.add(
                    StatementTransaction(
                        date = b.date,
                        type = "BOND",
                        reference = "سند #${b.bondNumber.ifEmpty { b.id.takeLast(4) }}",
                        note = b.notes.ifEmpty { if (isReceipt) "سند قبض نقدي" else "سند صرف" },
                        debit = if (!isReceipt) b.amount else 0.0,
                        credit = if (isReceipt) b.amount else 0.0,
                        runningBalance = 0.0
                    )
                )
            }

            // Sort by date ascending
            list.sortBy { it.date }

            // Calculate running balance
            var balance = 0.0
            val computedList = mutableListOf<StatementTransaction>()
            for (item in list) {
                balance += (item.debit - item.credit)
                computedList.add(item.copy(runningBalance = balance))
            }
            computedList
        }
    }

    val totalDebits = transactions.sumOf { it.debit }
    val totalCredits = transactions.sumOf { it.credit }
    val netBalance = totalDebits - totalCredits

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "كشوفات الحساب والتقارير المالية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "كشف حساب تفصيلي للعميل ومطابقة الأرصدة",
                                fontSize = 9.5.sp,
                                color = Color(0xFFCCFBF1)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F766E))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Sub Tabs Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == "customer") Color(0xFF0F766E) else Color.Transparent)
                            .clickable { activeTab = "customer" }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (activeTab == "customer") Color.White else Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "كشف حساب عميل",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == "customer") FontWeight.Black else FontWeight.Bold,
                                color = if (activeTab == "customer") Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == "cashier") Color(0xFF0F766E) else Color.Transparent)
                            .clickable { activeTab = "cashier" }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = if (activeTab == "cashier") Color.White else Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "تقارير وتصفية الكواشير",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == "cashier") FontWeight.Black else FontWeight.Bold,
                                color = if (activeTab == "cashier") Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            if (activeTab == "customer") {
                // 2. Customer Selection Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "اختر العميل لاستعراض كشف الحساب:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )

                            OutlinedTextField(
                                value = customerSearchQuery,
                                onValueChange = {
                                    customerSearchQuery = it
                                    showCustomerDropdown = true
                                },
                                placeholder = { Text("ابحث باسم أو هاتف العميل...", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF64748B))
                                },
                                trailingIcon = {
                                    if (customerSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { customerSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Fast Customer Selector Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredCustomers.take(4).forEach { cust ->
                                    val isSelected = selectedCustomer?.id == cust.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF0F766E) else Color(0xFFF1F5F9))
                                            .clickable {
                                                selectedCustomerId = cust.id
                                                customerSearchQuery = ""
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Customer Summary Card
                if (selectedCustomer != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedCustomer.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "هاتف: ${selectedCustomer.phone.ifEmpty { "غير مسجل" }} | كود: ${selectedCustomer.code}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Balance Status Pill
                                    val statusColor = when {
                                        netBalance > 0 -> Color(0xFFDC2626)
                                        netBalance < 0 -> Color(0xFF059669)
                                        else -> Color(0xFF0F766E)
                                    }
                                    val statusBg = when {
                                        netBalance > 0 -> Color(0xFFFEF2F2)
                                        netBalance < 0 -> Color(0xFFECFDF5)
                                        else -> Color(0xFFF0FDFA)
                                    }
                                    val statusText = when {
                                        netBalance > 0 -> "عليه مديونية"
                                        netBalance < 0 -> "له رصيد دائن"
                                        else -> "الحساب خالص"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(statusBg)
                                            .border(0.6.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = statusColor
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF0FDFA))
                                            .padding(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text("إجمالي المسحوبات", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text("${df.format(totalDebits)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFECFDF5))
                                            .padding(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text("إجمالي المدفوعات", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text("${df.format(totalCredits)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (netBalance > 0) Color(0xFFFEF2F2) else Color(0xFFF8FAFC))
                                            .padding(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text("صافي الرصيد", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text(
                                                "${df.format(netBalance.coerceAtLeast(0.0))} ${settings.currencyName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (netBalance > 0) Color(0xFFDC2626) else Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }

                                // WhatsApp & Share Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val message = "📄 *كشف حساب مالي*\n" +
                                                    "🏢 ${settings.businessName}\n" +
                                                    "👤 العميل: ${selectedCustomer.name}\n" +
                                                    "----------------------------\n" +
                                                    "📊 إجمالي المبيعات والمسحوبات: ${df.format(totalDebits)} ${settings.currencyName}\n" +
                                                    "💵 إجمالي السداد والمقبوضات: ${df.format(totalCredits)} ${settings.currencyName}\n" +
                                                    "🏦 *صافي الرصيد المتبقي: ${df.format(netBalance)} ${settings.currencyName}*\n" +
                                                    "----------------------------\n" +
                                                    "شكراً لتعاملكم معنا 🙏"

                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                val cleanPhone = selectedCustomer.phone.replace(Regex("[^0-9]"), "")
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, message)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إرسال عبر الواتساب 💬", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Transactions List
                    item {
                        Text(
                            text = "سجل العمليات والحركات المالية (${transactions.size}):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = "لا توجد عمليات مسجلة لهذا العميل حتى الآن",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(transactions) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tx.reference,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (tx.type == "INVOICE") Color(0xFF0F766E) else Color(0xFF059669)
                                            )
                                            Text(
                                                text = tx.date.take(10),
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Text(
                                            text = tx.note,
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (tx.debit > 0) {
                                            Text(
                                                text = "+ ${df.format(tx.debit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        if (tx.credit > 0) {
                                            Text(
                                                text = "- ${df.format(tx.credit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                        Text(
                                            text = "الرصيد: ${df.format(tx.runningBalance)}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Cashier Reports tab embedded
                item {
                    CashierReportsSectionContent(
                        settings = settings,
                        invoices = invoices,
                        bonds = bonds,
                        products = products,
                        currentUserName = currentUserName,
                        currentUserRole = currentUserRole
                    )
                }
            }
        }
    }
}

@Composable
private fun CashierReportsSectionContent(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    products: List<ProductEntity>,
    currentUserName: String,
    currentUserRole: String
) {
    val context = LocalContext.current
    val df = remember { DecimalFormat("#,##0") }

    val totalSales = invoices.filter { !it.isCancelled }.sumOf { it.total }
    val paidInvoices = invoices.filter { !it.isCancelled }.sumOf { it.paidAmount }
    val bondsCollected = bonds.filter { it.type == "RECEIPT" }.sumOf { it.amount }
    val drawerCash = paidInvoices + bondsCollected

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ملخص حركة ونقدية الصندوق:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0FDFA))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("إجمالي المبيعات", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text("${df.format(totalSales)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("المقبوض كاش", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text("${df.format(drawerCash)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                    }
                }
            }

            Button(
                onClick = {
                    val shareText = "📊 *تقرير تصفية الصندوق العام*\n" +
                            "🏢 ${settings.businessName}\n" +
                            "----------------------------\n" +
                            "📈 إجمالي المبيعات: ${df.format(totalSales)} ${settings.currencyName}\n" +
                            "💵 نقدية الفواتير: ${df.format(paidInvoices)} ${settings.currencyName}\n" +
                            "📑 مقبوضات السندات: ${df.format(bondsCollected)} ${settings.currencyName}\n" +
                            "🏦 *صافي نقدية الصندوق: ${df.format(drawerCash)} ${settings.currencyName}*\n" +
                            "----------------------------"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير التصفية"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة وطباعة التقرير الشامل 📄", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
