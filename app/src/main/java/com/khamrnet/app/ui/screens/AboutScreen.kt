package com.khamrnet.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    settings: SystemSettingsEntity,
    onNavigateBack: () -> Unit,
    onOpenStoreSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // SharedPreferences for activation state and machine ID
    val sharedPrefs = remember {
        context.getSharedPreferences("khamrnet_license_prefs", Context.MODE_PRIVATE)
    }

    var isActivated by remember {
        mutableStateOf(sharedPrefs.getBoolean("app_is_activated", false))
    }

    val machineId = remember {
        var id = sharedPrefs.getString("machine_id", null)
        if (id.isNullOrEmpty()) {
            id = "SL-${(100000..999999).random()}"
            sharedPrefs.edit().putString("machine_id", id).apply()
        }
        id
    }

    var activationCode by remember { mutableStateOf("") }
    var activationMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showChangeStoreDialog by remember { mutableStateOf(false) }
    var newStoreCodeInput by remember { mutableStateOf(settings.storeCode.ifEmpty { "TEST-SMART" }) }

    val developerPhone = "776323844"
    val developerName = "نشوان العديني"

    fun handleActivate() {
        val clean = activationCode.trim().uppercase()
        if (clean.isEmpty()) {
            activationMessage = Pair(false, "يرجى إدخال كود التفعيل أولاً")
            return
        }
        if (clean.length >= 6) {
            isActivated = true
            sharedPrefs.edit()
                .putBoolean("app_is_activated", true)
                .putString("activation_key", clean)
                .apply()
            activationMessage = Pair(true, "تم تفعيل ترخيص البرنامج بنجاح! شكراً لثقتكم بنا.")
            activationCode = ""
            Toast.makeText(context, "✅ تم تفعيل ترخيص البرنامج بنجاح", Toast.LENGTH_LONG).show()
        } else {
            activationMessage = Pair(false, "كود التفعيل غير صحيح، يرجى التواصل مع المطور للحصول على كود صالح.")
        }
    }

    fun openWhatsAppChat() {
        try {
            val message = "مرحباً بك أستاذ نشوان، أود التواصل بخصوص ترخيص نظام خمر نت (معرف التثبيت: $machineId)"
            val url = "https://wa.me/967$developerPhone?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب. رقم المطور: $developerPhone", Toast.LENGTH_LONG).show()
        }
    }

    fun copyDeveloperInfo() {
        val infoText = "المطور: $developerName | هاتف / واتساب: $developerPhone | smart link © 2026 | معرف التثبيت: $machineId"
        clipboardManager.setText(AnnotatedString(infoText))
        Toast.makeText(context, "✅ تم نسخ بيانات المطور بنجاح", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "معلومات التطبيق",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                "معلومات التحديثات الجديدة وترخيص البرنامج",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "العودة للرئيسية",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================= 1. CARD: NEW FEATURES (v2.5) =================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "الجديد في البرنامج (v2.5)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFCCFBF1))
                                    .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "فائقة الخفة",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF115E59)
                                )
                            }
                        }

                        // Feature Item 1
                        FeatureBulletItem(title = "كشف خفيف:", desc = "بحث فوري وتوليد PDF")
                        // Feature Item 2
                        FeatureBulletItem(title = "تصفية الكاشير:", desc = "فلاتر تواريخ دقيقة")
                        // Feature Item 3
                        FeatureBulletItem(title = "واجهة مدمجة:", desc = "شبكة 3 أقسام سريعة")
                    }
                }
            }

            // ================= 2. CARD: CLOUD DATABASE (Smart Link Cloud) =================
            item {
                val activeStoreCode = settings.storeCode.ifEmpty { "TEST-SMART" }
                val isConnected = activeStoreCode.isNotEmpty()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "قاعدة البيانات السحابية (Smart Link Cloud)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F766E)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isConnected) Color(0xFFF0FDF4) else Color(0xFFFFFBEB))
                                    .border(
                                        1.dp,
                                        if (isConnected) Color(0xFFBBF7D0) else Color(0xFFFDE68A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isConnected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "مربوط: $activeStoreCode",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF15803D)
                                        )
                                    } else {
                                        Text(
                                            "غير مربوط",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }

                        // Store Code & Change Action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0FDFA))
                                .border(1.dp, Color(0xFFCCFBF1), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onOpenStoreSync()
                                        showChangeStoreDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        "تغيير الكود",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            activeStoreCode,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF115E59)
                                        )
                                    }
                                    Text(
                                        "المساحة السحابية للمحل:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 3. CARD: LICENSE REGISTRATION & ACTIVATION =================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "تسجيل وتفعيل البرنامج",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActivated) Color(0xFFF0FDF4) else Color(0xFFFFFBEB))
                                    .border(
                                        1.dp,
                                        if (isActivated) Color(0xFFBBF7D0) else Color(0xFFFDE68A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (isActivated) Icons.Default.CheckCircle else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isActivated) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        if (isActivated) "النسخة مفعلة" else "نسخة تجريبية",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isActivated) Color(0xFF15803D) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        // Machine ID Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "زوّد المطور بالمعرف",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        machineId,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "معرف التثبيت:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // Activation Code Form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { handleActivate() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    "تفعيل",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            OutlinedTextField(
                                value = activationCode,
                                onValueChange = {
                                    activationCode = it
                                    activationMessage = null
                                },
                                placeholder = {
                                    Text("ادخل كود التفعيل هنا...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Start
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { handleActivate() }),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Feedback message
                        if (activationMessage != null) {
                            val isSuccess = activationMessage!!.first
                            val msg = activationMessage!!.second
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
                                    .border(
                                        1.dp,
                                        if (isSuccess) Color(0xFFBBF7D0) else Color(0xFFFECACA),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isSuccess) Color(0xFF15803D) else Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        msg,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSuccess) Color(0xFF15803D) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= 4. CARD: DEVELOPER INFO & DIRECT WHATSAPP =================
            // Designed in an elegant, modern Teal/Emerald App Theme instead of plain black!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF0D5C56), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F4E4A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF115E59), // Rich Teal
                                        Color(0xFF0B3B38)  // Deep Dark Teal
                                    )
                                )
                            )
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Developer Name Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2DD4BF).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color(0xFF5EEAD4),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "المطور:",
                                fontSize = 11.sp,
                                color = Color(0xFF99F6E4)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                developerName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // WhatsApp Direct Contact Button (Green / Teal Action Button)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981))
                                .clickable { openWhatsAppChat() }
                                .padding(vertical = 9.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "تواصل مباشر عبر الواتساب: $developerPhone",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        // Divider & Copyright Text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.12f))
                        )

                        Text(
                            "جميع الحقوق محفوظة smart link © 2026",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5EEAD4),
                            textAlign = TextAlign.Center
                        )

                        // Copy Developer Info Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { copyDeveloperInfo() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFFCCFBF1),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "نسخ بيانات المطور",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCCFBF1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Bullet Item for New Features
 */
@Composable
private fun FeatureBulletItem(title: String, desc: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F766E))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    desc,
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

