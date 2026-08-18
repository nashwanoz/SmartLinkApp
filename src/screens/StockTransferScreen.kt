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
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.StockTransfer
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferScreen(
    products: List<Product>,
    users: List<User>,
    currentUser: User,
    settings: SystemSettings,
    transfers: List<StockTransfer>,
    onExecuteTransfer: (productId: Long, toCashierId: String, quantity: Double, note: String?) -> Unit
) {
    val cashiers = users.filter { it.role == "CASHIER" }
    
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var productSearch by remember { mutableStateOf("") }
    var showProductDropdown by remember { mutableStateOf(false) }
    
    var selectedCashierId by remember { mutableStateOf(cashiers.firstOrNull()?.id ?: "") }
    var transferQty by remember { mutableStateOf("10") }
    var transferUnitType by remember { mutableStateOf("minor") }
    var transferNote by remember { mutableStateOf("تغذية عهدة كاشير") }
    var errorMsg by remember { mutableStateOf("") }
    
    val selectedProduct by remember {
        derivedStateOf { products.find { it.id == selectedProductId } ?: products.firstOrNull() }
    }
    
    val selectedCashier by remember {
        derivedStateOf { users.find { it.id == selectedCashierId } }
    }
    
    val filteredProducts by remember {
        derivedStateOf {
            val s = productSearch.lowercase().trim()
            if (s.isEmpty()) products
            else products.filter { p ->
                p.name.lowercase().contains(s) || p.barcode.contains(s)
            }
        }
    }
    
    val numQty = transferQty.toDoubleOrNull() ?: 0.0
    val effectiveMinorQty by remember {
        derivedStateOf {
            if (transferUnitType == "major" && selectedProduct != null) {
                numQty * (selectedProduct.caseQuantity ?: 1.0)
            } else {
                numQty
            }
        }
    }
    
    fun handleTransfer() {
        errorMsg = ""
        
        if (selectedProduct == null) {
            errorMsg = "يرجى اختيار الصنف المراد تحويله"
            return
        }
        
        if (selectedCashier == null) {
            errorMsg = "يرجى اختيار الكاشير المستلم"
            return
        }
        
        if (effectiveMinorQty <= 0) {
            errorMsg = "يرجى إدخال كمية تحويل صحيحة أكبر من الصفر"
            return
        }
        
        if (effectiveMinorQty > (selectedProduct?.stockMain ?: 0.0)) {
            errorMsg = "الكمية المطلوبة (${effectiveMinorQty.toInt()} ${selectedProduct?.unitName}) تفوق المتوفر بالمخزن الرئيسي (${selectedProduct?.stockMain?.toInt()} ${selectedProduct?.unitName})"
            return
        }
        
        onExecuteTransfer(
            selectedProduct!!.id,
            selectedCashier!!.id,
            effectiveMinorQty,
            transferNote
        )
        
        transferQty = "10"
        errorMsg = ""
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
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "التحويل المخزني (صلاحية المدير العام)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "تحويل الأصناف من المخزن الرئيسي إلى عهدة الكواشير",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "المدير: ${currentUser.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // Transfer Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "نموذج تحويل بضاعة لكاشير",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    // Error Message
                    if (errorMsg.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = errorMsg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                    
                    // Step 1: Select Product & Target Cashier
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Searchable Product Selector
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "الصنف المراد تحويله (بحث) *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                
                                Box {
                                    OutlinedTextField(
                                        value = productSearch,
                                        onValueChange = {
                                            productSearch = it
                                            showProductDropdown = true
                                        },
                                        placeholder = {
                                            Text(
                                                text = if (selectedProduct != null) {
                                                    "🔍 ${selectedProduct.name} (متوفر: ${selectedProduct.stockMain?.toInt()} ${selectedProduct.unitName})"
                                                } else {
                                                    "🔍 ابحث عن الصنف بالاسم أو الباركود..."
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
                                            if (productSearch.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { productSearch = "" }
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
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFEA580C),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                    
                                    // Product Search Results Dropdown
                                    if (showProductDropdown && filteredProducts.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
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
                                                items(filteredProducts, key = { it.id }) { p ->
                                                    Surface(
                                                        onClick = {
                                                            selectedProductId = p.id
                                                            productSearch = ""
                                                            showProductDropdown = false
                                                        },
                                                        color = if (selectedProductId == p.id) {
                                                            Color(0xFFEA580C)
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
                                                            Text(
                                                                text = p.name,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = if (selectedProductId == p.id) Color.White else Color(0xFF1E293B)
                                                            )
                                                            Text(
                                                                text = "بالمخزن: ${p.stockMain?.toInt()} ${p.unitName}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (selectedProductId ==
