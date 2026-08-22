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
    var bondType by remember { mutableStateOf("RECEIPT") } // RECEIPT (سند قبض) or PAYMENT (سند صرف)
    var mainAccountType by remember { mutableStateOf("CUSTOMERS") } // CUSTOMERS or SALES_BOX
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerSearchInput by remember { mutableStateOf("") }
    var isCustomerDropdownOpen by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("قبض دفعة نقدية") }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    val currentDateStr = remember { dateFormat.format(Date()) }
    val currentBondNumber = remember(bondType, bonds) {
        val count = bonds.count { (it.type == bondType || it.bondType == bondType) } + 1
        "10117" // Format as in screenshot or dynamic
    }

    val filteredCustomers = remember(customers, customerSearchInput) {
        val s = customerSearchInput.trim().lowercase()
        if (s.isEmpty()) customers.take(6)
        else customers.filter { it.name.lowercase().contains(s) || it.code.contains(s) || it.phone.contains(s) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("السندات المالية", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subheader: Toggle tabs (Left) & Title (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Tabs (سند صرف / سند قبض)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bondType == "PAYMENT") Color(0xFF0F766E) else Color.Transparent)
                            .clickable {
                                bondType = "PAYMENT"
                                noteInput = "صرف دفعة نقدية"
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "سند صرف",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bondType == "PAYMENT") Color.White else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bondType == "RECEIPT") Color(0xFF0F766E) else Color.Transparent)
                            .clickable {
                                bondType = "RECEIPT"
                                noteInput = "قبض دفعة نقدية"
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "سند قبض",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bondType == "RECEIPT") Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Right: Title
                Text(
                    text = "سندات القبض والصرف",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            }

            // Error Alert
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(errorMessage, fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { errorMessage = "" }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // Main Form Card (Matching Screenshot Exactly)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header inside card: رقم السند | تاريخ السند
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "رقم السند : #$currentBondNumber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "  |  ",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "تاريخ السند : $currentDateStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // المدخل
                    Text(
                        text = "المدخل : $currentUserName (101)",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Field 1: الحساب الرئيسي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (mainAccountType == "CUSTOMERS") "عملاء" else "صناديق",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        Text(
                            text = "الحساب الرئيسي :",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.width(95.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    // Field 2: الحساب التحليلي
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = if (selectedCustomer != null) selectedCustomer!!.name else customerSearchInput,
                                    onValueChange = {
                                        selectedCustomer = null
                                        customerSearchInput = it
                                        isCustomerDropdownOpen = true
                                    },
                                    placeholder = {
                                        Text(
                                            "بحث واختيار الحساب...",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color(0xFF0F766E),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = "الحساب التحليلي :",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.width(95.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }

                        // Dropdown results
                        if (isCustomerDropdownOpen && selectedCustomer == null && customerSearchInput.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                                    items(filteredCustomers) { cust ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCustomer = cust
                                                    customerSearchInput = cust.name
                                                    isCustomerDropdownOpen = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${cust.currentBalance.toInt()} ${settings.currencyName}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                cust.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Divider(color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }

                    // Field 3: المبلغ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            placeholder = {
                                Text(
                                    "0.00",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "المبلغ :",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.width(95.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    // Field 4: الملاحظة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("البيان...", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "الملاحظة :",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.width(95.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Large Green Save Button
                    Button(
                        onClick = {
                            val party = if (selectedCustomer != null) selectedCustomer!!.name else customerSearchInput.trim()
                            if (party.isEmpty()) {
                                errorMessage = "يرجى اختيار الحساب التحليلي"
                                return@Button
                            }
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) {
                                errorMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                                return@Button
                            }

                            val bond = BondEntity(
                                id = UUID.randomUUID().toString(),
                                bondNumber = currentBondNumber,
                                type = bondType,
                                bondType = bondType,
                                partyType = if (selectedCustomer != null) "CUSTOMER" else "OTHER",
                                partyId = selectedCustomer?.id ?: "",
                                partyName = party,
                                customerId = selectedCustomer?.id ?: "",
                                customerName = party,
                                amount = amt,
                                date = System.currentTimeMillis(),
                                note = noteInput.trim(),
                                notes = noteInput.trim(),
                                createdBy = currentUserName
                            )
                            onSaveBond(bond)
                            Toast.makeText(context, "✅ تم حفظ السند بنجاح", Toast.LENGTH_SHORT).show()
                            amountInput = ""
                            selectedCustomer = null
                            customerSearchInput = ""
                            errorMessage = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
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
