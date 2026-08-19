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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemSettings
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.utils.WhatsAppHelper

@Composable
fun SettingsScreen(
    settings: SystemSettings,
    onSaveSettings: (SystemSettings) -> Unit,
    onClearAllData: () -> Unit,
    onResetToDefaults: () -> Unit,
    onExportBackupJson: () -> String,
    onImportBackupJson: (String) -> Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var businessName by remember { mutableStateOf(settings.businessName) }
    var tagline by remember { mutableStateOf(settings.tagline) }
    var address by remember { mutableStateOf(settings.address) }
    var phone by remember { mutableStateOf(settings.phone) }
    var currency by remember { mutableStateOf(settings.currency) }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var autoPrint by remember { mutableStateOf(settings.autoPrintAfterInvoice) }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var backupExportedText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "بيانات المنشأة والمتجر",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Slate900
                )
            }

            // Store Info Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("اسم المحل / المنشأة *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("وصف النشاط / الشعار") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("العنوان") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = { Text("اسم العملة (مثال: ريال يمني)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = currencySymbol,
                                onValueChange = { currencySymbol = it },
                                label = { Text("الرمز (YER)") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("الطباعة التلقائية بعد الفاتورة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("إظهار نافذة المعاينة والطباعة مباشرة", fontSize = 10.sp, color = Slate500)
                            }
                            Switch(
                                checked = autoPrint,
                                onCheckedChange = { autoPrint = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "النسخ الاحتياطي وإدارة قاعدة البيانات",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Slate900,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Backup & Data management card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export Backup
                        Button(
                            onClick = {
                                val json = onExportBackupJson()
                                backupExportedText = json
                                clipboardManager.setText(AnnotatedString(json))
                                WhatsAppHelper.shareText(context, json, "نسخة احتياطية خمر نت POS")
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير ومشاركة نسخة احتياطية (JSON)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Import Backup
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد نسخة احتياطية (JSON)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = Slate200)

                        // Clear Data
                        Button(
                            onClick = { showClearDataDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError.copy(alpha = 0.85f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصفير الفواتير والحركات (بدء فترة جديدة)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Reset to defaults
                        OutlinedButton(
                            onClick = { showResetDefaultsDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة ضبط المصنع بالكامل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Save Settings Button
        Button(
            onClick = {
                val updated = settings.copy(
                    businessName = businessName.ifBlank { "شبكة خمر نت اللاسلكية" },
                    tagline = tagline,
                    address = address,
                    phone = phone,
                    currency = currency.ifBlank { "ريال يمني" },
                    currencySymbol = currencySymbol.ifBlank { "YER" },
                    autoPrintAfterInvoice = autoPrint
                )
                onSaveSettings(updated)
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ الإعدادات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد نسخة احتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الصق محتوى ملف النسخة الاحتياطية (JSON):", fontSize = 12.sp, color = Slate700)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = onImportBackupJson(importJsonText)
                        if (ok) showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("استيراد الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("تأكيد تصفير العمليات", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف جميع الفواتير والسندات والتحويلات؟ ستبقى بيانات الأصناف والمستخدمين والعملاء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("تصفير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showResetDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showResetDefaultsDialog = false },
            title = { Text("تأكيد إعادة ضبط المصنع", fontWeight = FontWeight.Bold) },
            text = { Text("تحذير: سيتم حذف كافة البيانات واستعادة الإعدادات والمستخدمين الافتراضيين للنظام.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefaults()
                        showResetDefaultsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("إعادة ضبط المصنع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
