package com.smartlink.erp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.InvoiceItem
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.utils.WhatsAppUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PosScreen(
    currentUser: User,
    products: List<Product>,
    customers: List<Customer>,
    settings: SystemSettings,
    onSaveInvoice: (Invoice) -> Unit,
    preselectedProduct: Product? = null,
    onClearPreselectedProduct: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Invoice items state
    var items by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    
    // Customer selection
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var customerSearch by remember { mutableStateOf("") }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }
    
    // Discount & Notes
    var discount by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    
    // Payment Mode: Default CREDIT (آجل)
    var paymentMethod by remember { mutableStateOf("CREDIT") }
    var paidAmountInput by remember { mutableStateOf("0") }
    
    // Product Selection Modal
    var modalProduct by remember { mutableStateOf<Product?>(null) }
    var modalUnitType by remember { mutableStateOf("minor") }
    var modalQty by remember { mutableStateOf("1") }
    
    // Completed Invoice Modal
    var completedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var copiedInvoice by remember { mutableStateOf(false) }
    
    // Selected customer object
    val selectedCustomer by remember {
        derivedStateOf {
            selectedCustomerId?.let { id -> customers.find { c -> c.id == id } }
        }
    }
    
    // Filter customers for search
    val filteredCustomers by remember {
        derivedStateOf {
            val s = customerSearch.lowercase().trim()
            if (s.isEmpty()) customers
            else customers.filter { c ->
                c.name.lowercase().contains(s) ||
                c.cCode.contains(s) ||
                (c.mobile?.contains(s) == true)
            }
        }
    }
    
    // Fast direct add when clicking a product
    fun handleQuickAddProduct(product: Product) {
        modalProduct = product
        modalUnitType = "minor"
        modalQty = "1"
    }
    
    fun handleConfirmItemAdd() {
        modalProduct?.let { product ->
            val qty = modalQty.toDoubleOrNull() ?: return
            if (qty <= 0) return
            
            val isMajor = modalUnitType == "major"
            val unitName = if (isMajor) product.caseUnitName else product.unitName
            val unitPrice = if (isMajor) product.casePrice else product.price
            val total = qty * unitPrice
            val convertedMinorQty = if (isMajor) qty * product.caseQuantity else qty
            
            val existingIndex = items.indexOfFirst {
                it.productId == product.id && it.unitType == modalUnitType
            }
            
            if (existingIndex > -1) {
                val updated = items.toMutableList()
                updated[existingIndex] = updated[existingIndex].copy(
                    quantity = updated[existingIndex].quantity + qty,
                    total = updated[existingIndex].total + total,
                    convertedMinorQty = updated[existingIndex].convertedMinorQty + convertedMinorQty
                )
                items = updated
            } else {
                items = items + InvoiceItem(
                    productId = product.id,
                    productName = product.name,
                    unitType = modalUnitType,
                    unitName = unitName,
                    quantity = qty,
                    unitPrice = unitPrice,
                    total = total,
                    convertedMinorQty = convertedMinorQty
                )
            }
            
            modalProduct = null
        }
    }
    
    fun handleRemoveItem(index: Int) {
        items = items.filterIndexed { i, _ -> i != index }
    }
    
    fun handleUpdateItemQty(index: Int, newQty: Double) {
        if (newQty <= 0) {
            handleRemoveItem(index)
            return
        }
        
        val item = items[index]
        val product = products.find { p -> p.id == item.productId } ?: return
        
        val isMajor = item.unitType == "major"
        val convertedMinorQty = if (isMajor) newQty * product.caseQuantity else newQty
        val total = newQty * item.unitPrice
        
        val updated = items.toMutableList()
        updated[index] = item.copy(
            quantity = newQty,
            total = total,
            convertedMinorQty = convertedMinorQty
        )
        items = updated
    }
    
    // Calculations
    val subtotal by remember {
        derivedStateOf {
            items.sumOf { it.total }
        }
    }
    
    val discountVal by remember {
        derivedStateOf {
            val d = discount.toDoubleOrNull() ?: 0.0
            minOf(subtotal, maxOf(0.0, d))
        }
    }
    
    val finalTotal by remember {
        derivedStateOf {
            maxOf(0.0, subtotal - discountVal)
        }
    }
    
    fun handlePaymentMethodChange(mode: String) {
        paymentMethod = mode
        validationError = ""
        
        when (mode) {
            "CASH" -> {
                paidAmountInput = finalTotal.toString()
                showCustomerDropdown = false
            }
            "CREDIT" -> {
                paidAmountInput = "0"
            }
            "PARTIAL" -> {
                if (paidAmountInput == "0" || paidAmountInput == finalTotal.toString()) {
                    paidAmountInput = (finalTotal / 2).toString()
                }
            }
        }
    }
    
    val paidAmount by remember {
        derivedStateOf {
            when (paymentMethod) {
                "CASH" -> finalTotal
                "CREDIT" -> 0.0
                "PARTIAL" -> {
                    val paid = paidAmountInput.toDoubleOrNull() ?: 0.0
                    minOf(finalTotal, maxOf(0.0, paid))
                }
                else -> 0.0
            }
        }
    }
    
    val remainingAmount by remember {
        derivedStateOf {
            maxOf(0.0, finalTotal - paidAmount)
        }
    }
    
    val prevCustomerBalance by remember {
        derivedStateOf {
            selectedCustomer?.balance ?: 0.0
        }
    }
    
    val newCustomerBalance by remember {
        derivedStateOf {
            prevCustomerBalance + remainingAmount
        }
    }
    
    // Checkout and Save
    fun handleSaveInvoice() {
        validationError = ""
        
        if (items.isEmpty()) {
            validationError = "يرجى إضافة أصناف إلى الفاتورة أولاً"
            return
        }
        
        // Validation: If CREDIT or PARTIAL, customer is STRICTLY required
        if (paymentMethod != "CASH" && selectedCustomer == null) {
            validationError = "يرجى البحث عن العميل واختياره أولاً للفواتير الآجلة أو السداد الجزئي"
            return
        }
        
        // Determine customer fields
        val isCashOnly = paymentMethod == "CASH" && selectedCustomer == null
        val customerId = if (isCashOnly) "CASH_CUSTOMER" else selectedCustomer!!.id
        val customerCode = if (isCashOnly) "0000" else selectedCustomer!!.cCode
        val customerName = if (isCashOnly) "عميل نقدي" else selectedCustomer!!.name
        val customerMobile = if (isCashOnly) "" else (selectedCustomer!!.mobile ?: "")
        
        val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(4)}"
        val newInvoice = Invoice(
            id = "inv_${System.currentTimeMillis()}",
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerCode = customerCode,
            customerName = customerName,
            customerMobile = customerMobile,
            cashierId = currentUser.id,
            cashierCode = currentUser.userCode,
            cashierName = currentUser.name,
            items = items.toList(),
            subtotal = subtotal,
            discount = discountVal,
            total = finalTotal,
            paymentMethod = paymentMethod,
            paidAmount = paidAmount,
            remainingAmount = if (isCashOnly) 0.0 else remainingAmount,
            prevCustomerBalance = if (isCashOnly) 0.0 else prevCustomerBalance,
            newCustomerBalance = if (isCashOnly) 0.0 else newCustomerBalance,
            date = System.currentTimeMillis(),
            notes = notes.trim()
        )
        
        onSaveInvoice(newInvoice)
        completedInvoice = newInvoice
        
        // Reset Form
        items = emptyList()
        selectedCustomerId = null
        customerSearch = ""
        discount = "0"
        paidAmountInput = "0"
        notes = ""
        validationError = ""
        
        // Auto-print if enabled
        if (settings.autoPrintAfterInvoice) {
            // TODO: Call print function
            Toast.makeText(context, "جاري الطباعة...", Toast.LENGTH_SHORT).show()
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. TOP HEADER: Cashier info & Customer Search Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF0F766E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    text = "إصدار الفاتورة المباشرة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "الكاشير: ${currentUser.name}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        
                        Surface(
                            color = if (paymentMethod == "CASH" && selectedCustomer == null) {
                                Color(0xFFD1FAE5)
                            } else if (selectedCustomer != null) {
                                Color(0xFFCCFBF1)
                            } else {
                                Color(0xFFFEE2E2)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    paymentMethod == "CASH" && selectedCustomer == null -> "💵 عميل نقدي"
                                    selectedCustomer != null -> "[${selectedCustomer!!.cCode}] ${selectedCustomer!!.name}"
                                    else -> "⚠️ لم يتم تحديد عميل"
                                },
                                fontSize = if (selectedCustomer != null) 10.sp else 11.sp,
                                fontWeight = if (paymentMethod == "CASH" && selectedCustomer == null) FontWeight.Black else FontWeight.Bold,
                                color = when {
                                    paymentMethod == "CASH" && selectedCustomer == null -> Color(0xFF065F46)
                                    selectedCustomer != null -> Color(0xFF134E4A)
                                    else -> Color(0xFFE11D48)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // CUSTOMER SEARCH BOX OR CASH CUSTOMER BADGE
                    if (paymentMethod == "CASH" && selectedCustomer == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFECFDF5)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF6EE7B7).copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✓",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "عميل نقدي (دفع فوري بالكامل)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF022C22)
                                        )
                                        Text(
                                            text = "لا يلزم اختيار عميل للفواتير النقدية",
                                            fontSize = 10.sp,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                                
                                TextButton(
                                    onClick = {
                                        customerSearch = ""
                                        showCustomerDropdown = true
                                    }
                                ) {
                                    Text(
                                        text = "تحديد عميل محدد؟",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }
                    } else {
                        Box {
                            OutlinedTextField(
                                value = if (selectedCustomer != null) {
                                    "👤 [${selectedCustomer!!.cCode}] ${selectedCustomer!!.name} (رصيد: ${String.format("%,.2f", selectedCustomer!!.balance)} ${settings.currencySymbol})"
                                } else {
                                    customerSearch
                                },
                                onValueChange = {
                                    customerSearch = it
                                    showCustomerDropdown = true
                                    if (selectedCustomerId != null) selectedCustomerId = null
                                },
                                placeholder = {
                                    Text(
                                        text = "🔍 مربع بحث عن العميل بالاسم أو رقم الحساب C_CODE...",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (customerSearch.isNotEmpty() || selectedCustomer != null) {
                                        IconButton(
                                            onClick = {
                                                selectedCustomerId = null
                                                customerSearch = ""
                                                showCustomerDropdown = false
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
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                enabled = selectedCustomer == null
                            )
                            
                            // Customer Search Dropdown
                            if (showCustomerDropdown && selectedCustomer == null) {
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
                                        if (filteredCustomers.isEmpty()) {
                                            item {
                                                Text(
                                                    text = "لا يوجد عميل مطابق للبحث",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp)
                                                )
                                            }
                                        } else {
                                            items(filteredCustomers, key = { it.id }) { customer ->
                                                Surface(
                                                    onClick = {
                                                        selectedCustomerId = customer.id
                                                        customerSearch = ""
                                                        showCustomerDropdown = false
                                                        validationError = ""
                                                    },
                                                    color = if (selectedCustomerId == customer.id) {
                                                        Color(0xFF0F766E)
                                                    } else {
                                                        Color(0xFFF0FDFA)
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
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "[${customer.cCode}]",
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = if (selectedCustomerId == customer.id) {
                                                                    Color.White.copy(alpha = 0.8f)
                                                                } else {
                                                                    Color(0xFF475569)
                                                                }
                                                            )
                                                            Text(
                                                                text = customer.name,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (selectedCustomerId == customer.id) {
                                                                    Color.White
                                                                } else {
                                                                    Color(0xFF1E293B)
                                                                }
                                                            )
                                                        }
                                                        
                                                        Text(
                                                            text = "رصيد: ${String.format("%,.2f", customer.balance)} ${settings.currencySymbol}",
                                                            fontSize = 10.sp,
                                                            color = if (selectedCustomerId == customer.id) {
                                                                Color.White.copy(alpha = 0.9f)
                                                            } else {
                                                                Color(0xFF64748B)
                                                            }
                                                        )
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
            }
        }
        
        // 2. HORIZONTAL SCROLLING PRODUCT RIBBON
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "شريط الأصناف (اسحب يميناً أو يساراً واختر الصنف):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "${products.size} صنف متوفر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D9488)
                        )
                    }
                    
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        products.forEach { product ->
                            Surface(
                                onClick = { handleQuickAddProduct(product) },
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .width(128.dp)
                                    .height(80.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = product.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${product.price} ${settings.currencySymbol}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF134E4A)
                                        )
                                        Surface(
                                            color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "+${product.unitName}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 3. CURRENT INVOICE ITEMS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "أصناف الفاتورة الحالية (${items.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        if (items.isNotEmpty()) {
                            TextButton(
                                onClick = { items = emptyList() }
                            ) {
                                Text(
                                    text = "مسح الكل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }
                    }
                    
                    // Validation Error Banner
                    if (validationError.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEE2E2)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = validationError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                    
                    // Items List
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "لا توجد أصناف مضافة في الفاتورة بعد.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "اضغط على أي صنف من الشريط العلوي لإضافته مباشرة أمامك في الفاتورة.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(items) { index, item ->
                                InvoiceItemRow(
                                    item = item,
                                    onDecreaseQty = { handleUpdateItemQty(index, item.quantity - 1) },
                                    onIncreaseQty = { handleUpdateItemQty(index, item.quantity + 1) },
                                    onRemove = { handleRemoveItem(index) }
                                )
                            }
                        }
                    }
                    
                    // 4. TOTALS, PARTIAL PAYMENT & SAVE BUTTON
                    if (items.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            
                            // Subtotal & Discount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "إجمالي الأصناف:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "${String.format("%,.2f", subtotal)} ${settings.currencySymbol}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الخصم الممنوح:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                                OutlinedTextField(
                                    value = discount,
                                    onValueChange = { discount = it },
                                    modifier = Modifier.width(96.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFE2E8F0),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "اجمالي الفاتورة:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F766E)
                                )
                                Text(
                                    text = "${String.format("%,.2f", finalTotal)} ${settings.currencySymbol}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F766E)
                                )
                            }
                            
                            // PARTIAL PAYMENT / PAYMENT SELECTION BOX
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "طريقة السداد:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = when (paymentMethod) {
                                                "PARTIAL" -> "سداد جزئي"
                                                "CASH" -> "نقداً كامل"
                                                else -> "آجل كامل"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D9488)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = { handlePaymentMethodChange("CREDIT") },
                                            colors = if (paymentMethod == "CREDIT") {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFEE2E2),
                                                    contentColor = Color(0xFF991B1B)
                                                )
                                            } else {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = Color(0xFF64748B)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                        ) {
                                            Text(
                                                text = "آجل كامل",
                                                fontSize = 10.sp,
                                                fontWeight = if (paymentMethod == "CREDIT") FontWeight.Black else FontWeight.Bold
                                            )
                                        }
                                        
                                        Button(
                                            onClick = { handlePaymentMethodChange("PARTIAL") },
                                            colors = if (paymentMethod == "PARTIAL") {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFEF3C7),
                                                    contentColor = Color(0xFF92400E)
                                                )
                                            } else {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = Color(0xFF64748B)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                        ) {
                                            Text(
                                                text = "سداد جزئي",
                                                fontSize = 10.sp,
                                                fontWeight = if (paymentMethod == "PARTIAL") FontWeight.Black else FontWeight.Bold
                                            )
                                        }
                                        
                                        Button(
                                            onClick = { handlePaymentMethodChange("CASH") },
                                            colors = if (paymentMethod == "CASH") {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFD1FAE5),
                                                    contentColor = Color(0xFF065F46)
                                                )
                                            } else {
                                                ButtonDefaults.buttonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = Color(0xFF64748B)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                        ) {
                                            Text(
                                                text = "نقداً كامل",
                                                fontSize = 10.sp,
                                                fontWeight = if (paymentMethod == "CASH") FontWeight.Black else FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    // Partial Payment Input Box
                                    if (paymentMethod == "PARTIAL") {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFEF3C7)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "المبلغ المدفوع حالياً (إيصال):",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF78350F)
                                                    )
                                                    OutlinedTextField(
                                                        value = paidAmountInput,
                                                        onValueChange = { paidAmountInput = it },
                                                        modifier = Modifier.width(112.dp),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        textStyle = LocalTextStyle.current.copy(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color(0xFF92400E)
                                                        ),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = Color(0xFFF59E0B),
                                                            unfocusedBorderColor = Color(0xFFF59E0B)
                                                        )
                                                    )
                                                }
                                                
                                                Divider(color = Color(0xFFFDE68A).copy(alpha = 0.6f), thickness = 1.dp)
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "المسدد نقداً: ${String.format("%,.2f", paidAmount)}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857)
                                                    )
                                                    Text(
                                                        text = "المتبقي آجل: ${String.format("%,.2f", remainingAmount)}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE11D48)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Balance projection
                                    if (selectedCustomer != null) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "الرصيد السابق: ${String.format("%,.2f", prevCustomerBalance)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569)
                                                )
                                                Text(
                                                    text = "الرصيد الإجمالي: ${String.format("%,.2f", newCustomerBalance)} ${settings.currencySymbol}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF134E4A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Optional Notes
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = {
                                    Text(
                                        text = "ملاحظات على الفاتورة (اختياري)...",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE2E8F0),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp
                                )
                            )
                            
                            // SAVE INVOICE BUTTON
                            Button(
                                onClick = { handleSaveInvoice() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F766E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "حفظ الفاتورة (${String.format("%,.2f", finalTotal)} ${settings.currencySymbol}) 🚀",
                                    fontSize = 14.sp,
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
    
    // MODAL: ADD ITEM DETAILS (Unit type and quantity)
    modalProduct?.let { product ->
        ProductSelectionModal(
            product = product,
            settings = settings,
            modalUnitType = modalUnitType,
            modalQty = modalQty,
            onUnitTypeChange = { modalUnitType = it },
            onQtyChange = { modalQty = it },
            onClose = { modalProduct = null },
            onConfirm = { handleConfirmItemAdd() }
        )
    }
    
    // COMPLETED INVOICE MODAL (THERMAL PRINT & WHATSAPP)
    completedInvoice?.let { invoice ->
        CompletedInvoiceModal(
            invoice = invoice,
            settings = settings,
            context = context,
            copiedInvoice = copiedInvoice,
            onCopySuccess = { copiedInvoice = true },
            onCopyReset = { copiedInvoice = false },
            onClose = { completedInvoice = null }
        )
    }
}

@Composable
private fun InvoiceItemRow(
    item: InvoiceItem,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.productName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
                Text(
                    text = "${item.quantity} ${item.unitName} × ${String.format("%,.2f", item.unitPrice)}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Stepper
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDecreaseQty,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = item.quantity.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(
                            onClick = onIncreaseQty,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                
                // Line Total
                Text(
                    text = String.format("%,.0f", item.total),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.width(64.dp)
                )
                
                // Remove Button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
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

@Composable
private fun ProductSelectionModal(
    product: Product,
    settings: SystemSettings,
    modalUnitType: String,
    modalQty: String,
    onUnitTypeChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit
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
                    Text(
                        text = product.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Unit choice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onUnitTypeChange("minor") },
                        colors = if (modalUnitType == "minor") {
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
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
                            .height(44.dp)
                    ) {
                        Text(
                            text = "${product.unitName} (${product.price} ${settings.currencySymbol})",
                            fontSize = 12.sp,
                            fontWeight = if (modalUnitType == "minor") FontWeight.Black else FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = { onUnitTypeChange("major") },
                        colors = if (modalUnitType == "major") {
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED),
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
                            .height(44.dp)
                    ) {
                        Text(
                            text = "${product.caseUnitName} (${product.casePrice} ${settings.currencySymbol})",
                            fontSize = 12.sp,
                            fontWeight = if (modalUnitType == "major") FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
                
                // Quantity
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "الكمية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val current = modalQty.toDoubleOrNull() ?: 1.0
                                onQtyChange(maxOf(1.0, current - 1.0).toString())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "-",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        OutlinedTextField(
                            value = modalQty,
                            onValueChange = onQtyChange,
                            modifier = Modifier
                                .width(80.dp)
                                .padding(horizontal = 8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF14B8A6),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        Button(
                            onClick = {
                                val current = modalQty.toDoubleOrNull() ?: 1.0
                                onQtyChange((current + 1.0).toString())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                // Total Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0FDFA)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF99F6E4))
                ) {
                    Text(
                        text = "الإجمالي: ${String.format("%,.2f", (modalQty.toDoubleOrNull() ?: 0.0) * (if (modalUnitType == "major") product.casePrice else product.price))} ${settings.currencySymbol}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF134E4A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
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
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "تأكيد الإضافة للفاتورة",
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

@Composable
private fun CompletedInvoiceModal(
    invoice: Invoice,
    settings: SystemSettings,
    context: Context,
    copiedInvoice: Boolean,
    onCopySuccess: () -> Unit,
    onCopyReset: () -> Unit,
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
                        text = "تم حفظ الفاتورة بنجاح ✨",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = invoice.invoiceNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF134E4A)
                    )
                }
                
                // Quick Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "العميل:",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = invoice.customerName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "اجمالي الفاتورة:",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "${String.format("%,.2f", invoice.total)} ${settings.currencySymbol}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        if (invoice.paidAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المبلغ المسدد (نقداً):",
                                    fontSize = 12.sp,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = "${String.format("%,.2f", invoice.paidAmount)} ${settings.currencySymbol}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                        if (invoice.remainingAmount > 0) {
                            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الرصيد الإجمالي:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF134E4A)
                                )
                                Text(
                                    text = "${String.format("%,.2f", invoice.newCustomerBalance)} ${settings.currencySymbol}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF134E4A)
                                )
                            }
                        }
                    }
                }
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ مشاركة PDF + نص (مرتبط بـ WhatsAppUtils)
                    Button(
                        onClick = {
                            // TODO: Generate PDF first
                            val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(
                                invoice.toAppInvoice(),
                                settings
                            )
                            // WhatsAppUtils.sharePdfWithText(context, pdfFile, invoice.customerMobile ?: "", message)
                            Toast.makeText(context, "جاري إنشاء PDF...", Toast.LENGTH_SHORT).show()
                        },
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
                            text = "مشاركة الفاتورة (ملف PDF + النص) للجوال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // طباعة
                        Button(
                            onClick = {
                                // TODO: Print invoice
                                Toast.makeText(context, "جاري الطباعة...", Toast.LENGTH_SHORT).show()
                            },
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
                        
                        // ✅ واتساب نص فقط (مرتبط بـ WhatsAppUtils)
                        Button(
                            onClick = {
                                val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(
                                    invoice.toAppInvoice(),
                                    settings
                                )
                                WhatsAppUtils.sendTextOnly(context, invoice.customerMobile ?: "", message)
                            },
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
                    
                    // ✅ نسخ النص (مرتبط بـ WhatsAppUtils)
                    Button(
                        onClick = {
                            val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(
                                invoice.toAppInvoice(),
                                settings
                            )
                            val success = WhatsAppUtils.copyToClipboard(context, message)
                            if (success) {
                                onCopySuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (copiedInvoice) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "تم نسخ نص الفاتورة بنجاح ✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
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
                                text = "نسخ نص الفاتورة للحافظة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                    
                    // إغلاق
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
                            text = "إغلاق وبدء فاتورة جديدة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to convert Invoice entity to app Invoice for WhatsAppUtils
private fun Invoice.toAppInvoice(): com.smartlink.erp.data.local.entity.Invoice {
    return this
}
