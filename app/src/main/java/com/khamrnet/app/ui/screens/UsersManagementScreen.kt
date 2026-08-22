package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.util.*

data class AppCashierUser(
    val id: String,
    val name: String,
    val username: String,
    val role: String,
    val pin: String,
    val drawerName: String,
    val isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var usersList by remember {
        mutableStateOf(
            listOf(
                AppCashierUser("1", "المدير العام", "admin", "مدير النظام", "1234", "الدرج الرئيسي"),
                AppCashierUser("2", "كاشير 1", "cashier1", "كاشير مبيعات", "1111", "درج نقطة بيع 1"),
                AppCashierUser("3", "كاشير 2", "cashier2", "كاشير مبيعات", "2222", "درج نقطة بيع 2"),
                AppCashierUser("4", "كاشير 3", "cashier3", "كاشير مبيعات", "3333", "درج نقطة بيع 3")
            )
        )
    }

    var showAddUserDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "إدارة حسابات الكاشير والمستخدمين",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "الصلاحيات والأدراج ورموز الدخول PIN",
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
                    IconButton(onClick = { showAddUserDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "إضافة كاشير", tint = Color(0xFF4F46E5))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddUserDialog = true },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة مستخدم جديد", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4338CA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("عدد حسابات الكاشير والنظام", fontSize = 12.sp, color = Color(0xFFE0E7FF))
                            Text(
                                "${usersList.size} مستخدمين",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            // User Cards List
            items(usersList, key = { it.id }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (user.role.contains("مدير")) Color(0xFFEEF2FF) else Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (user.role.contains("مدير")) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (user.role.contains("مدير")) Color(0xFF4F46E5) else Color(0xFF059669)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                Text(
                                    "${user.role} • ${user.drawerName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    "رمز الدخول PIN: •••• (${user.pin})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        if (user.username != "admin") {
                            IconButton(
                                onClick = {
                                    usersList = usersList.filter { it.id != user.id }
                                    Toast.makeText(context, "تم حذف حساب ${user.name}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        var formName by remember { mutableStateOf("") }
        var formUsername by remember { mutableStateOf("") }
        var formPin by remember { mutableStateOf("1234") }
        var formRole by remember { mutableStateOf("كاشير مبيعات") }
        var formDrawer by remember { mutableStateOf("درج جديد") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إضافة كاشير / مستخدم جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        label = { Text("اسم الموظف / الكاشير") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formUsername,
                        onValueChange = { formUsername = it },
                        label = { Text("اسم المستخدم للدخول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formPin,
                        onValueChange = { formPin = it },
                        label = { Text("رمز الدخول السريع (PIN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formName.isBlank() || formUsername.isBlank()) {
                            Toast.makeText(context, "يرجى تعبئة الحقول المطلوبة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newUser = AppCashierUser(
                            id = UUID.randomUUID().toString(),
                            name = formName,
                            username = formUsername,
                            role = formRole,
                            pin = formPin.ifBlank { "1234" },
                            drawerName = "درج $formName"
                        )
                        usersList = usersList + newUser
                        showAddUserDialog = false
                        Toast.makeText(context, "✅ تم إضافة المستخدم بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("حفظ المستخدم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }
}
