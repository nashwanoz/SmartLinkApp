package com.khamrnet.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khamrnet.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("khamrnet_users_pref", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    // --- State Persistence Helpers ---
    fun loadUsers(): List<AppUser> {
        val json = sharedPrefs.getString("app_users_list", null)
        return if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<AppUser>>() {}.type
                gson.fromJson(json, type) ?: UserDefaults.DEFAULT_USERS
            } catch (e: Exception) {
                UserDefaults.DEFAULT_USERS
            }
        } else {
            UserDefaults.DEFAULT_USERS
        }
    }

    fun saveUsers(list: List<AppUser>) {
        try {
            sharedPrefs.edit().putString("app_users_list", gson.toJson(list)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadDrawers(): List<CashDrawer> {
        val json = sharedPrefs.getString("app_drawers_list", null)
        return if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<CashDrawer>>() {}.type
                gson.fromJson(json, type) ?: UserDefaults.DEFAULT_CASH_DRAWERS
            } catch (e: Exception) {
                UserDefaults.DEFAULT_CASH_DRAWERS
            }
        } else {
            UserDefaults.DEFAULT_CASH_DRAWERS
        }
    }

    fun saveDrawers(list: List<CashDrawer>) {
        try {
            sharedPrefs.edit().putString("app_drawers_list", gson.toJson(list)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadWarehouses(): List<Warehouse> {
        val json = sharedPrefs.getString("app_warehouses_list", null)
        return if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Warehouse>>() {}.type
                gson.fromJson(json, type) ?: UserDefaults.DEFAULT_WAREHOUSES
            } catch (e: Exception) {
                UserDefaults.DEFAULT_WAREHOUSES
            }
        } else {
            UserDefaults.DEFAULT_WAREHOUSES
        }
    }

    fun saveWarehouses(list: List<Warehouse>) {
        try {
            sharedPrefs.edit().putString("app_warehouses_list", gson.toJson(list)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Main State ---
    var users by remember { mutableStateOf(loadUsers()) }
    var drawers by remember { mutableStateOf(loadDrawers()) }
    var warehouses by remember { mutableStateOf(loadWarehouses()) }

    var activeTab by remember { mutableStateOf("users") } // "users", "drawers", "warehouses"

    // Permissions Screen State
    var selectedUserForPermissions by remember { mutableStateOf<AppUser?>(null) }

    // User Dialog State
    var isUserModalOpen by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<AppUser?>(null) }
    var formUserCode by remember { mutableStateOf("") }
    var formUserName by remember { mutableStateOf("") }
    var formUserLogin by remember { mutableStateOf("") }
    var formUserRole by remember { mutableStateOf("CASHIER") }
    var formUserPin by remember { mutableStateOf("1234") }
    var formUserDrawerCode by remember { mutableStateOf("") }
    var formUserWarehouseCode by remember { mutableStateOf("") }

    // Drawer Dialog State
    var isDrawerModalOpen by remember { mutableStateOf(false) }
    var editingDrawer by remember { mutableStateOf<CashDrawer?>(null) }
    var formDrawerCode by remember { mutableStateOf("") }
    var formDrawerName by remember { mutableStateOf("") }
    var formDrawerIsMain by remember { mutableStateOf(false) }
    var formDrawerUserId by remember { mutableStateOf("") }
    var formDrawerNotes by remember { mutableStateOf("") }

    // Warehouse Dialog State
    var isWarehouseModalOpen by remember { mutableStateOf(false) }
    var editingWarehouse by remember { mutableStateOf<Warehouse?>(null) }
    var formWarehouseCode by remember { mutableStateOf("") }
    var formWarehouseName by remember { mutableStateOf("") }
    var formWarehouseIsMain by remember { mutableStateOf(false) }
    var formWarehouseLocation by remember { mutableStateOf("") }
    var formWarehouseUserId by remember { mutableStateOf("") }
    var formWarehouseNotes by remember { mutableStateOf("") }

    // Next code helper
    fun getNextUserCode(): String {
        if (users.isEmpty()) return "101"
        val codes = users.mapNotNull { it.userCode.toIntOrNull() }
        if (codes.isEmpty()) return "101"
        return (codes.maxOrNull()!! + 1).toString()
    }

    fun getNextDrawerCode(): String {
        val nextNum = drawers.size + 1
        return "CASH-${100 + nextNum}"
    }

    fun getNextWarehouseCode(): String {
        val nextNum = warehouses.size + 1
        return "WH-0$nextNum"
    }

    // Modal openers
    fun openAddUserModal() {
        val nextCode = getNextUserCode()
        editingUser = null
        formUserCode = nextCode
        formUserName = ""
        formUserLogin = "user_$nextCode"
        formUserRole = "CASHIER"
        formUserPin = nextCode
        formUserDrawerCode = drawers.firstOrNull { it.code.startsWith("CASH-") }?.code ?: drawers.firstOrNull()?.code ?: "CASH-$nextCode"
        formUserWarehouseCode = warehouses.firstOrNull()?.code ?: "WH-01"
        isUserModalOpen = true
    }

    fun openEditUserModal(u: AppUser) {
        editingUser = u
        formUserCode = u.userCode
        formUserName = u.name
        formUserLogin = u.username
        formUserRole = u.role
        formUserPin = u.pin
        formUserDrawerCode = u.assignedDrawerCode.ifEmpty { drawers.firstOrNull()?.code ?: "BOX-001" }
        formUserWarehouseCode = u.assignedWarehouseCode.ifEmpty { warehouses.firstOrNull()?.code ?: "WH-01" }
        isUserModalOpen = true
    }

    fun openAddDrawerModal() {
        editingDrawer = null
        formDrawerCode = getNextDrawerCode()
        formDrawerName = "صندوق كاشير ${drawers.size + 1}"
        formDrawerIsMain = false
        formDrawerUserId = users.firstOrNull()?.id ?: ""
        formDrawerNotes = ""
        isDrawerModalOpen = true
    }

    fun openEditDrawerModal(d: CashDrawer) {
        editingDrawer = d
        formDrawerCode = d.code
        formDrawerName = d.name
        formDrawerIsMain = d.isMain
        formDrawerUserId = d.assignedUserId ?: ""
        formDrawerNotes = d.notes ?: ""
        isDrawerModalOpen = true
    }

    fun openAddWarehouseModal() {
        editingWarehouse = null
        formWarehouseCode = getNextWarehouseCode()
        formWarehouseName = "مخزن فرعي ${warehouses.size + 1}"
        formWarehouseIsMain = false
        formWarehouseLocation = ""
        formWarehouseUserId = users.firstOrNull()?.id ?: ""
        formWarehouseNotes = ""
        isWarehouseModalOpen = true
    }

    fun openEditWarehouseModal(w: Warehouse) {
        editingWarehouse = w
        formWarehouseCode = w.code
        formWarehouseName = w.name
        formWarehouseIsMain = w.isMain
        formWarehouseLocation = w.location ?: ""
        formWarehouseUserId = w.assignedUserId ?: ""
        formWarehouseNotes = w.notes ?: ""
        isWarehouseModalOpen = true
    }

    // --- Sub-Screen: Permissions View ---
    if (selectedUserForPermissions != null) {
        val targetUser = selectedUserForPermissions!!
        PermissionsView(
            targetUser = targetUser,
            onBack = { selectedUserForPermissions = null },
            onSavePermissions = { updatedPerms ->
                val updatedList = users.map { u ->
                    if (u.id == targetUser.id) u.copy(permissions = updatedPerms) else u
                }
                users = updatedList
                saveUsers(updatedList)
                Toast.makeText(context, "✅ تم حفظ صلاحيات [${targetUser.name}] بنجاح", Toast.LENGTH_SHORT).show()
                selectedUserForPermissions = null
            }
        )
        return
    }

    // --- Main Screen ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "هيكلة المستخدمين والصناديق والمخازن",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "إنشاء الكواشير وتحديد الصلاحيات وربط الصندوق والمخزن المعتمد",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    // Quick add button corresponding to active tab
                    Button(
                        onClick = {
                            when (activeTab) {
                                "users" -> openAddUserModal()
                                "drawers" -> openAddDrawerModal()
                                "warehouses" -> openAddWarehouseModal()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (activeTab) {
                                "users" -> Color(0xFF4F46E5)
                                "drawers" -> Color(0xFF059669)
                                else -> Color(0xFFD97706)
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when (activeTab) {
                                "users" -> "مستخدم جديد"
                                "drawers" -> "إنشاء صندوق"
                                else -> "إنشاء مخزن"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Tab Navigation Header (Users, Drawers, Warehouses) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab 1: Users
                TabPillButton(
                    title = "المستخدمين والكواشير",
                    count = users.size,
                    isSelected = activeTab == "users",
                    icon = Icons.Default.People,
                    selectedColor = Color(0xFF4F46E5),
                    onClick = { activeTab = "users" },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Drawers
                TabPillButton(
                    title = "جدول الصناديق",
                    count = drawers.size,
                    isSelected = activeTab == "drawers",
                    icon = Icons.Default.PointOfSale,
                    selectedColor = Color(0xFF059669),
                    onClick = { activeTab = "drawers" },
                    modifier = Modifier.weight(1f)
                )

                // Tab 3: Warehouses
                TabPillButton(
                    title = "جدول المخازن",
                    count = warehouses.size,
                    isSelected = activeTab == "warehouses",
                    icon = Icons.Default.Storefront,
                    selectedColor = Color(0xFFD97706),
                    onClick = { activeTab = "warehouses" },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // --- Tab Content ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (activeTab) {
                    "users" -> {
                        items(users, key = { it.id }) { user ->
                            UserCardItem(
                                user = user,
                                isCurrentUser = user.name == currentUserName,
                                onOpenPermissions = { selectedUserForPermissions = user },
                                onEdit = { openEditUserModal(user) },
                                onDelete = {
                                    if (user.role == "ADMIN" && users.count { it.role == "ADMIN" } <= 1) {
                                        Toast.makeText(context, "⚠️ لا يمكن حذف المدير العام الوحيد للنظام", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val updated = users.filter { it.id != user.id }
                                        users = updated
                                        saveUsers(updated)
                                        Toast.makeText(context, "تم حذف المستخدم ${user.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                    "drawers" -> {
                        items(drawers, key = { it.id }) { drawer ->
                            DrawerCardItem(
                                drawer = drawer,
                                onEdit = { openEditDrawerModal(drawer) },
                                onDelete = {
                                    if (drawer.isMain) {
                                        Toast.makeText(context, "⚠️ لا يمكن حذف الصندوق الرئيسي (الخزينة العامة)", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val updated = drawers.filter { it.id != drawer.id }
                                        drawers = updated
                                        saveDrawers(updated)
                                        Toast.makeText(context, "تم حذف الصندوق ${drawer.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                    "warehouses" -> {
                        items(warehouses, key = { it.id }) { warehouse ->
                            WarehouseCardItem(
                                warehouse = warehouse,
                                onEdit = { openEditWarehouseModal(warehouse) },
                                onDelete = {
                                    if (warehouse.isMain) {
                                        Toast.makeText(context, "⚠️ لا يمكن حذف المستودع العام الرئيسي", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val updated = warehouses.filter { it.id != warehouse.id }
                                        warehouses = updated
                                        saveWarehouses(updated)
                                        Toast.makeText(context, "تم حذف المخزن ${warehouse.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODALS / DIALOGS
    // =========================================================================

    // --- 1. USER MODAL (Add/Edit Cashier with Drawer & Warehouse Selector) ---
    if (isUserModalOpen) {
        var drawerExpanded by remember { mutableStateOf(false) }
        var warehouseExpanded by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        val selectedDrawer = drawers.find { it.code == formUserDrawerCode } ?: drawers.firstOrNull()
        val selectedWarehouse = warehouses.find { it.code == formUserWarehouseCode } ?: warehouses.firstOrNull()

        AlertDialog(
            onDismissRequest = { isUserModalOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (editingUser != null) Icons.Default.Edit else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (editingUser != null) "تعديل بيانات المستخدم والربط" else "إضافة مستخدم وربطه بصندوق ومخزن",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                errorMessage,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // User Code & Name
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formUserCode,
                            onValueChange = { formUserCode = it },
                            label = { Text("كود المستخدم") },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formUserName,
                            onValueChange = { formUserName = it },
                            label = { Text("الاسم الكامل *") },
                            modifier = Modifier.weight(0.65f),
                            singleLine = true
                        )
                    }

                    // Username & PIN
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formUserLogin,
                            onValueChange = { formUserLogin = it },
                            label = { Text("اسم الدخول Login") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formUserPin,
                            onValueChange = { formUserPin = it },
                            label = { Text("رمز PIN السريع") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Role Selection
                    Text("الدور / الرتبة في النظام:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (formUserRole == "CASHIER") Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (formUserRole == "CASHIER") Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { formUserRole = "CASHIER" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (formUserRole == "CASHIER") Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "كاشير مبيعات",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (formUserRole == "CASHIER") Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (formUserRole == "ADMIN") Color(0xFFFAF5FF) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (formUserRole == "ADMIN") Color(0xFF9333EA) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { formUserRole = "ADMIN" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (formUserRole == "ADMIN") Color(0xFF9333EA) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "مدير عام للنظام",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (formUserRole == "ADMIN") Color(0xFF9333EA) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // --- ASSIGNED CASH DRAWER SELECTION (صندوق المستخدم) ---
                    Text("صندوق المستخدم (Cash Drawer):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    ExposedDropdownMenuBox(
                        expanded = drawerExpanded,
                        onExpandedChange = { drawerExpanded = !drawerExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDrawer?.let { "[${it.code}] ${it.name}" } ?: "اختر صندوقاً",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = drawerExpanded) },
                            leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF059669)) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = drawerExpanded,
                            onDismissRequest = { drawerExpanded = false }
                        ) {
                            drawers.forEach { drawer ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "[${drawer.code}] ${drawer.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (drawer.isMain) {
                                                Text("خزينة مركزية رئيسية", fontSize = 10.sp, color = Color(0xFF059669))
                                            }
                                        }
                                    },
                                    onClick = {
                                        formUserDrawerCode = drawer.code
                                        drawerExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF059669))
                                    }
                                )
                            }
                        }
                    }

                    // --- ASSIGNED WAREHOUSE SELECTION (مخزن المستخدم) ---
                    Text("مخزن المستخدم (Assigned Warehouse):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    ExposedDropdownMenuBox(
                        expanded = warehouseExpanded,
                        onExpandedChange = { warehouseExpanded = !warehouseExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWarehouse?.let { "[${it.code}] ${it.name}" } ?: "اختر مخزناً",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseExpanded) },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFD97706)) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = warehouseExpanded,
                            onDismissRequest = { warehouseExpanded = false }
                        ) {
                            warehouses.forEach { warehouse ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "[${warehouse.code}] ${warehouse.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (!warehouse.location.isNullOrEmpty()) {
                                                Text(warehouse.location, fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    },
                                    onClick = {
                                        formUserWarehouseCode = warehouse.code
                                        warehouseExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.HomeWork, contentDescription = null, tint = Color(0xFFD97706))
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = formUserName.trim()
                        if (trimmedName.isEmpty()) {
                            errorMessage = "يرجى كتابة اسم المستخدم الكامل"
                            return@Button
                        }

                        val drawer = drawers.find { it.code == formUserDrawerCode } ?: drawers.firstOrNull()
                        val warehouse = warehouses.find { it.code == formUserWarehouseCode } ?: warehouses.firstOrNull()

                        val newOrUpdatedUser = AppUser(
                            id = editingUser?.id ?: "user_${System.currentTimeMillis()}",
                            userCode = formUserCode.trim().ifEmpty { getNextUserCode() },
                            name = trimmedName,
                            username = formUserLogin.trim().ifEmpty { "user_${formUserCode}" },
                            role = formUserRole,
                            pin = formUserPin.trim().ifEmpty { "1234" },
                            active = true,
                            assignedDrawerCode = drawer?.code ?: "BOX-001",
                            assignedDrawerName = drawer?.name ?: "الصندوق الرئيسي",
                            assignedWarehouseCode = warehouse?.code ?: "WH-01",
                            assignedWarehouseName = warehouse?.name ?: "المستودع الرئيسي",
                            permissions = editingUser?.permissions ?: if (formUserRole == "ADMIN") UserDefaults.DEFAULT_ADMIN_PERMISSIONS else UserDefaults.DEFAULT_CASHIER_PERMISSIONS
                        )

                        val updatedList = if (editingUser != null) {
                            users.map { if (it.id == editingUser!!.id) newOrUpdatedUser else it }
                        } else {
                            users + newOrUpdatedUser
                        }

                        users = updatedList
                        saveUsers(updatedList)
                        Toast.makeText(context, "✅ تم حفظ بيانات المستخدم [${newOrUpdatedUser.name}]", Toast.LENGTH_SHORT).show()
                        isUserModalOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("حفظ المستخدم", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isUserModalOpen = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }

    // --- 2. CASH DRAWER MODAL ---
    if (isDrawerModalOpen) {
        var userSelectorExpanded by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        val assignedUser = users.find { it.id == formDrawerUserId }

        AlertDialog(
            onDismissRequest = { isDrawerModalOpen = false },
            title = {
                Text(
                    if (editingDrawer != null) "تعديل بيانات صندوق الكاشير" else "إنشاء صندوق كاشير جديد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formDrawerCode,
                            onValueChange = { formDrawerCode = it },
                            label = { Text("كود الصندوق") },
                            modifier = Modifier.weight(0.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formDrawerName,
                            onValueChange = { formDrawerName = it },
                            label = { Text("اسم الصندوق *") },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                    }

                    // Assigned User
                    Text("المستخدم / الكاشير المسؤول:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = userSelectorExpanded,
                        onExpandedChange = { userSelectorExpanded = !userSelectorExpanded }
                    ) {
                        OutlinedTextField(
                            value = assignedUser?.name ?: "غير محدد",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userSelectorExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = userSelectorExpanded,
                            onDismissRequest = { userSelectorExpanded = false }
                        ) {
                            users.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.role})") },
                                    onClick = {
                                        formDrawerUserId = u.id
                                        userSelectorExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formDrawerNotes,
                        onValueChange = { formDrawerNotes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = formDrawerName.trim()
                        if (name.isEmpty()) {
                            errorMessage = "يرجى إدخال اسم الصندوق"
                            return@Button
                        }
                        val assigned = users.find { it.id == formDrawerUserId }
                        val item = CashDrawer(
                            id = editingDrawer?.id ?: "drawer_${System.currentTimeMillis()}",
                            code = formDrawerCode.trim().ifEmpty { getNextDrawerCode() },
                            name = name,
                            isMain = formDrawerIsMain,
                            assignedUserId = assigned?.id,
                            assignedUserName = assigned?.name,
                            notes = formDrawerNotes.trim().ifEmpty { null }
                        )
                        val updated = if (editingDrawer != null) {
                            drawers.map { if (it.id == editingDrawer!!.id) item else it }
                        } else {
                            drawers + item
                        }
                        drawers = updated
                        saveDrawers(updated)
                        Toast.makeText(context, "✅ تم حفظ الصندوق بنجاح", Toast.LENGTH_SHORT).show()
                        isDrawerModalOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("حفظ الصندوق", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDrawerModalOpen = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- 3. WAREHOUSE MODAL ---
    if (isWarehouseModalOpen) {
        var userSelectorExpanded by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        val assignedUser = users.find { it.id == formWarehouseUserId }

        AlertDialog(
            onDismissRequest = { isWarehouseModalOpen = false },
            title = {
                Text(
                    if (editingWarehouse != null) "تعديل بيانات المخزن" else "إنشاء مخزن / مستودع جديد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formWarehouseCode,
                            onValueChange = { formWarehouseCode = it },
                            label = { Text("كود المخزن") },
                            modifier = Modifier.weight(0.4f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formWarehouseName,
                            onValueChange = { formWarehouseName = it },
                            label = { Text("اسم المخزن *") },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = formWarehouseLocation,
                        onValueChange = { formWarehouseLocation = it },
                        label = { Text("موقع المخزن / الفرع") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Assigned User
                    Text("المسؤول عن المخزن:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = userSelectorExpanded,
                        onExpandedChange = { userSelectorExpanded = !userSelectorExpanded }
                    ) {
                        OutlinedTextField(
                            value = assignedUser?.name ?: "غير محدد",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userSelectorExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = userSelectorExpanded,
                            onDismissRequest = { userSelectorExpanded = false }
                        ) {
                            users.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.role})") },
                                    onClick = {
                                        formWarehouseUserId = u.id
                                        userSelectorExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formWarehouseNotes,
                        onValueChange = { formWarehouseNotes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = formWarehouseName.trim()
                        if (name.isEmpty()) {
                            errorMessage = "يرجى إدخال اسم المخزن"
                            return@Button
                        }
                        val assigned = users.find { it.id == formWarehouseUserId }
                        val item = Warehouse(
                            id = editingWarehouse?.id ?: "wh_${System.currentTimeMillis()}",
                            code = formWarehouseCode.trim().ifEmpty { getNextWarehouseCode() },
                            name = name,
                            isMain = formWarehouseIsMain,
                            location = formWarehouseLocation.trim().ifEmpty { null },
                            assignedUserId = assigned?.id,
                            assignedUserName = assigned?.name,
                            notes = formWarehouseNotes.trim().ifEmpty { null }
                        )
                        val updated = if (editingWarehouse != null) {
                            warehouses.map { if (it.id == editingWarehouse!!.id) item else it }
                        } else {
                            warehouses + item
                        }
                        warehouses = updated
                        saveWarehouses(updated)
                        Toast.makeText(context, "✅ تم حفظ المخزن بنجاح", Toast.LENGTH_SHORT).show()
                        isWarehouseModalOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("حفظ المخزن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isWarehouseModalOpen = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// =========================================================================
// TAB PILL BUTTON
// =========================================================================
@Composable
private fun TabPillButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    icon: ImageVector,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) selectedColor else Color(0xFFE2E8F0)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else Color(0xFF475569),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                "($count)",
                fontSize = 10.sp,
                color = if (isSelected) selectedColor else Color(0xFF94A3B8)
            )
        }
    }
}

// =========================================================================
// USER CARD ITEM
// =========================================================================
@Composable
private fun UserCardItem(
    user: AppUser,
    isCurrentUser: Boolean,
    onOpenPermissions: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Code, Name, Role badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Code badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "كود: ${user.userCode}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            user.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "اسم الدخول: ${user.username} • PIN: ${user.pin}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Role badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (user.role == "ADMIN") Color(0xFFEDE9FE) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        if (user.role == "ADMIN") "مدير عام" else "كاشير مبيعات",
                        color = if (user.role == "ADMIN") Color(0xFF6D28D9) else Color(0xFF334155),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Grid: Assigned Drawer & Assigned Warehouse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drawer Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("صندوق المستخدم:", fontSize = 9.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            Text(
                                user.assignedDrawerName.ifEmpty { user.assignedDrawerCode },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Warehouse Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("مخزن المستخدم:", fontSize = 9.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            Text(
                                user.assignedWarehouseName.ifEmpty { user.assignedWarehouseCode },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Actions Footer: Permissions Button, Edit Button, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Permissions button (Primary CTA)
                Button(
                    onClick = onOpenPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الصلاحيات 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Edit button
                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 11.sp, color = Color(0xFF475569))
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// DRAWER CARD ITEM
// =========================================================================
@Composable
private fun DrawerCardItem(
    drawer: CashDrawer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            drawer.code,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column {
                        Text(drawer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        if (!drawer.assignedUserName.isNullOrEmpty()) {
                            Text("المسؤول: ${drawer.assignedUserName}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (drawer.isMain) Color(0xFFECFDF5) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        if (drawer.isMain) "خزينة رئيسية" else "صندوق كاشير",
                        color = if (drawer.isMain) Color(0xFF059669) else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (!drawer.notes.isNullOrEmpty()) {
                Text("ملاحظات: ${drawer.notes}", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 11.sp)
                }
                if (!drawer.isMain) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// WAREHOUSE CARD ITEM
// =========================================================================
@Composable
private fun WarehouseCardItem(
    warehouse: Warehouse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            warehouse.code,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column {
                        Text(warehouse.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        if (!warehouse.location.isNullOrEmpty()) {
                            Text("الموقع: ${warehouse.location}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (warehouse.isMain) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        if (warehouse.isMain) "مستودع رئيسي" else "مخزن فرعي",
                        color = if (warehouse.isMain) Color(0xFFB45309) else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (!warehouse.assignedUserName.isNullOrEmpty()) {
                Text("المسؤول: ${warehouse.assignedUserName}", fontSize = 11.sp, color = Color(0xFF64748B))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 11.sp)
                }
                if (!warehouse.isMain) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// FULL PERMISSIONS SCREEN / VIEW
// =========================================================================
data class PermissionItemDefinition(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val isAdminOnly: Boolean = false,
    val getValue: (UserPermissions) -> Boolean,
    val setValue: (UserPermissions, Boolean) -> UserPermissions
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsView(
    targetUser: AppUser,
    onBack: () -> Unit,
    onSavePermissions: (UserPermissions) -> Unit
) {
    var permissions by remember { mutableStateOf(targetUser.permissions) }

    val permissionItems = remember {
        listOf(
            PermissionItemDefinition(
                title = "نقطة البيع (POS)",
                description = "إصدار فواتير المبيعات نقدية أو آجلة وسداد جزئي والطباعة",
                icon = Icons.Default.ShoppingCart,
                iconTint = Color(0xFF0F766E),
                getValue = { it.canAccessPos },
                setValue = { p, v -> p.copy(canAccessPos = v) }
            ),
            PermissionItemDefinition(
                title = "السماح بالبيع بالسالب (تجاوز الرصيد)",
                description = "صلاحية بيع الأصناف حتى لو كان الرصيد غير كافٍ أو صفر في عهدة الكاشير (خاص بالإدارة)",
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFD97706),
                isAdminOnly = true,
                getValue = { it.canSellNegativeStock },
                setValue = { p, v -> p.copy(canSellNegativeStock = v) }
            ),
            PermissionItemDefinition(
                title = "الأصناف والمخزون",
                description = "عرض وتصفح وإدارة الأصناف والأسعار ووحدات الكراتين والحبات",
                icon = Icons.Default.Inventory,
                iconTint = Color(0xFF7C3AED),
                getValue = { it.canAccessProducts },
                setValue = { p, v -> p.copy(canAccessProducts = v) }
            ),
            PermissionItemDefinition(
                title = "دليل وبيانات العملاء",
                description = "عرض العملاء والبحث وإضافة عملاء جدد وأرقام هواتفهم",
                icon = Icons.Default.People,
                iconTint = Color(0xFF2563EB),
                getValue = { it.canAccessCustomers },
                setValue = { p, v -> p.copy(canAccessCustomers = v) }
            ),
            PermissionItemDefinition(
                title = "إدخال وتعديل الرصيد الافتتاحي للعملاء",
                description = "صلاحية حساسة: السماح بوضع أو تعديل الرصيد الافتتاحي للعميل (خاص بالإدارة)",
                icon = Icons.Default.AttachMoney,
                iconTint = Color(0xFFDC2626),
                isAdminOnly = true,
                getValue = { it.canSetOpeningBalance },
                setValue = { p, v -> p.copy(canSetOpeningBalance = v) }
            ),
            PermissionItemDefinition(
                title = "إظهار سجل الفواتير للمستخدم (سجل المبيعات)",
                description = "السماح للمستخدم بالدخول لسجل الفواتير وتصفح الفواتير وطباعتها ومشاركتها",
                icon = Icons.Default.Receipt,
                iconTint = Color(0xFF4F46E5),
                getValue = { it.canAccessInvoices },
                setValue = { p, v -> p.copy(canAccessInvoices = v) }
            ),
            PermissionItemDefinition(
                title = "الاطلاع على كافة فواتير النظام (لجميع الكواشير)",
                description = "عند التفعيل: يستطيع الكاشير الاطلاع على فواتير كافة الكواشير. عند الإيقاف: يرى فواتيره الخاصة فقط.",
                icon = Icons.Default.Visibility,
                iconTint = Color(0xFF7C3AED),
                getValue = { it.canViewAllInvoices },
                setValue = { p, v -> p.copy(canViewAllInvoices = v) }
            ),
            PermissionItemDefinition(
                title = "سندات القبض والصرف (الوصول وإصدار السندات)",
                description = "الوصول لتبويب السندات وإصدار سندات قبض نقدي وسندات الصرف",
                icon = Icons.Default.Description,
                iconTint = Color(0xFF059669),
                getValue = { it.canAccessBonds },
                setValue = { p, v -> p.copy(canAccessBonds = v) }
            ),
            PermissionItemDefinition(
                title = "الاطلاع على كافة سندات القبض والصرف (لجميع الكواشير)",
                description = "عند التفعيل: يستطيع الكاشير تصفح كافة السندات. عند الإيقاف: يقتصر على سنداته فقط.",
                icon = Icons.Default.Visibility,
                iconTint = Color(0xFF0D9488),
                getValue = { it.canViewAllBonds },
                setValue = { p, v -> p.copy(canViewAllBonds = v) }
            ),
            PermissionItemDefinition(
                title = "التحويل المخزني (بين المخازن والكواشير)",
                description = "تحويل كميات المنتجات من المخزن الرئيسي إلى مخزن الكاشير",
                icon = Icons.Default.SwapHoriz,
                iconTint = Color(0xFFEA580C),
                getValue = { it.canAccessTransfers },
                setValue = { p, v -> p.copy(canAccessTransfers = v) }
            ),
            PermissionItemDefinition(
                title = "تصفية الكاشير واستلام الإيرادات النقدية",
                description = "قبض مبالغ المبيعات من الكواشير وتخفيض ذممهم وإصدار سندات التصفية (خاص بالإدارة)",
                icon = Icons.Default.Payments,
                iconTint = Color(0xFF059669),
                isAdminOnly = true,
                getValue = { it.canAccessSettlements },
                setValue = { p, v -> p.copy(canAccessSettlements = v) }
            ),
            PermissionItemDefinition(
                title = "شاشة وسجل المصاريف اليومية",
                description = "تسجيل المصروفات والنثريات وإصدار سندات الصرف وتقارير المصاريف",
                icon = Icons.Default.ReceiptLong,
                iconTint = Color(0xFFE11D48),
                getValue = { it.canAccessExpenses },
                setValue = { p, v -> p.copy(canAccessExpenses = v) }
            ),
            PermissionItemDefinition(
                title = "توليد كروت المايكروتيك",
                description = "الوصول لقسم توليد الكروت وسحب البروفايلات وربط المايكروتيك",
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF0284C7),
                getValue = { it.canAccessCardGeneration },
                setValue = { p, v -> p.copy(canAccessCardGeneration = v) }
            ),
            PermissionItemDefinition(
                title = "القيود المحاسبية ودليل الحسابات (ias_post_dtl)",
                description = "عرض جدول الحركات المرحّلة، ميزان المراجعة، شجرة الحسابات والقيود المتزنة",
                icon = Icons.Default.Calculate,
                iconTint = Color(0xFF0F766E),
                isAdminOnly = true,
                getValue = { it.canAccessLedger },
                setValue = { p, v -> p.copy(canAccessLedger = v) }
            ),
            PermissionItemDefinition(
                title = "تهيئة وإعدادات الطابعة الحرارية والبلوتوث",
                description = "السماح للكاشير بالدخول لتهيئة الطابعة واختيار مقاس الرول والربط مع طابعة البلوتوث",
                icon = Icons.Default.Print,
                iconTint = Color(0xFF2563EB),
                getValue = { it.canAccessPrinterSettings },
                setValue = { p, v -> p.copy(canAccessPrinterSettings = v) }
            ),
            PermissionItemDefinition(
                title = "إدارة المستخدمين والصلاحيات",
                description = "إضافة وتعديل الكواشير والمستخدمين وتعيين كلمات المرور والصلاحيات",
                icon = Icons.Default.Person,
                iconTint = Color(0xFF0891B2),
                getValue = { it.canAccessUsers },
                setValue = { p, v -> p.copy(canAccessUsers = v) }
            ),
            PermissionItemDefinition(
                title = "إعدادات النظام والنسخ الاحتياطي",
                description = "تعديل اسم المنشأة، العملة، الاستيراد والتصدير وتصفير النظام",
                icon = Icons.Default.Settings,
                iconTint = Color(0xFF475569),
                getValue = { it.canAccessSettings },
                setValue = { p, v -> p.copy(canAccessSettings = v) }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "شاشة إدارة صلاحيات المستخدم",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "المستخدم: ${targetUser.name} (${targetUser.userCode})",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    Button(
                        onClick = { onSavePermissions(permissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ الصلاحيات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Target User Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4338CA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "المستخدم: ${targetUser.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "كود: ${targetUser.userCode} • اسم الدخول: ${targetUser.username}",
                                color = Color(0xFFC7D2FE),
                                fontSize = 11.sp
                            )
                            Text(
                                "صندوق: ${targetUser.assignedDrawerName} • مخزن: ${targetUser.assignedWarehouseName}",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Text(
                                if (targetUser.role == "ADMIN") "مدير عام" else "كاشير مبيعات",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Preset Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Preset: Default Cashier
                    Button(
                        onClick = { permissions = UserDefaults.DEFAULT_CASHIER_PERMISSIONS },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF), contentColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text("افتراضي الكاشير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Preset: All (Admin)
                    Button(
                        onClick = { permissions = UserDefaults.DEFAULT_ADMIN_PERMISSIONS },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECFDF5), contentColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text("تحديد الكل (كامل)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Preset: Clear All
                    Button(
                        onClick = { permissions = UserPermissions(
                            canAccessPos = false,
                            canSellNegativeStock = false,
                            canAccessProducts = false,
                            canAccessCustomers = false,
                            canSetOpeningBalance = false,
                            canAccessInvoices = false,
                            canViewAllInvoices = false,
                            canAccessBonds = false,
                            canViewAllBonds = false,
                            canAccessTransfers = false,
                            canAccessSettlements = false,
                            canAccessExpenses = false,
                            canAccessCardGeneration = false,
                            canAccessLedger = false,
                            canAccessPrinterSettings = false,
                            canAccessUsers = false,
                            canAccessSettings = false
                        ) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text("إلغاء الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Permissions Toggles List
            items(permissionItems) { itemDef ->
                val isChecked = itemDef.getValue(permissions)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(itemDef.iconTint.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    itemDef.icon,
                                    contentDescription = null,
                                    tint = itemDef.iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        itemDef.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (itemDef.isAdminOnly) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFEF2F2)
                                        ) {
                                            Text(
                                                "خاص بالإدارة",
                                                color = Color(0xFFDC2626),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    itemDef.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { newValue ->
                                permissions = itemDef.setValue(permissions, newValue)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4F46E5)
                            )
                        )
                    }
                }
            }

            // Bottom Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onSavePermissions(permissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ وتطبيق الصلاحيات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
