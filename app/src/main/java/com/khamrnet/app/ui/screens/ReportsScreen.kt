package com.khamrnet.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.FinancialBondEntity
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.ProductEntity
import com.khamrnet.app.data.model.SystemSettingsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    products: List<ProductEntity>,
    bonds: List<FinancialBondEntity>,
    onNavigateBack: () -> Unit
) {
    val totalSales = remember(invoices) { invoices.sumOf { it.total } }
    val cashSales = remember(invoices) { invoices.filter { it.billType == 1 || it.paymentMethod == "CASH" }.sumOf { it.total } }
    val creditSales = remember(invoices) { invoices.filter { it.billType == 4 || it.paymentMethod == "CREDIT" }.sumOf { it.total } }
    val lowStockProducts = remember(products) { products.filter { it.stockQuantity <= 10 } }

    val totalReceipts = remember(bonds) { bonds.filter { it.bondType == "RECEIPT" }.sumOf { it.amount } }
    val totalPayments = remember(bonds) { bonds.filter { it.bondType == "PAYMENT" }.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("التقارير والإحصائيات", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F766E))
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Sales Overview Cards
            item {
                Text("ملخص المبيعات والحركة المالية", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي المبيعات الكلية:", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text("${totalSales.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبيعات النقدية (كاش):", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text("${cashSales.toInt()} ${settings.currencyName}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبيعات الآجلة (ديون):", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text("${creditSales.toInt()} ${settings.currencyName}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // 2. Bonds Summary
            item {
                Text("حركة السندات والمقبوضات", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("المقبوضات (سندات قبض)", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                            Text("${totalReceipts.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("المصروفات (سندات صرف)", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                            Text("${totalPayments.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // 3. Low Stock Inventory Alert
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نواقص المخزون والأصناف الحرجة", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Text("${lowStockProducts.size} صنف منخفض", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }

            if (lowStockProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text("جميع الأصناف متوفرة بكميات ممتازة بالمستودع", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(lowStockProducts) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(prod.name, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                                Text("كود: ${prod.code}", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "المتبقي: ${prod.stockQuantity.toInt()} ${prod.baseUnitName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
