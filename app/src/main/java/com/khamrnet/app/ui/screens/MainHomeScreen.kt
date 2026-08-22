package com.khamrnet.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.BondEntity
import com.khamrnet.app.data.model.CustomerEntity
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.ProductEntity
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.khamrnet.app.sync.SyncState
import com.khamrnet.app.sync.SyncStatus
import com.khamrnet.app.ui.KhamrColors
import com.khamrnet.app.ui.components.StoreActivationDialog
import java.text.DecimalFormat

data class AppMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTintColor: Color,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    currentUserCode: String = "101",
    userRole: String = "ADMIN",
    invoices: List<InvoiceEntity> = emptyList(),
    bonds: List<BondEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    syncStatus: SyncStatus,
    onTriggerSync: () -> Unit,
    onUpdateStoreCode: (String) -> Unit = {},
    onNavigate: (screenRoute: String) -> Unit,
    onLogout: () -> Unit
) {
    val df = remember { DecimalFormat("#,##0") }
    var showStoreActivationModal by remember { mutableStateOf(false) }

    // Computations matching web HomeScreen.tsx
    val totalSales = remember(invoices) {
        invoices.filter { !it.isCancelled }.sumOf { it.total }
    }
    val totalCashSales = remember(invoices) {
        invoices.filter { !it.isCancelled }.sumOf { it.paidAmount }
    }
    val totalReceiptBonds = remember(bonds) {
        bonds.filter { it.type == "RECEIPT" }.sumOf { it.amount }
    }
    val totalPaymentBonds = remember(bonds) {
        bonds.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }
    val totalCashCollected = totalCashSales + totalReceiptBonds
    val drawerBalance = (totalCashCollected - totalPaymentBonds).coerceAtLeast(0.0)

    // Compute remaining cashiers due amount if any
    val cashiersRemainingDue = remember(invoices, bonds) {
        val cashierInvoices = invoices.filter { !it.isCancelled && it.createdBy.isNotEmpty() && it.createdBy != "المدير" && it.createdBy != "admin" }
        val cashierReceipts = bonds.filter { it.type == "RECEIPT" && it.createdBy.isNotEmpty() && it.createdBy != "المدير" && it.createdBy != "admin" }
        val cashierPayments = bonds.filter { it.type == "PAYMENT" && it.createdBy.isNotEmpty() && it.createdBy != "المدير" && it.createdBy != "admin" }
        (cashierInvoices.sumOf { it.paidAmount } + cashierReceipts.sumOf { it.amount } - cashierPayments.sumOf { it.amount }).coerceAtLeast(0.0)
    }

    // 12 Full Screen Grid Items exactly matching the screenshot
    val menuItems = listOf(
        // Row 1
        AppMenuItem(
            id = "products",
            title = "بيانات الاصناف",
            icon = Icons.Default.Inventory2,
            iconBgColor = Color(0xFFF0FDFA),
            iconTintColor = Color(0xFF0F766E)
        ),
        AppMenuItem(
            id = "customers",
            title = "العملاء",
            icon = Icons.Default.People,
            iconBgColor = Color(0xFFEFF6FF),
            iconTintColor = Color(0xFF2563EB)
        ),
        AppMenuItem(
            id = "invoices",
            title = "سجل الفواتير",
            icon = Icons.Default.ReceiptLong,
            iconBgColor = Color(0xFFFAF5FF),
            iconTintColor = Color(0xFF7C3AED)
        ),
        // Row 2
        AppMenuItem(
            id = "bonds",
            title = "السندات",
            icon = Icons.Default.Description,
            iconBgColor = Color(0xFFECFDF5),
            iconTintColor = Color(0xFF059669)
        ),
        AppMenuItem(
            id = "transfer",
            title = "التحويل المخزني",
            icon = Icons.Default.SwapHoriz,
            iconBgColor = Color(0xFFFFF7ED),
            iconTintColor = Color(0xFFD97706)
        ),
        AppMenuItem(
            id = "settlements",
            title = "تصفية الكاشير",
            icon = Icons.Default.Savings,
            iconBgColor = Color(0xFFECFDF5),
            iconTintColor = Color(0xFF059669)
        ),
        // Row 3
        AppMenuItem(
            id = "expenses",
            title = "المصاريف",
            icon = Icons.Default.MonetizationOn,
            iconBgColor = Color(0xFFFFF1F2),
            iconTintColor = Color(0xFFE11D48)
        ),
        AppMenuItem(
            id = "card-generation",
            title = "توليد الكروت",
            icon = Icons.Default.Wifi,
            iconBgColor = Color(0xFFF0F9FF),
            iconTintColor = Color(0xFF0284C7)
        ),
        AppMenuItem(
            id = "users",
            title = "الكاشير",
            icon = Icons.Default.PersonOutline,
            iconBgColor = Color(0xFFEEF2FF),
            iconTintColor = Color(0xFF4F46E5)
        ),
        // Row 4
        AppMenuItem(
            id = "ledger",
            title = "القيود المحاسبية",
            icon = Icons.Default.AutoStories,
            iconBgColor = Color(0xFFF0FDFA),
            iconTintColor = Color(0xFF0F766E)
        ),
        AppMenuItem(
            id = "settings",
            title = "إعدادات النظام",
            icon = Icons.Default.Settings,
            iconBgColor = Color(0xFFF1F5F9),
            iconTintColor = Color(0xFF475569)
        ),
        AppMenuItem(
            id = "about",
            title = "حول البرنامج",
            icon = Icons.Default.Info,
            iconBgColor = Color(0xFFF0FDFA),
            iconTintColor = Color(0xFF0F766E)
        )
    )

    // Pulsing animation for sync dot & dark banner dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.8.dp, color = Color(0xFFE2E8F0))
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Business Logo, Name & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F766E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CellTower,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = settings.businessName.ifEmpty { "شبكة خمر اللاسلكيه" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${settings.address.ifEmpty { "خمر - السوق العام" }} • ${settings.currencyName.ifEmpty { "YER" }}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Left: User Badge & Logout Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // User Badge Card
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F766E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUserCode,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = currentUserName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (userRole == "ADMIN") "مدير" else "كاشير",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }

                        // Logout Button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF1F2))
                                .border(0.8.dp, Color(0xFFFECDD3), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Cloud Sync Status Strip (Exact matching Web & Screenshot)
                val isSyncing = syncStatus.state == SyncState.SYNCING
                val isSuccess = syncStatus.state == SyncState.SUCCESS && settings.storeCode.isNotEmpty()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isSyncing -> Color(0xFFEFF6FF)
                                isSuccess -> Color(0xFFECFDF5)
                                else -> Color(0xFFFFFBEB)
                            }
                        )
                        .border(
                            width = 0.6.dp,
                            color = when {
                                isSyncing -> Color(0xFFBFDBFE)
                                isSuccess -> Color(0xFFA7F3D0)
                                else -> Color(0xFFFDE68A)
                            }
                        )
                        .clickable {
                            if (isSuccess) {
                                onTriggerSync()
                            } else {
                                showStoreActivationModal = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(if (isSuccess || isSyncing) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSyncing -> Color(0xFF2563EB)
                                        isSuccess -> Color(0xFF10B981)
                                        else -> Color(0xFFF59E0B)
                                    }
                                )
                        )
                        Text(
                            text = when {
                                isSyncing -> "جاري المزامنة السحابية..."
                                isSuccess -> "المزامنة السحابية متصلة [${settings.storeCode}]"
                                settings.storeCode.isNotEmpty() -> "حالة المزامنة السحابية غير متصلة [${settings.storeCode}]"
                                else -> "حالة المزامنة السحابية غير مفعلة"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSyncing -> Color(0xFF1E40AF)
                                isSuccess -> Color(0xFF065F46)
                                else -> Color(0xFF92400E)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isSyncing -> Color(0xFFBFDBFE).copy(alpha = 0.7f)
                                    isSuccess -> Color(0xFFA7F3D0).copy(alpha = 0.7f)
                                    else -> Color(0xFFFDE68A).copy(alpha = 0.7f)
                                }
                            )
                            .clickable { showStoreActivationModal = true }
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isSuccess) "إدارة الربط ❮" else "تفعيل الترخيص ❮",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSyncing -> Color(0xFF1E40AF)
                                isSuccess -> Color(0xFF065F46)
                                else -> Color(0xFF92400E)
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Pinned Bottom Navigation Bar (Matching Web App Bar & Screenshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.8.dp, color = Color(0xFFE2E8F0))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Default.Home, label = "الرئيسية", selected = true, onClick = {})
                BottomNavItem(icon = Icons.Default.ShoppingCart, label = "نقطة البيع", selected = false, onClick = { onNavigate("pos") })
                BottomNavItem(icon = Icons.Default.Inventory2, label = "الأصناف", selected = false, onClick = { onNavigate("products") })
                BottomNavItem(icon = Icons.Default.People, label = "العملاء", selected = false, onClick = { onNavigate("customers") })
                BottomNavItem(icon = Icons.Default.Description, label = "السندات", selected = false, onClick = { onNavigate("bonds") })
                BottomNavItem(icon = Icons.Default.Settings, label = "الإعدادات", selected = false, onClick = { onNavigate("settings") })
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Quick POS Launch Button (Matching Web POS banner)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("pos") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "فتح نقطة البيع (POS)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "إصدار فاتورة نقدية أو آجلة وسداد جزئي",
                                fontSize = 10.sp,
                                color = Color(0xFFCCFBF1)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "دخول البيع ⚡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            }

            // 2. Stats Overview - 3 Items in 1 Row (Sales, Cash Collected, Drawer Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Card 1: Total Sales
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = if (userRole == "ADMIN") "إجمالي المبيعات" else "مبيعاتك",
                    amount = df.format(totalSales),
                    currency = settings.currencyName,
                    subtitle = "${invoices.filter { !it.isCancelled }.size} فواتير",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF0F766E)
                )

                // Card 2: Total Cash Collected
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = if (userRole == "ADMIN") "المقبوضات" else "المقبوض كاش",
                    amount = df.format(totalCashCollected),
                    currency = settings.currencyName,
                    subtitle = "فواتير + سندات",
                    icon = Icons.Default.CreditCard,
                    iconColor = Color(0xFF059669)
                )

                // Card 3: Cash Box / Drawer Balance
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = if (userRole == "ADMIN") "خزينة الإدارة" else "رصيد صندوقك",
                    amount = df.format(drawerBalance),
                    currency = settings.currencyName,
                    subtitle = if (drawerBalance > 0) "المتبقي بالدرج" else "الخزينة الرئيسية",
                    icon = Icons.Default.Savings,
                    iconColor = Color(0xFF059669)
                )
            }

            // 3. Dark Navy Strip ("متبقي في عهد وأدراج الكواشير للمطالبة")
            if (userRole == "ADMIN") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .clickable { onNavigate("settlements") }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24))
                        )
                        Text(
                            text = "متبقي في عهد وأدراج الكواشير للمطالبة: ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "${settings.currencyName} ${df.format(cashiersRemainingDue)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE047)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "تصفية وقبض ⚡",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF99F6E4)
                        )
                    }
                }
            }

            // 4. Section Title
            Text(
                text = "شاشات وأقسام النظام المتاحة لك",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )

            // 5. Main Navigation Grid (3 Columns x 4 Rows = 12 Boxes exactly matching screenshot)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clickable { onNavigate(item.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Badge if present (like "قريباً")
                            if (item.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF59E0B))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(item.iconBgColor)
                                        .border(0.6.dp, item.iconTintColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.iconTintColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Store Activation & Tenant Space Link
    StoreActivationDialog(
        currentStoreCode = settings.storeCode,
        businessName = settings.businessName,
        isOpen = showStoreActivationModal,
        onDismiss = { showStoreActivationModal = false },
        onConfirmActivation = { newCode ->
            showStoreActivationModal = false
            onUpdateStoreCode(newCode)
        }
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    currency: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amount,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = currency,
                    fontSize = 8.sp,
                    color = Color(0xFF64748B)
                )
            }
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF0F766E) else Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) Color(0xFF0F766E) else Color(0xFF64748B)
        )
    }
}
