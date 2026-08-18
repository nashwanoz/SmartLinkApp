package com.smartlink.erp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.utils.WhatsAppUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    customers: List<Customer>,
    users: List<User>,
    products: List<Product>,
    invoices: List<Invoice>,
    bonds: List<Bond>,
    settings: SystemSettings,
    preselectedCustomerId: String? = null
) {
    // State variables
    var statementType by remember { mutableStateOf("customer") }
    var customerSearchTerm by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf(preselectedCustomerId ?: customers.firstOrNull()?.id ?: "") }
    var isCustomerSearchOpen by remember { mutableStateOf(false) }
    var selectedCashierId by remember { mutableStateOf(users.firstOrNull { it.role == "CASHIER" }?.id ?: users.firstOrNull()?.id ?: "") }
    
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var invoiceStartDate by remember { mutableStateOf(todayStr) }
    var invoiceEndDate by remember { mutableStateOf(todayStr) }
    var receiptStartDate by remember { mutableStateOf(todayStr) }
    var receiptEndDate by remember { mutableStateOf(todayStr) }
    
    // Derived states
    val customer by remember {
        derivedStateOf { customers.find { c -> c.id == selectedCustomerId } }
    }
    
    val cashier by remember {
        derivedStateOf { users.find { u -> u.id == selectedCashierId } }
    }
    
    val filteredCustomers by remember {
        derivedStateOf {
            val term = customerSearchTerm.trim().lowercase()
            if (term.isEmpty()) customers
            else customers.filter { c ->
                c.name.lowercase().contains(term) ||
                c.cCode.contains(term) ||
                (c.mobile?.contains(term) == true)
            }
        }
    }
    
    val customerInvoices by remember {
        derivedStateOf { invoices.filter { inv -> inv.customerId == selectedCustomerId } }
    }
    
    val customerBonds by remember {
        derivedStateOf { bonds.filter { b -> b.customerId == selectedCustomerId } }
    }
    
    val lastInvoice by remember {
        derivedStateOf { customerInvoices.maxByOrNull { it.date } }
    }
    
    val lastPayment by remember {
        derivedStateOf {
            val payments = mutableListOf<Pair<Long, String>>()
            customerBonds.filter { it.type == "RECEIPT" }.forEach { b ->
                payments.add(Pair(b.date, "سند #${b.bondNumber}"))
            }
            customerInvoices.filter { it.paidAmount > 0 }.forEach { i ->
                payments.add(Pair(i.date, "فاتورة ${i.invoiceNumber}"))
            }
            payments.maxByOrNull { it.first }
        }
    }
    
    val netBalance by remember {
        derivedStateOf { customer?.balance ?: 0.0 }
    }
    
    val cashierInvoices by remember {
        derivedStateOf { invoices.filter { inv -> inv.cashierId == selectedCashierId } }
    }
    
    val cashierBonds by remember {
        derivedStateOf { bonds.filter { b -> b.cashierId == selectedCashierId && b.type == "RECEIPT" } }
    }
    
    val cashierTotalSales by remember {
        derivedStateOf { cashierInvoices.sumOf { it.total } }
    }
    
    val cashierPaidInvoices by remember {
        derivedStateOf { cashierInvoices.sumOf { it.paidAmount } }
    }
    
    val cashierBondsCollected by remember {
        derivedStateOf { cashierBonds.sumOf { it.amount } }
    }
    
    val cashierDrawerCash by remember {
        derivedStateOf { cashierPaidInvoices + cashierBondsCollected }
    }
    
    // Main UI
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "نظام كشوفات الحسابات والتقارير",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "توليد كشوفات الحسابات المعتمدة وطباعتها بصيغة PDF نقية",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        
        // Navigation Sub-Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabButton(
                    text = "كشف حساب عميل",
                    icon = Icons.Default.People,
                    selected = statementType == "customer",
                    onClick = { statementType = "customer" },
                    modifier = Modifier.weight(1f)
                )
                
                TabButton(
                    text = "كشف مبيعات كاشير",
                    icon = Icons.Default.ReceiptLong,
                    selected = statementType == "cashier",
                    onClick = { statementType = "cashier" },
                    modifier = Modifier.weight(1f)
                )
                
                TabButton(
                    text = "كشف مخزون الكواشير",
                    icon = Icons.Default.Inventory,
                    selected = statementType == "inventory",
                    onClick = { statementType = "inventory" },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 1. CUSTOMER STATEMENT
        if (statementType == "customer") {
            item {
                CustomerStatementSection(
                    customer = customer,
                    customers = customers,
                    customerSearchTerm = customerSearchTerm,
                    filteredCustomers = filteredCustomers,
                    lastInvoice = lastInvoice,
                    lastPayment = lastPayment,
                    netBalance = netBalance,
                    settings = settings,
                    onSearchTermChange = { customerSearchTerm = it },
                    onSearchOpenChange = { isCustomerSearchOpen = it },
                    onCustomerSelect = { selectedCustomerId = it },
                    isSearchOpen = isCustomerSearchOpen
                )
            }
        }
        
        // 2. CASHIER STATEMENT
        if (statementType == "cashier" && cashier != null) {
            item {
                CashierStatementSection(
                    cashier = cashier!!,
                    cashierInvoices = cashierInvoices,
                    cashierBonds = cashierBonds,
                    cashierTotalSales = cashierTotalSales,
                    cashierPaidInvoices = cashierPaidInvoices,
                    cashierBondsCollected = cashierBondsCollected,
                    cashierDrawerCash = cashierDrawerCash,
                    users = users,
                    selectedCashierId = selectedCashierId,
                    invoiceStartDate = invoiceStartDate,
                    invoiceEndDate = invoiceEndDate,
                    receiptStartDate = receiptStartDate,
                    receiptEndDate = receiptEndDate,
                    settings = settings,
                    onCashierSelect = { selectedCashierId = it },
                    onInvoiceStartDateChange = { invoiceStartDate = it },
                    onInvoiceEndDateChange = { invoiceEndDate = it },
                    onReceiptStartDateChange = { receiptStartDate = it },
                    onReceiptEndDateChange = { receiptEndDate = it }
                )
            }
        }
        
        // 3. INVENTORY STATEMENT
        if (statementType == "inventory") {
            item {
                InventoryStatementSection(
                    products = products,
                    users = users,
                    selectedCashierId = selectedCashierId,
                    settings = settings,
                    onCashierSelect = { selectedCashierId = it }
                )
            }
        }
    }
}

