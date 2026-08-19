package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary

@Composable
fun PermissionsScreen(
    user: User?,
    onSavePermissions: (String, UserPermissions) -> Unit,
    onBack: () -> Unit
) {
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("يرجى اختيار مستخدم لتعديل صلاحياته")
        }
        return
    }

    var canPos by remember { mutableStateOf(user.permissions.canAccessPos) }
    var canNegativeStock by remember { mutableStateOf(user.permissions.canSellNegativeStock) }
    var canProducts by remember { mutableStateOf(user.permissions.canAccessProducts) }
    var canCustomers by remember { mutableStateOf(user.permissions.canAccessCustomers) }
    var canOpeningBalance by remember { mutableStateOf(user.permissions.canSetOpeningBalance) }
    var canStatements by remember { mutableStateOf(user.permissions.canAccessStatements) }
    var canInvoices by remember { mutableStateOf(user.permissions.canAccessInvoices) }
    var canBonds by remember { mutableStateOf(user.permissions.canAccessBonds) }
    var canTransfers by remember { mutableStateOf(user.permissions.canAccessTransfers) }
    var canUsers by remember { mutableStateOf(user.permissions.canAccessUsers) }
    var canSettings by remember { mutableStateOf(user.permissions.canAccessSettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        // User Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealContainer),
            modifier = Modifier.fillMaxWidth()
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
                        text = "صلاحيات المستخدم: ${user.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "كود: ${user.userCode} • ${if (user.role == UserRole.ADMIN) "مدير نظام" else "كاشير"}",
                        fontSize = 11.sp,
                        color = TealDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Permissions Switches List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                PermissionSwitchItem(
                    title = "الوصول إلى شاشة نقطة البيع (POS)",
                    subtitle = "السماح بإصدار فواتير المبيعات",
                    checked = canPos,
                    onCheckedChange = { canPos = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "البيع عند نفاذ الرصيد (مخزون سالب)",
                    subtitle = "السماح بالبيع حتى في حال كان رصيد الصنف صفر",
                    checked = canNegativeStock,
                    onCheckedChange = { canNegativeStock = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "الوصول وإدارة الأصناف والأسعار",
                    subtitle = "إضافة وتعديل بيانات الأصناف ووحدات القياس",
                    checked = canProducts,
                    onCheckedChange = { canProducts = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "الوصول لدليل العملاء",
                    subtitle = "إضافة وتعديل بيانات العملاء وأرقام الهواتف",
                    checked = canCustomers,
                    onCheckedChange = { canCustomers = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "تحديد وتعديل الرصيد الافتتاحي للعملاء",
                    subtitle = "صلاحية حساسة لتحديد رصيد العميل الأولي",
                    checked = canOpeningBalance,
                    onCheckedChange = { canOpeningBalance = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "الوصول لكشوفات الحسابات للعملاء",
                    subtitle = "عرض وطباعة وتصدير كشف حساب تفصيلي للعميل",
                    checked = canStatements,
                    onCheckedChange = { canStatements = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "الوصول لسجل الفواتير وأرشيف المبيعات",
                    subtitle = "عرض الفواتير السابقة وطباعتها",
                    checked = canInvoices,
                    onCheckedChange = { canInvoices = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "إصدار سندات القبض والصرف",
                    subtitle = "تحصيل المبالغ وتوريدها من وإلى العملاء",
                    checked = canBonds,
                    onCheckedChange = { canBonds = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "التحويل المخزني وتغذية العهد",
                    subtitle = "تحويل الأصناف من المخزن العام لنقاط البيع",
                    checked = canTransfers,
                    onCheckedChange = { canTransfers = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "إدارة المستخدمين والكاشيرات",
                    subtitle = "إضافة موظفين جدد وتغيير كلمات المرور",
                    checked = canUsers,
                    onCheckedChange = { canUsers = it }
                )
            }
            item {
                PermissionSwitchItem(
                    title = "إعدادات النظام والنسخ الاحتياطي",
                    subtitle = "تعديل اسم المحل، النسخ الاحتياطي وتصفير البيانات",
                    checked = canSettings,
                    onCheckedChange = { canSettings = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = {
                val newPerms = UserPermissions(
                    canAccessPos = canPos,
                    canSellNegativeStock = canNegativeStock,
                    canAccessProducts = canProducts,
                    canAccessCustomers = canCustomers,
                    canSetOpeningBalance = canOpeningBalance,
                    canAccessStatements = canStatements,
                    canAccessInvoices = canInvoices,
                    canAccessBonds = canBonds,
                    canAccessTransfers = canTransfers,
                    canAccessUsers = canUsers,
                    canAccessSettings = canSettings
                )
                onSavePermissions(user.id, newPerms)
                onBack()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ الصلاحيات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PermissionSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                Text(subtitle, fontSize = 10.sp, color = Slate500)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TealPrimary
                )
            )
        }
    }
}
