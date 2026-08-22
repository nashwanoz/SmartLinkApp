package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.ProductEntity
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class TransferItem(
    val product: ProductEntity,
    var quantity: Double,
    var isMajorUnit: Boolean = false // false = كرت, true = صفحة
)

data class CashierOption(
    val id: String,
    val name: String,
    val userCode: String
)

data class TransferHistoryItem(
    val id: String,
    val transferNumber: String,
    val productName: String,
    val quantity: Double,
    val unitName: String,
    val toCashierName: String,
    val timestamp: Long,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferScreen(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    currentUserName: String,
    onSaveTransfer: (transfers: List<TransferHistoryItem>) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val df = remember { DecimalFormat("#,##0") }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    // Cashiers list
    val cashiers = remember {
        listOf(
            CashierOption("cashier_1", "كاشير 1 - نقطة السوق", "102"),
            CashierOption("cashier_2", "كاشير 2 - نقطة المحطة", "103"),
            CashierOption("cashier_3", "كاشير 3 - نقطة الجولة", "104")
        )
    }
    var selectedCashier by remember { mutableStateOf(cashiers.first()) }
    var isCashierMenuExpanded by remember { mutableStateOf(false) }

    // View mode: "grid", "ribbon", "list"
    var productViewMode by remember { mutableStateOf("grid") }
    var searchQuery by remember { mutableStateOf("") }

    // Transfer cart items
    val transferItems = remember { mutableStateListOf<TransferItem>() }
    var transferNote by remember { mutableStateOf("") }

    // Transfer history (local state for immediate UI feedback)
    val transferHistory = remember {
        mutableStateListOf(
            TransferHistoryItem(
                id = "tr_1",
                transferNumber = "#TR-1082",
                productName = "كروت ابو 100",
                quantity = 50.0,
                unitName = "كرت",
                toCashierName = "كاشير 1 - نقطة السوق (102)",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                note = "تغذية بداية الوردية"
            ),
            TransferHistoryItem(
                id = "tr_2",
                transferNumber = "#TR-1081",
                productName = "كروت ابو 200",
                quantity = 2.0,
                unitName = "صفحة",
                toCashierName = "كاشير 1 - نقطة السوق (102)",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                note = "دفعة إضافية"
            )
        )
    }

    // Filter products
    val displayedProducts = remember(products, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) products
        else products.filter {
            it.name.lowercase().contains(q) ||
            it.code.lowercase().contains(q) ||
            it.barcode.lowercase().contains(q)
        }
    }

    // Helper to add or increment item in transfer cart
    fun addItem(product: ProductEntity, isMajor: Boolean) {
        val existingIndex = transferItems.indexOfFirst { it.product.id == product.id && it.isMajorUnit == isMajor }
        if (existingIndex >= 0) {
            val item = transferItems[existingIndex]
            transferItems[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            transferItems.add(TransferItem(product = product, quantity = 1.0, isMajorUnit = isMajor))
        }
        Toast.makeText(context, "+1 ${if (isMajor) "صفحه" else (product.baseUnitName.ifEmpty { "كرت" })}: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "التحويل المخزني وتغذية الكاشير",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ================= 1. TOP HEADER & CASHIER SELECTOR =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Bar with Manager Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right: Icon + Title + Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDFA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    "تغذية الكاشير والتحويل المخزني",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "تحويل الأصناف من المخزن الرئيسي إلى عهدة الكاشير",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Left: Manager Badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7))
                                .border(1.dp, Color(0xFFA7F3D0), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "المدير: $currentUserName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }

                    // Cashier Selector Box (Matching UI Box)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Dropdown Trigger
                            Box {
                                OutlinedButton(
                                    onClick = { isCashierMenuExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "${selectedCashier.name} (كود ${selectedCashier.userCode})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = isCashierMenuExpanded,
                                    onDismissRequest = { isCashierMenuExpanded = false }
                                ) {
                                    cashiers.forEach { c ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "${c.name} (كود ${c.userCode})",
                                                    fontWeight = if (c.id == selectedCashier.id) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (c.id == selectedCashier.id) Color(0xFF0F766E) else Color(0xFF1E293B)
                                                )
                                            },
                                            onClick = {
                                                selectedCashier = c
                                                isCashierMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Right: Info and Icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "الكاشير / نقطة البيع",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "المستلمة للعهدة:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        "[${selectedCashier.userCode}] ${selectedCashier.name}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFA7F3D0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. MAIN WAREHOUSE PRODUCTS SECTION =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Title and View Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: View Mode Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(
                                Triple("list", "قائمة", Icons.Default.List),
                                Triple("ribbon", "شريط", Icons.Default.ChevronRight),
                                Triple("grid", "شبكة", Icons.Default.GridView)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = productViewMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { productViewMode = mode }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "أصناف المخزن الرئيسي للتحويل:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "ابحث بالاسم أو الباركود لعرض وتصفية الأصناف فوراً...",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF64748B))
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFF94A3B8))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F766E),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Total Count Label
                    Text(
                        "إجمالي الأصناف: ${products.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Product Cards Layout (Grid - 2 Columns)
                    if (productViewMode == "grid") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayedProducts.chunked(2).forEach { rowProducts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowProducts.forEach { product ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ProductTransferCard(
                                                product = product,
                                                onAddMinor = { addItem(product, false) },
                                                onAddMajor = { addItem(product, true) }
                                            )
                                        }
                                    }
                                    if (rowProducts.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else if (productViewMode == "ribbon") {
                        // Ribbon / Carousel Horizontal Scroll
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(displayedProducts) { product ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    ProductTransferCard(
                                        product = product,
                                        onAddMinor = { addItem(product, false) },
                                        onAddMajor = { addItem(product, true) }
                                    )
                                }
                            }
                        }
                    } else {
                        // List View
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedProducts.forEach { product ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Action buttons
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { addItem(product, false) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("+1 ${product.baseUnitName.ifEmpty { "كرت" }}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { addItem(product, true) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("+1 صفحه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Right: Name and Stock
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Text(
                                                "المتوفر: ${df.format(product.stockQuantity)} ${product.baseUnitName.ifEmpty { "كرت" }}",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF059669),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 3. TRANSFER CART TABLE =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (transferItems.isNotEmpty()) {
                            TextButton(onClick = { transferItems.clear() }) {
                                Text("إفراغ الجدول", fontSize = 11.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "جدول أصناف سند التحويل (${transferItems.size})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (transferItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(40.dp)
                            )
                            Text("لم تتم إضافة أي أصناف بعد للتحويل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(
                                "انقر على أزرار الأصناف أعلاه (+1 كرت أو +1 صفحه) لإضافتها لجدول التحويل",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        // Transfer Cart Table Rows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transferItems.forEachIndexed { index, item ->
                                val unitLabel = if (item.isMajorUnit) "صفحه" else (item.product.baseUnitName.ifEmpty { "كرت" })
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Delete Button
                                        IconButton(
                                            onClick = { transferItems.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                        }

                                        // Qty Controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE2E8F0))
                                                    .clickable {
                                                        if (item.quantity > 1) {
                                                            transferItems[index] = item.copy(quantity = item.quantity - 1)
                                                        } else {
                                                            transferItems.removeAt(index)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("-", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1E293B))
                                            }

                                            Text(
                                                "${df.format(item.quantity)} $unitLabel",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF0F766E))
                                                    .clickable {
                                                        transferItems[index] = item.copy(quantity = item.quantity + 1)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                            }
                                        }

                                        // Item Name and Index
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(item.product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text(
                                                    "المتبقي بالمخزن: ${df.format(item.product.stockQuantity)}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE2E8F0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            }
                                        }
                                    }
                                }
                            }

                            // Note Field
                            OutlinedTextField(
                                value = transferNote,
                                onValueChange = { transferNote = it },
                                placeholder = { Text("ملاحظات على سند التحويل...", fontSize = 11.5.sp, color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0F766E),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Submit Save Button
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val newTransfers = transferItems.map { itm ->
                                        TransferHistoryItem(
                                            id = UUID.randomUUID().toString(),
                                            transferNumber = "#TR-${(1000..9999).random()}",
                                            productName = itm.product.name,
                                            quantity = itm.quantity,
                                            unitName = if (itm.isMajorUnit) "صفحه" else (itm.product.baseUnitName.ifEmpty { "كرت" }),
                                            toCashierName = "${selectedCashier.name} (${selectedCashier.userCode})",
                                            timestamp = now,
                                            note = transferNote.ifEmpty { "تغذية عهدة كاشير" }
                                        )
                                    }
                                    transferHistory.addAll(0, newTransfers)
                                    onSaveTransfer(newTransfers)
                                    transferItems.clear()
                                    transferNote = ""
                                    Toast.makeText(context, "✅ تم حفظ واعتماد سند التحويل المخزني بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "اعتماد وحفظ سند التحويل المخزني (${transferItems.size} أصناف)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ================= 4. TRANSFER HISTORY LOG =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عرض الحركات المسجلة", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "سجل عمليات التحويل المخزني",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }

                    if (transferHistory.isEmpty()) {
                        Text(
                            "لا توجد حركات تحويل مسجلة بعد",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transferHistory.forEach { history ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (history.note.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(history.note, fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    "(${df.format(history.quantity)} ${history.unitName})",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF059669)
                                                )
                                                Text(history.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFDCFCE7))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(history.transferNumber, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF065F46))
                                                }
                                            }
                                            Text(
                                                "إلى: ${history.toCashierName} • ${dateFormat.format(Date(history.timestamp))}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
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

/**
 * Single Product Card Component (Matching exact visual in screenshot)
 */
@Composable
fun ProductTransferCard(
    product: ProductEntity,
    onAddMinor: () -> Unit,
    onAddMajor: () -> Unit
) {
    val df = remember { DecimalFormat("#,##0") }
    val minorUnit = product.baseUnitName.ifEmpty { "كرت" }
    val majorUnit = "صفحه"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Title (Right aligned)
            Text(
                product.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Available Stock Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Badge (Left)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFDCFCE7))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${df.format(product.stockQuantity)} $minorUnit",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF065F46)
                    )
                }

                // Label (Right)
                Text(
                    "المتوفر بالمخزن:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }

            // Action Buttons Row (Matching the screenshot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left Button: +1 صفحة
                Button(
                    onClick = onAddMajor,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text(
                        "+ 1 $majorUnit",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Right Button: +1 كرت
                Button(
                    onClick = onAddMinor,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text(
                        "+ 1 $minorUnit",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
