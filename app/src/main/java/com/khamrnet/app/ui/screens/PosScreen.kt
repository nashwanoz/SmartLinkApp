package com.khamrnet.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khamrnet.app.data.model.*
import com.khamrnet.app.printer.BluetoothPrinterManager
import com.khamrnet.app.util.ArabicNumberConverter
import com.khamrnet.app.util.PdfThermalGenerator
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceEntity> = emptyList(),
    bonds: List<BondEntity> = emptyList(),
    currentUserName: String,
    onSaveInvoice: (InvoiceEntity, List<InvoiceItem>) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }
    val numberFormat = remember { DecimalFormat("#,##0") }
    val currencyName = settings.currencyName.ifEmpty { "ريال" }

    // Live Treasury / Drawer Box Balance calculation (Matching Web ERP)
    val totalCashSales = remember(invoices) {
        invoices.filter { !it.isCancelled }.sumOf { it.paidAmount }
    }
    val totalReceiptBonds = remember(bonds) {
        bonds.filter { it.type == "RECEIPT" }.sumOf { it.amount }
    }
    val totalPaymentBonds = remember(bonds) {
        bonds.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }
    val displayBoxBalance = (totalCashSales + totalReceiptBonds - totalPaymentBonds)

    // Current date and invoice number
    val currentDateFormatted = remember {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        sdf.format(Date())
    }
    val currentInvoiceNumber = remember(invoices) {
        val nextSeq = invoices.size + 1
        "INV-${String.format(Locale.US, "%04d", nextSeq)}"
    }

    // Invoice items state (Directly displayed on the same screen)
    var cartItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Customer Selection (No default customer - must search and choose or cash mode)
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    // Discount & Notes
    var discountInput by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    // Payment Mode: "CREDIT" (آجل كامل), "PARTIAL" (سداد جزئي), "CASH" (نقداً كامل)
    var paymentMethod by remember { mutableStateOf("CREDIT") }
    var paidAmountInput by remember { mutableStateOf("0") }

    // Product Ribbon Quick Filter & Modal Selector
    var productSearchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, productSearchQuery) {
        val q = productSearchQuery.trim().lowercase()
        if (q.isEmpty()) products
        else products.filter {
            it.name.lowercase().contains(q) || it.code.lowercase().contains(q) || it.barcode.contains(q)
        }
    }

    // Selected product for Unit / Qty modal
    var modalProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var modalUnits by remember { mutableStateOf<List<ProductUnit>>(emptyList()) }
    var modalSelectedUnitIndex by remember { mutableIntStateOf(0) }
    var modalQuantityInput by remember { mutableStateOf("1") }

    // Completed Invoice Modal state
    var completedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var completedInvoiceItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Filter customers for dropdown search
    val filteredCustomers = remember(customers, customerSearchQuery) {
        val s = customerSearchQuery.trim().lowercase()
        if (s.isEmpty()) customers
        else customers.filter {
            it.name.lowercase().contains(s) || it.code.lowercase().contains(s) || it.phone.contains(s)
        }
    }

    // Calculations
    val subtotal = remember(cartItems) { cartItems.sumOf { it.total } }
    val discountVal = (discountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val finalTotal = (subtotal - discountVal).coerceAtLeast(0.0)

    val paidAmount = when (paymentMethod) {
        "CASH" -> finalTotal
        "CREDIT" -> 0.0
        else -> (paidAmountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, finalTotal) // PARTIAL
    }
    val remainingAmount = (finalTotal - paidAmount).coerceAtLeast(0.0)

    val prevCustomerBalance = selectedCustomer?.currentBalance ?: 0.0
    val newCustomerBalance = if (paymentMethod == "CASH" && selectedCustomer == null) 0.0 else prevCustomerBalance + remainingAmount

    // Change payment method handler
    fun handlePaymentMethodChange(mode: String) {
        paymentMethod = mode
        validationError = ""
        when (mode) {
            "CASH" -> {
                paidAmountInput = if (finalTotal > 0) finalTotal.toInt().toString() else "0"
                showCustomerDropdown = false
            }
            "CREDIT" -> {
                paidAmountInput = "0"
            }
            "PARTIAL" -> {
                if (paidAmountInput == "0" || paidAmountInput == finalTotal.toInt().toString()) {
                    paidAmountInput = (finalTotal / 2).toInt().toString()
                }
            }
        }
    }

    // Open item selection modal
    fun handleQuickAddProduct(product: ProductEntity) {
        val listType = object : TypeToken<List<ProductUnit>>() {}.type
        val parsedUnits: List<ProductUnit> = try {
            Gson().fromJson(product.unitsJson, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val finalUnits = parsedUnits.ifEmpty {
            listOf(
                ProductUnit(
                    id = "base",
                    name = product.baseUnitName.ifEmpty { "حبة" },
                    factor = 1.0,
                    purchasePrice = product.purchasePrice,
                    salePrice = product.salePrice,
                    isDefault = true
                )
            )
        }
        modalProduct = product
        modalUnits = finalUnits
        modalSelectedUnitIndex = 0
        modalQuantityInput = "1"
    }

    // Confirm adding item to current invoice
    fun handleConfirmItemAdd() {
        val prod = modalProduct ?: return
        val qty = modalQuantityInput.toDoubleOrNull() ?: 1.0
        if (qty <= 0) return

        val selectedUnit = modalUnits.getOrNull(modalSelectedUnitIndex) ?: modalUnits.first()
        val unitPrice = selectedUnit.salePrice
        val total = qty * unitPrice

        val existingIndex = cartItems.indexOfFirst {
            it.productId == prod.id && it.unitName == selectedUnit.name
        }

        if (existingIndex > -1) {
            val updated = cartItems.toMutableList()
            val existing = updated[existingIndex]
            val newQty = existing.quantity + qty
            val newTotal = newQty * existing.unitPrice
            updated[existingIndex] = existing.copy(quantity = newQty, total = newTotal)
            cartItems = updated
        } else {
            val newItem = InvoiceItem(
                id = UUID.randomUUID().toString(),
                productId = prod.id,
                productCode = prod.code,
                productName = prod.name,
                unitId = selectedUnit.id,
                unitName = selectedUnit.name,
                unitFactor = selectedUnit.factor,
                quantity = qty,
                unitPrice = unitPrice,
                costPrice = prod.purchasePrice,
                total = total
            )
            cartItems = cartItems + newItem
        }

        modalProduct = null
    }

    // Update item quantity directly on screen
    fun handleUpdateItemQty(index: Int, newQty: Double) {
        if (newQty <= 0) {
            cartItems = cartItems.filterIndexed { i, _ -> i != index }
            return
        }
        val updated = cartItems.toMutableList()
        val item = updated[index]
        updated[index] = item.copy(quantity = newQty, total = newQty * item.unitPrice)
        cartItems = updated
    }

    // Save Invoice
    fun handleSaveInvoice() {
        validationError = ""

        if (cartItems.isEmpty()) {
            validationError = "يرجى إضافة أصناف إلى الفاتورة أولاً"
            return
        }

        if (paymentMethod != "CASH" && selectedCustomer == null) {
            validationError = "يرجى البحث عن العميل واختياره أولاً للفواتير الآجلة أو السداد الجزئي"
            return
        }

        val isCashOnly = paymentMethod == "CASH"
        val customerId = if (isCashOnly && selectedCustomer == null) "cash-cust" else selectedCustomer!!.id
        val customerCode = if (isCashOnly && selectedCustomer == null) "0000" else selectedCustomer!!.code
        val customerName = if (isCashOnly && selectedCustomer == null) "عميل نقدي" else selectedCustomer!!.name

        val billTypeCode = if (paymentMethod == "CASH") 1 else 4
        val invoiceNo = currentInvoiceNumber

        val newInvoice = InvoiceEntity(
            id = UUID.randomUUID().toString(),
            invoiceNumber = invoiceNo,
            billNo = (System.currentTimeMillis() % 100000).toString(),
            billType = billTypeCode,
            paymentMethod = paymentMethod,
            customerId = customerId,
            customerCode = customerCode,
            customerName = customerName,
            date = System.currentTimeMillis(),
            subtotal = subtotal,
            discount = discountVal,
            total = finalTotal,
            paidAmount = paidAmount,
            remainingAmount = if (isCashOnly && selectedCustomer == null) 0.0 else remainingAmount,
            previousCustomerBalance = if (isCashOnly && selectedCustomer == null) 0.0 else prevCustomerBalance,
            newCustomerBalance = if (isCashOnly && selectedCustomer == null) 0.0 else newCustomerBalance,
            itemsJson = Gson().toJson(cartItems),
            notes = notes.trim(),
            createdBy = currentUserName
        )

        onSaveInvoice(newInvoice, cartItems)
        completedInvoice = newInvoice
        completedInvoiceItems = cartItems

        // Reset form
        cartItems = emptyList()
        selectedCustomer = null
        customerSearchQuery = ""
        discountInput = "0"
        paidAmountInput = "0"
        notes = ""
        validationError = ""
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "فاتورة مبيعات (POS)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (cartItems.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${cartItems.size} صنف",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    actions = {
                        if (cartItems.isNotEmpty()) {
                            IconButton(onClick = { cartItems = emptyList() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "تفريغ السلة", tint = Color(0xFFFECDD3))
                            }
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
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // =========================================================================
                // 1. CARD 1: TOP HEADER & CUSTOMER SEARCH (Matching Web ERP)
                // =========================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top row with Badges & Treasury Box Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFCCFBF1))
                                        .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF0F766E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "فاتورة مبيعات",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Invoice number badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFECFDF5))
                                                .border(0.8.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = currentInvoiceNumber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF065F46)
                                            )
                                        }

                                        // Date badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = currentDateFormatted,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }

                            // Cash Drawer / Treasury Balance Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("الخزينة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    Text(
                                        text = numberFormat.format(displayBoxBalance),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(currencyName, fontSize = 9.sp, color = Color(0xFFB45309))
                                }
                            }
                        }

                        // Customer Selection or Cash Customer Banner
                        if (paymentMethod == "CASH" && selectedCustomer == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Column {
                                        Text("عميل نقدي (دفع فوري بالكامل)", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF064E3B))
                                        Text("لا يلزم اختيار عميل للفواتير النقدية", fontSize = 9.5.sp, color = Color(0xFF047857))
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        customerSearchQuery = ""
                                        showCustomerDropdown = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("تحديد عميل؟", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (selectedCustomer != null) {
                                    // Selected customer banner with clear button
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF0FDFA))
                                            .border(1.dp, Color(0xFF5EEAD4), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(
                                                    text = "[${selectedCustomer!!.code}] ${selectedCustomer!!.name}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF134E4A)
                                                )
                                                Text(
                                                    text = "الرصيد السابق: ${numberFormat.format(selectedCustomer!!.currentBalance)} $currencyName",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedCustomer!!.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedCustomer = null
                                                customerSearchQuery = ""
                                                showCustomerDropdown = true
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "تغيير العميل", tint = Color(0xFF0F766E), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    // Customer search box
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = customerSearchQuery,
                                            onValueChange = {
                                                customerSearchQuery = it
                                                showCustomerDropdown = true
                                            },
                                            placeholder = {
                                                Text("🔍 مربع بحث عن العميل بالاسم أو رقم الكود...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                                focusedBorderColor = Color(0xFF0F766E),
                                                unfocusedBorderColor = Color(0xFFCBD5E1)
                                            ),
                                            trailingIcon = {
                                                if (customerSearchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { customerSearchQuery = "" }) {
                                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        )
                                    }
                                }

                                // Autocomplete dropdown list for customers
                                if (showCustomerDropdown && selectedCustomer == null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 180.dp)
                                                .padding(4.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            if (filteredCustomers.isEmpty()) {
                                                Text(
                                                    "لا يوجد عميل مطابق للبحث",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            } else {
                                                filteredCustomers.take(8).forEach { cust ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                selectedCustomer = cust
                                                                showCustomerDropdown = false
                                                                customerSearchQuery = ""
                                                                validationError = ""
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "[${cust.code}] ${cust.name}",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            if (cust.phone.isNotEmpty()) {
                                                                Text("هاتف: ${cust.phone}", fontSize = 9.5.sp, color = Color(0xFF64748B))
                                                            }
                                                        }
                                                        Text(
                                                            text = "رصيد: ${numberFormat.format(cust.currentBalance)} $currencyName",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (cust.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                                        )
                                                    }
                                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 2. CARD 2: HORIZONTAL PRODUCT RIBBON (شريط أصناف واحد متحرك يميناً ويساراً)
                // =========================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "شريط الأصناف (اسحب يميناً أو يساراً واختر الصنف):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "${products.size} صنف متوفر بالمخزن",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Search Bar for products
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = { productSearchQuery = it },
                            placeholder = { Text("🔍 بحث سريع عن صنف بالاسم أو الباركود...", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            trailingIcon = {
                                if (productSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { productSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        )

                        // Horizontal Ribbon of Products
                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚠️ لا توجد أصناف مطابقة للبحث", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredProducts, key = { it.id }) { prod ->
                                    Card(
                                        modifier = Modifier
                                            .width(132.dp)
                                            .height(82.dp)
                                            .clickable { handleQuickAddProduct(prod) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = prod.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            HorizontalDivider(color = Color(0xFFE2E8F0))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${numberFormat.format(prod.salePrice)} $currencyName",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF0F766E)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFCCFBF1))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "${prod.stockQuantity.toInt()} ${prod.baseUnitName.ifEmpty { "حبة" }}",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F766E)
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

                // =========================================================================
                // 3. CARD 3: CURRENT INVOICE ITEMS (المحتويات مباشرة في نفس الشاشة)
                // =========================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "أصناف الفاتورة الحالية (${cartItems.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (cartItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { cartItems = emptyList() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("مسح الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }

                        // Validation Error Alert Banner
                        if (validationError.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Text(validationError, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                            }
                        }

                        // Items List or Empty Placeholder
                        if (cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(vertical = 24.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(36.dp))
                                    Text("لا توجد أصناف مضافة في الفاتورة بعد.", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text("اضغط على أي صنف من الشريط العلوي لإضافته مباشرة أمامك بالفاتورة.", fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cartItems.forEachIndexed { index, it ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product details
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(
                                                text = it.productName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${if (it.quantity % 1.0 == 0.0) it.quantity.toInt() else it.quantity} ${it.unitName} × ${numberFormat.format(it.unitPrice)}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        // Stepper & Line Total & Delete
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Stepper
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { handleUpdateItemQty(index, it.quantity - 1.0) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(12.dp))
                                                }

                                                Text(
                                                    text = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )

                                                IconButton(
                                                    onClick = { handleUpdateItemQty(index, it.quantity + 1.0) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                }
                                            }

                                            // Line Total
                                            Text(
                                                text = numberFormat.format(it.total),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF0F766E),
                                                modifier = Modifier.widthIn(min = 45.dp),
                                                textAlign = TextAlign.End
                                            )

                                            // Remove
                                            IconButton(
                                                onClick = { cartItems = cartItems.filterIndexed { i, _ -> i != index } },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // 4. TOTALS, PAYMENT METHOD SELECTION & SAVE BUTTON
                        // =========================================================================
                        if (cartItems.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Subtotal
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي الأصناف:", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    Text(
                                        "${numberFormat.format(subtotal)} $currencyName",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Discount Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الخصم الممنوح:", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = discountInput,
                                        onValueChange = { discountInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color(0xFFF8FAFC)
                                        ),
                                        modifier = Modifier.width(90.dp).height(38.dp)
                                    )
                                }

                                // Final Total
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDFA))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("اجمالي الفاتورة:", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                                    Text(
                                        "${numberFormat.format(finalTotal)} $currencyName",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0F766E)
                                    )
                                }

                                // Payment Method Switcher (3 options matching Web POS)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // آجل كامل (CREDIT)
                                        Surface(
                                            onClick = { handlePaymentMethodChange("CREDIT") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (paymentMethod == "CREDIT") Color(0xFFFEE2E2) else Color.Transparent,
                                            border = if (paymentMethod == "CREDIT") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)) else null
                                        ) {
                                            Text(
                                                "آجل كامل",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (paymentMethod == "CREDIT") Color(0xFF991B1B) else Color(0xFF475569),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        // سداد جزئي (PARTIAL)
                                        Surface(
                                            onClick = { handlePaymentMethodChange("PARTIAL") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (paymentMethod == "PARTIAL") Color(0xFFFEF3C7) else Color.Transparent,
                                            border = if (paymentMethod == "PARTIAL") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)) else null
                                        ) {
                                            Text(
                                                "سداد جزئي",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (paymentMethod == "PARTIAL") Color(0xFF92400E) else Color(0xFF475569),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        // نقداً كامل (CASH)
                                        Surface(
                                            onClick = { handlePaymentMethodChange("CASH") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (paymentMethod == "CASH") Color(0xFFD1FAE5) else Color.Transparent,
                                            border = if (paymentMethod == "CASH") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7)) else null
                                        ) {
                                            Text(
                                                "نقداً كامل",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (paymentMethod == "CASH") Color(0xFF065F46) else Color(0xFF475569),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }

                                    // Partial payment input box
                                    if (paymentMethod == "PARTIAL") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFFBEB))
                                                .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("المدفوع نقداً (إيصال):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                            OutlinedTextField(
                                                value = paidAmountInput,
                                                onValueChange = { paidAmountInput = it },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = Color.White,
                                                    unfocusedContainerColor = Color.White
                                                ),
                                                modifier = Modifier.width(100.dp).height(38.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("المسدد نقداً: ${numberFormat.format(paidAmount)}", fontSize = 9.5.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                                            Text("المتبقي آجل: ${numberFormat.format(remainingAmount)}", fontSize = 9.5.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Customer balance projection
                                if (selectedCustomer != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الرصيد السابق: ${numberFormat.format(prevCustomerBalance)}", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                        Text("الرصيد الإجمالي: ${numberFormat.format(newCustomerBalance)} $currencyName", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                                    }
                                }

                                // Notes input
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    placeholder = { Text("ملاحظات على الفاتورة (اختياري)...", fontSize = 10.5.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                )

                                // Big Save Button
                                Button(
                                    onClick = { handleSaveInvoice() },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حفظ الفاتورة (${numberFormat.format(finalTotal)} $currencyName) 🚀",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MODAL 1: PRODUCT UNIT & QUANTITY SELECTOR (مطابق للمودال بالويب)
        // =========================================================================
        modalProduct?.let { prod ->
            Dialog(onDismissRequest = { modalProduct = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prod.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { modalProduct = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color(0xFF94A3B8))
                            }
                        }

                        // Stock indicator badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0FDFA))
                                .border(0.8.dp, Color(0xFF99F6E4), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الرصيد المتوفر بالمخزن:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                Text(
                                    "${prod.stockQuantity.toInt()} ${prod.baseUnitName.ifEmpty { "حبة" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF115E59)
                                )
                            }
                        }

                        // Unit Selector Buttons
                        Text("اختر الوحدة وسعر البيع:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            modalUnits.forEachIndexed { idx, u ->
                                val isSelected = modalSelectedUnitIndex == idx
                                Surface(
                                    onClick = { modalSelectedUnitIndex = idx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0F766E) else Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = u.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${numberFormat.format(u.salePrice)} $currencyName",
                                            fontSize = 9.5.sp,
                                            color = if (isSelected) Color(0xFFCCFBF1) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity Stepper
                        Text("الكمية المطلوبة:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val current = modalQuantityInput.toDoubleOrNull() ?: 1.0
                                    if (current > 1.0) modalQuantityInput = (current - 1.0).toInt().toString()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }

                            OutlinedTextField(
                                value = modalQuantityInput,
                                onValueChange = { modalQuantityInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.width(80.dp).padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    val current = modalQuantityInput.toDoubleOrNull() ?: 1.0
                                    modalQuantityInput = (current + 1.0).toInt().toString()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }

                        // Total Preview Box
                        val selectedUnit = modalUnits.getOrNull(modalSelectedUnitIndex) ?: modalUnits.first()
                        val q = modalQuantityInput.toDoubleOrNull() ?: 1.0
                        val previewTotal = q * selectedUnit.salePrice

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0FDFA))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "الإجمالي: ${numberFormat.format(previewTotal)} $currencyName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E)
                            )
                        }

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { modalProduct = null },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontSize = 11.5.sp)
                            }

                            Button(
                                onClick = { handleConfirmItemAdd() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("تأكيد الإضافة للفاتورة", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MODAL 2: COMPLETED INVOICE ACTIONS (طباعة حرارية، واتساب، نسخ، مشاركة)
        // =========================================================================
        completedInvoice?.let { inv ->
            Dialog(onDismissRequest = { completedInvoice = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                        }

                        Text("تم حفظ الفاتورة بنجاح ✨", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Text("رقم الفاتورة: #${inv.billNo.ifEmpty { inv.invoiceNumber }}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))

                        // Summary Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("العميل:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(inv.customerName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("اجمالي الفاتورة:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${numberFormat.format(inv.total)} $currencyName", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                            }
                            if (inv.paidAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المبلغ المسدد (نقداً):", fontSize = 11.sp, color = Color(0xFF059669))
                                    Text("${numberFormat.format(inv.paidAmount)} $currencyName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }
                            if (inv.remainingAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الرصيد الإجمالي الحالي:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    Text("${numberFormat.format(inv.newCustomerBalance)} $currencyName", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                                }
                            }
                        }

                        // Action 1: Share PDF + Text to WhatsApp / Android share
                        Button(
                            onClick = {
                                PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة الفاتورة (ملف PDF + النص) للجوال", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        // Action 2 & 3 in 1 Row: Thermal Print & Text WhatsApp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val paired = printerManager.getPairedPrinters()
                                        if (paired.isNotEmpty()) {
                                            val targetDevice = paired.first()
                                            val result = printerManager.printInvoiceSilent(targetDevice.address, inv, settings)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "✅ تمت الطباعة الحرارية بنجاح", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "خطأ في الطباعة: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "لا توجد طابعة بلوتوث مقترنة بالهاتف", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة حرارية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val msg = formatPosInvoiceWhatsAppMessage(inv, completedInvoiceItems, settings)
                                    val customerPhone = customers.find { it.id == inv.customerId }?.phone ?: ""
                                    sendPosWhatsAppMessage(context, customerPhone, msg)
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نص واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Action 4: Copy to Clipboard
                        OutlinedButton(
                            onClick = {
                                val msg = formatPosInvoiceWhatsAppMessage(inv, completedInvoiceItems, settings)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Invoice", msg)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "✅ تم نسخ نص الفاتورة للحافظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF475569))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ نص الفاتورة للحافظة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }

                        // Action 5: Close and Start New
                        TextButton(
                            onClick = { completedInvoice = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق وبدء فاتورة جديدة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format official WhatsApp message for Invoice (matching Web)
 */
private fun formatPosInvoiceWhatsAppMessage(
    invoice: InvoiceEntity,
    items: List<InvoiceItem>,
    settings: SystemSettingsEntity
): String {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd, hh:mm a", Locale("ar"))
    val formattedDate = dateFormat.format(Date(invoice.date))
    val currencyName = settings.currencyName.ifEmpty { "ريال" }
    val numberFormat = DecimalFormat("#,##0")

    val itemsList = if (items.isNotEmpty()) {
        "\n📦 *الأصناف:*\n" + items.mapIndexed { index, it ->
            "${index + 1}. *${it.productName}*\n   الكمية: ${if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString()} ${it.unitName} × ${numberFormat.format(it.unitPrice)} = ${numberFormat.format(it.total)} $currencyName"
        }.joinToString("\n") + "\n"
    } else ""

    val totalWords = ArabicNumberConverter.convertToArabicWords(invoice.total, currencyName)

    return """
🧾 *فاتورة مبيعات - ${settings.businessName}*
🔢 *رقم الفاتورة:* [#${invoice.billNo.ifEmpty { invoice.invoiceNumber }}]
📅 *التاريخ:* $formattedDate
$itemsList
👤 عميلنا المحترم : ${invoice.customerName}
💵 عليكم مبلغ سابق : ${numberFormat.format(invoice.previousCustomerBalance)} $currencyName
🧾 قيمة الفاتورة : ${numberFormat.format(invoice.total)} $currencyName
${if (invoice.paidAmount > 0) "💰 المبلغ المسدد (نقداً): ${numberFormat.format(invoice.paidAmount)} $currencyName\n" else ""}⚖️ الرصيد الاجمالي : ${numberFormat.format(invoice.newCustomerBalance)} $currencyName
✍️ فقط : $totalWords
""".trimIndent()
}

/**
 * Sends text directly to WhatsApp contact or app
 */
private fun sendPosWhatsAppMessage(context: Context, phone: String, message: String) {
    try {
        var clean = phone.filter { it.isDigit() }
        if (clean.startsWith("07") && clean.length == 10) {
            clean = "967" + clean.substring(1)
        } else if ((clean.startsWith("70") || clean.startsWith("71") || clean.startsWith("73") || clean.startsWith("77") || clean.startsWith("78")) && clean.length == 9) {
            clean = "967" + clean
        } else if (clean.startsWith("0") && clean.length >= 9) {
            clean = "967" + clean.substring(1)
        }
        val url = if (clean.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(message)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
