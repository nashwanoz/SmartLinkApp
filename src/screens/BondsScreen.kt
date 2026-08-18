package com.smartlink.erp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.utils.WhatsAppUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BondsScreen(
    bonds: List<Bond>,
    customers: List<Customer>,
    currentUser: User,
    settings: SystemSettings,
    onSaveBond: (Bond) -> Unit,
    onDeleteBond: ((String) -> Unit)? = null,
    preselectedCustomer: Customer? = null,
    onClearPreselectedCustomer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    var searchTerm by remember { mutableStateOf("") }
    var isModalOpen by remember { mutableStateOf(false) }
    var lastCreatedBond by remember { mutableStateOf<Bond?>(null) }
    var selectedBondForPreview by remember { mutableStateOf<Bond?>(null) }
    var bondToDelete by remember { mutableStateOf<Bond?>(null) }
    var copiedBondId by remember { mutableStateOf<String?>(null) }
    
    val isAdmin = currentUser.role == "ADMIN"
    
    // Form states
    var formType by remember { mutableStateOf("RECEIPT") }
    var selectedCustomerId by remember { mutableStateOf(preselectedCustomer?.id ?: "") }
    var customerSearchInput by remember { mutableStateOf("") }
    var showCustomerResults by remember { mutableStateOf(false) }
    var formAmount by remember { mutableStateOf("") }
    var formNote by remember { mutableStateOf("قبض") }
    var errorMsg by remember { mutableStateOf("") }
    
    val selectedCustomer by remember {
        derivedStateOf {
            selectedCustomerId.let { id -> customers.find { c -> c.id == id } }
        }
    }
    
    val searchedCustomers by remember {
        derivedStateOf {
            val s = customerSearchInput.lowercase().trim()
            if (s.isEmpty()) customers
            else customers.filter { c ->
                c.name.lowercase().contains(s) ||
                c.cCode.contains(s) ||
                (c.mobile?.contains(s) == true)
            }
        }
    }
    
    fun nextBondNumber(): String {
        if (bonds.isEmpty()) return "685"
        val nums = bonds.mapNotNull { b -> b.bondNumber.toIntOrNull() }
        if (nums.isEmpty()) return "685"
        return (maxOf(nums) + 1).toString()
    }
    
    fun openAddModal(type: String = "RECEIPT") {
        formType = type
        formAmount = ""
        formNote = if (type == "RECEIPT") "قبض" else "صرف"
        errorMsg = ""
        if (preselectedCustomer == null) {
            selectedCustomerId = ""
            customerSearchInput = ""
        }
        showCustomerResults = false
        isModalOpen = true
    }
    
    fun handleSubmit() {
        errorMsg = ""
        
        val customer = selectedCustomer
        if (customer == null) {
            errorMsg = "يرجى اختيار العميل"
            return
        }
        
        val amt = formAmount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            errorMsg = "يرجى كتابة مبلغ صحيح أكبر من الصفر"
            return
        }
        
        val prevBalance = customer.balance
        val newBalance = if (formType == "RECEIPT") {
            prevBalance - amt
        } else {
            prevBalance + amt
        }
        
        val newBond = Bond(
            id = "bond_${System.currentTimeMillis()}",
            bondNumber = nextBondNumber(),
            type = formType,
            customerId = customer.id,
            customerCode = customer.cCode,
            customerName = customer.name,
            customerMobile = customer.mobile ?: "",
            cashierId = currentUser.id,
            cashierCode = currentUser.userCode,
            cashierName = currentUser.name,
            amount = amt,
            note = formNote.trim().ifEmpty { if (formType == "RECEIPT") "قبض" else "صرف" },
            prevCustomerBalance = prevBalance,
            newCustomerBalance = newBalance,
            date = System.currentTimeMillis()
        )
        
        onSaveBond(newBond)
        lastCreatedBond = newBond
        isModalOpen = false
    }
    
    val filtered by remember {
        derivedStateOf {
            val s = searchTerm.lowercase().trim()
            if (s.isEmpty()) bonds
            else bonds.filter { b ->
                b.customerName.lowercase().contains(s) ||
                b.bondNumber.contains(s) ||
                b.customerCode.contains(s)
            }
        }
    }
    
    // Handle preselected customer
    LaunchedEffect(preselectedCustomer) {
        preselectedCustomer?.let { customer ->
            selectedCustomerId = customer.id
            customerSearchInput = "[${customer.cCode}] ${customer.name}"
            isModalOpen = true
            onClearPreselectedCustomer?.invoke()
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "سندات القبض والصرف (رسائل نصية واتساب)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "إشعار فوري للعميل بحركة الحساب والرصيد السابق والجديد",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { openAddModal("RECEIPT") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF047857)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+ سند قبض",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Button(
                        onClick = { openAddModal("PAYMENT") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+ صرف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Search
        item {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = {
                    Text(
                        text = "🔍 ابحث برقم السند، اسم العميل، أو كود العميل...",
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchTerm.isNotEmpty()) {
                        IconButton(onClick = { searchTerm = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Bonds List
        items(filtered, key = { it.id }) { bond ->
            BondCard(
                bond = bond,
                settings = settings,
                isAdmin = isAdmin,
                context = context,
                customers = customers,
                onPreviewClick = { selectedBondForPreview = bond },
                onDeleteClick = { bondToDelete = bond },
                onCopyClick = {
                    val msg = formatBondWhatsAppMessage(bond, settings)
                    val success = WhatsAppUtils.copyToClipboard(context, msg)
                    if (success) {
                        copiedBondId = bond.id
                    }
                },
                onWhatsAppClick = {
                    val msg = formatBondWhatsAppMessage(bond, settings)
                    WhatsAppUtils.sendTextOnly(context, bond.customerMobile, msg)
                },
                isCopied = copiedBondId == bond.id
            )
        }
        
        // Empty state
        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد سندات مسجلة بعد",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
    
    // CREATE BOND MODAL
    if (isModalOpen) {
        BondModal(
            formType = formType,
            selectedCustomer = selectedCustomer,
            customerSearchInput = customerSearchInput,
            searchedCustomers = searchedCustomers,
            formAmount = formAmount,
            formNote = formNote,
            errorMsg = errorMsg,
            settings = settings,
            showCustomerResults = showCustomerResults,
            onTypeChange = { formType = it },
            onCustomerSearchChange = {
                customerSearchInput = it
                showCustomerResults = true
                if (selectedCustomerId.isNotEmpty()) selectedCustomerId = ""
            },
            onCustomerSelect = { customer ->
                selectedCustomerId = customer.id
                customerSearchInput = "[${customer.cCode}] ${customer.name}"
                showCustomerResults = false
                errorMsg = ""
            },
            onAmountChange = { formAmount = it },
            onNoteChange = { formNote = it },
            onClose = { isModalOpen = false },
            onSubmit = { handleSubmit() }
        )
    }
    
    // INSTANT WHATSAPP SEND DIALOG AFTER CREATING BOND
    lastCreatedBond?.let { bond ->
        CreatedBondDialog(
            bond = bond,
            settings = settings,
            context = context,
            customers = customers,
            copiedBondId = copiedBondId,
            onCopySuccess = { copiedBondId = it },
            onPrintClick = { /* TODO: Print bond */ },
            onWhatsAppClick = {
                val msg = formatBondWhatsAppMessage(bond, settings)
                WhatsAppUtils.sendTextOnly(context, bond.customerMobile, msg)
            },
            onClose = { lastCreatedBond = null }
        )
    }
    
    // DETAILED BOND THERMAL PREVIEW MODAL
    selectedBondForPreview?.let { bond ->
        BondPreviewModal(
            bond = bond,
            settings = settings,
            context = context,
            customers = customers,
            isAdmin = isAdmin,
            onDeleteClick = {
                bondToDelete = bond
                selectedBondForPreview = null
            },
            onPrintClick = { /* TODO: Print bond */ },
            onWhatsAppClick = {
                val msg = formatBondWhatsAppMessage(bond, settings)
                WhatsAppUtils.sendTextOnly(context, bond.customerMobile, msg)
            },
            onClose = { selectedBondForPreview = null }
        )
    }
    
    // MANAGER DELETE CONFIRMATION DIALOG
    bondToDelete?.let { bond ->
        DeleteBondDialog(
            bond = bond,
            settings = settings,
            onDeleteConfirm = {
                onDeleteBond?.invoke(bond.id)
                bondToDelete = null
            },
            onClose = { bondToDelete = null }
        )
    }
}

@Composable
private fun BondCard(
    bond: Bond,
    settings: SystemSettings,
    isAdmin: Boolean,
    context: Context,
    customers: List<Customer>,
    onPreviewClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    isCopied: Boolean
) {
    val isReceipt = bond.type == "RECEIPT"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isReceipt) Color(0xFFA7F3D0) else Color(0xFFFECACA))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (isReceipt) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isReceipt) "سند قبض نقداً" else "سند صرف" + " #${bond.bondNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isReceipt) Color(0xFF065F46) else Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = bond.customerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Text(
                        text = "كود العميل: ${bond.customerCode} • ${formatDateTime(bond.date)} • بواسطة: ${bond.cashierName}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    
                    Text(
                        text = "البيان: ${bond.note}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${String.format("%,.2f", bond.amount)} ${settings.currencySymbol}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "الرصيد بعد السند: ${String.format("%,.2f", kotlin.math.abs(bond.newCustomerBalance))} (${if (bond.newCustomerBalance >= 0) "عليه" else "له"})",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreviewClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "معاينة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { /* TODO: Print */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0F766E)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4))
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(12.dp)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "طباعة حرارية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE11D48)
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "حذف",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onCopyClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isCopied) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "تم النسخ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        } else {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "نسخ النص",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Button(
                        onClick = onWhatsAppClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "واتساب",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BondModal(
    formType: String,
    selectedCustomer: Customer?,
    customerSearchInput: String,
    searchedCustomers: List<Customer>,
    formAmount: String,
    formNote: String,
    errorMsg: String,
    settings: SystemSettings,
    showCustomerResults: Boolean,
    onTypeChange: (String) -> Unit,
    onCustomerSearchChange: (String) -> Unit,
    onCustomerSelect: (Customer) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    var showResults by remember { mutableStateOf(showCustomerResults) }
    
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (formType == "RECEIPT") Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (formType == "RECEIPT") Color(0xFF047857) else Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (formType == "RECEIPT") "تحرير سند قبض نقداً" else "تحرير سند صرف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Error Message
                if (errorMsg.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEE2E2)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = errorMsg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                
                // Form
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onTypeChange("RECEIPT") },
                            colors = if (formType == "RECEIPT") {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF047857),
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF475569)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = "سند قبض (استلام)",
                                fontSize = 11.sp,
                                fontWeight = if (formType == "RECEIPT") FontWeight.Black else FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = { onTypeChange("PAYMENT") },
                            colors = if (formType == "PAYMENT") {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11D48),
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF475569)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = "سند صرف (دفع)",
                                fontSize = 11.sp,
                                fontWeight = if (formType == "PAYMENT") FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }
                    
                    // Customer Search Box
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مربع بحث عن العميل *",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            
                            if (selectedCustomer != null) {
                                Surface(
                                    color = Color(0xFFF0FDFA),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF99F6E4))
                                ) {
                                    Text(
                                        text = "رصيده الحالي: ${String.format("%,.2f", selectedCustomer.balance)} ${settings.currencySymbol} (${if (selectedCustomer.balance >= 0) "عليه" else "له"})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF134E4A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Box {
                            OutlinedTextField(
                                value = customerSearchInput,
                                onValueChange = onCustomerSearchChange,
                                placeholder = {
                                    Text(
                                        text = "🔍 اكتب اسم العميل أو كود الحساب C_CODE للبحث...",
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (customerSearchInput.isNotEmpty() || selectedCustomer != null) {
                                        IconButton(
                                            onClick = {
                                                onCustomerSearchChange("")
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                colors = if (selectedCustomer != null) {
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF14B8A6),
                                        unfocusedBorderColor = Color(0xFF14B8A6),
                                        focusedContainerColor = Color(0xFFF0FDFA).copy(alpha = 0.4f),
                                        unfocusedContainerColor = Color(0xFFF0FDFA).copy(alpha = 0.4f)
                                    )
                                } else {
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFCBD5E1),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                },
                                textStyle = if (selectedCustomer != null) {
                                    LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF134E4A)
                                    )
                                } else {
                                    LocalTextStyle.current.copy(
                                        fontSize = 12.sp
                                    )
                                }
                            )
                            
                            // Customer Search Live Results
                            if (showResults && searchedCustomers.isNotEmpty() && selectedCustomer == null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .align(Alignment.BottomStart),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(searchedCustomers, key = { it.id }) { customer ->
                                            Surface(
                                                onClick = {
                                                    onCustomerSelect(customer)
                                                    showResults = false
                                                },
                                                color = if (selectedCustomer?.id == customer.id) {
                                                    Color(0xFFF0FDFA)
                                                } else {
                                                    Color(0xFFF8FAFC)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                color = Color(0xFFD1FAE5).copy(alpha = 0.7f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "[${customer.cCode}]",
                                                                    fontSize = 10.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = Color(0xFF134E4A),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = customer.name,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1E293B)
                                                            )
                                                        }
                                                        
                                                        if (!customer.mobile.isNullOrBlank()) {
                                                            Text(
                                                                text = customer.mobile,
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Text(
                                                        text = "${String.format("%,.2f", customer.balance)} ${settings.currencySymbol}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Amount Input
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مبلغ السند (${settings.currencySymbol}) *",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        
                        OutlinedTextField(
                            value = formAmount,
                            onValueChange = onAmountChange,
                            placeholder = {
                                Text(
                                    text = "0.00",
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    // Note / Statement
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "البيان / ملاحظات",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        
                        OutlinedTextField(
                            value = formNote,
                            onValueChange = onNoteChange,
                            placeholder = {
                                Text(
                                    text = "قبض دفعة من الحساب",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    // Amount in Words preview
                    formAmount.toDoubleOrNull()?.let { amount ->
                        if (amount > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFECFDF5).copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "المبلغ كتابة: ${numberToArabicWords(amount, settings.currency ?: "ريال", "فلس")}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF064E3B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                    
                    // Submit Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "إلغاء",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = onSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (formType == "RECEIPT") Color(0xFF047857) else Color(0xFFE11D48)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "اعتماد السند 🚀",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatedBondDialog(
    bond: Bond,
    settings: SystemSettings,
    context: Context,
    customers: List<Customer>,
    copiedBondId: String?,
    onCopySuccess: (String) -> Unit,
    onPrintClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "تم اعتماد السند بنجاح",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "رقم الحركة: [${bond.bondNumber}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF047857)
                    )
                }
                
                // Preview of the text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = formatBondWhatsAppMessage(bond, settings),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPrintClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "طباعة حرارية 🖨️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        
                        Button(
                            onClick = onWhatsAppClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "إرسال واتساب",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            val msg = formatBondWhatsAppMessage(bond, settings)
                            val success = WhatsAppUtils.copyToClipboard(context, msg)
                            if (success) {
                                onCopySuccess("modal-created")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (copiedBondId == "modal-created") {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "تم نسخ نص الإشعار بنجاح ✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        } else {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "نسخ نص السند للحافظة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "إغلاق",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BondPreviewModal(
    bond: Bond,
    settings: SystemSettings,
    context: Context,
    customers: List<Customer>,
    isAdmin: Boolean,
    onDeleteClick: () -> Unit,
    onPrintClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${if (bond.type == "RECEIPT") "سند قبض" else "سند صرف"} رقم: ${bond.bondNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Thermal Bond Preview Placeholder
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "معاينة السند (HTML Template)",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Share PDF + Text */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "مشاركة السند (ملف PDF + النص) للجوال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPrintClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "طباعة حرارية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Button(
                            onClick = onWhatsAppClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "نص واتساب فقط",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE11D48)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "حذف هذا السند نهائياً (خاص بالمدير)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteBondDialog(
    bond: Bond,
    settings: SystemSettings,
    onDeleteConfirm: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = "تأكيد حذف السند المالي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "خاص بصلاحيات المدير العام",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Bond Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2).copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "رقم السند:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "#${bond.bondNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "العميل:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = bond.customerName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مبلغ السند:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "${String.format("%,.2f", bond.amount)} ${settings.currencySymbol}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE11D48)
                            )
                        }
                        
                        Divider(color = Color(0xFFFECACA), thickness = 1.dp)
                        
                        Text(
                            text = "⚠️ سيتم التراجع عن حركة الرصيد وإعادة حساب العميل كما كان قبل السند.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "إلغاء",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = onDeleteConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "نعم، احذف السند",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}

private fun formatBondWhatsAppMessage(bond: Bond, settings: SystemSettings): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    val dateStr = dateFormat.format(Date(bond.date))
    
    return """
📄 *${if (bond.type == "RECEIPT") "سند قبض" else "سند صرف"} رقم ${bond.bondNumber}*
━━━━━━━━━━━━━━━━
👤 العميل: ${bond.customerName}
📅 التاريخ: $dateStr
💰 المبلغ: ${String.format("%,.2f", bond.amount)} ${settings.currencySymbol}
📝 البيان: ${bond.note}
━━━━━━━━━━━━━━━━
الرصيد السابق: ${String.format("%,.2f", bond.prevCustomerBalance)} ${settings.currencySymbol}
الرصيد الجديد: ${String.format("%,.2f", bond.newCustomerBalance)} ${settings.currencySymbol}
━━━━━━━━━━━━━━━━
${settings.companyName ?: "شكراً لتعاملكم معنا!"}
    """.trimIndent()
}

private fun numberToArabicWords(amount: Double, currency: String, subCurrency: String): String {
    val wholePart = amount.toInt()
    val decimalPart = ((amount - wholePart) * 100).toInt()
    
    return "$wholePart $currency و $decimalPart $subCurrency"
}
