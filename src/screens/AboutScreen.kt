package com.smartlink.erp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlink.khamernet.data.local.DataStoreManager
import com.smartlink.khamernet.data.local.entity.SystemSettings
import com.smartlink.khamernet.data.local.entity.User
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AboutScreen(
    settings: SystemSettings,
    currentUser: User,
    onBack: () -> Unit = {},
    storeCode: String? = null,
    onOpenStoreSync: () -> Unit = {},
    dataStoreManager: DataStoreManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Activation code state
    var activationCode by remember { mutableStateOf("") }
    var isActivated by remember { 
        mutableStateOf(
            dataStoreManager.getBooleanSync("app_is_activated", false)
        ) 
    }
    var activationMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var copiedDevInfo by remember { mutableStateOf(false) }
    
    // App Machine ID / Installation Fingerprint for activation
    val machineId by remember {
        mutableStateOf(
            dataStoreManager.getStringSync("khamernet_machine_id", "")
                .ifEmpty { "SL-${(100000..999999).random()}" }
        )
    }
    
    // Save machine ID if not exists
    LaunchedEffect(Unit) {
        if (dataStoreManager.getStringSync("khamernet_machine_id", "").isEmpty()) {
            dataStoreManager.saveString("khamernet_machine_id", machineId)
        }
    }
    
    // Handle License Activation
    fun handleActivateApp() {
        val cleanCode = activationCode.trim().uppercase()
        
        if (cleanCode.isEmpty()) {
            activationMsg = Pair(false, "يرجى إدخال كود التفعيل أولاً")
            return
        }
        
        // Example valid format or universal master activation code
        if (cleanCode.length >= 6) {
            isActivated = true
            scope.launch {
                dataStoreManager.saveBoolean("app_is_activated", true)
                dataStoreManager.saveString("app_activation_key", cleanCode)
            }
            activationMsg = Pair(true, "تم تفعيل ترخيص البرنامج بنجاح! شكراً لثقتكم بنا.")
            activationCode = ""
        } else {
            activationMsg = Pair(false, "كود التفعيل غير صحيح، يرجى التواصل مع المطور للحصول على كود صالح.")
        }
    }
    
    fun copyContact() {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(
            "Developer Info",
            "المطور: نشوان العديني - Smart Link - 776323844"
        )
        clipboardManager.setPrimaryClip(clip)
        copiedDevInfo = true
        Toast.makeText(context, "تم نسخ بيانات المطور", Toast.LENGTH_SHORT).show()
        scope.launch {
            kotlinx.coroutines.delay(2000)
            copiedDevInfo = false
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "معلومات التطبيق",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "معلومات التحديثات الجديدة وترخيص البرنامج",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "العودة للرئيسية",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
        
        // 1. مربع الجديد في البرنامج (مختصر ومدمج)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "الجديد في البرنامج (v2.5)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF99F6E4))
                        ) {
                            Text(
                                text = "فائقة الخفة",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF115E59),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NewFeatureItem(
                            text = "كشف خفيف: بحث فوري وتوليد PDF",
                            color = Color(0xFF0F766E)
                        )
                        NewFeatureItem(
                            text = "تصفية الكاشير: فلاتر تواريخ دقيقة",
                            color = Color(0xFF0F766E)
                        )
                        NewFeatureItem(
                            text = "واجهة مدمجة: شبكة 3 أقسام سريعة",
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            }
        }
        
        // 2. مزامنة قاعدة البيانات المركزية وكود المحل
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFF99F6E4))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "قاعدة البيانات السحابية (Smart Link Cloud)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF134E4A)
                            )
                        }
                        
                        if (storeCode != null) {
                            Surface(
                                color = Color(0xECFDF5),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "مربوط: $storeCode",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Text(
                                    text = "غير مربوط",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xCCD9F9F3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (storeCode != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "المساحة السحابية للمحل: ",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF99F6E4))
                                    ) {
                                        Text(
                                            text = storeCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF134E4A),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Button(
                                    onClick = onOpenStoreSync,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F766E)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "تغيير الكود",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = "لم يتم ربط كود المحل بعد للمزامنة السحابية الفورية.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                                Button(
                                    onClick = onOpenStoreSync,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F766E)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "ربط كود المحل ❯",
                                        fontSize = 10.sp,
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
        
        // 3. مربع تسجيل البرنامج (كود التفعيل)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "تسجيل وتفعيل البرنامج",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        if (isActivated) {
                            Surface(
                                color = Color(0xECFDF5),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "النسخة مفعلة",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF92400E),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "نسخة تجريبية",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                    
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "معرف التثبيت:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = machineId,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1E293B),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "زوّد المطور بالمعرف",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = activationCode,
                                onValueChange = { 
                                    activationCode = it
                                    activationMsg = null
                                },
                                placeholder = {
                                    Text(
                                        text = "ادخل كود التفعيل هنا...",
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            
                            Button(
                                onClick = { handleActivateApp() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F46E5)
                                ),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = "تفعيل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        
                        activationMsg?.let { msg ->
                            Surface(
                                color = if (msg.first) Color(0xECFDF5) else Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (msg.first) Color(0xFFA7F3D0) else Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (msg.first) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (msg.first) Color(0xFF059669) else Color(0xFFDC2626),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = msg.second,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.first) Color(0xFF047857) else Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 4. بيانات المطور وحقوق الملكية
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xCC14B8A6),
                            shape = RoundedCornerShape(percent = 50),
                            border = BorderStroke(1.dp, Color(0xCC14B8A6)),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color(0xFF5EEAD4),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المطور: ",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "نشوان العديني",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "جميع الحقوق محفوظة smart link",
                            fontSize = 10.sp,
                            color = Color(0xFF5EEAD4),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}",
                            fontSize = 10.sp,
                            color = Color(0xFF5EEAD4),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    TextButton(
                        onClick = { copyContact() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (copiedDevInfo) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedDevInfo) Color(0xFF34D399) else Color(0xFF94A3B8),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = if (copiedDevInfo) "تم النسخ" else "نسخ بيانات المطور",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewFeatureItem(
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(percent = 50))
        )
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color(0xFF475569),
            lineHeight = 12.sp
        )
    }
}
