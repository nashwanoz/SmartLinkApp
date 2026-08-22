package com.khamrnet.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.khamrnet.app.data.model.ProductEntity
import com.khamrnet.app.data.model.ProductUnit
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

enum class ProductViewMode {
    GRID, STRIP, TABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(ProductViewMode.GRID) }
    var searchTerm by remember { mutableStateOf("") }
    var isModalOpen by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    // Filtered products list
    val filteredProducts = remember(products, searchTerm) {
        val s = searchTerm.trim().lowercase()
        if (s.isEmpty()) products
        else {
            products.filter { p ->
                p.name.lowercase().contains(s) ||
                p.code.lowercase().contains(s) ||
                p.barcode.lowercase().contains(s) ||
                p.baseUnitName.lowercase().contains(s)
            }
        }
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
                            text = "إدارة الأصناف والمخزون",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${products.size} صنف",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "الرجوع للرئيسية",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            editingProduct = null
                            isModalOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إضافة صنف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F766E)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // View Mode Switcher + Search Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode buttons: Grid / Strip / Table
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Grid Mode
                    Surface(
                        onClick = { viewMode = ProductViewMode.GRID },
                        shape = RoundedCornerShape(10.dp),
                        color = if (viewMode == ProductViewMode.GRID) Color.White else Color.Transparent,
                        shadowElevation = if (viewMode == ProductViewMode.GRID) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (viewMode == ProductViewMode.GRID) Color(0xFF0F172A) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "شبكة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (viewMode == ProductViewMode.GRID) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Strip Mode
                    Surface(
                        onClick = { viewMode = ProductViewMode.STRIP },
                        shape = RoundedCornerShape(10.dp),
                        color = if (viewMode == ProductViewMode.STRIP) Color.White else Color.Transparent,
                        shadowElevation = if (viewMode == ProductViewMode.STRIP) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = null,
                                tint = if (viewMode == ProductViewMode.STRIP) Color(0xFF0F172A) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "شريط",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (viewMode == ProductViewMode.STRIP) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Table Mode
                    Surface(
                        onClick = { viewMode = ProductViewMode.TABLE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (viewMode == ProductViewMode.TABLE) Color.White else Color.Transparent,
                        shadowElevation = if (viewMode == ProductViewMode.TABLE) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = if (viewMode == ProductViewMode.TABLE) Color(0xFF0F172A) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "قائمة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (viewMode == ProductViewMode.TABLE) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Count Indicator
                Text(
                    text = "المعروض: ${filteredProducts.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = {
                    Text(
                        "🔍 ابحث بالاسم أو الباركود أو الكود...",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (searchTerm.isNotEmpty()) {
                        IconButton(onClick = { searchTerm = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0F766E),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Content Views
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchTerm.isNotEmpty()) "لا توجد أصناف مطابقة لبحثك" else "لا توجد أي أصناف مسجلة حتى الآن",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                when (viewMode) {
                    ProductViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                ProductGridCard(
                                    product = product,
                                    currency = settings.currencyName,
                                    onEdit = {
                                        editingProduct = product
                                        isModalOpen = true
                                    },
                                    onDelete = {
                                        productToDelete = product
                                    }
                                )
                            }
                        }
                    }
                    ProductViewMode.STRIP -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                ProductStripCard(
                                    product = product,
                                    currency = settings.currencyName,
                                    onEdit = {
                                        editingProduct = product
                                        isModalOpen = true
                                    },
                                    onDelete = {
                                        productToDelete = product
                                    }
                                )
                            }
                        }
                    }
                    ProductViewMode.TABLE -> {
                        ProductTableCard(
                            products = filteredProducts,
                            currency = settings.currencyName,
                            onEdit = { product ->
                                editingProduct = product
                                isModalOpen = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Product
    if (isModalOpen) {
        ProductFormDialog(
            existingProduct = editingProduct,
            existingProductsCount = products.size,
            currency = settings.currencyName,
            onDismiss = { isModalOpen = false },
            onSave = { savedEntity ->
                onSaveProduct(savedEntity)
                isModalOpen = false
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("حذف الصنف", fontWeight = FontWeight.Black, fontSize = 16.sp) },
            text = {
                Text(
                    "هل أنت متأكد من حذف الصنف (${prod.name})؟ سيتم إزالته من قائمة الأصناف.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، حذف", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Component: Product Grid Card
// -------------------------------------------------------------
@Composable
fun ProductGridCard(
    product: ProductEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سعر البيع:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "${product.salePrice.toInt()} $currency",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F766E)
                )
            }

            // Stock Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المتوفر:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "${product.stockQuantity.toInt()} ${product.baseUnitName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF065F46)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Product Strip Card
// -------------------------------------------------------------
@Composable
fun ProductStripCard(
    product: ProductEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "سعر البيع: ${product.salePrice.toInt()} $currency",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${product.stockQuantity.toInt()} ${product.baseUnitName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF065F46)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Product Table Card
// -------------------------------------------------------------
@Composable
fun ProductTableCard(
    products: List<ProductEntity>,
    currency: String,
    onEdit: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اسم الصنف",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF475569),
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "سعر البيع",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "الكمية",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "إجراء",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Body List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products, key = { it.id }) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(product) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "${product.salePrice.toInt()} $currency",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${product.stockQuantity.toInt()} ${product.baseUnitName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onEdit(product) },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Dialog for Adding / Editing Product
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    existingProduct: ProductEntity?,
    existingProductsCount: Int,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var barcode by remember {
        mutableStateOf(existingProduct?.barcode ?: "6281000${(existingProductsCount + 1).toString().padStart(3, '0')}")
    }
    var code by remember {
        mutableStateOf(existingProduct?.code ?: "${1000 + existingProductsCount + 1}")
    }

    // Units and prices matching current system exactly
    var mainUnitName by remember { mutableStateOf(existingProduct?.baseUnitName ?: "كرت") }
    var mainUnitPrice by remember {
        mutableStateOf(if (existingProduct != null && existingProduct.salePrice > 0) existingProduct.salePrice.toInt().toString() else "")
    }

    var subUnitName by remember { mutableStateOf("صفحه") }
    var subUnitFactor by remember { mutableStateOf("24") }
    var subUnitPrice by remember { mutableStateOf("") }

    var stockQuantity by remember {
        mutableStateOf(if (existingProduct != null) existingProduct.stockQuantity.toInt().toString() else "100")
    }

    var errorMessage by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Initialize units if editing
    LaunchedEffect(existingProduct) {
        if (existingProduct != null && existingProduct.unitsJson.isNotEmpty()) {
            try {
                val listType = object : TypeToken<List<ProductUnit>>() {}.type
                val units: List<ProductUnit> = Gson().fromJson(existingProduct.unitsJson, listType) ?: emptyList()
                val sub = units.firstOrNull { it.factor > 1.0 }
                if (sub != null) {
                    subUnitName = sub.name
                    subUnitFactor = sub.factor.toInt().toString()
                    subUnitPrice = sub.salePrice.toInt().toString()
                }
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F766E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (existingProduct == null) Icons.Default.AddBox else Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (existingProduct == null) "إضافة صنف جديد" else "تعديل بيانات الصنف",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "جدول الأصناف (ias_itm)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Error alert
                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(8.dp)
                    )
                }

                // 1. Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف *", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    placeholder = { Text("مثال: كرت شحن 1000 ريال", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F766E),
                        focusedLabelColor = Color(0xFF0F766E)
                    )
                )

                // 2. Barcode & Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("كود الصنف", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // 3. Main Unit (الوحدة الرئيسية)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. الوحدة الرئيسية (الأساسية):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F766E)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mainUnitName,
                            onValueChange = { mainUnitName = it },
                            label = { Text("اسم الوحدة", fontSize = 11.sp) },
                            placeholder = { Text("كرت", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = mainUnitPrice,
                            onValueChange = { mainUnitPrice = it },
                            label = { Text("سعر البيع ($currency)", fontSize = 11.sp) },
                            placeholder = { Text("1000", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 4. Sub Unit (الوحدة الفرعية - اختياري)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "2. الوحدة الفرعية (تجزئة اختيارية):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = subUnitName,
                            onValueChange = { subUnitName = it },
                            label = { Text("اسم الوحدة", fontSize = 11.sp) },
                            placeholder = { Text("صفحه / حبة", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = subUnitFactor,
                            onValueChange = { subUnitFactor = it },
                            label = { Text("عدد الوحدات", fontSize = 11.sp) },
                            placeholder = { Text("24", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = subUnitPrice,
                            onValueChange = { subUnitPrice = it },
                            label = { Text("سعر البيع", fontSize = 11.sp) },
                            placeholder = { Text("50", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 5. Stock Quantity
                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("كمية المخزون الافتتاحية ($mainUnitName)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    placeholder = { Text("100", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Actions: Save / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                errorMessage = "يرجى كتابة اسم الصنف"
                                return@Button
                            }
                            val priceVal = mainUnitPrice.toDoubleOrNull() ?: 0.0
                            val stockVal = stockQuantity.toDoubleOrNull() ?: 0.0

                            // Build Units JSON
                            val unitsList = mutableListOf<ProductUnit>()
                            unitsList.add(
                                ProductUnit(
                                    id = "unit-1",
                                    name = mainUnitName.ifEmpty { "كرت" },
                                    factor = 1.0,
                                    purchasePrice = 0.0,
                                    salePrice = priceVal,
                                    isDefault = true
                                )
                            )
                            if (subUnitName.isNotBlank() && (subUnitFactor.toDoubleOrNull() ?: 0.0) > 1.0) {
                                unitsList.add(
                                    ProductUnit(
                                        id = "unit-2",
                                        name = subUnitName.trim(),
                                        factor = subUnitFactor.toDoubleOrNull() ?: 1.0,
                                        purchasePrice = 0.0,
                                        salePrice = subUnitPrice.toDoubleOrNull() ?: 0.0,
                                        isDefault = false
                                    )
                                )
                            }

                            val newOrUpdatedProduct = ProductEntity(
                                id = existingProduct?.id ?: UUID.randomUUID().toString(),
                                code = code.ifEmpty { (1000 + existingProductsCount + 1).toString() },
                                name = trimmedName,
                                barcode = barcode.ifEmpty { code },
                                salePrice = priceVal,
                                purchasePrice = existingProduct?.purchasePrice ?: 0.0,
                                stockQuantity = stockVal,
                                baseUnitName = mainUnitName.ifEmpty { "كرت" },
                                unitsJson = Gson().toJson(unitsList),
                                createdAt = existingProduct?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            onSave(newOrUpdatedProduct)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text(
                            text = if (existingProduct == null) "حفظ وإضافة الصنف" else "حفظ التعديلات",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
