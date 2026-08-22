package com.khamrnet.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.window.Dialog
import com.khamrnet.app.data.model.CustomerEntity
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    settings: SystemSettingsEntity,
    customers: List<CustomerEntity>,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var searchTerm by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var isModalOpen by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    // Calculation of Total Debts
    val totalDebts = remember(customers) { customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance } }

    val filteredCustomers = remember(customers, searchTerm) {
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) customers
        else customers.filter {
            it.name.lowercase().contains(s) || it.code.contains(s) || it.phone.contains(s)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("العملاء والحسابات", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${customers.size} عميل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        onClick = {
                            editingCustomer = null
                            isModalOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة عميل", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
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
            // 1. Debt Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("إجمالي المديونية عند العملاء:", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                            Text("${totalDebts.toInt()} ${settings.currencyName}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${customers.count { it.currentBalance > 0 }} عليهم ديون",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }

            // 2. Search Bar
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = { Text("🔍 ابحث باسم العميل أو الكود أو الهاتف...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                trailingIcon = {
                    if (searchTerm.isNotEmpty()) {
                        IconButton(onClick = { searchTerm = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0F766E),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Customers List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                        Text(if (searchTerm.isNotEmpty()) "لا يوجد عملاء يطابقون بحثك" else "لا يوجد عملاء مسجلين حتى الآن", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCustomers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingCustomer = cust
                                    isModalOpen = true
                                },
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
                                            .background(if (cust.currentBalance > 0) Color(0xFFFEF2F2) else Color(0xFFECFDF5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.take(1),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (cust.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }

                                    Column {
                                        Text(cust.name, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("كود: ${cust.code}", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            if (cust.phone.isNotBlank()) {
                                                Text("• ${cust.phone}", fontSize = 10.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Balance Badge
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${cust.currentBalance.toInt()} ${settings.currencyName}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (cust.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                        Text(
                                            text = if (cust.currentBalance > 0) "عليه دين" else "خالص الحساب",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }

                                    // WhatsApp Action
                                    if (cust.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val url = "https://api.whatsapp.com/send?phone=${cust.phone}"
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "لا يمكن فتح الواتساب", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDCFCE7))
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Edit Action
                                    IconButton(
                                        onClick = {
                                            editingCustomer = cust
                                            isModalOpen = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Customer Dialog
    if (isModalOpen) {
        CustomerFormDialog(
            existingCustomer = editingCustomer,
            nextCode = (customers.size + 1001).toString(),
            currency = settings.currencyName,
            onDismiss = { isModalOpen = false },
            onSave = { savedCust ->
                onSaveCustomer(savedCust)
                isModalOpen = false
            }
        )
    }
}

@Composable
fun CustomerFormDialog(
    existingCustomer: CustomerEntity?,
    nextCode: String,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingCustomer?.name ?: "") }
    var code by remember { mutableStateOf(existingCustomer?.code ?: nextCode) }
    var phone by remember { mutableStateOf(existingCustomer?.phone ?: "7") }
    var address by remember { mutableStateOf(existingCustomer?.address ?: "خمر") }
    var initialBalance by remember {
        mutableStateOf(if (existingCustomer != null) existingCustomer.initialBalance.toInt().toString() else "0")
    }
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
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existingCustomer == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Text(errorMessage, fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *", fontSize = 11.sp) },
                    placeholder = { Text("مثال: علي محمد صالح", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("كود العميل", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المنطقة", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingCustomer == null) {
                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = { Text("الرصيد الافتتاحي السابق ($currency)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                errorMessage = "يرجى إدخال اسم العميل"
                                return@Button
                            }
                            val initBalVal = initialBalance.toDoubleOrNull() ?: 0.0

                            val saved = CustomerEntity(
                                id = existingCustomer?.id ?: UUID.randomUUID().toString(),
                                code = code.ifEmpty { nextCode },
                                name = trimmedName,
                                phone = phone.trim(),
                                address = address.trim(),
                                initialBalance = if (existingCustomer == null) initBalVal else existingCustomer.initialBalance,
                                currentBalance = if (existingCustomer == null) initBalVal else existingCustomer.currentBalance,
                                createdAt = existingCustomer?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(saved)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text(if (existingCustomer == null) "حفظ العميل" else "تعديل البيانات", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
