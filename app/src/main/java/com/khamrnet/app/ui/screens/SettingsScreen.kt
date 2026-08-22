package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.khamrnet.app.printer.BluetoothPrinterManager
import com.khamrnet.app.printer.PrinterDeviceInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SystemSettingsEntity,
    onSaveSettings: (SystemSettingsEntity) -> Unit,
    onManualSync: (storeCode: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    var storeCode by remember { mutableStateOf(settings.storeCode) }
    var businessName by remember { mutableStateOf(settings.businessName) }
    var phone by remember { mutableStateOf(settings.phone) }
    var address by remember { mutableStateOf(settings.address) }
    var currencyName by remember { mutableStateOf(settings.currencyName) }
    var invoiceFooterMessage by remember { mutableStateOf(settings.invoiceFooterMessage) }
    var thermalPaperWidth by remember { mutableStateOf(settings.thermalPaperWidth) }

    var pairedDevices by remember { mutableStateOf<List<PrinterDeviceInfo>>(emptyList()) }
    var selectedPrinterMac by remember { mutableStateOf(settings.defaultPrinterMac) }
    var selectedPrinterName by remember { mutableStateOf(settings.defaultPrinterName) }

    // Load paired devices
    LaunchedEffect(Unit) {
        pairedDevices = printerManager.getPairedPrinters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("إعدادات النظام والمزامنة والطابعة", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F766E))
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Store Code & Cloud Sync Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF0F766E))
                        Text("كود المحل والمزامنة السحابية", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
                    }

                    Text(
                        "أدخل كود المحل لربط هذا الجهاز بالسحابة لمزامنة الفواتير والمخزون والحسابات بين جميع الأجهزة:",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = storeCode,
                        onValueChange = { storeCode = it.uppercase() },
                        label = { Text("كود المحل (Store Code) *", fontSize = 11.sp) },
                        placeholder = { Text("مثال: KHAMR01", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (storeCode.trim().isEmpty()) {
                                Toast.makeText(context, "يرجى كتابة كود المحل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onManualSync(storeCode.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مزامنة فورية الآن مع السحابة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Business Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("معلومات المنشأة والترويسة", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("اسم المحل / المنشأة", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currencyName,
                            onValueChange = { currencyName = it },
                            label = { Text("العملة", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان / الموقع", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = invoiceFooterMessage,
                        onValueChange = { invoiceFooterMessage = it },
                        label = { Text("رسالة تذييل الفاتورة", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Thermal Printer Bluetooth Config
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إعدادات الطابعة الحرارية والبلوتوث", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))

                    // Paper Width (80mm vs 58mm)
                    Text("مقاس رول الورق الحراري:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { thermalPaperWidth = "80mm" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (thermalPaperWidth == "80mm") Color(0xFF0F766E) else Color.Transparent
                        ) {
                            Text(
                                "80mm (كبير)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (thermalPaperWidth == "80mm") Color.White else Color(0xFF475569),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            onClick = { thermalPaperWidth = "58mm" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (thermalPaperWidth == "58mm") Color(0xFF0F766E) else Color.Transparent
                        ) {
                            Text(
                                "58mm (صغير/محمول)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (thermalPaperWidth == "58mm") Color.White else Color(0xFF475569),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Paired Bluetooth Devices List
                    Text("طابعات البلوتوث المقترنة بالهاتف:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    if (pairedDevices.isEmpty()) {
                        Text("لا توجد أجهزة بلوتوث مقترنة. يرجى إقران الطابعة أولاً من إعدادات البلوتوث بالهاتف.", fontSize = 10.sp, color = Color(0xFFDC2626))
                    } else {
                        pairedDevices.forEach { dev ->
                            val isSelected = selectedPrinterMac == dev.address
                            Surface(
                                onClick = {
                                    selectedPrinterMac = dev.address
                                    selectedPrinterName = dev.name
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF0F766E) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dev.name, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        Text(dev.address, fontSize = 9.sp, color = Color(0xFF64748B))
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Test Print Button
                    if (selectedPrinterMac.isNotEmpty()) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = printerManager.testPrint(selectedPrinterMac, thermalPaperWidth)
                                    if (res.isSuccess) Toast.makeText(context, "✅ تمت طباعة ورقة الاختبار بنجاح", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "فشل الاتصال بالطابعة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة تجريبية على الطابعة المحددة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    val updated = settings.copy(
                        storeCode = storeCode.trim().ifEmpty { "KHAMR01" },
                        businessName = businessName.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        currencyName = currencyName.trim(),
                        invoiceFooterMessage = invoiceFooterMessage.trim(),
                        thermalPaperWidth = thermalPaperWidth,
                        defaultPrinterMac = selectedPrinterMac,
                        defaultPrinterName = selectedPrinterName
                    )
                    onSaveSettings(updated)
                    Toast.makeText(context, "✅ تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ كافة الإعدادات", fontSize = 13.5.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
