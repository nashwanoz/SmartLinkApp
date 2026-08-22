package com.khamrnet.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.khamrnet.app.data.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class AppBackupData(
    val version: String = "2.0.0",
    val timestamp: String = "",
    val storeCode: String = "",
    val businessName: String = "",
    val settings: SystemSettingsEntity? = null,
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val bonds: List<BondEntity> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    settings: SystemSettingsEntity,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceEntity>,
    bonds: List<BondEntity>,
    onRestoreData: (
        products: List<ProductEntity>,
        customers: List<CustomerEntity>,
        invoices: List<InvoiceEntity>,
        bonds: List<BondEntity>,
        settings: SystemSettingsEntity?
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val gson: Gson = remember { GsonBuilder().setPrettyPrinting().create() }

    var backupJsonToExport by remember { mutableStateOf("") }
    var importedBackupData by remember { mutableStateOf<AppBackupData?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var rawPasteJsonText by remember { mutableStateOf("") }
    var isPasteMode by remember { mutableStateOf(false) }

    fun generateBackupData(): AppBackupData {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        return AppBackupData(
            version = "2.0.0",
            timestamp = sdf.format(Date()),
            storeCode = settings.storeCode,
            businessName = settings.businessName,
            settings = settings,
            products = products,
            customers = customers,
            invoices = invoices,
            bonds = bonds
        )
    }

    // Save File Launcher for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val data = generateBackupData()
                val jsonString = gson.toJson(data)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "✅ تم حفظ النسخة الاحتياطية بنجاح", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ حدث خطأ أثناء الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Open File Launcher for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val stringBuilder = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                reader.close()
                val jsonString = stringBuilder.toString()
                val parsed = gson.fromJson(jsonString, AppBackupData::class.java)
                if (parsed != null && (parsed.products.isNotEmpty() || parsed.customers.isNotEmpty() || parsed.invoices.isNotEmpty())) {
                    importedBackupData = parsed
                    showImportConfirmDialog = true
                } else {
                    Toast.makeText(context, "⚠️ الملف المحدد لا يحتوي على بيانات صالحة للنظام", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ فشلت قراءة ملف النسخة: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "النسخ الاحتياطي واستعادة البيانات",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "تصدير واستيراد بيانات المحل بصيغة JSON بأمان",
                                fontSize = 9.5.sp,
                                color = Color(0xFFCCFBF1)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F766E))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Current System Stats Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إحصائيات بيانات النظام الحالية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "[ ${settings.storeCode.ifEmpty { "غير متصل" }} ]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "الأصناف",
                                value = "${products.size}",
                                color = Color(0xFF0F766E)
                            )
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "العملاء",
                                value = "${customers.size}",
                                color = Color(0xFF2563EB)
                            )
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "الفواتير",
                                value = "${invoices.size}",
                                color = Color(0xFF7C3AED)
                            )
                            StatBox(
                                modifier = Modifier.weight(1f),
                                label = "السندات",
                                value = "${bonds.size}",
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // 2. Export Backup Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تصدير نسخة احتياطية جديدة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "توليد وحفظ ملف JSON يحتوي على كافة السجلات والبيانات",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
                                    val fileName = "khamrnet_backup_${settings.storeCode.ifEmpty { "local" }}_$dateStr.json"
                                    exportLauncher.launch(fileName)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "حفظ كملف JSON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val data = generateBackupData()
                                    val jsonString = gson.toJson(data)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Backup JSON", jsonString)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "📋 تم نسخ كود النسخة الاحتياطية إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF0F766E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نسخ كود JSON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Import / Restore Backup Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "استعادة البيانات من نسخة سابقة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "استيراد ملف JSON واستبدال أو دمج بيانات النظام",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Toggle Buttons: Pick File vs Paste Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اختيار ملف JSON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    isPasteMode = !isPasteMode
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPasteMode) Icons.Default.Close else Icons.Default.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPasteMode) "إغلاق اللصق" else "لصق نص JSON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isPasteMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = rawPasteJsonText,
                                    onValueChange = { rawPasteJsonText = it },
                                    label = { Text("الصق محتوى ملف JSON هنا") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = {
                                        try {
                                            val parsed = gson.fromJson(rawPasteJsonText, AppBackupData::class.java)
                                            if (parsed != null && (parsed.products.isNotEmpty() || parsed.customers.isNotEmpty() || parsed.invoices.isNotEmpty())) {
                                                importedBackupData = parsed
                                                showImportConfirmDialog = true
                                            } else {
                                                Toast.makeText(context, "⚠️ النص المدخل لا يطابق تركيبة النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ كود JSON غير صالح: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                                ) {
                                    Text(
                                        "فحص واستعادة كود JSON ⚡",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Instructions & Safety Advice Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFDE68A))
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "إرشادات الأمان والمزامنة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF92400E)
                            )
                        }

                        Text(
                            text = "• يُنصح بأخذ نسخة احتياطية يومية وحفظها على الذاكرة الخارجية أو سحابياً.",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            text = "• استعادة النسخة الاحتياطية ستقوم بحذف البيانات المحلية الحالية واستبدالها بالكامل ببيانات ملف النسخة.",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            text = "• عند الاتصال بالسيرفر السحابي، يتم رفع التحديثات تلقائياً وحمايتها من الضياع.",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Modal for Import
    if (showImportConfirmDialog && importedBackupData != null) {
        val backup = importedBackupData!!
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "تأكيد استعادة النسخة الاحتياطية",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "هل أنت متأكد من رغبتك في استعادة هذه النسخة؟ سيتم استبدال البيانات الحالية.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "اسم المحل: ${backup.businessName.ifEmpty { "غير محدد" }}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "كود المحل: ${backup.storeCode.ifEmpty { "لا يوجد" }}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "تاريخ النسخة: ${backup.timestamp.ifEmpty { "غير معروف" }}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Text(
                                "الأصناف: ${backup.products.size} | العملاء: ${backup.customers.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                "الفواتير: ${backup.invoices.size} | السندات: ${backup.bonds.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreData(
                            backup.products,
                            backup.customers,
                            backup.invoices,
                            backup.bonds,
                            backup.settings
                        )
                        showImportConfirmDialog = false
                        importedBackupData = null
                        isPasteMode = false
                        rawPasteJsonText = ""
                        Toast.makeText(context, "✅ تمت استعادة كافة بيانات النسخة الاحتياطية بنجاح!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("نعم، استعادة واستبدال", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showImportConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إلغاء", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.6.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
        }
    }
}
