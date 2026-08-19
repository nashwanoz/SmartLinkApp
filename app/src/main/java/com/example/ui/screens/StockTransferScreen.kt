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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StockTransfer
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
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

@Composable
fun StockTransferScreen(
    transfers: List<StockTransfer>,
    products: List<Product>,
    users: List<User>,
    currentUser: User?,
    settings: SystemSettings,
    onExecuteTransfer: (Long, Double, User, String) -> Unit
) {
    var isAddModalOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التحويل وتغذية عهد الكاشيرات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text(
                        text = "تحويل كميات من المخزن الرئيسي إلى نقاط البيع",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }

                Button(
                    onClick = { isAddModalOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحويل جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Stats
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
                        text = "سجل التحويلات: ${transfers.size} عملية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "المحول: المخزن الرئيسي ❯ عهد الكاشير",
                        fontSize = 10.sp,
                        color = TealDark
                    )
                }
            }

            // Transfers History List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(transfers) { transfer ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TealContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = transfer.productName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "عملية: [${transfer.transferNumber}] • ${Formatters.formatDateTime(transfer.date)}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldContainer
                                ) {
                                    Text(
                                        text = "+${Formatters.formatCurrency(transfer.quantity)} ${transfer.unitName}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate50, RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إلى الكاشير: ${transfer.toCashierName} (${transfer.toCashierCode})", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                Text("المشرف: ${transfer.adminName}", fontSize = 10.sp, color = Slate500)
                            }

                            if (transfer.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ملاحظة: ${transfer.note}", fontSize = 10.sp, color = Slate500)
                            }
                        }
                    }
                }

                if (transfers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد عمليات تحويل مخزني مسجلة", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Add Transfer Dialog
        if (isAddModalOpen) {
            ExecuteTransferDialog(
                products = products,
                users = users.filter { it.active },
                onDismiss = { isAddModalOpen = false },
                onConfirm = { prodId, qty, cashier, note ->
                    onExecuteTransfer(prodId, qty, cashier, note)
                    isAddModalOpen = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteTransferDialog(
    products: List<Product>,
    users: List<User>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, User, String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var selectedCashier by remember { mutableStateOf<User?>(users.firstOrNull { it.id != "user_101" } ?: users.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("10") }
    var note by remember { mutableStateOf("") }

    var isProdDropdownExpanded by remember { mutableStateOf(false) }
    var isUserDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحويل كمية من المخزن إلى كاشير",
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
                // Product selector
                ExposedDropdownMenuBox(
                    expanded = isProdDropdownExpanded,
                    onExpandedChange = { isProdDropdownExpanded = !isProdDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.name} (المخزن: ${Formatters.formatCurrency(it.stockMain)} ${it.unitName})" } ?: "اختر الصنف",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصنف المراد تحويله") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProdDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isProdDropdownExpanded,
                        onDismissRequest = { isProdDropdownExpanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("رصيد المخزن: ${Formatters.formatCurrency(prod.stockMain)} ${prod.unitName}", fontSize = 10.sp, color = TealDark)
                                    }
                                },
                                onClick = {
                                    selectedProduct = prod
                                    isProdDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Cashier selector
                ExposedDropdownMenuBox(
                    expanded = isUserDropdownExpanded,
                    onExpandedChange = { isUserDropdownExpanded = !isUserDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCashier?.let { "${it.name} (${it.userCode})" } ?: "اختر الكاشير",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الكاشير المستلم للعهدة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isUserDropdownExpanded,
                        onDismissRequest = { isUserDropdownExpanded = false }
                    ) {
                        users.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.name} (كود: ${u.userCode})", fontSize = 12.sp) },
                                onClick = {
                                    selectedCashier = u
                                    isUserDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("الكمية المحولة (بالـ ${selectedProduct?.unitName ?: "حبة"}) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("البيان / ملاحظات التحويل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prod = selectedProduct ?: return@Button
                    val cashier = selectedCashier ?: return@Button
                    val qty = quantityStr.replace(",", "").toDoubleOrNull() ?: 0.0
                    if (qty <= 0) return@Button
                    onConfirm(prod.id, qty, cashier, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("تأكيد التحويل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
