package com.khamrnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreActivationDialog(
    currentStoreCode: String = "",
    businessName: String = "",
    isOpen: Boolean = true,
    onDismiss: () -> Unit = {},
    onConfirmActivation: (newStoreCode: String) -> Unit = {},
    onSaveStoreCode: ((newStoreCode: String) -> Unit)? = null
) {
    if (!isOpen) return

    val handleSave = onSaveStoreCode ?: onConfirmActivation

    var inputCode by remember(currentStoreCode) { mutableStateOf(currentStoreCode) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0FDFA))
                                .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ربط كود قاعدة البيانات للمحل",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "مزامنة سحابية معزولة وفورية لنقاط البيع",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Info Box (Matching Web)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "بياناتك معزولة ومحمية سحابياً",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                        Text(
                            text = "عند إدخال كود المحل، ينشئ النظام مساحة خاصة بفواتيرك وأصنافك في قاعدة البيانات المركزية، وتتزامن البيانات تلقائياً بين تطبيق الأندرويد وصفحة الويب.",
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }
                }

                // Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "كود توجيه المحل (Store Code):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = {
                            inputCode = it.uppercase().trim()
                            errorMessage = null
                        },
                        placeholder = { Text("مثال: KHAMR01 أو STORE-01", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F766E),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    if (currentStoreCode.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "الكود المتصل حالياً:",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF0FDFA))
                                    .border(0.6.dp, Color(0xFF99F6E4), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = currentStoreCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }
                    }
                }

                // Error Message if any
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF1F2))
                            .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                fontSize = 10.sp,
                                color = Color(0xFF9F1239),
                                fontWeight = FontWeight.Bold
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
                        onClick = onDismiss,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val code = inputCode.trim().uppercase()
                            if (code.length < 3) {
                                errorMessage = "يرجى إدخال كود صحيح يتكون من 3 أحرف على الأقل"
                                return@Button
                            }
                            isLoading = true
                            handleSave(code)
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.weight(2f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الربط والمزامنة...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تفعيل وربط المساحة السحابية", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
