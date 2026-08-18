package com.smartlink.erp.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.utils.WhatsAppTemplates
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    currentUser: User,
    settings: SystemSettings,
    onSaveCustomer: (Customer, Boolean) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onOpenBondModal: (Customer) -> Unit,
    onOpenStatement: (Customer) -> Unit
) {
    var searchTerm by remember { mutableStateOf("") }
    var isModalOpen by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    
    // Permission check for opening balance
    val canEditOpeningBalance = currentUser.role == "ADMIN"
    
    // Form state
    var formName by remember { mutableStateOf("") }
    var formMobile by remember { mutableStateOf("7") }
    var formAddress by remember { mutableStateOf("خمر") }
    var formBalance by remember { mutableStateOf("0") }
    var formCCode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    
    fun getNextCustomerCode(): String {
        if (customers.isEmpty()) return "1001"
        val codes = customers.mapNotNull { c -> c.cCode.toIntOrNull() }
        if (codes.isEmpty()) return "1001"
        val maxCode = maxOf(codes.max(), 1000)
        return (maxCode + 1).toString()
    }
    
    fun openAddModal() {
        editingCustomer = null
        formCCode = getNextCustomerCode()
        formName = ""
        formMobile = "7"
        formAddress = "خمر"
        formBalance = "0"
        errorMsg = ""
        isModalOpen = true
    }
    
    fun openEditModal(customer: Customer) {
        editingCustomer = customer
        formCCode = customer.cCode
        formName = customer.name
        formMobile = customer.mobile ?: ""
        formAddress = customer.address ?: ""
        formBalance = customer.balance.toString()
        errorMsg = ""
        isModalOpen = true
    }
    
    fun handleSubmit() {
        errorMsg = ""
        
        val trimmed = formName.trim()
        if (trimmed.isEmpty()) {
            errorMsg = "يرجى كتابة اسم العميل"
            return
        }
        
        val bal = if (canEditOpeningBalance) {
            formBalance.toDoubleOrNull() ?: 0.0
        } else {
            editingCustomer?.balance ?: 0.0
        }
        
        val customer = Customer(
            id = editingCustomer?.id ?: "cust_${System.currentTimeMillis()}",
            cCode = formCCode.trim().ifEmpty { getNextCustomerCode() },
            name = trimmed,
            mobile = formMobile.trim(),
            address = formAddress.trim(),
            balance = bal,
            createdAt = editingCustomer?.createdAt ?: System.currentTimeMillis()
        )
        
        onSaveCustomer(customer, editingCustomer != null)
        isModalOpen = false
    }
    
    val filteredCustomers by remember {
        derivedStateOf {
            val s = searchTerm.lowercase().trim()
            if (s.isEmpty()) customers
            else customers.filter { c ->
                c.name.lowercase().contains(s) ||
                c.cCode.contains(s) ||
                (c.mobile?.contains(s) == true)
            }
        }
    }
    
    val totalDebents by remember {
        derivedStateOf {
            customers.sumOf { c -> if (c.balance > 0) c.balance else 0.0 }
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
                    Text(
                        text = "دليل وبحث العملاء (أكواد C_CODE تبدأ من 1001)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "إجمالي الديون القائمة: ${String.format("%,.2f", totalDebents)} ${settings.currencySymbol}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Button(
                    onClick = { openAddModal() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F766E)
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
                        text = "+ عميل جديد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
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
                        text = "🔍 ابحث بالاسم، كود العميل (C_CODE)، أو رقم الهاتف...",
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
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Customers List
        items(filteredCustomers, key = { it.id }) { customer ->
            CustomerItem(
                customer = customer,
                settings = settings,
                onOpenStatement = { onOpenStatement(customer) },
                onOpenBondModal = { onOpenBondModal(customer) },
                onEditClick = { openEditModal(customer) },
                onDeleteClick = { onDeleteCustomer(customer.id) }
            )
        }
        
        // Empty state
        if (filteredCustomers.isEmpty()) {
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
                            text = "لا يوجد عملاء مطابقين للبحث",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
    
    // Add / Edit Customer Modal
    if (isModalOpen) {
        CustomerModal(
            editingCustomer = editingCustomer,
            formCCode = formCCode,
            formName = formName,
            formMobile = formMobile,
            formAddress = formAddress,
            formBalance = formBalance,
            errorMsg = errorMsg,
            canEditOpeningBalance = canEditOpeningBalance,
            settings = settings,
            onCCodeChange = { formCCode = it },
            onNameChange = { formName = it },
            onMobileChange = { formMobile = it },
            onAddressChange = { formAddress = it },
            onBalanceChange = { formBalance = it },
            onClose = { isModalOpen = false },
            onSubmit = { handleSubmit() }
        )
    }
}

@Composable
private fun CustomerItem(
    customer: Customer,
    settings: SystemSettings,
    onOpenStatement: () -> Unit,
    onOpenBondModal: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDebtor = customer.balance > 0
    val isCreditor = customer.balance < 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                            color = Color(0xFFDBEAFE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "C_CODE: ${customer.cCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = customer.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = customer.mobile ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                        if (!customer.address.isNullOrBlank()) {
                            Text(
                                text = "• ${customer.address}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الرصيد الحالي:",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%,.2f", kotlin.math.abs(customer.balance))} ${settings.currencySymbol}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDebtor) Color(0xFFE11D48) 
                              else if (isCreditor) Color(0xFF047857) 
                              else Color(0xFF475569)
                    )
                    Text(
                        text = if (isDebtor) "(عليه / مدين)" 
                               else if (isCreditor) "(له / دائن)" 
                               else "(خالص)",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onOpenStatement,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEF3C7)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "كشف حساب",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                    
                    Button(
                        onClick = onOpenBondModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECFDF5)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "سند قبض",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                    
                    if (!customer.mobile.isNullOrBlank()) {
                        IconButton(
                            onClick = { 
                                // TODO: Open WhatsApp
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// TODO: Add CustomerModal component
