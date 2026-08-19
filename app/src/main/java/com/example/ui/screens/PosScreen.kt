package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.components.InvoiceReceiptDialog
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.CartItem
import com.example.utils.Formatters
import kotlin.math.abs

@Composable
fun PosScreen(
    currentUser: User?,
    products: List<Product>,
    customers: List<Customer>,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    discount: Double,
    paidAmount: Double,
    paymentMethod: PaymentMethod,
    settings: SystemSettings,
    onAddToCart: (Product, String) -> Unit,
    onUpdateCartQty: (Long, String, Double) -> Unit,
    onRemoveFromCart: (Long, String) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onSetDiscount: (Double) -> Unit,
    onSetPaidAmount: (Double) -> Unit,
    onSetPaymentMethod: (PaymentMethod) -> Unit,
    onCheckoutInvoice: (String) -> Invoice?
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var isCartExpanded by remember { mutableStateOf(true) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var completedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var invoiceNotes by remember { mutableStateOf("") }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }

    // Categories
    val categories = remember(products) {
        listOf("الكل") + products.map { it.category.ifBlank { "عام" } }.distinct()
    }

    // Filtered products
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "الكل" || p.category == selectedCategory
            matchSearch && matchCategory
        }
    }

    // Cart calculations
    val subtotal = cartItems.sumOf { it.total }
    val total = maxOf(0.0, subtotal - discount)
    val cartCount = cartItems.sumOf { it.quantity }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Search Bar & Barcode ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الصنف أو الباركود...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )

            // Customer Selector Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selectedCustomer != null) TealContainer else Slate100,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedCustomer != null) TealPrimary else Slate300
                ),
                modifier = Modifier
                    .clickable { showCustomerPicker = !showCustomerPicker }
                    .padding(vertical = 2.dp)
                    .testTag("pos_customer_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedCustomer != null) TealPrimary else Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedCustomer?.name ?: "عميل نقدي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCustomer != null) TealDark else Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Customer Quick Picker Popup / List if opened
        if (showCustomerPicker) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختيار العميل للفاتورة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Slate800
                        )
                        TextButton(onClick = {
                            onSelectCustomer(null)
                            showCustomerPicker = false
                        }) {
                            Text("عميل نقدي عام (افتراضي)", fontSize = 11.sp)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(customers) { c ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedCustomer?.id == c.id) TealPrimary else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                                modifier = Modifier.clickable {
                                    onSelectCustomer(c)
                                    showCustomerPicker = false
                                }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = c.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCustomer?.id == c.id) Color.White else Slate900
                                    )
                                    val debtStatus = if (c.balance >= 0) "عليه" else "له"
                                    Text(
                                        text = "الرصيد: ${Formatters.formatCurrency(abs(c.balance))} [$debtStatus]",
                                        fontSize = 9.sp,
                                        color = if (selectedCustomer?.id == c.id) TealContainer else Slate500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) TealPrimary else Slate100,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) TealPrimary else Slate200
                    ),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Slate700,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- Main Screen Split: Products List vs Cart ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Section 1: Cart Items Preview (if cart not empty)
            if (cartItems.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TealContainer.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealLight.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سلة الفاتورة (${cartItems.size} أصناف)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TealDark
                                    )
                                }
                                TextButton(onClick = onClearCart) {
                                    Text("تفريغ السلة", color = RoseError, fontSize = 11.sp)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = TealLight.copy(alpha = 0.3f))

                            cartItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "${Formatters.formatCurrency(item.unitPrice)} $currencyName / ${item.unitName}",
                                            fontSize = 10.sp,
                                            color = Slate600
                                        )
                                    }

                                    // Stepper quantity
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                onUpdateCartQty(item.product.id, item.unitType, item.quantity - 1.0)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Slate200)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "نقص", modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = Formatters.formatCurrency(item.quantity),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                onUpdateCartQty(item.product.id, item.unitType, item.quantity + 1.0)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(TealPrimary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        IconButton(
                                            onClick = { onRemoveFromCart(item.product.id, item.unitType) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Products Catalog List
            item {
                Text(
                    text = "الأصناف المتاحة (${filteredProducts.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(filteredProducts) { product ->
                val cashierStock = (currentUser?.let { product.stockCashier[it.id] }) ?: 0.0
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "الباركود: ${product.barcode}",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                                Text(
                                    text = "المخزون: ${Formatters.formatCurrency(cashierStock)} ${product.unitName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cashierStock > 0) EmeraldSuccess else RoseError
                                )
                            }
                        }

                        // Add buttons for Minor and Major units
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Minor unit (e.g. حبة)
                            Button(
                                onClick = { onAddToCart(product, "minor") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "+ ${product.unitName} (${Formatters.formatCurrency(product.price)})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Major unit (e.g. كرتون) if configured
                            if (product.caseQuantity > 1.0) {
                                OutlinedButton(
                                    onClick = { onAddToCart(product, "major") },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "+ ${product.caseUnitName} (${Formatters.formatCurrency(product.casePrice)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد أصناف تطابق البحث", color = Slate400, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Bottom POS Checkout Panel ---
        if (cartItems.isNotEmpty()) {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Payment Method selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("نقداً (Cash)", PaymentMethod.CASH, EmeraldSuccess),
                            Triple("آجل (Credit)", PaymentMethod.CREDIT, AmberWarning),
                            Triple("جزئي (Partial)", PaymentMethod.PARTIAL, BlueInfo)
                        ).forEach { (title, method, col) ->
                            val isSelected = paymentMethod == method
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) col.copy(alpha = 0.15f) else Slate100,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) col else Slate300
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetPaymentMethod(method) }
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) col else Slate700,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Discount & Total Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الإجمالي: ${Formatters.formatCurrency(subtotal)} $currencyName",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                            Text(
                                text = "الصافي: ${Formatters.formatCurrency(total)} $currencyName",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TealPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val inv = onCheckoutInvoice(invoiceNotes)
                                if (inv != null) {
                                    completedInvoice = inv
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("checkout_invoice_button")
                        ) {
                            Text("حفظ وطباعة الفاتورة ❯", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Invoice Thermal Receipt Preview Dialog
    completedInvoice?.let { inv ->
        InvoiceReceiptDialog(
            invoice = inv,
            settings = settings,
            onDismiss = { completedInvoice = null }
        )
    }
}
