package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.InvoiceItem
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.khamrnet.app.printer.BluetoothPrinterManager
import com.khamrnet.app.util.PdfThermalGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity>,
    onCancelInvoice: (InvoiceEntity, List<InvoiceItem>) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    var searchTerm by remember { mutableStateOf("") }
    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToCancel by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filteredInvoices = remember(invoices, searchTerm) {
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) invoices
        else invoices.filter {
            it.invoiceNumber.lowercase().contains(s) ||
            it.billNo.contains(s) ||
            it.customerName.lowercase().contains(s) ||
            it.customerCode.contains(s)
        }
    }

    val totalSales = remember(invoices) { invoices.sumOf { it.total } }
    val totalRemaining = remember(invoices) { invoices.sumOf { it.remainingAmount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("سجل الفواتير", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${invoices.size} فاتورة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("إجمالي المبيعات", fontSize = 10.5.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                        Text("${totalSales.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("المتبقي آجل (دين)", fontSize = 10.5.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                        Text("${totalRemaining.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = { Text("🔍 ابحث برقم الفاتورة أو اسم العميل...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0F766E),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فواتير مسجلة", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredInvoices, key = { it.id }) { inv ->
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar"))
                        val formattedDate = dateFormat.format(Date(inv.date))
                        val isCredit = inv.billType == 4 || inv.paymentMethod == "CREDIT"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInvoiceForDetails = inv },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isCredit) Color(0xFFFEF2F2) else Color(0xFFECFDF5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCredit) Icons.Default.ReceiptLong else Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = if (isCredit) Color(0xFFDC2626) else Color(0xFF059669),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("#${inv.billNo.ifEmpty { inv.invoiceNumber }}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isCredit) Color(0xFFFEE2E2) else Color(0xFFDCFCE7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isCredit) "آجل" else "نقدي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isCredit) Color(0xFF991B1B) else Color(0xFF15803D))
                                            }
                                        }
                                        Text(inv.customerName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formattedDate, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${inv.total.toInt()} ${settings.currencyName}", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                                        if (isCredit && inv.remainingAmount > 0) {
                                            Text("متبقي: ${inv.remainingAmount.toInt()}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }

                                    // Quick Print Button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val paired = printerManager.getPairedPrinters()
                                                if (paired.isNotEmpty()) {
                                                    val res = printerManager.printInvoiceSilent(paired.first().address, inv, settings)
                                                    if (res.isSuccess) Toast.makeText(context, "✅ تمت الطباعة الحرارية", Toast.LENGTH_SHORT).show()
                                                    else Toast.makeText(context, "خطأ في الطباعة", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "لا توجد طابعة بلوتوث متصلة", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9))
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                                    }

                                    // WhatsApp Share Button
                                    IconButton(
                                        onClick = {
                                            PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                                        },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFDCFCE7))
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Invoice Details View
    selectedInvoiceForDetails?.let { inv ->
        val itemType = object : TypeToken<List<InvoiceItem>>() {}.type
        val items: List<InvoiceItem> = try {
            Gson().fromJson(inv.itemsJson, itemType) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        Dialog(onDismissRequest = { selectedInvoiceForDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفاصيل الفاتورة #${inv.billNo.ifEmpty { inv.invoiceNumber }}", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        IconButton(onClick = { selectedInvoiceForDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text("العميل: ${inv.customerName} [${inv.customerCode}]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("الإجمالي: ${inv.total.toInt()} ${settings.currencyName}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(items) { itm ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${itm.productName} (${itm.quantity.toInt()} ${itm.unitName})", fontSize = 11.5.sp)
                                Text("${itm.total.toInt()} ${settings.currencyName}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val paired = printerManager.getPairedPrinters()
                                    if (paired.isNotEmpty()) {
                                        printerManager.printInvoiceSilent(paired.first().address, inv, settings)
                                        Toast.makeText(context, "تمت الطباعة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
