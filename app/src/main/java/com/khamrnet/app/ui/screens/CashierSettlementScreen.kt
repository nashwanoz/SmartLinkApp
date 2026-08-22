package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.BondEntity
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.ProductEntity
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierSettlementScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    products: List<ProductEntity>,
    currentUserName: String,
    onSaveSettlement: (Double, String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencyName.ifEmpty { "YER" }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    // List of simulated cashiers or extracted from invoices/bonds
    val distinctCashiers = remember(invoices, bonds) {
        val names = mutableSetOf("كاشير 1", "كاشير 2", "كاشير 3")
        invoices.forEach { if (it.createdBy.isNotEmpty() && it.createdBy != "المدير" && it.createdBy != "admin") names.add(it.createdBy) }
        bonds.forEach { if (it.createdBy.isNotEmpty() && it.createdBy != "المدير" && it.createdBy != "admin") names.add(it.createdBy) }
        names.toList()
    }

    var selectedCashier by remember { mutableStateOf(distinctCashiers.firstOrNull() ?: "كاشير 1") }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Financial calculations for the selected cashier
    val cashierInvoices = remember(invoices, selectedCashier) {
        invoices.filter { !it.isCancelled && (it.createdBy == selectedCashier || selectedCashier == "الكل") }
    }
    val totalCashSales = remember(cashierInvoices) {
        cashierInvoices.sumOf { it.paidAmount }
    }
    val totalCreditSales = remember(cashierInvoices) {
        cashierInvoices.sumOf { it.remainingAmount }
    }
    val totalSales = remember(cashierInvoices) {
        cashierInvoices.sumOf { it.totalAmount }
    }

    val cashierReceipts = remember(bonds, selectedCashier) {
        bonds.filter { it.type == "RECEIPT" && (it.createdBy == selectedCashier || selectedCashier == "الكل") }
    }
    val totalReceipts = remember(cashierReceipts) {
        cashierReceipts.sumOf { it.amount }
    }

    val cashierPayments = remember(bonds, selectedCashier) {
        bonds.filter { it.type == "PAYMENT" && (it.createdBy == selectedCashier || selectedCashier == "الكل") }
    }
    val totalPayments = remember(cashierPayments) {
        cashierPayments.sumOf { it.amount }
    }

    // Net Cash in Drawer
    val netCashInDrawer = remember(totalCashSales, totalReceipts, totalPayments) {
        (totalCashSales + totalReceipts - totalPayments).coerceAtLeast(0.0)
    }

    var settlementAmountText by remember(netCashInDrawer) {
        mutableStateOf(if (netCashInDrawer > 0) netCashInDrawer.toInt().toString() else "")
    }
    var settlementNotes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "تصفية حسابات الكاشير والصندوق",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "جرد النقدية والعهد وتوريد الصندوق للمدير",
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
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "تم تحديث بيانات التصفية", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF059669))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cashier Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "تحديد الكاشير المراد تصفيته:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            // Dropdown Trigger
                            Box {
                                OutlinedButton(
                                    onClick = { expandedDropdown = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Text(selectedCashier, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    distinctCashiers.forEach { cashier ->
                                        DropdownMenuItem(
                                            text = { Text(cashier, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                selectedCashier = cashier
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary of Drawer & Balances
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF047857)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "صافي النقدية المفترضة بالدرج (العهدة الحالية)",
                            fontSize = 13.sp,
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${numberFormat.format(netCashInDrawer)} $currency",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0x33FFFFFF))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي المبيعات", fontSize = 11.sp, color = Color(0xFFD1FAE5))
                                Text(
                                    "${numberFormat.format(totalSales)} $currency",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المقبوض كاش", fontSize = 11.sp, color = Color(0xFFD1FAE5))
                                Text(
                                    "${numberFormat.format(totalCashSales)} $currency",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المبيعات الآجلة", fontSize = 11.sp, color = Color(0xFFD1FAE5))
                                Text(
                                    "${numberFormat.format(totalCreditSales)} $currency",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFEF08A)
                                )
                            }
                        }
                    }
                }
            }

            // Bonds Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Receipts
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سندات قبض (+)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${numberFormat.format(totalReceipts)} $currency",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text("العدد: ${cashierReceipts.size}", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Payments
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سندات صرف (-)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${numberFormat.format(totalPayments)} $currency",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text("العدد: ${cashierPayments.size}", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            // Settlement Action Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "توريد وتصفية عهدة النقدية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )

                        OutlinedTextField(
                            value = settlementAmountText,
                            onValueChange = { settlementAmountText = it },
                            label = { Text("المبلغ المستلم والمورد ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = settlementNotes,
                            onValueChange = { settlementNotes = it },
                            label = { Text("ملاحظات التصفية وتوقيع الاستلام") },
                            placeholder = { Text("مثال: تم جرد واستلام إيراد الوردية بالكامل...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                val amt = settlementAmountText.toDoubleOrNull() ?: 0.0
                                if (amt <= 0) {
                                    Toast.makeText(context, "يرجى إدخال مبلغ تصفية صحيح", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                showConfirmDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اعتماد وحفظ سند التصفية", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("تأكيد تصفية الصندوق", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من تصفية واستلام مبلغ (${settlementAmountText} $currency) من الكاشير ($selectedCashier) وإقفال حسابه للوردية الحالية؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settlementAmountText.toDoubleOrNull() ?: 0.0
                        onSaveSettlement(amt, selectedCashier, settlementNotes)
                        showConfirmDialog = false
                        Toast.makeText(context, "✅ تم تسجيل التصفية بنجاح", Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("تأكيد التصفية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }
}
