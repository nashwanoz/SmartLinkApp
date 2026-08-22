package com.khamrnet.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.khamrnet.app.data.model.*
import com.khamrnet.app.printer.BluetoothPrinterManager
import com.khamrnet.app.printer.PrinterDeviceInfo
import com.khamrnet.app.sync.SyncState
import com.khamrnet.app.sync.SyncStatus
import com.khamrnet.app.ui.components.StoreActivationDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SystemSettingsEntity,
    currentUserName: String = "المدير العام (نشوان)",
    syncStatus: SyncStatus = SyncStatus(),
    products: List<ProductEntity> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    invoices: List<InvoiceEntity> = emptyList(),
    bonds: List<BondEntity> = emptyList(),
    onSaveSettings: (SystemSettingsEntity) -> Unit,
    onTriggerSync: () -> Unit = {},
    onUpdateStoreCode: (String) -> Unit = {},
    onResetToDefaults: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onNavigate: (route: String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    // Active sub-dialog modals
    var activeModal by remember { mutableStateOf<String?>(null) } // "business", "currency", "printer", "backup", "reset", "storeCode"

    // Animation for pulse sync dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        bottomBar = {
            SettingsBottomNavBar(
                currentRoute = "settings",
                onNavigate = onNavigate
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // =========================================================================
            // 1. TOP STATUS BAR & HEADER (Matching Web and Screen Exactly)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Store & User Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Store Name & Location/Currency
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0FDFA))
                                .border(0.8.dp, Color(0xFF99F6E4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = settings.businessName.ifEmpty { "شبكة خمر اللاسلكيه" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${settings.address.ifEmpty { "خمر - السوق العام" }} • ${settings.currencyName}",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Left: User Badge & Logout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // User Badge Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(0.8.dp, Color(0xFFBBF7D0), RoundedCornerShape(18.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // User Number Badge
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F766E))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "101",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currentUserName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1
                                )
                                Text(
                                    text = "مدير",
                                    fontSize = 8.sp,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }

                        // Logout / Switch button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF1F2))
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "تسجيل خروج",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Cloud Sync Status Strip
                val isSyncing = syncStatus.state == SyncState.SYNCING
                val isSuccess = syncStatus.state == SyncState.SUCCESS && settings.storeCode.isNotEmpty()
                val currentStoreCode = settings.storeCode.ifEmpty { "TEST-SMART" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
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
                                activeModal = "storeCode"
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Sync status label and glowing pulse dot
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
                                isSuccess -> "المزامنة السحابية متصلة [$currentStoreCode]"
                                settings.storeCode.isNotEmpty() -> "حالة المزامنة السحابية غير متصلة [$currentStoreCode]"
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

                    // Left: Manage link button
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
                            .clickable { activeModal = "storeCode" }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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

            // Divider
            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // =========================================================================
                // 2. MAIN SECTION HEADER & RESET DEFAULT BUTTON
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Title & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "تهيئة وإعدادات النظام العام",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "اختر أحد أقسام التهيئة من المربعات أعلاه لفتح الشاشة المخصصة",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Left: Reset Defaults Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeModal = "reset" }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "استعادة الافتراضي",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // =========================================================================
                // 3. THE 4 CONFIGURATION GRID CARDS (Matching Screenshot 2x2 Layout)
                // =========================================================================
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: تهيئة العملات (Left) & تهيئة النشاط (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: تهيئة العملات (Currency)
                        QuickConfigCard(
                            modifier = Modifier.weight(1f),
                            title = "تهيئة العملات",
                            subtitle = "دليل العملات وأسعار الصرف",
                            icon = Icons.Default.AttachMoney,
                            iconColor = Color(0xFF047857),
                            iconBgColor = Color(0xFFD1FAE5),
                            borderColor = Color(0xFFA7F3D0),
                            onClick = { activeModal = "currency" }
                        )

                        // Card 2: تهيئة النشاط (Business)
                        QuickConfigCard(
                            modifier = Modifier.weight(1f),
                            title = "تهيئة النشاط",
                            subtitle = "الاسم، الموقع، الجوال واللوجو",
                            icon = Icons.Default.Smartphone,
                            iconColor = Color(0xFF0F766E),
                            iconBgColor = Color(0xFFCCFBF1),
                            borderColor = Color(0xFF99F6E4),
                            onClick = { activeModal = "business" }
                        )
                    }

                    // Row 2: تهيئة النسخ الاحتياطي (Left) & تهيئة الطابعة (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: تهيئة النسخ الاحتياطي (Backup)
                        QuickConfigCard(
                            modifier = Modifier.weight(1f),
                            title = "تهيئة النسخ الاحتياطي",
                            subtitle = "تصدير واستعادة وتأمين البيانات",
                            icon = Icons.Default.Storage,
                            iconColor = Color(0xFF4338CA),
                            iconBgColor = Color(0xFFE0E7FF),
                            borderColor = Color(0xFFC7D2FE),
                            onClick = { activeModal = "backup" }
                        )

                        // Card 4: تهيئة الطابعة (Printer)
                        QuickConfigCard(
                            modifier = Modifier.weight(1f),
                            title = "تهيئة الطابعة",
                            subtitle = "الطابعة الحرارية، البلوتوث والرول",
                            icon = Icons.Default.Print,
                            iconColor = Color(0xFF1D4ED8),
                            iconBgColor = Color(0xFFDBEAFE),
                            borderColor = Color(0xFFBFDBFE),
                            onClick = { activeModal = "printer" }
                        )
                    }
                }

                // =========================================================================
                // 4. SYSTEM STATUS SUMMARY OVERVIEW CARD (Matching Screenshot)
                // =========================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Overview Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge on Left
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "خمر نت v2.5",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }

                            // Title on Right
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "نظرة عامة على بيانات وإعدادات النظام الحالية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

                        // 4 Interactive List Items
                        // 1. هوية النشاط
                        OverviewRowItem(
                            label = "هوية النشاط:",
                            mainValue = settings.businessName.ifEmpty { "شبكة خمر اللاسلكيه" },
                            subValue = "${settings.address.ifEmpty { "خمر - السوق العام" }} • ${settings.phone.ifEmpty { "783888185" }}",
                            mainColor = Color(0xFF0F172A),
                            onClick = { activeModal = "business" }
                        )

                        // 2. العملة الافتراضية
                        OverviewRowItem(
                            label = "العملة الافتراضية:",
                            mainValue = "ريال يمني (YER)",
                            subValue = "عدد العملات المعتمدة: 3",
                            mainColor = Color(0xFF047857),
                            onClick = { activeModal = "currency" }
                        )

                        // 3. الطابعة والطباعة
                        OverviewRowItem(
                            label = "الطابعة والطباعة:",
                            mainValue = "مقاس الرول: ${settings.thermalPaperWidth.ifEmpty { "80mm" }} • مقياس %${settings.thermalPrintScale}",
                            subValue = "${if (settings.autoPrintOnSave) "الطباعة التلقائية مفعلة ☑️" else "الطباعة التلقائية معطلة"} • بلوتوث",
                            mainColor = Color(0xFF1E40AF),
                            onClick = { activeModal = "printer" }
                        )

                        // 4. النسخ الاحتياطي
                        OverviewRowItem(
                            label = "النسخ الاحتياطي:",
                            mainValue = "تصدير واستعادة ملفات JSON",
                            subValue = "إجمالي الأصناف: ${products.size} • العملاء: ${customers.size} • الفواتير: ${invoices.size}",
                            mainColor = Color(0xFF4338CA),
                            onClick = { activeModal = "backup" }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // SUB-MODALS AND INTERACTIVE DIALOGS
    // =========================================================================

    // 1. Modal: تهيئة النشاط (Business Info Dialog)
    if (activeModal == "business") {
        BusinessInfoDialog(
            settings = settings,
            onDismiss = { activeModal = null },
            onSave = { updated ->
                onSaveSettings(updated)
                activeModal = null
                Toast.makeText(context, "✅ تم حفظ وتحديث بيانات النشاط بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Modal: تهيئة العملات (Currency Dialog)
    if (activeModal == "currency") {
        CurrencySettingsDialog(
            settings = settings,
            onDismiss = { activeModal = null },
            onSave = { updated ->
                onSaveSettings(updated)
                activeModal = null
                Toast.makeText(context, "✅ تم حفظ العملات وأسعار الصرف بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Modal: تهيئة الطابعة (Printer Dialog)
    if (activeModal == "printer") {
        PrinterSettingsDialog(
            settings = settings,
            printerManager = printerManager,
            onDismiss = { activeModal = null },
            onSave = { updated ->
                onSaveSettings(updated)
                activeModal = null
                Toast.makeText(context, "✅ تم حفظ إعدادات الطابعة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Modal: تهيئة النسخ الاحتياطي (Backup Dialog)
    if (activeModal == "backup") {
        BackupSettingsDialog(
            settings = settings,
            products = products,
            customers = customers,
            invoices = invoices,
            bonds = bonds,
            onDismiss = { activeModal = null },
            onClearAllData = {
                onClearAllData()
                activeModal = null
            }
        )
    }

    // 5. Modal: استعادة الافتراضي (Reset Confirmation Dialog)
    if (activeModal == "reset") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = {
                Text(
                    text = "استعادة الإعدادات الافتراضية",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في استعادة جميع إعدادات النظام الافتراضية (الاسم، العملات، الطابعة)؟",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val defaultSettings = SystemSettingsEntity()
                        onSaveSettings(defaultSettings)
                        onResetToDefaults()
                        activeModal = null
                        Toast.makeText(context, "تمت استعادة الإعدادات الافتراضية", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، استعادة الافتراضي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { activeModal = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // 6. Modal: كود المحل والمزامنة السحابية (Store Activation Dialog)
    StoreActivationDialog(
        currentStoreCode = settings.storeCode,
        businessName = settings.businessName,
        isOpen = activeModal == "storeCode",
        onDismiss = { activeModal = null },
        onConfirmActivation = { newCode ->
            activeModal = null
            onUpdateStoreCode(newCode)
        }
    )
}

// =========================================================================
// HELPER SUB-COMPONENTS
// =========================================================================

@Composable
private fun QuickConfigCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OverviewRowItem(
    label: String,
    mainValue: String,
    subValue: String,
    mainColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Arrow Chevron
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )

            // Right: Content
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mainValue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = mainColor
                )
                Text(
                    text = subValue,
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

// =========================================================================
// 1. BUSINESS INFO DIALOG (تهيئة النشاط التجاري)
// =========================================================================

@Composable
private fun BusinessInfoDialog(
    settings: SystemSettingsEntity,
    onDismiss: () -> Unit,
    onSave: (SystemSettingsEntity) -> Unit
) {
    var businessName by remember { mutableStateOf(settings.businessName) }
    var address by remember { mutableStateOf(settings.address) }
    var phone by remember { mutableStateOf(settings.phone) }
    var footerText by remember { mutableStateOf(settings.invoiceFooterMessage) }
    var storeCode by remember { mutableStateOf(settings.storeCode) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0FDFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تهيئة النشاط التجاري والهوية", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("اسم المحل، العنوان، الجوال، الترويسة", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Inputs
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("اسم المحل / المنشأة *", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("الموقع والعنوان *", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / الجوال", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("رسالة تذييل الفاتورة", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeCode,
                    onValueChange = { storeCode = it.uppercase() },
                    label = { Text("كود المحل للمزامنة السحابية (Store Code)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val updated = settings.copy(
                                businessName = businessName.trim().ifEmpty { "شبكة خمر اللاسلكيه" },
                                address = address.trim().ifEmpty { "خمر - السوق العام" },
                                phone = phone.trim(),
                                footerText = footerText.trim(),
                                invoiceFooterMessage = footerText.trim(),
                                storeCode = storeCode.trim().uppercase().ifEmpty { "TEST-SMART" }
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ بيانات النشاط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. CURRENCY SETTINGS DIALOG (تهيئة العملات وأسعار الصرف)
// =========================================================================

data class CurrencyItem(
    val code: String,
    val name: String,
    val rate: Double,
    val isDefault: Boolean
)

@Composable
private fun CurrencySettingsDialog(
    settings: SystemSettingsEntity,
    onDismiss: () -> Unit,
    onSave: (SystemSettingsEntity) -> Unit
) {
    var defaultCurrency by remember { mutableStateOf(settings.currencyName.ifEmpty { "YER" }) }
    var yerRate by remember { mutableStateOf("1.0") }
    var sarRate by remember { mutableStateOf("140.0") }
    var usdRate by remember { mutableStateOf("535.0") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تهيئة العملات وأسعار الصرف", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("دليل العملات المعتمدة وأسعار التحويل", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Currency Rates List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العملات المعتمدة وأسعار الصرف:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))

                    // 1. ريال يمني
                    CurrencyRow(
                        code = "YER",
                        name = "ريال يمني (العملة الأساسية)",
                        rate = yerRate,
                        isDefault = defaultCurrency == "YER",
                        onSelectDefault = { defaultCurrency = "YER" },
                        onRateChange = { yerRate = it }
                    )

                    // 2. ريال سعودي
                    CurrencyRow(
                        code = "SAR",
                        name = "ريال سعودي",
                        rate = sarRate,
                        isDefault = defaultCurrency == "SAR",
                        onSelectDefault = { defaultCurrency = "SAR" },
                        onRateChange = { sarRate = it }
                    )

                    // 3. دولار أمريكي
                    CurrencyRow(
                        code = "USD",
                        name = "دولار أمريكي",
                        rate = usdRate,
                        isDefault = defaultCurrency == "USD",
                        onSelectDefault = { defaultCurrency = "USD" },
                        onRateChange = { usdRate = it }
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val updated = settings.copy(currencyName = defaultCurrency)
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ العملة الافتراضية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    name: String,
    rate: String,
    isDefault: Boolean,
    onSelectDefault: () -> Unit,
    onRateChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDefault) Color(0xFFECFDF5) else Color(0xFFF8FAFC))
            .border(0.8.dp, if (isDefault) Color(0xFFA7F3D0) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button / check
            RadioButton(
                selected = isDefault,
                onClick = onSelectDefault,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF047857))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(text = "الرمز: $code • سعر الصرف: $rate", fontSize = 9.sp, color = Color(0xFF64748B))
            }
        }
    }
}

// =========================================================================
// 3. PRINTER SETTINGS DIALOG (تهيئة الطابعة والطباعة الحرارية)
// =========================================================================

@Composable
private fun PrinterSettingsDialog(
    settings: SystemSettingsEntity,
    printerManager: BluetoothPrinterManager,
    onDismiss: () -> Unit,
    onSave: (SystemSettingsEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var paperWidth by remember { mutableStateOf(settings.thermalPaperWidth.ifEmpty { "80mm" }) }
    var printScale by remember { mutableStateOf(settings.thermalPrintScale) }
    var autoPrint by remember { mutableStateOf(settings.autoPrintOnSave) }

    var pairedPrinters by remember { mutableStateOf<List<PrinterDeviceInfo>>(emptyList()) }
    var selectedPrinterMac by remember { mutableStateOf(settings.defaultPrinterMac) }
    var selectedPrinterName by remember { mutableStateOf(settings.defaultPrinterName) }
    var isTestingPrint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pairedPrinters = printerManager.getPairedPrinters()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تهيئة الطابعة والطباعة الحرارية", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("طابعات البلوتوث، مقاس الرول، الطباعة التلقائية", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Paper Width Selection (80mm / 72mm / 58mm)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مقاس رول الورق الحراري:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("80mm", "72mm", "58mm").forEach { width ->
                            val isSelected = paperWidth == width
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF1D4ED8) else Color(0xFFF1F5F9))
                                    .clickable { paperWidth = width }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = width,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                // Paired Bluetooth Printers
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("طابعة البلوتوث الافتراضية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        TextButton(
                            onClick = { pairedPrinters = printerManager.getPairedPrinters() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("تحديث القائمة", fontSize = 10.sp, color = Color(0xFF1D4ED8))
                        }
                    }

                    if (pairedPrinters.isEmpty()) {
                        Text(
                            "لم يتم العثور على أجهزة بلوتوث مقترنة. يرجى اقتران الطابعة أولاً من إعدادات الهاتف.",
                            fontSize = 10.sp,
                            color = Color(0xFFE11D48)
                        )
                    } else {
                        pairedPrinters.forEach { printer ->
                            val isSelected = selectedPrinterMac == printer.address
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                    .border(0.8.dp, if (isSelected) Color(0xFFBFDBFE) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedPrinterMac = printer.address
                                        selectedPrinterName = printer.name
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                        Text(text = printer.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text(text = printer.address, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPrinterMac = printer.address
                                            selectedPrinterName = printer.name
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1D4ED8))
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto Print Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الطباعة التلقائية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("طباعة الفاتورة فور حفظها تلقائياً", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = autoPrint,
                        onCheckedChange = { autoPrint = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1D4ED8), checkedTrackColor = Color(0xFFBFDBFE))
                    )
                }

                // Test Print Button
                OutlinedButton(
                    onClick = {
                        if (selectedPrinterMac.isEmpty()) {
                            Toast.makeText(context, "يرجى اختيار طابعة بلوتوث أولاً", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        isTestingPrint = true
                        coroutineScope.launch {
                            val res = printerManager.testPrint(selectedPrinterMac, paperWidth)
                            isTestingPrint = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "✅ تمت طباعة الفاتورة التجريبية بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تعذر الاتصال بالطابعة: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingPrint) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري إرسال أمر الطباعة...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة إيصال تجريبي عبر البلوتوث", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val updated = settings.copy(
                                thermalPaperWidth = paperWidth,
                                thermalPrintScale = printScale,
                                defaultPrinterMac = selectedPrinterMac,
                                defaultPrinterName = selectedPrinterName,
                                autoPrintOnSave = autoPrint
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ إعدادات الطابعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. BACKUP SETTINGS DIALOG (تهيئة النسخ الاحتياطي وتأمين البيانات)
// =========================================================================

@Composable
private fun BackupSettingsDialog(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    onDismiss: () -> Unit,
    onClearAllData: () -> Unit
) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تهيئة النسخ الاحتياطي وتأمين البيانات", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("تصدير واستعادة ملفات JSON محلياً وسحابياً", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Stats Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("إحصائيات السجلات المسجلة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الأصناف: ${products.size}", fontSize = 10.sp, color = Color(0xFF475569))
                            Text("العملاء: ${customers.size}", fontSize = 10.sp, color = Color(0xFF475569))
                            Text("الفواتير: ${invoices.size}", fontSize = 10.sp, color = Color(0xFF475569))
                            Text("السندات: ${bonds.size}", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                    }
                }

                // Export Button
                Button(
                    onClick = {
                        val backupMap = mapOf(
                            "appName" to "نظام خمر نت المحاسبي",
                            "version" to "2.5.0",
                            "exportedAt" to System.currentTimeMillis(),
                            "storeCode" to settings.storeCode,
                            "businessName" to settings.businessName,
                            "products" to products,
                            "customers" to customers,
                            "invoices" to invoices,
                            "bonds" to bonds
                        )
                        val json = GsonBuilder().setPrettyPrinting().create().toJson(backupMap)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("KhamrNet_Backup", json)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "✅ تم تصدير ونسخ بيانات النظام إلى الحافظة بنجاح", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير نسخة احتياطية محلية (JSON)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Clear/Reset DB Button
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE11D48)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصفير قاعدة البيانات وإعادة التهيئة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (showResetConfirm) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirm = false },
                        title = { Text("تحذير تصفير البيانات", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFE11D48)) },
                        text = { Text("هل أنت متأكد من رغبتك في حذف جميع الفواتير والسندات من قاعدة البيانات المحلية؟", fontSize = 11.sp) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showResetConfirm = false
                                    onClearAllData()
                                    onDismiss()
                                    Toast.makeText(context, "تم تصفير البيانات بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text("تأكيد التصفير", fontSize = 11.sp)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showResetConfirm = false }) {
                                Text("إلغاء", fontSize = 11.sp)
                            }
                        },
                        containerColor = Color.White
                    )
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", fontSize = 11.sp)
                }
            }
        }
    }
}

// =========================================================================
// 5. BOTTOM NAVIGATION BAR FOR SETTINGS SCREEN
// =========================================================================

@Composable
private fun SettingsBottomNavBar(
    currentRoute: String,
    onNavigate: (route: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. الإعدادات (Settings - Active)
            SettingsBottomItem(
                icon = Icons.Default.Settings,
                label = "الإعدادات",
                selected = currentRoute == "settings",
                onClick = { onNavigate("settings") }
            )

            // 2. السندات (Bonds)
            SettingsBottomItem(
                icon = Icons.Default.ReceiptLong,
                label = "السندات",
                selected = currentRoute == "bonds",
                onClick = { onNavigate("bonds") }
            )

            // 3. العملاء (Customers)
            SettingsBottomItem(
                icon = Icons.Default.People,
                label = "العملاء",
                selected = currentRoute == "customers",
                onClick = { onNavigate("customers") }
            )

            // 4. الأصناف (Products)
            SettingsBottomItem(
                icon = Icons.Default.Inventory2,
                label = "الأصناف",
                selected = currentRoute == "products",
                onClick = { onNavigate("products") }
            )

            // 5. نقطة البيع (POS)
            SettingsBottomItem(
                icon = Icons.Default.ShoppingCart,
                label = "نقطة البيع",
                selected = currentRoute == "pos",
                onClick = { onNavigate("pos") }
            )

            // 6. الرئيسية (Home)
            SettingsBottomItem(
                icon = Icons.Default.Home,
                label = "الرئيسية",
                selected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )
        }
    }
}

@Composable
private fun SettingsBottomItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) Color(0xFF0F766E) else Color(0xFF64748B)
        )
    }
}
