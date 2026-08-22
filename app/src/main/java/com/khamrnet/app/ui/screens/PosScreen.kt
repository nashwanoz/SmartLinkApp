package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.khamrnet.app.data.model.*
import com.khamrnet.app.printer.BluetoothPrinterManager
import com.khamrnet.app.util.ArabicNumberConverter
import com.khamrnet.app.util.PdfThermalGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    currentUserName: String,
    onSaveInvoice: (InvoiceEntity, List<InvoiceItem>) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    // Cart Items
    var cartItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Customer Selection
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showCustomerSearchDialog by remember { mutableStateOf(false) }

    // Payment method: "CASH" (نقدي), "CREDIT" (آجل)
    var paymentMethod by remember { mutableStateOf("CREDIT") }
    var paidAmountInput by remember { mutableStateOf("0") }
    var discountInput by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    // Selected product for unit & qty modal
    var selectedProductForCart by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductUnits by remember { mutableStateOf<List<ProductUnit>>(emptyList()) }
    var selectedUnitIndex by remember { mutableIntStateOf(0) }
    var inputQuantity by remember { mutableStateOf("1") }

    // Completed Invoice Dialog
    var completedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var completedInvoiceItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Calculations
    val subtotal = remember(cartItems) { cartItems.sumOf { it.total } }
    val discountVal = discountInput.toDoubleOrNull() ?: 0.0
    val totalAmount = (subtotal - discountVal).coerceAtLeast(0.0)
    val paidAmountVal = if (paymentMethod == "CASH") totalAmount else (paidAmountInput.toDoubleOrNull() ?: 0.0)
    val remainingAmount = (totalAmount - paidAmountVal).coerceAtLeast(0.0)

    val previousCustomerBalance = selectedCustomer?.currentBalance ?: 0.0
    val newCustomerBalance = if (paymentMethod == "CREDIT") previousCustomerBalance + remainingAmount else previousCustomerBalance

    // Filter products for top quick bar
    var productSearchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, productSearchQuery) {
        val q = productSearchQuery.trim().lowercase()
        if (q.isEmpty()) products
        else products.filter { it.name.lowercase().contains(q) || it.code.contains(q) || it.barcode.contains(q) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("نقطة البيع (الكاشير)", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${cartItems.size} صنف بالسلة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Customer Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomerSearchDialog = true },
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = selectedCustomer?.name ?: "انقر لاختيار العميل (مطلوب للآجل)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedCustomer != null) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                            if (selectedCustomer != null) {
                                Text(
                                    text = "الرصيد السابق: ${selectedCustomer!!.currentBalance.toInt()} ${settings.currencyName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCustomer!!.currentBalance > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showCustomerSearchDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (selectedCustomer == null) "اختيار عميل" else "تغيير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Quick Products Horizontal Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text("🔍 بحث سريع عن صنف لإضافته...", fontSize = 11.5.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0F766E),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        Surface(
                            onClick = {
                                // Parse units for this product
                                val listType = object : TypeToken<List<ProductUnit>>() {}.type
                                val units: List<ProductUnit> = try {
                                    Gson().fromJson(prod.unitsJson, listType) ?: emptyList()
                                } catch (_: Exception) { emptyList() }

                                val finalUnits = units.ifEmpty {
                                    listOf(
                                        ProductUnit(
                                            id = "default",
                                            name = prod.baseUnitName,
                                            factor = 1.0,
                                            purchasePrice = prod.purchasePrice,
                                            salePrice = prod.salePrice,
                                            isDefault = true
                                        )
                                    )
                                }
                                selectedProductUnits = finalUnits
                                selectedUnitIndex = 0
                                inputQuantity = "1"
                                selectedProductForCart = prod
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.width(130.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = prod.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${prod.salePrice.toInt()} ${settings.currencyName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F766E)
                                    )
                                    Text(
                                        text = "${prod.stockQuantity.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Cart Items Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(42.dp))
                            Text("السلة فارغة، اختر أصناف لإضافتها للفاتورة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الصنف والوحدة", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569), modifier = Modifier.weight(2f))
                                Text("الكمية", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569), textAlign = TextAlign.Center, modifier = Modifier.weight(1.2f))
                                Text("السعر", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text("الإجمالي", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                        }

                        itemsIndexed(cartItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                    Text("${item.unitName} × ${item.unitPrice.toInt()}", fontSize = 10.sp, color = Color(0xFF64748B))
                                }

                                // Quantity Stepper
                                Row(
                                    modifier = Modifier.weight(1.2f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1) {
                                                val updated = cartItems.toMutableList()
                                                val newQty = item.quantity - 1
                                                updated[index] = item.copy(quantity = newQty, total = newQty * item.unitPrice)
                                                cartItems = updated
                                            } else {
                                                cartItems = cartItems.filterIndexed { i, _ -> i != index }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }

                                    Text("${item.quantity.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))

                                    IconButton(
                                        onClick = {
                                            val updated = cartItems.toMutableList()
                                            val newQty = item.quantity + 1
                                            updated[index] = item.copy(quantity = newQty, total = newQty * item.unitPrice)
                                            cartItems = updated
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Text("${item.unitPrice.toInt()}", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text("${item.total.toInt()}", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = { cartItems = cartItems.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF8FAFC))
                        }
                    }
                }
            }

            // 4. Payment & Totals Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Payment Method Toggle (نقدي / آجل)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { paymentMethod = "CREDIT" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (paymentMethod == "CREDIT") Color(0xFFDC2626) else Color.Transparent
                        ) {
                            Text(
                                "🔴 فاتورة آجلة (دين)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (paymentMethod == "CREDIT") Color.White else Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = { paymentMethod = "CASH" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (paymentMethod == "CASH") Color(0xFF059669) else Color.Transparent
                        ) {
                            Text(
                                "🟢 فاتورة نقدية (كاش)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (paymentMethod == "CASH") Color.White else Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Cash paid row if CREDIT
                    if (paymentMethod == "CREDIT") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = paidAmountInput,
                                onValueChange = { paidAmountInput = it },
                                label = { Text("المدفوع نقداً مقدم", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المتبقي آجل:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                Text("${remainingAmount.toInt()} ${settings.currencyName}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                        }
                    }

                    // Total & Save Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إجمالي الفاتورة:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text(
                                "${totalAmount.toInt()} ${settings.currencyName}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E)
                            )
                        }

                        Button(
                            onClick = {
                                if (cartItems.isEmpty()) {
                                    Toast.makeText(context, "يرجى إضافة أصناف أولاً للسلة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (paymentMethod == "CREDIT" && selectedCustomer == null) {
                                    Toast.makeText(context, "⚠️ يجب اختيار العميل للفاتورة الآجلة!", Toast.LENGTH_LONG).show()
                                    showCustomerSearchDialog = true
                                    return@Button
                                }

                                val customerObj = selectedCustomer ?: CustomerEntity(
                                    id = "cash-cust",
                                    code = "1001",
                                    name = "عميل نقدي عام",
                                    currentBalance = 0.0
                                )

                                val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
                                val newInvoice = InvoiceEntity(
                                    id = UUID.randomUUID().toString(),
                                    invoiceNumber = invoiceNumber,
                                    billNo = (System.currentTimeMillis() % 100000).toString(),
                                    billType = if (paymentMethod == "CREDIT") 4 else 1,
                                    paymentMethod = paymentMethod,
                                    customerId = customerObj.id,
                                    customerCode = customerObj.code,
                                    customerName = customerObj.name,
                                    date = System.currentTimeMillis(),
                                    subtotal = subtotal,
                                    discount = discountVal,
                                    total = totalAmount,
                                    paidAmount = paidAmountVal,
                                    remainingAmount = remainingAmount,
                                    previousCustomerBalance = previousCustomerBalance,
                                    newCustomerBalance = newCustomerBalance,
                                    itemsJson = Gson().toJson(cartItems),
                                    notes = notes,
                                    createdBy = currentUserName
                                )

                                onSaveInvoice(newInvoice, cartItems)
                                completedInvoice = newInvoice
                                completedInvoiceItems = cartItems
                                cartItems = emptyList()
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وطباعة الفاتورة", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    // Modal: Product Unit & Quantity Selector
    selectedProductForCart?.let { prod ->
        Dialog(onDismissRequest = { selectedProductForCart = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(prod.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))

                    Text("اختر الوحدة وسعر البيع:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                    // Unit Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedProductUnits.forEachIndexed { index, unit ->
                            Surface(
                                onClick = { selectedUnitIndex = index },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedUnitIndex == index) Color(0xFF0F766E) else Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        unit.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedUnitIndex == index) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        "${unit.salePrice.toInt()} ${settings.currencyName}",
                                        fontSize = 10.sp,
                                        color = if (selectedUnitIndex == index) Color(0xFFCCFBF1) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Input
                    OutlinedTextField(
                        value = inputQuantity,
                        onValueChange = { inputQuantity = it },
                        label = { Text("الكمية المطلوبة", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Add to Cart Button
                    Button(
                        onClick = {
                            val qty = inputQuantity.toDoubleOrNull() ?: 1.0
                            val selectedUnit = selectedProductUnits.getOrNull(selectedUnitIndex) ?: selectedProductUnits.first()
                            val newItem = InvoiceItem(
                                id = UUID.randomUUID().toString(),
                                productId = prod.id,
                                productCode = prod.code,
                                productName = prod.name,
                                unitId = selectedUnit.id,
                                unitName = selectedUnit.name,
                                unitFactor = selectedUnit.factor,
                                quantity = qty,
                                unitPrice = selectedUnit.salePrice,
                                costPrice = prod.purchasePrice,
                                total = qty * selectedUnit.salePrice
                            )
                            cartItems = cartItems + newItem
                            selectedProductForCart = null
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text("إضافة للسلة", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Modal: Customer Search Dialog
    if (showCustomerSearchDialog) {
        Dialog(onDismissRequest = { showCustomerSearchDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(420.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بحث واختيار عميل", fontSize = 15.sp, fontWeight = FontWeight.Black)

                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        placeholder = { Text("ابحث بالاسم أو رقم الهاتف...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val filteredCusts = remember(customers, customerSearchQuery) {
                        val q = customerSearchQuery.trim().lowercase()
                        if (q.isEmpty()) customers
                        else customers.filter { it.name.lowercase().contains(q) || it.phone.contains(q) || it.code.contains(q) }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredCusts) { cust ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = cust
                                        showCustomerSearchDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Text("كود: ${cust.code} • هاتف: ${cust.phone.ifEmpty { "لا يوجد" }}", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                Text("${cust.currentBalance.toInt()} ${settings.currencyName}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }

    // Modal: Invoice Success & Print / WhatsApp Actions
    completedInvoice?.let { inv ->
        Dialog(onDismissRequest = { completedInvoice = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                    }

                    Text("تم حفظ الفاتورة بنجاح!", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Text("رقم الفاتورة: #${inv.billNo.ifEmpty { inv.invoiceNumber }}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                    // Instant Direct Bluetooth Thermal Print
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة حرارية فورية (بلوتوث)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    // Share PDF to WhatsApp
                    Button(
                        onClick = {
                            PdfThermalGenerator.shareInvoiceToWhatsApp(context, inv, settings)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال PDF للواتساب", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { completedInvoice = null },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إغلاق والعودة لنقطة البيع", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}