// ============================================================================
// TabButton Component
// ============================================================================
@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = if (selected) {
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
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color(0xFF475569),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
        )
    }
}

// ============================================================================
// CustomerStatementSection
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerStatementSection(
    customer: Customer?,
    customers: List<Customer>,
    customerSearchTerm: String,
    filteredCustomers: List<Customer>,
    lastInvoice: Invoice?,
    lastPayment: Pair<Long, String>?,
    netBalance: Double,
    settings: SystemSettings,
    onSearchTermChange: (String) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    onCustomerSelect: (String) -> Unit,
    isSearchOpen: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Customer Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Box Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مربع بحث عن العميل (كود C_CODE أو الاسم أو الهاتف):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        
                        if (customer != null) {
                            Surface(
                                color = Color(0xFFF0FDFA),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF99F6E4))
                            ) {
                                Text(
                                    text = "الرصيد: ${String.format("%,.2f", customer.balance)} ${settings.currencySymbol}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF134E4A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    // Search TextField with Dropdown
                    Box {
                        OutlinedTextField(
                            value = customerSearchTerm,
                            onValueChange = {
                                onSearchTermChange(it)
                                onSearchOpenChange(true)
                            },
                            placeholder = {
                                Text(
                                    text = if (customer != null) {
                                        "👤 [${customer.cCode}] ${customer.name} (رصيده: ${String.format("%,.2f", customer.balance)} ${settings.currencySymbol})"
                                    } else {
                                        "🔍 اكتب اسم العميل أو كود الحساب للبحث..."
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (customerSearchTerm.isNotEmpty() || customer != null) {
                                    IconButton(
                                        onClick = {
                                            onSearchTermChange("")
                                            onSearchOpenChange(false)
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
                            colors = if (customer != null) {
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF14B8A6),
                                    unfocusedBorderColor = Color(0xFF14B8A6),
                                    focusedContainerColor = Color(0xFFF0FDFA).copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color(0xFFF0FDFA).copy(alpha = 0.3f)
                                )
                            } else {
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFCBD5E1),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            }
                        )
                        
                        // Customer Search Dropdown Results
                        if (isSearchOpen && filteredCustomers.isNotEmpty() && customer == null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .align(Alignment.BottomStart),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredCustomers, key = { it.id }) { c ->
                                        Surface(
                                            onClick = {
                                                onCustomerSelect(c.id)
                                                onSearchTermChange("")
                                                onSearchOpenChange(false)
                                            },
                                            color = if (customer?.id == c.id) {
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
                                                                text = "[${c.cCode}]",
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Color(0xFF134E4A),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = c.name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                    }
                                                    
                                                    if (!c.mobile.isNullOrBlank()) {
                                                        Text(
                                                            text = c.mobile,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }
                                                
                                                Text(
                                                    text = "${String.format("%,.2f", c.balance)} ${settings.currencySymbol}",
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
                
                // Customer Summary Card
                if (customer != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = customer.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1E293B)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "كود العميل: ${customer.cCode}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        if (!customer.mobile.isNullOrBlank()) {
                                            Text(
                                                text = "• هاتف: ${customer.mobile}",
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
                                        text = "الرصيد المتبقي الحالي:",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format("%,.2f", kotlin.math.abs(netBalance))} ${settings.currencySymbol}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (netBalance > 0) Color(0xFFE11D48) 
                                              else if (netBalance < 0) Color(0xFF047857) 
                                              else Color(0xFF475569)
                                    )
                                    Text(
                                        text = if (netBalance > 0) "(عليه / مدين)" 
                                               else if (netBalance < 0) "(له / دائن)" 
                                               else "(خالص)",
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            
                            // Last Invoice & Last Payment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "آخر فاتورة:",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (lastInvoice != null) formatDateTime(lastInvoice.date) else "لا توجد فواتير",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }
                                
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "آخر سداد:",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (lastPayment != null) formatDateTime(lastPayment.first) else "لا يوجد سداد",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                            
                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { /* TODO: Open interactive modal */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "عرض كشف الحساب التفاعلي 📱💻",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                
                                Button(
                                    onClick = { /* TODO: Generate PDF */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF92400E)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "توليد PDF 📄",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        val msg = buildCustomerStatementMessage(customer, settings, netBalance, customerInvoices, customerBonds)
                                        WhatsAppUtils.sendTextOnly(
                                            androidx.compose.ui.platform.LocalContext.current,
                                            customer.mobile ?: "",
                                            msg
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "واتساب",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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

// ============================================================================
// CashierStatementSection
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierStatementSection(
    cashier: User,
    cashierInvoices: List<Invoice>,
    cashierBonds: List<Bond>,
    cashierTotalSales: Double,
    cashierPaidInvoices: Double,
    cashierBondsCollected: Double,
    cashierDrawerCash: Double,
    users: List<User>,
    selectedCashierId: String,
    invoiceStartDate: String,
    invoiceEndDate: String,
    receiptStartDate: String,
    receiptEndDate: String,
    settings: SystemSettings,
    onCashierSelect: (String) -> Unit,
    onInvoiceStartDateChange: (String) -> Unit,
    onInvoiceEndDateChange: (String) -> Unit,
    onReceiptStartDateChange: (String) -> Unit,
    onReceiptEndDateChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Select Cashier Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر الكاشير / المستخدم (يبدأ من كود 101)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                
                OutlinedTextField(
                    value = users.find { u -> u.id == selectedCashierId }?.name ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFCBD5E1),
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledTextColor = Color(0xFF1E293B)
                    )
                )
                
                // Liquidation & Drawer Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "إجمالي مبيعات الفواتير:",
                        value = String.format("%,.2f", cashierTotalSales),
                        subtitle = "${cashierInvoices.size} فواتير",
                        bgColor = Color(0xFFF0FDFA),
                        borderColor = Color(0xFF99F6E4),
                        valueColor = Color(0xFF134E4A),
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryCard(
                        title = "المقبوض نقداً بالفواتير:",
                        value = String.format("%,.2f", cashierPaidInvoices),
                        subtitle = "سداد فوري",
                        bgColor = Color(0xFFECFDF5),
                        borderColor = Color(0xFFA7F3D0),
                        valueColor = Color(0xFF047857),
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryCard(
                        title = "المقبوض بسندات قبض:",
                        value = String.format("%,.2f", cashierBondsCollected),
                        subtitle = "${cashierBonds.size} سندات",
                        bgColor = Color(0xFFFEF3C7),
                        borderColor = Color(0xFFFDE68A),
                        valueColor = Color(0xFF92400E),
                        modifier = Modifier.weight(1f)
                    )
                    
                    SummaryCard(
                        title = "صافي الصندوق للتصفية:",
                        value = String.format("%,.2f", cashierDrawerCash),
                        subtitle = "نقدية توريد الصندوق",
                        bgColor = Color(0xFF0F766E),
                        borderColor = Color(0xFF0F766E),
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Report Section 1: Invoices by Date Range
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "استعراض فواتير الكاشير حسب الفترة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Text(
                        text = "0 فاتورة بالفترة",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = invoiceStartDate,
                        onValueChange = onInvoiceStartDateChange,
                        label = { Text("من تاريخ:", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D9488),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    
                    OutlinedTextField(
                        value = invoiceEndDate,
                        onValueChange = onInvoiceEndDateChange,
                        label = { Text("إلى تاريخ:", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D9488),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
                
                Button(
                    onClick = { /* TODO: Generate PDF */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "توليد كشف الفواتير PDF 📄",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        
        // Report Section 2: Receipts by Date Range
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "المقبوضات والتحصيلات النقدية حسب الفترة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Text(
                        text = "0 عملية مقبوضة",
                        fontSize = 10.sp,
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = receiptStartDate,
                        onValueChange = onReceiptStartDateChange,
                        label = { Text("من تاريخ:", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    
                    OutlinedTextField(
                        value = receiptEndDate,
                        onValueChange = onReceiptEndDateChange,
                        label = { Text("إلى تاريخ:", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
                
                Button(
                    onClick = { /* TODO: Generate PDF */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "توليد كشف المقبوضات PDF 💵",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================================
// InventoryStatementSection
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryStatementSection(
    products: List<Product>,
    users: List<User>,
    selectedCashierId: String,
    settings: SystemSettings,
    onCashierSelect: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cashier
