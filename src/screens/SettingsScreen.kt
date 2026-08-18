package com.smartlink.erp.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    settings: SystemSettings,
    currentUser: User,
    onSaveSettings: (SystemSettings) -> Unit,
    onResetToDefaults: () -> Unit,
    onClearAllData: () -> Unit
) {
    var businessName by remember { mutableStateOf(settings.businessName ?: "") }
    var tagline by remember { mutableStateOf(settings.tagline ?: "") }
    var address by remember { mutableStateOf(settings.address ?: "") }
    var phone by remember { mutableStateOf(settings.phone ?: "") }
    var currency by remember { mutableStateOf(settings.currency ?: "ريال يمني") }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol ?: "YER") }
    var logoUrl by remember { mutableStateOf(settings.logoUrl ?: "") }
    var whatsappMode by remember { mutableStateOf(settings.whatsappMode ?: "text") }
    var autoPrint by remember { mutableStateOf(settings.autoPrintAfterInvoice) }
    
    var savedSuccess by remember { mutableStateOf(false) }
    
    // Dynamic Password Modal State for Zeroing Database
    var isResetModalOpen by remember { mutableStateOf(false) }
    var resetPasswordInput by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf("") }
    
    // Calculate current dynamic password (YYYYMMDD221411)
    fun getExpectedResetPassword(): String {
        val d = Calendar.getInstance()
        val year = d.get(Calendar.YEAR)
        val month = String.format("%02d", d.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", d.get(Calendar.DAY_OF_MONTH))
        return "${year}${month}${day}221411"
    }
    
    fun handleOpenResetModal() {
        resetPasswordInput = ""
        resetError = ""
        isResetModalOpen = true
    }
    
    fun handleConfirmReset() {
        val expected = getExpectedResetPassword()
        val trimmedInput = resetPasswordInput.trim()
        
        if (trimmedInput.isEmpty()) {
            resetError = "يرجى إدخال كلمة سر تصفير البيانات"
            return
        }
        
        if (trimmedInput != expected) {
            resetError = "كلمة السر غير صحيحة! يرجى التواصل مع مطور النظام."
            return
        }
        
        // Success: Clear Data
        onClearAllData()
        isResetModalOpen = false
        resetPasswordInput = ""
        resetError = ""
    }
    
    fun handleSave() {
        onSaveSettings(
            SystemSettings(
                businessName = businessName.trim().ifEmpty { "شبكة خمر نت اللاسلكية" },
                tagline = tagline.trim(),
                address = address.trim().ifEmpty { "خمر - السوق العام" },
                phone = phone.trim(),
                currency = currency.trim().ifEmpty { "ريال يمني" },
                currencySymbol = currencySymbol.trim().ifEmpty { "YER" },
                logoUrl = logoUrl.trim(),
                whatsappMode = whatsappMode,
                autoPrintAfterInvoice = autoPrint
            )
        )
        savedSuccess = true
        // Auto-dismiss after 3 seconds
        androidx.compose.runtime.LaunchedEffect(savedSuccess) {
            kotlinx.coroutines.delay(3000)
            savedSuccess = false
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تهيئة وإعدادات النظام العام",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "تخصيص بيانات المنشأة والعملة الافتراضية وشعار النشاط وقنوات الإرسال",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                TextButton(
                    onClick = onResetToDefaults
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "استعادة الافتراضي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
        
        // Success Message
        if (savedSuccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تم حفظ وتطبيق جميع إعدادات النظام بنجاح!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
        
        // Section 1: Business Identity
        item {
            SectionCard(
                title = "1. بيانات وهوية النشاط التجاري",
                icon = Icons.Default.Business,
                iconColor = Color(0xFF0F766E)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("اسم النشاط / المحل *", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان والموقع *", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم هاتف النشاط للتواصل", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("الوصف / الترويسة الفرعية", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D9488),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }
        }
        
        // Section 2: Logo Image
        item {
            SectionCard(
                title = "2. شعار النشاط (يظهر في الفواتير والتقارير)",
                icon = Icons.Default.Image,
                iconColor = Color(0xFF7C3AED)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Preview
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, Color(0xFFE2E8F0).copy(alpha = 0.5f)),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoUrl.isNotEmpty()) {
                                // TODO: Use Coil or similar for image loading
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "لا يوجد شعار",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // TODO: Implement file picker for logo upload
                        Button(
                            onClick = { /* TODO: Open file picker */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "استعراض واختيار صورة الشعار من الجهاز 📁",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                        
                        if (logoUrl.isNotEmpty()) {
                            TextButton(
                                onClick = { logoUrl = "" }
                            ) {
                                Text(
                                    text = "إزالة الشعار الحالي",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }
                        
                        Text(
                            text = "يدعم صيغ PNG, JPG, WEBP. يفضل صورة مربعة واضحة.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
        
        // Section 3: Currency Configuration
        item {
            SectionCard(
                title = "3. إعدادات العملة والرموز المحاسبية",
                icon = Icons.Default.AttachMoney,
                iconColor = Color(0xFF047857)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("اسم العملة الأساسية *", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("رمز العملة المختصر (Symbol) *", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF047857)
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }
        
        // Section 4: WhatsApp & Printing Settings
        item {
            SectionCard(
                title = "4. خيارات إرسال رسائل الواتساب والطباعة",
                icon = Icons.Default.Message,
                iconColor = Color(0xFF059669)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "طريقة إرسال الفواتير عبر الواتساب:",
                            fontSize = 11.sp,
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
                                onClick = { whatsappMode = "text" },
                                colors = if (whatsappMode == "text") {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF047857),
                                        contentColor = Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF475569)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "رسالة نصية منسقة (الأسرع والأخف)",
                                    fontSize = 11.sp,
                                    fontWeight = if (whatsappMode == "text") FontWeight.Black else FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = { whatsappMode = "pdf" },
                                colors = if (whatsappMode == "pdf") {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF047857),
                                        contentColor = Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF475569)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "قالب الفاتورة PDF والطباعة",
                                    fontSize = 11.sp,
                                    fontWeight = if (whatsappMode == "pdf") FontWeight.Black else FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = "* ملاحظة: السندات ترسل كنصوص منسقة دائماً وفقاً للشروط المحددة.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = autoPrint,
                            onCheckedChange = { autoPrint = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF0D9488),
                                uncheckedColor = Color(0xFFCBD5E1)
                            )
                        )
                        Text(
                            text = "فتح نافذة الطباعة التلقائية فور حفظ الفاتورة بنقطة البيع",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
        
        // Submit Button
        item {
            Button(
                onClick = { handleSave() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F766E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "حفظ وتطبيق جميع الإعدادات الآن 🚀",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        
        // Section 5: Zero Database & Clean Start (ADMIN only)
        if (currentUser.role == "ADMIN") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "منطقة تصفير البيانات والبدء من الصفر (محمية بكلمة سر)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF991B1B)
                            )
                        }
                        
                        Text(
                            text = "يمكنك مسح وتصفير كافة بيانات العملاء، الأصناف، الفواتير التجريبية، السندات، وحركات التحويل المخزني نهائياً للبدء بالعمل الفعلي على النظام من الصفر. هذه العملية تتطلب إدخال كلمة سر الحماية اليومية.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C),
                            lineHeight = 16.sp
                        )
                        
                        Button(
                            onClick = { handleOpenResetModal() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "تصفير ومسح جميع البيانات والبدء من الصفر 🗑️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Reset Password Modal
    if (isResetModalOpen) {
        ResetPasswordModal(
            resetPasswordInput = resetPasswordInput,
            resetError = resetError,
            onPasswordChange = { resetPasswordInput = it },
            onErrorChange = { resetError = it },
            onClose = { isResetModalOpen = false },
            onConfirm = { handleConfirmReset() }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
            }
            
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            
            content()
        }
    }
}

@Composable
private fun ResetPasswordModal(
    resetPasswordInput: String,
    resetError: String,
    onPasswordChange: (String) -> Unit,
    onErrorChange: (String) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تأكيد كلمة سر تصفير البيانات",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE11D48)
                        )
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Warning Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "إجراء حساس ولا يمكن التراجع عنه:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                        
                        Text(
                            text = "سيتم مسح وتصفير كافة سجلات العملاء، الأصناف، الفواتير، السندات والتحويلات نهائياً.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
                
                // Password Input
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "أدخل كلمة السر الأمنية للتصفير:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    
                    OutlinedTextField(
                        value = resetPasswordInput,
                        onValueChange = {
                            onPasswordChange(it)
                            onErrorChange("")
                        },
                        placeholder = {
                            Text(
                                text = "تواصل مع مطور النظام",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE11D48),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            errorBorderColor = Color(0xFFE11D48)
                        ),
                        isError = resetError.isNotEmpty()
                    )
                    
                    if (resetError.isNotEmpty()) {
                        Text(
                            text = "⚠️ $resetError",
                            fontSize
