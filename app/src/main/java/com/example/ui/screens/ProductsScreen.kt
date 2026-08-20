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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.utils.Formatters

@Composable
fun ProductsScreen(
    products: List<Product>,
    currentUser: User?,
    settings: SystemSettings,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onSelectForPos: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }

    val filtered = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Search & Add
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
                    placeholder = { Text("بحث في الأصناف والباركود...", fontSize = 12.sp) },
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
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        editingProduct = null
                        isAddDialogOpen = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("add_product_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صنف جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary bar
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي الأصناف: ${products.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "المخزن الرئيسي: ${Formatters.formatCurrency(products.sumOf { it.stockMain })} قطعة",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            // Products list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { product ->
                    val cashierStock = currentUser?.let { product.stockCashier[it.id] } ?: 0.0
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "باركود: ${product.barcode}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                        Text(
                                            text = "التصنيف: ${product.category}",
                                            fontSize = 10.sp,
                                            color = TealDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onSelectForPos(product) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TealContainer)
                                    ) {
                                        Icon(Icons.Default.PointOfSale, contentDescription = "بيع", tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            editingProduct = product
                                            isAddDialogOpen = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Slate100)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Slate700, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { productToDelete = product },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RoseContainer)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dual unit prices & stocks
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate50, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "الوحدة الصغرى (${product.unitName})",
                                        fontSize = 9.sp,
                                        color = Slate500
                                    )
                                    Text(
                                        text = "${Formatters.formatCurrency(product.price)} $currencyName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TealPrimary
                                    )
                                }

                                if (product.caseQuantity > 1.0) {
                                    Column {
                                        Text(
                                            text = "الوحدة الكبرى (${product.caseUnitName} × ${product.caseQuantity.toInt()})",
                                            fontSize = 9.sp,
                                            color = Slate500
                                        )
                                        Text(
                                            text = "${Formatters.formatCurrency(product.casePrice)} $currencyName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TealDark
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "المخزن العام: ${Formatters.formatCurrency(product.stockMain)}",
                                        fontSize = 10.sp,
                                        color = Slate600
                                    )
                                    Text(
                                        text = "عهدة الكاشير: ${Formatters.formatCurrency(cashierStock)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cashierStock > 0) EmeraldSuccess else RoseError
                                    )
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد أصناف مسجلة", color = Slate400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Add/Edit Product Dialog
        if (isAddDialogOpen) {
            ProductEditDialog(
                product = editingProduct,
                settings = settings,
                onDismiss = { isAddDialogOpen = false },
                onSave = { saved ->
                    onSaveProduct(saved)
                    isAddDialogOpen = false
                }
            )
        }

        // Delete Confirm Dialog
        productToDelete?.let { prod ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("تأكيد حذف الصنف", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف الصنف (${prod.name})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteProduct(prod.id)
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun ProductEditDialog(
    product: Product?,
    settings: SystemSettings,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "عام") }
    var unitName by remember { mutableStateOf(product?.unitName ?: "حبة") }
    var priceStr by remember { mutableStateOf(product?.price?.let { Formatters.formatCurrency(it) } ?: "100") }
    var caseUnitName by remember { mutableStateOf(product?.caseUnitName ?: "كرتون") }
    var caseQuantityStr by remember { mutableStateOf(product?.caseQuantity?.toInt()?.toString() ?: "24") }
    var casePriceStr by remember { mutableStateOf(product?.casePrice?.let { Formatters.formatCurrency(it) } ?: "2200") }
    var stockMainStr by remember { mutableStateOf(product?.stockMain?.let { Formatters.formatCurrency(it) } ?: "100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل الصنف" else "إضافة صنف جديد",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Minor unit row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("الوحدة الصغرى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر البيع (${settings.currencySymbol})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Major unit row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = caseUnitName,
                        onValueChange = { caseUnitName = it },
                        label = { Text("الوحدة الكبرى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = caseQuantityStr,
                        onValueChange = { caseQuantityStr = it },
                        label = { Text("سعة الكرتون") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = casePriceStr,
                        onValueChange = { casePriceStr = it },
                        label = { Text("سعر الكرتون") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = stockMainStr,
                    onValueChange = { stockMainStr = it },
                    label = { Text("الرصيد في المخزن الرئيسي (بالحبة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val price = priceStr.replace(",", "").toDoubleOrNull() ?: 0.0
                    val caseQuantity = caseQuantityStr.toDoubleOrNull() ?: 1.0
                    val casePrice = casePriceStr.replace(",", "").toDoubleOrNull() ?: (price * caseQuantity)
                    val stockMain = stockMainStr.replace(",", "").toDoubleOrNull() ?: 0.0

                    val targetId = product?.id ?: System.currentTimeMillis()
                    val targetBarcode = if (barcode.isBlank()) targetId.toString().takeLast(6) else barcode

                    val result = Product(
                        id = targetId,
                        name = name,
                        barcode = targetBarcode,
                        unitName = unitName.ifBlank { "حبة" },
                        price = price,
                        caseUnitName = caseUnitName.ifBlank { "كرتون" },
                        caseQuantity = caseQuantity,
                        casePrice = casePrice,
                        stockMain = stockMain,
                        stockCashier = product?.stockCashier ?: emptyMap(),
                        category = category.ifBlank { "عام" }
                    )
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("حفظ الصنف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
