package com.smartlink.erp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.data.local.entity.UserPermissions

@Composable
fun PermissionsScreen(
    targetUser: User,
    currentUser: User,
    onSavePermissions: (String, UserPermissions) -> Unit,
    onBack: () -> Unit
) {
    var permissions by remember {
        mutableStateOf(getUserPermissions(targetUser))
    }
    var savedSuccess by remember { mutableStateOf(false) }
    
    val isEditingAdmin = targetUser.role == "ADMIN"
    
    fun togglePermission(key: String) {
        permissions = when (key) {
            "canAccessPos" -> permissions.copy(canAccessPos = !permissions.canAccessPos)
            "canAccessProducts" -> permissions.copy(canAccessProducts = !permissions.canAccessProducts)
            "canAccessCustomers" -> permissions.copy(canAccessCustomers = !permissions.canAccessCustomers)
            "canSetOpeningBalance" -> permissions.copy(canSetOpeningBalance = !permissions.canSetOpeningBalance)
            "canAccessStatements" -> permissions.copy(canAccessStatements = !permissions.canAccessStatements)
            "canAccessInvoices" -> permissions.copy(canAccessInvoices = !permissions.canAccessInvoices)
            "canAccessBonds" -> permissions.copy(canAccessBonds = !permissions.canAccessBonds)
            "canAccessTransfers" -> permissions.copy(canAccessTransfers = !permissions.canAccessTransfers)
            "canAccessUsers" -> permissions.copy(canAccessUsers = !permissions.canAccessUsers)
            "canAccessSettings" -> permissions.copy(canAccessSettings = !permissions.canAccessSettings)
            else -> permissions
        }
    }
    
    fun applyPreset(preset: String) {
        permissions = when (preset) {
            "all" -> DEFAULT_ADMIN_PERMISSIONS
            "none" -> UserPermissions(
                canAccessPos = false,
                canAccessProducts = false,
                canAccessCustomers = false,
                canSetOpeningBalance = false,
                canAccessStatements = false,
                canAccessInvoices = false,
                canAccessBonds = false,
                canAccessTransfers = false,
                canAccessUsers = false,
                canAccessSettings = false
            )
            "cashier" -> DEFAULT_CASHIER_PERMISSIONS
            else -> permissions
        }
    }
    
    fun handleSave() {
        onSavePermissions(targetUser.id, permissions)
        savedSuccess = true
        // Auto-dismiss after 800ms
        androidx.compose.runtime.LaunchedEffect(savedSuccess) {
            kotlinx.coroutines.delay(800)
            savedSuccess = false
            onBack()
        }
    }
    
    val permissionItems = listOf(
        PermissionItemDef(
            key = "canAccessPos",
            title = "نقطة البيع (POS)",
            desc = "إصدار فواتير المبيعات نقدية أو آجلة وسداد جزئي والطباعة",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF0D9488),
            bgColor = Color(0xFFF0FDFA),
            borderColor = Color(0xFF99F6E4)
        ),
        PermissionItemDef(
            key = "canAccessProducts",
            title = "الأصناف والمخزون",
            desc = "عرض وتصفح وإدارة الأصناف والأسعار ووحدات الكراتين والحبات",
            icon = Icons.Default.Inventory,
            color = Color(0xFF7C3AED),
            bgColor = Color(0xFFF5F3FF),
            borderColor = Color(0xFFE9D5FF)
        ),
        PermissionItemDef(
            key = "canAccessCustomers",
            title = "دليل وبيانات العملاء",
            desc = "عرض العملاء والبحث وإضافة عملاء جدد وأرقام هواتفهم",
            icon = Icons.Default.People,
            color = Color(0xFF2563EB),
            bgColor = Color(0xFFDBEAFE),
            borderColor = Color(0xFFBFDBFE)
        ),
        PermissionItemDef(
            key = "canSetOpeningBalance",
            title = "إدخال وتعديل الرصيد الافتتاحي للعملاء",
            desc = "صلاحية حساسة: السماح بوضع أو تعديل الرصيد الافتتاحي للعميل (محجوبة عن الكواشير ومخصصة للمدير)",
            icon = Icons.Default.AttachMoney,
            color = Color(0xFFE11D48),
            bgColor = Color(0xFFFEE2E2),
            borderColor = Color(0xFFFECACA),
            adminOnlyHighlight = true
        ),
        PermissionItemDef(
            key = "canAccessStatements",
            title = "كشوفات الحسابات الشاملة",
            desc = "استعراض كشف حساب عميل، كشف حركة كاشير، وحركة الأصناف",
            icon = Icons.Default.List,
            color = Color(0xFF92400E),
            bgColor = Color(0xFFFEF3C7),
            borderColor = Color(0xFFFDE68A)
        ),
        PermissionItemDef(
            key = "canAccessInvoices",
            title = "سجل وأرشيف الفواتير",
            desc = "تصفح الفواتير السابقة وطباعتها ومشاركتها عبر الواتساب",
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF4F46E5),
            bgColor = Color(0xFFEDE9FE),
            borderColor = Color(0xFFC7D2FE)
        ),
        PermissionItemDef(
            key = "canAccessBonds",
            title = "سندات القبض والصرف",
            desc = "إصدار سندات قبض نقدي من العملاء وسندات الصرف والخصم من الأرصدة",
            icon = Icons.Default.Description,
            color = Color(0xFF047857),
            bgColor = Color(0xFFECFDF5),
            borderColor = Color(0xFFA7F3D0)
        ),
        PermissionItemDef(
            key = "canAccessTransfers",
            title = "التحويل المخزني (بين المخازن والكواشير)",
            desc = "تحويل كميات المنتجات من المخزن الرئيسي إلى مخزن الكاشير",
            icon = Icons.Default.SwapHoriz,
            color = Color(0xFFEA580C),
            bgColor = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFED7AA)
        ),
        PermissionItemDef(
            key = "canAccessUsers",
            title = "إدارة المستخدمين والصلاحيات",
            desc = "إضافة وتعديل الكواشير والمستخدمين وتعيين كلمات المرور والصلاحيات",
            icon = Icons.Default.PersonCheck,
            color = Color(0xFF0891B2),
            bgColor = Color(0xFFECFEFF),
            borderColor = Color(0xFFA5F3FC)
        ),
        PermissionItemDef(
            key = "canAccessSettings",
            title = "إعدادات النظام والنسخ الاحتياطي",
            desc = "تعديل اسم المنشأة، العملة، الاستيراد والتصدير وتصفير النظام",
            icon = Icons.Default.Settings,
            color = Color(0xFF475569),
            bgColor = Color(0xFFF1F5F9),
            borderColor = Color(0xFFCBD5E1)
        )
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "شاشة إدارة صلاحيات المستخدم",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "المستخدم: ${targetUser.name} (كود: ${targetUser.userCode})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (targetUser.role == "ADMIN") "👑 مدير عام" else "💼 كاشير مبيعات",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { handleSave() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "حفظ الصلاحيات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Success Message
        if (savedSuccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF059669)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
