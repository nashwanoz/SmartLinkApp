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

    // Take only the last 5 invoices
    val filteredInvoices = remember(invoices, searchTerm) {
        val sorted = invoices.sortedByDescending { it.date }
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) sorted.take(5)
        else sorted.filter {
            it.invoiceNumber.lowercase().contains(s) ||
            it.billNo.contains(s) ||
            it.customerName.lowercase().contains(s) ||
            it.customerCode.contains(s)
        }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("سجل الفواتير", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
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
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subheader: Title "سجل الفواتير (آخر 5 فواتير حديثة)" & Button "+ اضافه فاتورة"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Invoice Button (Left)
                Button(
                    onClick = { /* Add Invoice */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "اضافه فاتورة",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Title & Tag (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "(آخر 5 فواتير حديثة)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "سجل الفواتير",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .border(0.8.dp, Color(0xFFA7F3D0), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = {
                    Text(
                        "ابحث عن الفواتير باسم العميل أو الكود...",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchTerm.isNotEmpty()) {
                        IconButton(onClick = { searchTerm = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Invoices List (5 max)
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فواتير مسجلة", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredInvoices, key = { it.id }) { inv ->
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a", Locale.US)
                        val formattedDate = dateFormat.format(Date(inv.date))
                        val isCash = inv.billType == 1 || inv.paymentMethod == "CASH"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top row: Total (left) & Code + Type + Customer (right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${settings.currencyName} ${inv.total.toInt()}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isCash) "نقدي" else "آجل",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF059669)
                                        )
                                        Text(
                                            text = inv.customerName.ifEmpty { "عميل نقدي" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "#${inv.billNo.ifEmpty { inv.invoiceNumber }}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                // Middle row: items count & date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "1 أصناف",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "التاريخ: $formattedDate",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

                                // Bottom toolbar: Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: واتساب, طباعة, مشاركة
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF059669))
                                                .clickable {
                                                    PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("واتساب", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF059669))
                                                .clickable {
                                                    coroutineScope.launch {
                                                        val paired = printerManager.getPairedPrinters()
                                                        if (paired.isNotEmpty()) {
                                                            printerManager.printInvoiceSilent(paired.first().address, inv, settings)
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("طباعة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF059669))
                                                .clickable {
                                                    PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("مشاركة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    // Right: معاينة
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { selectedInvoiceForDetails = inv }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text("معاينة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                        }
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
