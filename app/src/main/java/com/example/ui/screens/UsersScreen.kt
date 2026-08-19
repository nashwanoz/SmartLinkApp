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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary

@Composable
fun UsersScreen(
    users: List<User>,
    currentUser: User?,
    onSaveUser: (User) -> Unit,
    onDeleteUser: (String) -> Unit,
    onOpenPermissions: (User) -> Unit
) {
    var editingUser by remember { mutableStateOf<User?>(null) }
    var isAddUserDialogOpen by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إدارة المستخدمين والكاشيرات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "إضافة وتعديل بيانات الكاشيرات ورموز الـ PIN",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }

                Button(
                    onClick = {
                        editingUser = null
                        isAddUserDialogOpen = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مستخدم جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Users List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(users) { user ->
                    val isAdmin = user.role == UserRole.ADMIN
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isAdmin) TealPrimary else Slate700),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.userCode,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "رمز PIN: ${user.pin} • الفرع: ${user.assignedBranch}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAdmin) TealContainer else Slate100
                                ) {
                                    Text(
                                        text = if (isAdmin) "مدير النظام" else "كاشير",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) TealDark else Slate700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate100)

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onOpenPermissions(user) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealContainer),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل الصلاحيات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealDark)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingUser = user
                                            isAddUserDialogOpen = true
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate100)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Slate700, modifier = Modifier.size(15.dp))
                                    }

                                    if (!isAdmin) {
                                        IconButton(
                                            onClick = { userToDelete = user },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RoseContainer)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit User Dialog
        if (isAddUserDialogOpen) {
            UserEditDialog(
                user = editingUser,
                onDismiss = { isAddUserDialogOpen = false },
                onSave = { saved ->
                    onSaveUser(saved)
                    isAddUserDialogOpen = false
                }
            )
        }

        // Delete Confirm Dialog
        userToDelete?.let { u ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("تأكيد حذف المستخدم", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف المستخدم (${u.name})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteUser(u.id)
                            userToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun UserEditDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    val isEdit = user != null
    var name by remember { mutableStateOf(user?.name ?: "") }
    var userCode by remember { mutableStateOf(user?.userCode ?: "") }
    var pin by remember { mutableStateOf(user?.pin ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var assignedBranch by remember { mutableStateOf(user?.assignedBranch ?: "الفرع الرئيسي") }
    var role by remember { mutableStateOf(user?.role ?: UserRole.CASHIER) }
    var active by remember { mutableStateOf(user?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل بيانات المستخدم" else "إضافة مستخدم جديد",
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
                    label = { Text("الاسم الكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = userCode,
                        onValueChange = { userCode = it },
                        label = { Text("كود الكاشير (مثال: 104)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("رمز PIN للدخول *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = assignedBranch,
                    onValueChange = { assignedBranch = it },
                    label = { Text("الفرع / نقطة البيع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Role toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (role == UserRole.CASHIER) TealPrimary else Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { role = UserRole.CASHIER }
                    ) {
                        Text(
                            text = "كاشير",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (role == UserRole.CASHIER) Color.White else Slate700,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (role == UserRole.ADMIN) TealDark else Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { role = UserRole.ADMIN }
                    ) {
                        Text(
                            text = "مدير نظام",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (role == UserRole.ADMIN) Color.White else Slate700,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || pin.isBlank()) return@Button
                    val targetId = user?.id ?: "user_${System.currentTimeMillis()}"
                    val targetCode = if (userCode.isBlank()) (100 + (4..99).random()).toString() else userCode

                    val result = User(
                        id = targetId,
                        userCode = targetCode,
                        name = name,
                        role = role,
                        username = username.ifBlank { "user_$targetCode" },
                        pin = pin,
                        active = active,
                        assignedBranch = assignedBranch.ifBlank { "نقطة بيع" },
                        permissions = user?.permissions ?: if (role == UserRole.ADMIN) {
                            UserPermissions(
                                canAccessPos = true,
                                canSellNegativeStock = true,
                                canAccessProducts = true,
                                canAccessCustomers = true,
                                canSetOpeningBalance = true,
                                canAccessStatements = true,
                                canAccessInvoices = true,
                                canAccessBonds = true,
                                canAccessTransfers = true,
                                canAccessUsers = true,
                                canAccessSettings = true
                            )
                        } else {
                            UserPermissions()
                        }
                    )
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("حفظ المستخدم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
