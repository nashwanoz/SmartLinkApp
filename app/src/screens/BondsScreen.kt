package com.smartlink.erp.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import com.smartlink.kerp.data.local.entity.Bond
import com.smartlink.kerp.data.local.entity.BondType
import com.smartlink.kerp.data.local.entity.Customer
import com.smartlink.kerp.data.local.entity.SystemSettings
import com.smartlink.kerp.data.local.entity.User
import com.smartlink.kerp.utils.NumberToArabicWords
import com.smartlink.kerp.utils.WhatsAppTemplates
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
    val scope = rememberCoroutineScope()
    
    var searchTerm by remember { mutableStateOf("") }
    var isModalOpen by remember { mutableStateOf(false) }
    var lastCreatedBond by remember { mutableStateOf<Bond?>(null) }
    var selectedBondForPreview by remember { mutableStateOf<Bond?>(null) }
    var bondToDelete by remember { mutableStateOf<Bond?>(null) }
    var copiedBondId by remember { mutableStateOf<String?>(null) }
    
    val isAdmin = currentUser.role == "ADMIN"
    
    // Form states
    var formType by remember { mutableStateOf(BondType.RECEIPT) }
    var selectedCustomerId by remember { mutableStateOf(preselectedCustomer?.id ?: "") }
    var customerSearchInput by remember { mutableStateOf("") }
    var showCustomerResults by remember { mutableStateOf(false) }
    var formAmount by remember { mutableStateOf("") }
    var formNote by remember { mutableStateOf("قبض") }
    var errorMsg by remember { mutableStateOf("") }
    
    val selectedCustomer by remember {
        derivedStateOf { customers.find { c -> c.id == selectedCustomerId } }
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
    
    // Handle preselected customer
    LaunchedEffect(preselectedCustomer) {
        preselectedCustomer?.let { customer ->
            selectedCustomerId = customer.id
            customerSearchInput = "[${customer.cCode}] ${customer.name}"
            isModalOpen = true
            onClearPreselectedCustomer?.invoke()
        }
    }
    
    fun nextBondNumber(): String {
        if (bonds.isEmpty()) return "685"
        val nums = bonds.mapNotNull { b -> b.bondNumber.toIntOrNull() }
        if (nums.isEmpty()) return "685"
        return (nums.max() + 1).toString()
    }
    
    fun openAddModal(type: BondType = BondType.RECEIPT) {
        formType = type
        formAmount = ""
        formNote = if (type == BondType.RECEIPT) "قبض" else "صرف"
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
        val newBalance = if (formType == BondType.RECEIPT) {
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
            customerMobile = customer.mobile,
            cashierId = currentUser.id,
            cashierCode = currentUser.userCode,
            cashierName = currentUser.name,
            amount = amt,
            note = formNote.trim().ifEmpty { if (formType == BondType.RECEIPT) "قبض" else "صرف" },
            prevCustomerBalance = prevBalance,
            newCustomerBalance = newBalance,
            date = System.currentTimeMillis()
        )
        
        onSaveBond(newBond)
        lastCreatedBond = newBond
        isModalOpen = false
    }
    
    val filteredBonds by remember {
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
    
    // Reset copied bond ID after 2 seconds
    LaunchedEffect(copiedBondId) {
        if (copiedBondId != null) {
            kotlinx.coroutines.delay(2000)
            copiedBondId = null
        }
    }
    
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bond Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
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
                    Text(
                        text = "سندات القبض والصرف (رسائل نصية واتساب)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
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
                        onClick = { openAddModal(BondType.RECEIPT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF047857)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+ سند قبض",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Button(
                        onClick = { openAddModal(BondType.PAYMENT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "+ صرف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
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
        items(filteredBonds, key = { it.id }) { bond ->
            BondItem(
                bond = bond,
                settings = settings,
                isAdmin = isAdmin,
                copiedBondId = copiedBondId,
                onCopyClick = { 
                    val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                    copyToClipboard(msg)
                    copiedBondId = bond.id
                },
                onWhatsAppClick = {
                    val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                    WhatsAppTemplates.sendToWhatsApp(context, bond.customerMobile ?: "", msg)
                },
                onPreviewClick = { selectedBondForPreview = bond },
                onPrintClick = { /* TODO: Implement print */ },
                onDeleteClick = { bondToDelete = bond }
            )
        }
        
        // Empty state
        if (filteredBonds.isEmpty()) {
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
        CreateBondModal(
            formType = formType,
            customers = customers,
            selectedCustomerId = selectedCustomerId,
            customerSearchInput = customerSearchInput,
            showCustomerResults = showCustomerResults,
            formAmount = formAmount,
            formNote = formNote,
            errorMsg = errorMsg,
            settings = settings,
            onTypeChange = { formType = it },
            onCustomerSearchChange = { 
                customerSearchInput = it
                showCustomerResults = true
                if (selectedCustomerId.isNotEmpty()) selectedCustomerId = ""
            },
            onCustomerSelected = { customerId, displayText ->
                selectedCustomerId = customerId
                customerSearchInput = displayText
                showCustomerResults = false
                errorMsg = ""
            },
            onShowResultsChange = { showCustomerResults = it },
            onAmountChange = { formAmount = it },
            onNoteChange = { formNote = it },
            onClose = { isModalOpen = false },
            onSubmit = { handleSubmit() }
        )
    }
    
    // INSTANT WHATSAPP SEND DIALOG AFTER CREATING BOND
    lastCreatedBond?.let { bond ->
        WhatsAppSendDialog(
            bond = bond,
            settings = settings,
            copiedBondId = copiedBondId,
            onCopyClick = {
                val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                copyToClipboard(msg)
                copiedBondId = "modal-created"
            },
            onWhatsAppClick = {
                val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                WhatsAppTemplates.sendToWhatsApp(context, bond.customerMobile ?: "", msg)
            },
            onPrintClick = { /* TODO: Implement print */ },
            onClose = { lastCreatedBond = null }
        )
    }
    
    // BOND PREVIEW MODAL
    selectedBondForPreview?.let { bond ->
        BondPreviewModal(
            bond = bond,
            settings = settings,
            isAdmin = isAdmin,
            onCopyClick = {
                val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                copyToClipboard(msg)
            },
            onWhatsAppClick = {
                val msg = WhatsAppTemplates.formatBondWhatsAppMessage(bond, settings)
                WhatsAppTemplates.sendToWhatsApp(context, bond.customerMobile ?: "", msg)
            },
            onPrintClick = { /* TODO: Implement print */ },
            onShareClick = { /* TODO: Implement share with PDF */ },
            onDeleteClick = {
                bondToDelete = bond
                selectedBondForPreview = null
            },
            onClose = { selectedBondForPreview = null }
        )
    }
    
    // DELETE CONFIRMATION DIALOG
    bondToDelete?.let { bond ->
        DeleteConfirmationDialog(
            bond = bond,
            settings = settings,
            onConfirm = {
                onDeleteBond?.invoke(bond.id)
                bondToDelete = null
            },
            onCancel = { bondToDelete = null }
        )
    }
}

@Composable
private fun BondItem(
    bond: Bond,
    settings: SystemSettings,
    isAdmin: Boolean,
    copiedBondId: String?,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onPrintClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isReceipt = bond.type == BondType.RECEIPT
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isReceipt) Color(0xFFA7F3D0) else Color(0xFFFECACA)
        )
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
                            color = if (isReceipt) Color(0xFFD1FAE5) else Color(0xFFFFE4E6),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isReceipt) "سند قبض نقداً" else "سند صرف",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isReceipt) Color(0xFF065F46) else Color(0xFF9F1239),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "#${bond.bondNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
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
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${String.format("%,.2f", bond.amount)} ${settings.currencySymbol}",
                        fontSize = 16.sp,
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
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "معاينة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = onPrintClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCCFBF1)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "طباعة حرارية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F766E)
                        )
                    }
                    
                    if (isAdmin) {
                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEF2F2)
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
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
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onCopyClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        if (copiedBondId == bond.id) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
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
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
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
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}

// TODO: Add remaining modal components (CreateBondModal, WhatsAppSendDialog, BondPreviewModal, DeleteConfirmationDialog)
