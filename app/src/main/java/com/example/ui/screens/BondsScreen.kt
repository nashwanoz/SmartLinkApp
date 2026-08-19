package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Customer
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.BondReceiptDialog
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.utils.Formatters
import com.example.utils.NumberToArabicWords
import com.example.utils.WhatsAppHelper
import kotlin.math.abs

@Composable
fun BondsScreen(
    bonds: List<Bond>,
    customers: List<Customer>,
    currentUser: User?,
    settings: SystemSettings,
    preselectedCustomer: Customer? = null,
    onSaveBond: (BondType, Customer, Double, String) -> Bond?,
    onDeleteBond: (String) -> Unit,
    onClearPreselectedCustomer: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilterType by remember { mutableStateOf<BondType?>(null) } // null = All
    var searchQuery by remember { mutableStateOf("") }
    var isCreateDialogOpen by remember { mutableStateOf(preselectedCustomer != null) }
    var viewingBond by remember { mutableStateOf<Bond?>(null) }
    var bondToDelete by remember { mutableStateOf<Bond?>(null) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }
    val isManager = currentUser?.role == UserRole.ADMIN

    val filteredBonds = remember(bonds, selectedFilterType, searchQuery) {
        bonds.filter { b ->
            val matchType = selectedFilterType == null || b.type == selectedFilterType
            val matchSearch = searchQuery.isBlank() ||
                    b.customerName.contains(searchQuery, ignoreCase = true) ||
                    b.bondNumber.contains(searchQuery, ignoreCase = true) ||
                    b.note.contains(searchQuery, ignoreCase = true)
            matchType && matchSearch
        }
    }

    val totalReceipts = bonds.filter { it.type == BondType.RECEIPT }.sumOf { it.amount }
    val totalPayments = bonds.filter { it.type == BondType.PAYMENT }.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Search & Add
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث برقم السند، العميل، البيان...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Slate400)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { isCreateDialogOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("add_bond_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سند جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilterType == null) TealPrimary else Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFilterType == null) TealPrimary else Slate200),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilterType = null }
                ) {
                    Text(
                        text = "الكل (${bonds.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedFilterType == null) Color.White else Slate700,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilterType == BondType.RECEIPT) EmeraldSuccess else EmeraldContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilterType = BondType.RECEIPT }
                ) {
                    Text(
                        text = "سندات القبض (توريد)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedFilterType == BondType.RECEIPT) Color.White else EmeraldSuccess,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilterType == BondType.PAYMENT) RoseError else RoseContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseError),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilterType = BondType.PAYMENT }
                ) {
                    Text(
                        text = "سندات الصرف (دفع)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedFilterType == BondType.PAYMENT) Color.White else RoseError,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            // Summary Totals
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي المقبوضات: ${Formatters.formatCurrency(totalReceipts)} $currencyName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "إجمالي المصروفات: ${Formatters.formatCurrency(totalPayments)} $currencyName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseError
                    )
                }
            }

            // Bonds List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredBonds) { bond ->
                    val isReceipt = bond.type == BondType.RECEIPT
                    val typeText = if (isReceipt) "سند قبض" else "سند صرف"
                    val typeColor = if (isReceipt) EmeraldSuccess else RoseError
                    val typeBg = if (isReceipt) EmeraldContainer else RoseContainer

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = typeBg,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = typeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = typeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = bond.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "سند رقم: [${bond.bondNumber}] • ${Formatters.formatDateTime(bond.date)}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Text(
                                    text = "${Formatters.formatCurrency(bond.amount)} $currencyName",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = typeColor
                                )
                            }

                            if (bond.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "البيان: ${bond.note}",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Slate100)

                            // Footer with Balance after bond and Actions
                            val debtStatus = if (bond.newCustomerBalance >= 0) "عليه" else "له"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الرصيد بعد السند: ${Formatters.formatCurrency(abs(bond.newCustomerBalance))} $currencyName [$debtStatus]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (bond.newCustomerBalance > 0) RoseError else EmeraldSuccess
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewingBond = bond },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TealContainer)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = TealPrimary, modifier = Modifier.size(15.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val msg = WhatsAppHelper.formatBondWhatsAppMessage(bond, settings)
                                            WhatsAppHelper.sendWhatsApp(context, bond.customerMobile, msg)
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldContainer)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "واتساب", tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                    }

                                    if (isManager) {
                                        IconButton(
                                            onClick = { bondToDelete = bond },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RoseContainer)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف السند", tint = RoseError, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredBonds.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد سندات مسجلة", color = Slate400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Create Bond Modal Dialog
        if (isCreateDialogOpen) {
            CreateBondDialog(
                customers = customers,
                preselectedCustomer = preselectedCustomer,
                settings = settings,
                onDismiss = {
                    isCreateDialogOpen = false
                    onClearPreselectedCustomer()
                },
                onSave = { type, customer, amount, note ->
                    val created = onSaveBond(type, customer, amount, note)
                    isCreateDialogOpen = false
                    onClearPreselectedCustomer()
                    if (created != null) {
                        viewingBond = created
                    }
                }
            )
        }

        // Bond Thermal Receipt Dialog
        viewingBond?.let { bond ->
            BondReceiptDialog(
                bond = bond,
                settings = settings,
                onDismiss = { viewingBond = null }
            )
        }

        // Delete Confirm Dialog
        bondToDelete?.let { b ->
            AlertDialog(
                onDismissRequest = { bondToDelete = null },
                title = { Text("تأكيد حذف السند", fontWeight = FontWeight.Bold) },
                text = {
                    Text("هل أنت متأكد من حذف السند رقم [${b.bondNumber}]؟ سيتم عكس تأثير المبلغ على رصيد العميل (${b.customerName}) تلقائياً.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteBond(b.id)
                            bondToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("حذف وعكس الرصيد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bondToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBondDialog(
    customers: List<Customer>,
    preselectedCustomer: Customer?,
    settings: SystemSettings,
    onDismiss: () -> Unit,
    onSave: (BondType, Customer, Double, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(BondType.RECEIPT) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(preselectedCustomer ?: customers.firstOrNull()) }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val currencyName = settings.currency.ifBlank { "ريال يمني" }
    val amountNum = amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
    val tafqeetText = if (amountNum > 0) NumberToArabicWords.convert(amountNum, currencyName, "فلس") else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedType == BondType.RECEIPT) "تحرير سند قبض (توريد نقدية)" else "تحرير سند صرف (دفع نقدية)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Selector Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedType == BondType.RECEIPT) EmeraldSuccess else EmeraldContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = BondType.RECEIPT }
                    ) {
                        Text(
                            text = "سند قبض (استلام)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == BondType.RECEIPT) Color.White else EmeraldSuccess,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedType == BondType.PAYMENT) RoseError else RoseContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = BondType.PAYMENT }
                    ) {
                        Text(
                            text = "سند صرف (دفع)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == BondType.PAYMENT) Color.White else RoseError,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Customer Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCustomerDropdownExpanded,
                    onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (الرصيد: ${Formatters.formatCurrency(it.balance)})" } ?: "اختر العميل",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العميل *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCustomerDropdownExpanded,
                        onDismissRequest = { isCustomerDropdownExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "الرصيد: ${Formatters.formatCurrency(cust.balance)} $currencyName",
                                            fontSize = 10.sp,
                                            color = if (cust.balance > 0) RoseError else EmeraldSuccess
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCustomer = cust
                                    isCustomerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (${settings.currencySymbol}) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (tafqeetText.isNotBlank()) {
                    Text(
                        text = "فقط: $tafqeetText",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealDark
                    )
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("البيان / ملاحظات") },
                    placeholder = { Text(if (selectedType == BondType.RECEIPT) "سداد من الحساب / دفعة نقدية" else "صرف نقدي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val customer = selectedCustomer ?: return@Button
                    if (amountNum <= 0) return@Button
                    onSave(selectedType, customer, amountNum, note)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == BondType.RECEIPT) EmeraldSuccess else RoseError
                )
            ) {
                Text("حفظ وإصدار السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
