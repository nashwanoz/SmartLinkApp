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
    var isDropdownOpen by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchTerm) {
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) emptyList()
        else customers.filter {
            it.name.lowercase().contains(s) || it.code.contains(s) || it.phone.contains(s)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("بيانات العملاء", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
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
            // 1. Subheader: Title "بيانات العملاء" & Button "+ اضافه عميل"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Customer Button (Left)
                Button(
                    onClick = {
                        editingCustomer = null
                        isModalOpen = true
                    },
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
                        text = "اضافه عميل",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Title & Subtitle (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "بيانات العملاء",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "البحث عن الحسابات واستعراض تفاصيل الأرصدة وآخر الحركات",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0FDFA))
                            .border(0.8.dp, Color(0xFFCCFBF1), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Search Bar
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = {
                        searchTerm = it
                        isDropdownOpen = true
                    },
                    placeholder = {
                        Text(
                            "ابحث عن العميل بالاسم، كود العميل (C_CODE)، أو رقم الهاتف...",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchTerm.isNotEmpty()) {
                            IconButton(onClick = {
                                searchTerm = ""
                                isDropdownOpen = false
                            }) {
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
            }

            // 3. Main Body
            if (isDropdownOpen && searchTerm.isNotBlank()) {
                // Dropdown suggestions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                ) {
                    if (filteredCustomers.isEmpty()) {
                        Text(
                            text = "لا يوجد عملاء يطابقون البحث",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                            items(filteredCustomers) { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCustomer = cust
                                            searchTerm = ""
                                            isDropdownOpen = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cust.currentBalance.toInt()} ${settings.currencyName}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (cust.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = cust.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = cust.code,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                                Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                            }
                        }
                    }
                }
            } else if (selectedCustomer == null) {
                // Empty State (Image 1)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Text(
                            text = "لم يتم اختيار عميل بعد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )

                        Text(
                            text = "يرجى كتابة اسم أو كود العميل في مربع البحث أعلاه لعرض بطاقة الحساب والبيانات المالية.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // Selected Customer Cards (Image 2)
                val cust = selectedCustomer!!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dark Banner: Account Name + Code + Search Other
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search other button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                    .clickable { selectedCustomer = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "→ بحث عن عميل أخر",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            // Account name & code
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "اسم الحساب:",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1E293B))
                                                .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "C_CODE: ${cust.code}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                        Text(
                                            text = cust.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F766E).copy(alpha = 0.3f))
                                        .border(0.8.dp, Color(0xFF0F766E), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF2DD4BF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Card 1: Last Invoice (تاريخ اخر فاتورة)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("IAS_BILL", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${settings.currencyName} 540", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("تاريخ اخر فاتورة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                }
                                Text("٢٠٢٦/٠٨/٢١", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text("فاتورة رقم: 101431", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // Card 2: Last Payment (تاريخ اخر سداد)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFECFDF5))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("RECEIPT", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${settings.currencyName} 780", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("تاريخ اخر سداد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                }
                                Text("٢٠٢٦/٠٨/٢٠", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text("سند رقم: 10313", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // Card 3: Customer Balance (رصيد العميل)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFECDD3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFE4E6))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (cust.currentBalance > 0) "مدين (عليه)" else "خالص",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFBE123C)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("رصيد العميل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                                }
                                Text("${settings.currencyName} ${cust.currentBalance.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFBE123C))
                                Text("الرصيد المحاسبي الإجمالي للعميل", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // Card 4: Contact info + Actions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Contact Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cust.phone.isNotBlank()) {
                                    Text(cust.phone, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cust.address.ifBlank { "خمر" }, fontSize = 11.sp, color = Color(0xFF334155))
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                            }

                            Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

                            // Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Right: Bond button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFECFDF5))
                                        .border(0.8.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                        .clickable {
                                            // Handle bond
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(13.dp))
                                        Text("سند سداد / قبض", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                    }
                                }

                                // Left: WhatsApp, Edit, Delete
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            editingCustomer = cust
                                            isModalOpen = true
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9))
                                            .border(0.8.dp, Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            onDeleteCustomer(cust)
                                            selectedCustomer = null
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFF1F2))
                                            .border(0.8.dp, Color(0xFFFECDD3), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(13.dp))
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
                selectedCustomer = savedCust
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
