package com.khamrnet.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.khamrnet.app.data.model.*
import com.khamrnet.app.sync.SyncState
import com.khamrnet.app.sync.SyncStatus
import com.khamrnet.app.ui.components.StoreActivationDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class MainAccountCategory(val label: String, val code: String) {
    CUSTOMERS("عملاء", "1201"),
    CASHIERS("كاشير", "1102"),
    TREASURY("صناديق", "1101"),
    INVENTORY("مخزون", "1301"),
    SALES_REVENUE("ايراد مبيعات", "401001"),
    EXPENSES("مصروفات", "501001"),
    OWNER_DRAWINGS("مسحوبات جاري المالك", "301001")
}

data class AnalyticalAccountItem(
    val id: String,
    val name: String,
    val code: String,
    val extra: String = ""
)

data class LedgerStatementRow(
    val id: String,
    val date: Long,
    val docType: String,
    val docNumber: String,
    val description: String,
    val debitAmount: Double,
    val creditAmount: Double,
    val runningBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingLedgerScreen(
    settings: SystemSettingsEntity,
    invoices: List<InvoiceEntity> = emptyList(),
    bonds: List<BondEntity> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    currentUserName: String = "المدير العام (نشوان)",
    syncStatus: SyncStatus = SyncStatus(),
    onTriggerSync: () -> Unit = {},
    onUpdateStoreCode: (String) -> Unit = {},
    onNavigate: (route: String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 } }
    val displayDateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.US) }
    val fullDateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // 1. نوع التقرير
    var selectedCategory by remember { mutableStateOf(MainAccountCategory.CUSTOMERS) }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    // 2. الحساب التحليلي
    var selectedAnalytical by remember {
        mutableStateOf(AnalyticalAccountItem(id = "ALL", name = "الكل (كافة الحسابات)", code = "ALL"))
    }
    var analyticalSearchText by remember { mutableStateOf("") }
    var isAnalyticalSearchModalOpen by remember { mutableStateOf(false) }

    // 3. الفترة من / إلى
    val calendar = remember { Calendar.getInstance() }
    val currentMonthStart = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.timeInMillis
    }
    var startDateMillis by remember { mutableStateOf(currentMonthStart) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // 4. العملة
    var selectedCurrencyCode by remember { mutableStateOf(settings.currencyName.ifEmpty { "YER" }) }
    var isCurrencyDropdownOpen by remember { mutableStateOf(false) }

    // Modal dialogs
    var isStatementPreviewOpen by remember { mutableStateOf(false) }
    var isStoreActivationOpen by remember { mutableStateOf(false) }

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

    // Currencies List
    val currencyRate = when (selectedCurrencyCode) {
        "SAR" -> 140.0
        "USD" -> 535.0
        else -> 1.0
    }

    // Generate analytical list based on selected category
    val allAnalyticalOptions = remember(selectedCategory, customers, currentUserName) {
        when (selectedCategory) {
            MainAccountCategory.CUSTOMERS -> {
                listOf(AnalyticalAccountItem("ALL", "كافة العملاء (إجمالي)", "ALL")) +
                        customers.map {
                            AnalyticalAccountItem(
                                id = it.id,
                                name = it.name,
                                code = it.code.ifEmpty { "CUST-${it.id.take(4)}" },
                                extra = it.phone
                            )
                        }
            }
            MainAccountCategory.CASHIERS -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة الكواشير (إجمالي)", "ALL"),
                    AnalyticalAccountItem("user_101", currentUserName, "101", "صندوق: CASH-101")
                )
            }
            MainAccountCategory.TREASURY -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة الصناديق والخزائن (إجمالي)", "ALL"),
                    AnalyticalAccountItem("BOX_MAIN", "الخزينة العامة الرئيسية", "1101", "الخزينة المركزية"),
                    AnalyticalAccountItem("BOX_POS1", "صندوق الكاشير رقم 1", "1102", "نقطة البيع الرئيسية")
                )
            }
            MainAccountCategory.INVENTORY -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة المخازن والمستودعات (إجمالي)", "ALL"),
                    AnalyticalAccountItem("WH_MAIN", "المستودع الرئيسي", "1301", "خمر - الفرع الرئيسي")
                )
            }
            MainAccountCategory.SALES_REVENUE -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة إيرادات المبيعات", "401001"),
                    AnalyticalAccountItem("REV_CARDS", "إيراد مبيعات كروت الشبكة والخدمات", "401001")
                )
            }
            MainAccountCategory.EXPENSES -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة المصروفات التشغيلية", "501001"),
                    AnalyticalAccountItem("EXP_RENT", "مصروف إيجار", "501002"),
                    AnalyticalAccountItem("EXP_SALARIES", "رواتب وأجور", "501003"),
                    AnalyticalAccountItem("EXP_UTILITIES", "كهرباء ومياه وإنترنت", "501004"),
                    AnalyticalAccountItem("EXP_MAINT", "صيانة ونظافة ونثريات", "501005")
                )
            }
            MainAccountCategory.OWNER_DRAWINGS -> {
                listOf(
                    AnalyticalAccountItem("ALL", "كافة مسحوبات جاري المالك", "301001"),
                    AnalyticalAccountItem("DRAWING_OWNER", "جاري المالك / الإدارة", "301001")
                )
            }
        }
    }

    // Filtered analytical options for search dialog
    val filteredAnalyticalOptions = remember(allAnalyticalOptions, analyticalSearchText) {
        if (analyticalSearchText.trim().isEmpty()) {
            allAnalyticalOptions
        } else {
            val query = analyticalSearchText.trim().lowercase()
            allAnalyticalOptions.filter {
                it.name.lowercase().contains(query) ||
                        it.code.lowercase().contains(query) ||
                        it.extra.lowercase().contains(query)
            }
        }
    }

    // Calculate statement entries & sums
    val calculatedReport = remember(
        selectedCategory,
        selectedAnalytical,
        startDateMillis,
        endDateMillis,
        invoices,
        bonds
    ) {
        val rawRows = mutableListOf<LedgerStatementRow>()

        when (selectedCategory) {
            MainAccountCategory.CUSTOMERS -> {
                // Customer sales invoices
                invoices.filter { !it.isCancelled }.forEach { inv ->
                    if (inv.date in startDateMillis..endDateMillis) {
                        if (selectedAnalytical.id == "ALL" || inv.customerId == selectedAnalytical.id) {
                            // Invoice creates debit on customer for remaining/credit or total
                            rawRows.add(
                                LedgerStatementRow(
                                    id = "inv_${inv.id}",
                                    date = inv.date,
                                    docType = "فاتورة",
                                    docNumber = inv.invoiceNumber,
                                    description = "مبيعات فاتورة - ${inv.customerName} (${inv.itemsCount} أصناف)",
                                    debitAmount = inv.totalAmount,
                                    creditAmount = 0.0,
                                    runningBalance = 0.0
                                )
                            )

                            // If paid in cash on invoice, creates immediate credit on customer
                            if (inv.paidAmount > 0) {
                                rawRows.add(
                                    LedgerStatementRow(
                                        id = "inv_paid_${inv.id}",
                                        date = inv.date,
                                        docType = "سداد فاتورة",
                                        docNumber = inv.invoiceNumber,
                                        description = "دفعة نقدية مسددة مع الفاتورة - ${inv.customerName}",
                                        debitAmount = 0.0,
                                        creditAmount = inv.paidAmount,
                                        runningBalance = 0.0
                                    )
                                )
                            }
                        }
                    }
                }

                // Customer bonds (Receipt & Payment)
                bonds.forEach { bond ->
                    if (bond.date in startDateMillis..endDateMillis) {
                        if (selectedAnalytical.id == "ALL" || bond.customerId == selectedAnalytical.id) {
                            if (bond.type == "RECEIPT") {
                                // Receipt from customer -> Credit customer
                                rawRows.add(
                                    LedgerStatementRow(
                                        id = "bond_${bond.id}",
                                        date = bond.date,
                                        docType = "سند قبض",
                                        docNumber = bond.bondNumber,
                                        description = "قبض دفعة نقدية - ${bond.notes.ifEmpty { "تحصيل حساب" }}",
                                        debitAmount = 0.0,
                                        creditAmount = bond.amount,
                                        runningBalance = 0.0
                                    )
                                )
                            } else {
                                // Payment to customer / refund -> Debit customer
                                rawRows.add(
                                    LedgerStatementRow(
                                        id = "bond_${bond.id}",
                                        date = bond.date,
                                        docType = "سند صرف",
                                        docNumber = bond.bondNumber,
                                        description = "صرف نقدي - ${bond.notes.ifEmpty { "صرف لحساب العميل" }}",
                                        debitAmount = bond.amount,
                                        creditAmount = 0.0,
                                        runningBalance = 0.0
                                    )
                                )
                            }
                        }
                    }
                }
            }

            MainAccountCategory.CASHIERS, MainAccountCategory.TREASURY -> {
                // Invoices cash collection
                invoices.filter { !it.isCancelled && it.paidAmount > 0 }.forEach { inv ->
                    if (inv.date in startDateMillis..endDateMillis) {
                        rawRows.add(
                            LedgerStatementRow(
                                id = "cash_inv_${inv.id}",
                                date = inv.date,
                                docType = "فاتورة نقدية",
                                docNumber = inv.invoiceNumber,
                                description = "مبيعات نقدية في الصندوق - ${inv.customerName}",
                                debitAmount = inv.paidAmount,
                                creditAmount = 0.0,
                                runningBalance = 0.0
                            )
                        )
                    }
                }

                // Bonds
                bonds.forEach { bond ->
                    if (bond.date in startDateMillis..endDateMillis) {
                        if (bond.type == "RECEIPT") {
                            rawRows.add(
                                LedgerStatementRow(
                                    id = "box_rec_${bond.id}",
                                    date = bond.date,
                                    docType = "سند قبض",
                                    docNumber = bond.bondNumber,
                                    description = "توريد نقدي للصندوق من ${bond.customerName}",
                                    debitAmount = bond.amount,
                                    creditAmount = 0.0,
                                    runningBalance = 0.0
                                )
                            )
                        } else {
                            rawRows.add(
                                LedgerStatementRow(
                                    id = "box_pay_${bond.id}",
                                    date = bond.date,
                                    docType = "سند صرف",
                                    docNumber = bond.bondNumber,
                                    description = "صرف نقدي من الصندوق لـ ${bond.customerName}",
                                    debitAmount = 0.0,
                                    creditAmount = bond.amount,
                                    runningBalance = 0.0
                                )
                            )
                        }
                    }
                }
            }

            MainAccountCategory.SALES_REVENUE -> {
                invoices.filter { !it.isCancelled }.forEach { inv ->
                    if (inv.date in startDateMillis..endDateMillis) {
                        rawRows.add(
                            LedgerStatementRow(
                                id = "rev_${inv.id}",
                                date = inv.date,
                                docType = "مبيعات",
                                docNumber = inv.invoiceNumber,
                                description = "إيراد مبيعات فاتورة #${inv.invoiceNumber}",
                                debitAmount = 0.0,
                                creditAmount = inv.totalAmount,
                                runningBalance = 0.0
                            )
                        )
                    }
                }
            }

            MainAccountCategory.EXPENSES, MainAccountCategory.OWNER_DRAWINGS -> {
                bonds.filter { it.type == "PAYMENT" }.forEach { bond ->
                    if (bond.date in startDateMillis..endDateMillis) {
                        rawRows.add(
                            LedgerStatementRow(
                                id = "exp_${bond.id}",
                                date = bond.date,
                                docType = if (selectedCategory == MainAccountCategory.EXPENSES) "سند صرف" else "مسحوبات",
                                docNumber = bond.bondNumber,
                                description = bond.notes.ifEmpty { "صرف نقدي عام" },
                                debitAmount = bond.amount,
                                creditAmount = 0.0,
                                runningBalance = 0.0
                            )
                        )
                    }
                }
            }

            MainAccountCategory.INVENTORY -> {
                invoices.filter { !it.isCancelled }.forEach { inv ->
                    if (inv.date in startDateMillis..endDateMillis) {
                        rawRows.add(
                            LedgerStatementRow(
                                id = "inv_stock_${inv.id}",
                                date = inv.date,
                                docType = "صرف مخزني",
                                docNumber = inv.invoiceNumber,
                                description = "خروج أصناف فاتورة #${inv.invoiceNumber}",
                                debitAmount = 0.0,
                                creditAmount = inv.totalAmount,
                                runningBalance = 0.0
                            )
                        )
                    }
                }
            }
        }

        // Sort by date ascending to calculate running balance
        rawRows.sortBy { it.date }

        var running = 0.0
        val computedRows = rawRows.map { row ->
            running += (row.debitAmount - row.creditAmount)
            row.copy(runningBalance = running)
        }

        val totalDebit = computedRows.sumOf { it.debitAmount }
        val totalCredit = computedRows.sumOf { it.creditAmount }
        val netBalance = totalDebit - totalCredit

        object {
            val rows = computedRows
            val debit = totalDebit
            val credit = totalCredit
            val balance = netBalance
            val count = computedRows.size
        }
    }

    Scaffold(
        bottomBar = {
            SettingsBottomNavBar(
                currentRoute = "ledger",
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
            // 1. TOP STATUS BAR & HEADER (Exact replica of Web & App Standard)
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
                                isStoreActivationOpen = true
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
                            .clickable { isStoreActivationOpen = true }
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

            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =========================================================================
                // 2. CARD 1: ERP FILTER CONTROLS (Exact layout from Screenshot)
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. نوع التقرير
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selector Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .clickable { isCategoryDropdownOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedCategory.label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isCategoryDropdownOpen,
                                    onDismissRequest = { isCategoryDropdownOpen = false }
                                ) {
                                    MainAccountCategory.values().forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    cat.label,
                                                    fontWeight = if (selectedCategory == cat) FontWeight.Black else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                            },
                                            onClick = {
                                                selectedCategory = cat
                                                selectedAnalytical = AnalyticalAccountItem(
                                                    id = "ALL",
                                                    name = "الكل (كافة الحسابات)",
                                                    code = "ALL"
                                                )
                                                isCategoryDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "نوع التقرير:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // 2. الحساب التحليلي
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .clickable {
                                        analyticalSearchText = ""
                                        isAnalyticalSearchModalOpen = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (selectedAnalytical.id == "ALL") "بحث أو اختر الحساب..." else selectedAnalytical.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedAnalytical.id == "ALL") FontWeight.Normal else FontWeight.Bold,
                                        color = if (selectedAnalytical.id == "ALL") Color(0xFF94A3B8) else Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "الحساب التحليلي:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // 3. التواريخ من / إلى
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // من تاريخ
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = displayDateFormat.format(Date(startDateMillis)),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            Text("من:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                            // إلى تاريخ
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = displayDateFormat.format(Date(endDateMillis)),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            Text("إلى:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        }

                        // 4. العملة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                    .clickable { isCurrencyDropdownOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    Text(
                                        text = when (selectedCurrencyCode) {
                                            "SAR" -> "SAR (ريال سعودي)"
                                            "USD" -> "USD (دولار أمريكي)"
                                            else -> "YER (ريال يمني)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isCurrencyDropdownOpen,
                                    onDismissRequest = { isCurrencyDropdownOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("YER (ريال يمني)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { selectedCurrencyCode = "YER"; isCurrencyDropdownOpen = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("SAR (ريال سعودي)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { selectedCurrencyCode = "SAR"; isCurrencyDropdownOpen = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("USD (دولار أمريكي)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { selectedCurrencyCode = "USD"; isCurrencyDropdownOpen = false }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text("العملة:", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                        }

                        // 5. Action Buttons (عرض التقرير + طباعة PDF + زر المزامنة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // زر إعادة التحميل والمزامنة
                            IconButton(
                                onClick = {
                                    onTriggerSync()
                                    Toast.makeText(context, "تم تحديث ومطابقة قيود الحسابات", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                            }

                            // زر طباعة PDF
                            Button(
                                onClick = {
                                    isStatementPreviewOpen = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // زر عرض التقرير
                            Button(
                                onClick = {
                                    isStatementPreviewOpen = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عرض التقرير", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. CARD 2: ERP DIRECT SUMMARY DETAILS (Exact replica of Screenshot)
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top row: نوع الحساب
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedCategory.label}${if (selectedAnalytical.id != "ALL") " - ${selectedAnalytical.name}" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نوع الحساب:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64748B)
                            )
                        }

                        Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

                        // 3 Grid Box Metrics in 1 Row: [الرصيد النهائي] [مدين] [دائن]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. الرصيد النهائي (Left Box)
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "الرصيد النهائي",
                                value = "${numberFormat.format(Math.abs(calculatedReport.balance))}",
                                subLabel = "(${if (calculatedReport.balance >= 0) "مدين" else "دائن"})",
                                valueColor = Color(0xFF0F172A)
                            )

                            // 2. مدين (Center Box)
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "مدين",
                                value = numberFormat.format(calculatedReport.debit),
                                subLabel = null,
                                valueColor = Color(0xFF047857)
                            )

                            // 3. دائن (Right Box)
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "دائن",
                                value = numberFormat.format(calculatedReport.credit),
                                subLabel = null,
                                valueColor = Color(0xFF1D4ED8)
                            )
                        }

                        // Bottom Info: عملة الحساب، سعر الصرف، عدد الحركات
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.6.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سعر الصرف (cur_rate) : $currencyRate",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$selectedCurrencyCode (${when (selectedCurrencyCode) { "SAR" -> "ريال سعودي"; "USD" -> "دولار أمريكي"; else -> "ريال يمني" }})",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "عملة الحساب:",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${calculatedReport.count}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "عدد الحركات :",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAL 1: SEARCH & SELECT ANALYTICAL ACCOUNT
    // =========================================================================
    if (isAnalyticalSearchModalOpen) {
        Dialog(
            onDismissRequest = { isAnalyticalSearchModalOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 520.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختيار الحساب التحليلي (${selectedCategory.label})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { isAnalyticalSearchModalOpen = false }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = analyticalSearchText,
                        onValueChange = { analyticalSearchText = it },
                        placeholder = { Text("بحث بالاسم، الكود أو الهاتف...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Options List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredAnalyticalOptions, key = { it.id }) { opt ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedAnalytical.id == opt.id) Color(0xFFF0FDFA) else Color(0xFFF8FAFC)
                                    )
                                    .border(
                                        0.8.dp,
                                        if (selectedAnalytical.id == opt.id) Color(0xFF99F6E4) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedAnalytical = opt
                                        isAnalyticalSearchModalOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedAnalytical.id == opt.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = opt.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "كود: ${opt.code}${if (opt.extra.isNotEmpty()) " | ${opt.extra}" else ""}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
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
    // MODAL 2: FULL STATEMENT / REPORT VIEW (عرض وطباعة كشف الحساب)
    // =========================================================================
    if (isStatementPreviewOpen) {
        Dialog(
            onDismissRequest = { isStatementPreviewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .padding(6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // WhatsApp Share button
                            IconButton(
                                onClick = {
                                    val msg = buildString {
                                        appendLine("📊 *${settings.businessName}*")
                                        appendLine("*تقرير كشف حساب تحليلي*")
                                        appendLine("-------------------------")
                                        appendLine("نوع التقرير: ${selectedCategory.label}")
                                        appendLine("الحساب: ${selectedAnalytical.name}")
                                        appendLine("دائن: ${numberFormat.format(calculatedReport.credit)} $selectedCurrencyCode")
                                        appendLine("مدين: ${numberFormat.format(calculatedReport.debit)} $selectedCurrencyCode")
                                        appendLine("الرصيد النهائي: ${numberFormat.format(Math.abs(calculatedReport.balance))} $selectedCurrencyCode (${if (calculatedReport.balance >= 0) "مدين" else "دائن"})")
                                        appendLine("عدد الحركات: ${calculatedReport.count}")
                                        appendLine("المستخدم: $currentUserName")
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/?text=${Uri.encode(msg)}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "واتساب غير مثبت", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "واتساب", tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            }

                            // Print Button
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "🖨️ جاري إرسال التقرير للطباعة...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "كشف حساب تحليلي (${selectedCategory.label})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            IconButton(onClick = { isStatementPreviewOpen = false }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                            }
                        }
                    }

                    // Content Scrollable Table
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Statement Header Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(settings.businessName, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text(
                                    "الحساب: ${selectedAnalytical.name} • كود: ${selectedAnalytical.code}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                )
                                Text(
                                    "الفترة: ${displayDateFormat.format(Date(startDateMillis))} إلى ${displayDateFormat.format(Date(endDateMillis))} • العملة: $selectedCurrencyCode",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Summary Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "الرصيد: ${numberFormat.format(Math.abs(calculatedReport.balance))} (${if (calculatedReport.balance >= 0) "مدين" else "دائن"})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text("مدين: ${numberFormat.format(calculatedReport.debit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Text("دائن: ${numberFormat.format(calculatedReport.credit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }

                        // Table Rows
                        if (calculatedReport.rows.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد حركات مسجلة لهذا الحساب خلال الفترة المحددة", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        } else {
                            calculatedReport.rows.forEachIndexed { index, row ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .border(0.6.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${fullDateFormat.format(Date(row.date))}",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "${row.docType} #${row.docNumber}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE2E8F0)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${index + 1}", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Text(
                                            text = row.description,
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF475569),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Divider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الرصيد: ${numberFormat.format(Math.abs(row.runningBalance))} (${if (row.runningBalance >= 0) "مدين" else "دائن"})",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (row.creditAmount > 0) {
                                                    Text(
                                                        text = "دائن: ${numberFormat.format(row.creditAmount)}",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1D4ED8)
                                                    )
                                                }
                                                if (row.debitAmount > 0) {
                                                    Text(
                                                        text = "مدين: ${numberFormat.format(row.debitAmount)}",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Dismiss
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = { isStatementPreviewOpen = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal 3: Store Activation
    StoreActivationDialog(
        currentStoreCode = settings.storeCode,
        businessName = settings.businessName,
        isOpen = isStoreActivationOpen,
        onDismiss = { isStoreActivationOpen = false },
        onConfirmActivation = { newCode ->
            isStoreActivationOpen = false
            onUpdateStoreCode(newCode)
        }
    )
}

// =========================================================================
// HELPER STAT BOX COMPONENT (Matching screenshot 3 boxes)
// =========================================================================

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subLabel: String? = null,
    valueColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            if (subLabel != null) {
                Text(
                    text = subLabel,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
