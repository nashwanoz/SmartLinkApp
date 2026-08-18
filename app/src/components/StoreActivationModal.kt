package com.smartlink.erp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.StoreMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class StoreActivationModalProps(
    val currentStoreCode: String?,
    val businessName: String,
    val isOpen: Boolean,
    val onClose: () -> Unit,
    val onActivated: (String, StoreMetadata?) -> Unit
)

@Composable
fun StoreActivationModal(
    currentStoreCode: String?,
    businessName: String,
    isOpen: Boolean,
    onClose: () -> Unit,
    onActivated: (String, StoreMetadata?) -> Unit
) {
    if (!isOpen) return
    
    var inputCode by remember { mutableStateOf(currentStoreCode ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun handleConnect() {
        errorMsg = null
        successMsg = null
        
        val formattedCode = inputCode.trim().uppercase()
        if (formattedCode.length < 3) {
            errorMsg = "يرجى إدخال كود توجيه صحيح (مثال: KHAMR-883 أو STORE-01)"
            return
        }
        
        isLoading = true
        
        scope.launch {
            try {
                // Simulate API call
                delay(1500)
                
                // TODO: Replace with actual API call
                // val res = registerOrConnectStore(formattedCode, businessName)
                
                // Mock success
                val res = StoreActivationResult(
                    success = true,
                    error = null,
                    metadata = StoreMetadata(
                        storeCode = formattedCode,
                        businessName = businessName,
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                if (res.success) {
                    successMsg = "تم ربط وتوجيه المحل بنجاح إلى المساحة: $formattedCode"
                    delay(1000)
                    onActivated(formattedCode, res.metadata)
                    onClose()
                } else {
                    errorMsg = res.error ?: "تعذر الربط، يرجى المحاولة مرة أخرى."
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "حدث خطأ أثناء الاتصال بقاعدة البيانات"
            } finally {
                isLoading = false
            }
        }
    }
    
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = "ربط كود قاعدة البيانات للمحل",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "مزامنة سحابية معزولة وفورية لنقاط البيع (Multi-Tenant)",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Informational Banner
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "بياناتك معزولة ومحمية سحابياً",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF134E4A)
                            )
                        }
                        Text(
                            text = "عند إدخال كود المحل، ينشئ النظام مجلداً فرعياً خاصاً بفواتيرك وأصنافك في قاعدة البيانات المركزية. وإذا أدخلت نفس الكود في جهاز كاشير آخر، تتزامن البيانات فوراً.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }
                
                // Form
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "كود توجيه المحل (Store Code):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "مثال: KHAMR-883",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it.uppercase() },
                            placeholder = {
                                Text(
                                    text = "أدخل كود المحل هنا (مثل: KHAMR-883)...",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = androidx.compose.ui.text.Spacer(2.dp)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        if (currentStoreCode != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "الكود المتصل حالياً:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                Surface(
                                    color = Color(0xFFF0FDFA),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF99F6E4))
                                ) {
                                    Text(
                                        text = currentStoreCode,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF134E4A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Feedback messages
                    if (errorMsg != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF2F2)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = errorMsg!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    
                    if (successMsg != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFECFDF5)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = successMsg!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                    
                    // Action Buttons
                    Button(
                        onClick = { handleConnect() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotateBy(360f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "جارِ الاتصال بالمساحة...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تفعيل وربط المساحة السحابية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
