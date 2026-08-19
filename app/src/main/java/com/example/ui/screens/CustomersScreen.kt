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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.Customer
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSecondary
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
import com.example.utils.PermissionsHelper
import com.example.utils.WhatsAppHelper
import kotlin.math.abs

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    currentUser: User?,
    settings: SystemSettings,
    onSaveCustomer: (Customer) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onOpenBondModal: (Customer) -> Unit,
    onOpenStatement: (Customer) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }
    val canEditOpeningBalance = PermissionsHelper.hasPermission(currentUser) { it.canSetOpeningBalance }

    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.cCode.contains(searchQuery, ignoreCase = true) ||
                    it.mobile.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDebit = customers.filter { it.balance > 0 }.sumOf { it.balance }
    val totalCredit = customers.filter { it.balance < 0 }.sumOf { abs(it.balance) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Add Button
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
                    placeholder = { Text("بحث باسم العميل، الكود، أو الهاتف...", fontSize = 12.sp) },
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
                    onClick = {
                        editingCustomer = null
                        isAddDialogOpen = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("add_customer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عميل جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Balance Bar
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
                        text = "العملاء: ${customers.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "إجمالي الديون (عليهم): ${Formatters.formatCurrency(totalDebit)} $currencyName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseError
                    )
                }
            }

            // Customers List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { customer ->
                    val isDebit = customer.balance > 0
                    val isCredit = customer.balance < 0
                    val statusText = if (isDebit) "عليه" else if (isCredit) "له" else "متزن"
                    val statusColor = if (isDebit) RoseError else if (isCredit) MintSecondary else EmeraldSuccess
                    val statusBg = if (isDebit) RoseContainer else if (isCredit) MintContainer else EmeraldContainer

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
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(statusBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.cCode,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = statusColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Slate900
                                        )
                                        if (customer.mobile.isNotBlank()) {
                                            Text(
                                                text = "هاتف: ${customer.mobile}",
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }

                                // Balance badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${Formatters.formatCurrency(abs(customer.balance))} $currencyName",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = statusColor
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBg,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Bond (سند)
                                Button(
                                    onClick = { onOpenBondModal(customer) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سند مالي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                // Statement (كشف حساب)
                                Button(
                                    onClick = { onOpenStatement(customer) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كشف حساب", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                // WhatsApp Statement
                                IconButton(
                                    onClick = {
                                        val msg = WhatsAppHelper.formatCustomerStatementWhatsApp(
                                            customerName = customer.name,
                                            balance = customer.balance,
                                            invoicesCount = 0,
                                            totalPurchases = 0.0,
                                            totalPayments = 0.0,
                                            settings = settings
                                        )
                                        WhatsAppHelper.sendWhatsApp(context, customer.mobile, msg)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldContainer)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "واتساب", tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                }

                                // Edit
                                IconButton(
                                    onClick = {
                                        editingCustomer = customer
                                        isAddDialogOpen = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate100)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Slate700, modifier = Modifier.size(14.dp))
                                }

                                // Delete
                                IconButton(
                                    onClick = { customerToDelete = customer },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RoseContainer)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا يوجد عملاء مسجلين", color = Slate400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Add/Edit Customer Dialog
        if (isAddDialogOpen) {
            CustomerEditDialog(
                customer = editingCustomer,
                canEditOpeningBalance = canEditOpeningBalance,
                settings = settings,
                onDismiss = { isAddDialogOpen = false },
                onSave = { saved ->
                    onSaveCustomer(saved)
                    isAddDialogOpen = false
                }
            )
        }

        // Delete Confirm Dialog
        customerToDelete?.let { cust ->
            AlertDialog(
                onDismissRequest = { customerToDelete = null },
                title = { Text("تأكيد حذف العميل", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف العميل (${cust.name})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCustomer(cust.id)
                            customerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun CustomerEditDialog(
    customer: Customer?,
    canEditOpeningBalance: Boolean,
    settings: SystemSettings,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    val isEdit = customer != null
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var cCode by remember { mutableStateOf(customer?.cCode ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var balanceStr by remember { mutableStateOf(customer?.balance?.let { Formatters.formatCurrency(it) } ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل بيانات العميل" else "إضافة عميل جديد",
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = cCode,
                        onValueChange = { cCode = it },
                        label = { Text("كود العميل") },
                        placeholder = { Text("1001") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("رقم الجوال / واتساب") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المنطقة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (canEditOpeningBalance) {
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("الرصيد الافتتاحي (موجب = دين عليه، سالب = دائن)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val targetId = customer?.id ?: "cust_${System.currentTimeMillis()}"
                    val targetCode = if (cCode.isBlank()) (1000 + (1..900).random()).toString() else cCode
                    val targetBal = if (canEditOpeningBalance) {
                        balanceStr.replace(",", "").toDoubleOrNull() ?: 0.0
                    } else {
                        customer?.balance ?: 0.0
                    }

                    val result = Customer(
                        id = targetId,
                        cCode = targetCode,
                        name = name,
                        mobile = mobile,
                        balance = targetBal,
                        address = address,
                        createdAt = customer?.createdAt ?: Formatters.currentIsoDate()
                    )
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("حفظ العميل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
