package com.smartlink.erp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.data.local.entity.UserPermissions

@Composable
fun UsersScreen(
    users: List<User>,
    currentUser: User,
    settings: SystemSettings,
    onSaveUser: (User, Boolean) -> Unit,
    onDeleteUser: (String) -> Unit,
    onOpenPermissions: (User) -> Unit
) {
    var isModalOpen by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    
    // Form states
    var formCode by remember { mutableStateOf("") }
    var formName by remember { mutableStateOf("") }
    var formUsername by remember { mutableStateOf("") }
    var formRole by remember { mutableStateOf("CASHIER") }
    var formPin by remember { mutableStateOf("1234") }
    var errorMsg by remember { mutableStateOf("") }
    
    fun getNextUserCode(): String {
        if (users.isEmpty()) return "101"
        val codes = users.mapNotNull { u -> u.userCode.toIntOrNull() }
        if (codes.isEmpty()) return "101"
        val maxCode = maxOf(codes.max(), 100)
        return (maxCode + 1).toString()
    }
    
    fun openAddModal() {
        editingUser = null
        formCode = getNextUserCode()
        formName = ""
        formUsername = "user_${getNextUserCode()}"
        formRole = "CASHIER"
        formPin = "1234"
        errorMsg = ""
        isModalOpen = true
    }
    
    fun openEditModal(user: User) {
        editingUser = user
        formCode = user.userCode
        formName = user.name
        formUsername = user.username ?: ""
        formRole = user.role
        formPin = user.pin ?: "1234"
        errorMsg = ""
        isModalOpen = true
    }
    
    fun handleSubmit() {
        errorMsg = ""
        
        val trimmedName = formName.trim()
        if (trimmedName.isEmpty()) {
            errorMsg = "يرجى كتابة اسم المستخدم"
            return
        }
        
        val user = User(
            id = editingUser?.id ?: "user_${System.currentTimeMillis()}",
            userCode = formCode.trim().ifEmpty { getNextUserCode() },
            name = trimmedName,
            username = formUsername.trim().ifEmpty { "user_$formCode" },
            role = formRole,
            pin = formPin.trim().ifEmpty { "1234" },
            active = true,
            permissions = editingUser?.permissions ?: DEFAULT_CASHIER_PERMISSIONS
        )
        
        onSaveUser(user, editingUser != null)
        isModalOpen = false
    }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icons.Default.PersonCheck,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "إدارة المستخدمين والكواشير (أكواد تبدأ من 101)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "تخصيص الكواشير والصلاحيات ورمز المرور (PIN)",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Button(
                    onClick = { openAddModal() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+ مستخدم جديد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        
        // Users Grid
        items(users, key = { it.id }) { user ->
            UserCard(
                user = user,
                isCurrentUser = user.id == currentUser.id,
                onEditClick = { openEditModal(user) },
                onDeleteClick = { onDeleteUser(user.id) },
                onPermissionsClick = { onOpenPermissions(user) }
            )
        }
    }
    
    // USER MODAL
    if (isModalOpen) {
        UserModal(
            editingUser = editingUser,
            formCode = formCode,
            formName = formName,
            formUsername = formUsername,
            formRole = formRole,
            formPin = formPin,
            errorMsg = errorMsg,
            onCodeChange = { formCode = it },
            onNameChange = { formName = it },
            onUsernameChange = { formUsername = it },
            onRoleChange = { formRole = it },
            onPinChange = { formPin = it },
            onClose = { isModalOpen = false },
            onSubmit = { handleSubmit() }
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    isCurrentUser: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPermissionsClick: () -> Unit
) {
    val isAdmin = user.role == "ADMIN"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdmin) Color(0xFFF5F3FF).copy(alpha = 0.2f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isAdmin) Color(0xFFC7D2FE) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFE0E7FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "كود: ${user.userCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF312E81),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = user.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Text(
                        text = "اسم الدخول: ${user.username ?: ""} • الرمز السري PIN: ${user.pin ?: "1234"}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Surface(
                    color = if (isAdmin) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAdmin) "مدير عام" else "كاشير مبيعات",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAdmin) Color.White else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPermissionsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0E7FF)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "الصلاحيات 🛡️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4F46E5)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    if (!isCurrentUser) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserModal(
    editingUser: User?,
    formCode: String,
    formName: String,
    formUsername: String,
    formRole: String,
    formPin: String,
    errorMsg: String,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        Surface(
                            color = Color(0xFFE0E7FF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PersonCheck,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (editingUser != null) "تعديل بيانات المستخدم" else "إضافة مستخدم جديد",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Error Message
                if (errorMsg.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEE2E2)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = errorMsg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                
                // Form
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Code + Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formCode,
                            onValueChange = onCodeChange,
                            label = { Text("كود المستخدم", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF312E81)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        OutlinedTextField(
                            value = formName,
                            onValueChange = onNameChange,
                            label = { Text("الاسم الثلاثي *", fontSize = 10.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    // Username + PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formUsername,
                            onValueChange = onUsernameChange,
                            label = { Text("اسم المستخدم (Login)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        OutlinedTextField(
                            value = formPin,
                            onValueChange = onPinChange,
                            label = { Text("رمز PIN الدخول", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    // Role Selection
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "نوع الصلاحية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onRoleChange("CASHIER") },
                                colors = if (formRole == "CASHIER") {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF312E81)
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                elevation = if (formRole == "CASHIER") {
                                    ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                } else {
                                    ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                }
                            ) {
                                Text(
                                    text = "كاشير مبيعات",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = { onRoleChange("ADMIN") },
                                colors = if (formRole == "ADMIN") {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4F46E5),
                                        contentColor = Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                elevation = if (formRole == "ADMIN") {
                                    ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                } else {
                                    ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                }
                            ) {
                                Text(
                                    text = "مدير عام للنظام",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (formRole == "ADMIN") Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "إلغاء",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = onSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (editingUser != null) "حفظ التعديل" else "إضافة المستخدم",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
