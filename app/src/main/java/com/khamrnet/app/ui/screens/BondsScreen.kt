package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.khamrnet.app.data.model.CustomerEntity
import com.khamrnet.app.data.model.BondEntity
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BondsScreen(
    settings: SystemSettingsEntity,
    bonds: List<BondEntity>,
    customers: List<CustomerEntity>,
    currentUserName: String,
    onSaveBond: (BondEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isAddModalOpen by remember { mutableStateOf(false) }
    var bondType by remember { mutableStateOf("RECEIPT") } // RECEIPT (قبض) or PAYMENT (صرف)
    var searchTerm by remember { mutableStateOf("") }

    val filteredBonds = remember(bonds, searchTerm) {
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) bonds
        else bonds.filter {
            val pName = it.partyName.ifEmpty { it.customerName }
            val nText = it.notes.ifEmpty { it.note }
            it.bondNumber.contains(s) || pName.lowercase().contains(s) || nText.lowercase().contains(s)
        }
    }

    val totalReceipts = remember(bonds) {
        bonds.filter { it.type == "RECEIPT" || it.bondType == "RECEIPT" }.sumOf { it.amount }
    }
    val totalPayments = remember(bonds) {
        bonds.filter { it.type == "PAYMENT" || it.bondType == "PAYMENT" }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("السندات المالية", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${bonds.size} سند", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { isAddModalOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سند جديد", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
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
            // Stats Row
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
                        Text("إجمالي سندات القبض", fontSize = 10.5.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                        Text("${totalReceipts.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("إجمالي سندات الصرف", fontSize = 10.5.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                        Text("${totalPayments.toInt()} ${settings.currencyName}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                }
            }

            // Bonds List
            if (filteredBonds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد سندات مسجلة حتى الآن", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBonds, key = { it.id }) { bond ->
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar"))
                        val formattedDate = dateFormat.format(Date(bond.date))
                        val isReceipt = (bond.type == "RECEIPT" || bond.bondType == "RECEIPT")
                        val party = bond.partyName.ifEmpty { bond.customerName }
                        val noteText = bond.notes.ifEmpty { bond.note }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isReceipt) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isReceipt) Color(0xFF15803D) else Color(0xFFB91C1C),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(party, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isReceipt) Color(0xFFECFDF5) else Color(0xFFFEF2F2))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    if (isReceipt) "سند قبض" else "سند صرف",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReceipt) Color(0xFF047857) else Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                        Text(formattedDate, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                        if (noteText.isNotBlank()) {
                                            Text("ملاحظة: $noteText", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                }

                                Text(
                                    text = "${bond.amount.toInt()} ${settings.currencyName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isReceipt) Color(0xFF047857) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Bond Dialog
    if (isAddModalOpen) {
        AddBondDialog(
            customers = customers,
            currency = settings.currencyName,
            currentUserName = currentUserName,
            onDismiss = { isAddModalOpen = false },
            onSave = { newBond ->
                onSaveBond(newBond)
                isAddModalOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBondDialog(
    customers: List<CustomerEntity>,
    currency: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (BondEntity) -> Unit
) {
    var bondType by remember { mutableStateOf("RECEIPT") } // RECEIPT or PAYMENT
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var manualPartyName by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (bondType == "RECEIPT") "إنشاء سند قبض (استلام مبلغ)" else "إنشاء سند صرف (دفع مبلغ)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )

                // Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { bondType = "RECEIPT" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (bondType == "RECEIPT") Color(0xFF059669) else Color.Transparent
                    ) {
                        Text(
                            "سند قبض (قبض من عميل)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bondType == "RECEIPT") Color.White else Color(0xFF475569),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Surface(
                        onClick = { bondType = "PAYMENT" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (bondType == "PAYMENT") Color(0xFFDC2626) else Color.Transparent
                    ) {
                        Text(
                            "سند صرف (مصاريف/دفع)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bondType == "PAYMENT") Color.White else Color(0xFF475569),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Text(errorMessage, fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }

                // Party selection or manual input
                OutlinedTextField(
                    value = if (selectedCustomer != null) selectedCustomer!!.name else manualPartyName,
                    onValueChange = {
                        selectedCustomer = null
                        manualPartyName = it
                    },
                    label = { Text(if (bondType == "RECEIPT") "استلمنا من السيد / العميل *" else "صرفنا للسيد / الجهه *", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("المبلغ ($currency) *", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان / الملاحظات (اختياري)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val party = if (selectedCustomer != null) selectedCustomer!!.name else manualPartyName.trim()
                            if (party.isEmpty()) {
                                errorMessage = "يرجى كتابة أو اختيار اسم الشخص أو العميل"
                                return@Button
                            }
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) {
                                errorMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                                return@Button
                            }

                            val bond = BondEntity(
                                id = UUID.randomUUID().toString(),
                                bondNumber = "BOND-${System.currentTimeMillis().toString().takeLast(6)}",
                                type = bondType,
                                bondType = bondType,
                                partyType = if (selectedCustomer != null) "CUSTOMER" else "OTHER",
                                partyId = selectedCustomer?.id ?: "",
                                partyName = party,
                                customerId = selectedCustomer?.id ?: "",
                                customerName = party,
                                amount = amt,
                                date = System.currentTimeMillis(),
                                note = notes.trim(),
                                notes = notes.trim(),
                                createdBy = currentUserName
                            )
                            onSave(bond)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text("حفظ السند", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.5f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}
