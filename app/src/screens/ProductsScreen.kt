package com.smartlink.erp.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User

@Composable
fun ProductsScreen(
    products: List<Product>,
    currentUser: User,
    settings: SystemSettings,
    onSaveProduct: (Product, Boolean) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    onSelectForPos: ((Product) -> Unit)? = null
) {
    var searchTerm by remember { mutableStateOf("") }
    var isModalOpen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    
    // Compact modal form states
    var formName by remember { mutableStateOf("") }
    var formBarcode by remember { mutableStateOf("") }
    var formUnitName by remember { mutableStateOf("حبة") }
    var formPrice by remember { mutableStateOf("") }
    var formCaseUnitName by remember { mutableStateOf("كرتون") }  // ✅ الافتراضي: كرتونة
    var formCaseQuantity by remember { mutableStateOf("24") }
    var formCasePrice by remember { mutableStateOf("") }
    var formStockMain by remember { mutableStateOf("100") }
    var errorMsg by remember { mutableStateOf("") }
    
    fun openAddModal() {
        editingProduct = null
        formName = ""
        formBarcode = "6281000${(products.size + 1).toString().padStart(3, '0')}"
        formUnitName = "حبة"
        formPrice = ""
        formCaseUnitName = "كرتون"  // ✅ الافتراضي: كرتونة
        formCaseQuantity = "24"
        formCasePrice = ""
        formStockMain = "100"
        errorMsg = ""
        isModalOpen = true
    }
    
    fun openEditModal(product: Product) {
        editingProduct = product
        formName = product.name
        formBarcode = product.barcode ?: ""
        formUnitName = product.unitName
        formPrice = product.price.toString()
        formCaseUnitName = product.caseUnitName
        formCaseQuantity = product.caseQuantity.toString()
        formCasePrice = product.casePrice.toString()
        formStockMain = product.stockMain.toString()
        errorMsg = ""
        isModalOpen = true
    }
    
    fun handleSubmit() {
        errorMsg = ""
        
        val trimmed = formName.trim()
        if (trimmed.isEmpty()) {
            errorMsg = "يرجى كتابة اسم الصنف"
            return
        }
        
        val priceNum = formPrice.toDoubleOrNull()
        if (priceNum == null || priceNum <= 0) {
            errorMsg = "سعر الصغرى غير صحيح"
            return
        }
        
        val caseQtyNum = formCaseQuantity.toIntOrNull() ?: 1
        val casePriceNum = formCasePrice.toDoubleOrNull() ?: (priceNum * caseQtyNum)
        val stockNum = formStockMain.toIntOrNull() ?: 0
        
        val product = Product(
            id = editingProduct?.id ?: System.currentTimeMillis().toInt(),
            name = trimmed,
            barcode = formBarcode.trim().ifEmpty { "6281000${System.currentTimeMillis().toString().takeLast(4)}" },
            unitName = formUnitName.trim().ifEmpty { "حبة" },
            price = priceNum,
            caseUnitName = formCaseUnitName.trim().ifEmpty { "كرتون" },
            caseQuantity = caseQtyNum,
            casePrice = casePriceNum,
            stockMain = stockNum,
            stockCashier = editingProduct?.stockCashier ?: emptyMap()
        )
        
        onSaveProduct(product, editingProduct != null)
        isModalOpen = false
    }
    
    // Filter products
    val filtered by remember {
        derivedStateOf {
            val s = searchTerm.lowercase().trim()
            if (s.isEmpty()) products
            else products.filter { p ->
                p.name.lowercase().contains(s) || (p.barcode?.contains(s) == true)
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen Header
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
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "دليل وإدارة الأصناف والمخزون",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "إجمالي ${products.size} أصناف مسجلة بالوحدتين",
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
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+ صنف جديد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        
        // Quick Search
        item {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = {
                    Text(
                        text = "🔍 ابحث بالاسم أو الباركود...",
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
                    focusedBorderColor = Color(0xFF0D9488),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Products Grid
        items(filtered, key = { it.id }) { product ->
            ProductCard(
                product = product,
                settings = settings,
                onEditClick = { openEditModal(product) },
                onSelectForPos = { onSelectForPos?.invoke(product) },
                onDeleteClick = { onDeleteProduct(product.id) }
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
                            text = "لا توجد أصناف مطابقة للبحث",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
    
    // COMPACT POPUP MODAL FOR ADD / EDIT PRODUCT
    if (isModalOpen) {
        ProductModal(
            editingProduct = editingProduct,
            formName = formName,
            formBarcode = formBarcode,
            formUnitName = formUnitName,
            formPrice = formPrice,
            formCaseUnitName = formCaseUnitName,
            formCaseQuantity = formCaseQuantity,
            formCasePrice = formCasePrice,
            formStockMain = formStockMain,
            errorMsg = errorMsg,
            settings = settings,
            onNameChange = { formName = it },
            onBarcodeChange = { formBarcode = it },
            onUnitNameChange = { formUnitName = it },
            onPriceChange = { formPrice = it },
            onCaseUnitNameChange = { formCaseUnitName = it },
            onCaseQuantityChange = { formCaseQuantity = it },
            onCasePriceChange = { formCasePrice = it },
            onStockMainChange = { formStockMain = it },
            onClose = { isModalOpen = false },
            onSubmit = { handleSubmit() }
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    settings: SystemSettings,
    onEditClick: () -> Unit,
    onSelectForPos: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cashierUnits = product.stockCashier?.values?.sumOf { it ?: 0.0 } ?: 0.0
    val totalStock = product.stockMain + cashierUnits.toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المخزون: ${totalStock} ${product.unitName}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "|",
                        fontSize = 9.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "${String.format("%.2f", product.price)} ${settings.currencySymbol}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D9488)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "تعديل",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (onSelectForPos != null) {
                    IconButton(
                        onClick = onSelectForPos,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
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

@Composable
private fun ProductModal(
    editingProduct: Product?,
    formName: String,
    formBarcode: String,
    formUnitName: String,
    formPrice: String,
    formCaseUnitName: String,
    formCaseQuantity: String,
    formCasePrice: String,
    formStockMain: String,
    errorMsg: String,
    settings: SystemSettings,
    onNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onUnitNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCaseUnitNameChange: (String) -> Unit,
    onCaseQuantityChange: (String) -> Unit,
    onCasePriceChange: (String) -> Unit,
    onStockMainChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 550.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (editingProduct != null) "تعديل الصنف والوحدات" else "إضافة صنف جديد",
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = errorMsg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
                
                // Form
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Name & Barcode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = formName,
                            onValueChange = onNameChange,
                            label = { Text("اسم الصنف *", fontSize = 10.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        OutlinedTextField(
                            value = formStockMain,
                            onValueChange = onStockMainChange,
                            label = { Text("الرصيد المبدئي", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    // Row 2: Minor Unit Details (الوحدة الصغرى)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0FDFA)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الوحدة الصغرى (التجزئة):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = formUnitName,
                                    onValueChange = onUnitNameChange,
                                    label = { Text("اسم الصغرى (حبة/علبة)", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF14B8A6),
                                        unfocusedBorderColor = Color(0xFF5EEAD4)
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = formPrice,
                                    onValueChange = onPriceChange,
                                    label = { Text("سعر البيع (${settings.currencySymbol})", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF14B8A6),
                                        unfocusedBorderColor = Color(0xFF5EEAD4)
                                    )
                                )
                            }
                        }
                    }
                    
                    // Row 3: Major Unit Details (الوحدة الكبرى) - ✅ الافتراضي: كرتونة
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F3FF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الوحدة الكبرى (كرتون/شدة):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6D28D9)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = formCaseUnitName,
                                    onValueChange = onCaseUnitNameChange,
                                    label = { Text("اسم الكبرى", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFFC4B5FD)
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = formCaseQuantity,
                                    onValueChange = onCaseQuantityChange,
                                    label = { Text("العدد/السعة", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFFC4B5FD)
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = formCasePrice,
                                    onValueChange = onCasePriceChange,
                                    label = { Text("سعر الكبرى", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFFC4B5FD)
                                    )
