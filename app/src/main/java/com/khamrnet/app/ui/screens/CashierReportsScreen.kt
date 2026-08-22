package com.khamrnet.app.ui.screens

import android.content.Intent
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

data class CashierStockItem(
    val product: ProductEntity,
    val transferredQty: Double,
    val soldUnits: Double,
    val currentStock: Double,
    val stockValue: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierReportsScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    products: List<ProductEntity>,
    currentUserName: String,
    currentUserRole: String = "ADMIN",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val df = remember { DecimalFormat("#,##0") }

    var activeReportType by remember { mutableStateOf("cashier") } // "cashier" or "inventory"

    // Helper for today's date
    val todayDate = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.format(Date())
    }

    // Unique cashiers list extracted from invoices and bonds
    val cashierNames = remember(invoices, bonds) {
        val names = (invoices.map { it.createdBy } + bonds.map { it.createdBy })
            .filter { it.isNotEmpty() }
            .distinct()
        if (names.isEmpty()) listOf("المدير", "كاشير 1") else names
    }

    var selectedCashier by remember {
        mutableStateOf(if (currentUserRole == "CASHIER") currentUserName else cashierNames.firstOrNull() ?: "المدير")
    }

    // Date filters
    var invoiceStartDate by remember { mutableStateOf(todayDate) }
    var invoiceEndDate by remember { mutableStateOf(todayDate) }
    var receiptStartDate by remember { mutableStateOf(todayDate) }
    var receiptEndDate by remember { mutableStateOf(todayDate) }
    var inventoryStartDate by remember { mutableStateOf(todayDate) }
    var inventoryEndDate by remember { mutableStateOf(todayDate) }

    // Computations for selected cashier
    val effectiveCashier = if (currentUserRole == "CASHIER") currentUserName else selectedCashier

    val cashierInvoices = remember(invoices, effectiveCashier) {
        invoices.filter { !it.isCancelled && (it.createdBy == effectiveCashier || effectiveCashier == "الكل") }
    }

    val cashierBonds = remember(bonds, effectiveCashier) {
        bonds.filter { it.type == "RECEIPT" && (it.createdBy == effectiveCashier || effectiveCashier == "الكل") }
    }

    val totalSales = cashierInvoices.sumOf { it.total }
    val paidInvoices = cashierInvoices.sumOf { it.paidAmount }
    val bondsCollected = cashierBonds.sumOf { it.amount }
    val drawerCash = paidInvoices + bondsCollected

    // Stock report for Cashier
    val cashierStockList: List<CashierStockItem> = remember(products, cashierInvoices) {
        products.map { p ->
            val sold = cashierInvoices.flatMap { it.items ?: emptyList() }
                .filter { it.productId == p.id }
                .sumOf { it.quantity * it.unitFactor }
            val currentStock = (p.stockQuantity - sold).coerceAtLeast(0.0)
            val stockVal = currentStock * p.salePrice
            CashierStockItem(
                product = p,
                transferredQty = p.stockQuantity,
                soldUnits = sold,
                currentStock = currentStock,
                stockValue = stockVal
            )
        }
    }

    val totalStockValue = remember(cashierStockList) {
        cashierStockList.sumOf { it.stockValue }
    }

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
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "تقارير وتصفية الكواشير",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "متابعة إيرادات ومبيعات ومخزون عهدة كل مستخدم",
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
                    TabButton(
                        modifier = Modifier.weight(1f),
                        title = "كشف مبيعات وتصفية كاشير",
                        icon = Icons.Default.Receipt,
                        selected = activeReportType == "cashier",
                        onClick = { activeReportType = "cashier" }
                    )
                    TabButton(
                        modifier = Modifier.weight(1f),
                        title = "كشف مخزون ومطابقة كاشير",
                        icon = Icons.Default.Inventory2,
                        selected = activeReportType == "inventory",
                        onClick = { activeReportType = "inventory" }
                    )
                }
            }

            // 2. Cashier Selector Card
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
                            text = if (currentUserRole == "CASHIER") "حساب الكاشير الحالي:" else "اختر الكاشير / المستخدم:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        if (currentUserRole == "CASHIER") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDFA))
                                    .border(0.8.dp, Color(0xFF99F6E4), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentUserName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F766E)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFCCFBF1))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("كاشير", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cashierNames.forEach { name ->
                                    val isSelected = selectedCashier == name
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF0F766E) else Color(0xFFF1F5F9))
                                            .clickable { selectedCashier = name }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF475569),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 4 Stats KPI Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KpiCard(
                                modifier = Modifier.weight(1f),
                                label = "إجمالي الفواتير",
                                value = "${df.format(totalSales)} ${settings.currencyName}",
                                count = "${cashierInvoices.size} فواتير",
                                color = Color(0xFF0F766E),
                                bgColor = Color(0xFFF0FDFA)
                            )
                            KpiCard(
                                modifier = Modifier.weight(1f),
                                label = "المقبوض بالفواتير",
                                value = "${df.format(paidInvoices)} ${settings.currencyName}",
                                count = "سداد فوري",
                                color = Color(0xFF059669),
                                bgColor = Color(0xFFECFDF5)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KpiCard(
                                modifier = Modifier.weight(1f),
                                label = "المقبوض بسندات",
                                value = "${df.format(bondsCollected)} ${settings.currencyName}",
                                count = "${cashierBonds.size} سندات",
                                color = Color(0xFFD97706),
                                bgColor = Color(0xFFFFFBEB)
                            )
                            KpiCard(
                                modifier = Modifier.weight(1f),
                                label = "صافي الصندوق للتصفية",
                                value = "${df.format(drawerCash)} ${settings.currencyName}",
                                count = "توريد الصندوق",
                                color = Color.White,
                                bgColor = Color(0xFF0F766E)
                            )
                        }
                    }
                }
            }

            // Report Details based on active tab
            if (activeReportType == "cashier") {
                // Invoices Report Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF0F766E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تقرير فواتير ومبيعات الكاشير",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "${cashierInvoices.size} فاتورة",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Button(
                                onClick = {
                                    val shareText = "📄 *كشف فواتير ومبيعات الكاشير: $effectiveCashier*\n" +
                                            "🏢 ${settings.businessName}\n" +
                                            "----------------------------\n" +
                                            "📊 إجمالي المبيعات: ${df.format(totalSales)} ${settings.currencyName}\n" +
                                            "💵 المقبوض نقداً: ${df.format(paidInvoices)} ${settings.currencyName}\n" +
                                            "🧾 عدد الفواتير: ${cashierInvoices.size}\n" +
                                            "----------------------------\n" +
                                            "تاريخ الكشف: $todayDate"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير المبيعات"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة وطباعة كشف الفواتير 📄", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }

                // Receipts Report Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "المقبوضات والتحصيلات النقدية",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "${cashierInvoices.count { it.paidAmount > 0 } + cashierBonds.size} عملية",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }

                            Button(
                                onClick = {
                                    val shareText = "💵 *كشف المقبوضات والتحصيلات: $effectiveCashier*\n" +
                                            "🏢 ${settings.businessName}\n" +
                                            "----------------------------\n" +
                                            "💰 مقبوض بالفواتير: ${df.format(paidInvoices)} ${settings.currencyName}\n" +
                                            "📑 مقبوض بسندات قبض: ${df.format(bondsCollected)} ${settings.currencyName}\n" +
                                            "🏦 صافي النقدية للتصفية: ${df.format(drawerCash)} ${settings.currencyName}\n" +
                                            "----------------------------\n" +
                                            "تاريخ الكشف: $todayDate"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة كشف المقبوضات"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة وطباعة كشف المقبوضات 💵", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Inventory Matching Table
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مطابقة وجرد عهدة الأصناف (${cashierStockList.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "إجمالي: ${df.format(totalStockValue)} ${settings.currencyName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF7C3AED)
                                )
                            }

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الصنف", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1.5f))
                                Text("الوارد", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text("المباع", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text("المتبقي", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text("القيمة", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            }

                            // Table Items
                            cashierStockList.take(20).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.product.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.weight(1.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${item.transferredQty.toInt()}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${item.soldUnits.toInt()}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${item.currentStock.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${df.format(item.stockValue)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF8FAFC))
                            }

                            // Bottom Value Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFAF5FF))
                                    .border(0.8.dp, Color(0xFFE9D5FF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي قيمة البضاعة المتبقية بالعهدة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                                    Text("${df.format(totalStockValue)} ${settings.currencyName}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF581C87))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF0F766E) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = if (selected) Color.White else Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    count: String,
    color: Color,
    bgColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.6.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (bgColor == Color(0xFF0F766E)) Color(0xFFCCFBF1) else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (bgColor == Color(0xFF0F766E)) Color.White else color,
                maxLines = 1
            )
            Text(
                text = count,
                fontSize = 8.sp,
                color = if (bgColor == Color(0xFF0F766E)) Color(0xFF99F6E4) else Color(0xFF94A3B8)
            )
        }
    }
}
