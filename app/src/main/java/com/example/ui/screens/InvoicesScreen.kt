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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.example.data.model.Invoice
import com.example.data.model.PaymentMethod
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.InvoiceReceiptDialog
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BlueInfo
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
import com.example.utils.WhatsAppHelper

@Composable
fun InvoicesScreen(
    invoices: List<Invoice>,
    currentUser: User?,
    settings: SystemSettings,
    onDeleteInvoice: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var viewingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var selectedMethodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

    val currencyName = settings.currencySymbol.ifBlank { "YER" }
    val isManager = currentUser?.role == UserRole.ADMIN

    val filteredInvoices = remember(invoices, searchQuery, selectedMethodFilter) {
        invoices.filter { inv ->
            val matchMethod = selectedMethodFilter == null || inv.paymentMethod == selectedMethodFilter
            val matchSearch = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.cashierName.contains(searchQuery, ignoreCase = true)
            matchMethod && matchSearch
        }
    }

    val totalSales = invoices.sumOf { it.total }
    val totalPaid = invoices.sumOf { it.paidAmount }
    val totalRemaining = invoices.sumOf { it.remainingAmount }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث برقم الفاتورة، العميل، الكاشير...", fontSize = 12.sp) },
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment method filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedMethodFilter == null) TealPrimary else Slate100,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMethodFilter = null }
                ) {
                    Text(
                        text = "الكل (${invoices.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMethodFilter == null) Color.White else Slate700,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedMethodFilter == PaymentMethod.CASH) EmeraldSuccess else EmeraldContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMethodFilter = PaymentMethod.CASH }
                ) {
                    Text(
                        text = "نقداً (كاش)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMethodFilter == PaymentMethod.CASH) Color.White else EmeraldSuccess,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedMethodFilter == PaymentMethod.CREDIT) AmberWarning else AmberContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMethodFilter = PaymentMethod.CREDIT }
                ) {
                    Text(
                        text = "آجل (دين)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMethodFilter == PaymentMethod.CREDIT) Color.White else AmberWarning,
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
                        text = "المبيعات: ${Formatters.formatCurrency(totalSales)} $currencyName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Text(
                        text = "المحصل: ${Formatters.formatCurrency(totalPaid)} $currencyName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "المتبقي: ${Formatters.formatCurrency(totalRemaining)} $currencyName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseError
                    )
                }
            }

            // Invoices List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredInvoices) { invoice ->
                    val methodText = when (invoice.paymentMethod) {
                        PaymentMethod.CASH -> "نقداً"
                        PaymentMethod.CREDIT -> "آجل"
                        PaymentMethod.PARTIAL -> "جزئي"
                    }
                    val methodColor = when (invoice.paymentMethod) {
                        PaymentMethod.CASH -> EmeraldSuccess
                        PaymentMethod.CREDIT -> AmberWarning
                        PaymentMethod.PARTIAL -> BlueInfo
                    }
                    val methodBg = when (invoice.paymentMethod) {
                        PaymentMethod.CASH -> EmeraldContainer
                        PaymentMethod.CREDIT -> AmberContainer
                        PaymentMethod.PARTIAL -> BlueContainer
                    }

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
                                        color = methodBg,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = methodText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = methodColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = invoice.customerName,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "فاتورة: [${invoice.invoiceNumber}] • ${Formatters.formatDateTime(invoice.date)}",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Text(
                                    text = "${Formatters.formatCurrency(invoice.total)} $currencyName",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = TealPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الأصناف (${invoice.items.size}): " + invoice.items.joinToString("، ") { "${it.productName} (${it.quantity.toInt()} ${it.unitName})" },
                                fontSize = 10.sp,
                                color = Slate600,
                                maxLines = 1
                            )

                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Slate100)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الكاشير: ${invoice.cashierName}",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewingInvoice = invoice },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TealContainer)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = TealPrimary, modifier = Modifier.size(15.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val msg = WhatsAppHelper.formatInvoiceWhatsAppMessage(invoice, settings)
                                            WhatsAppHelper.sendWhatsApp(context, invoice.customerMobile, msg)
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
                                            onClick = { invoiceToDelete = invoice },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RoseContainer)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الفاتورة", tint = RoseError, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredInvoices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد فواتير مطابقة", color = Slate400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Viewing Receipt Dialog
        viewingInvoice?.let { inv ->
            InvoiceReceiptDialog(
                invoice = inv,
                settings = settings,
                onDismiss = { viewingInvoice = null }
            )
        }

        // Delete Confirm Dialog
        invoiceToDelete?.let { inv ->
            AlertDialog(
                onDismissRequest = { invoiceToDelete = null },
                title = { Text("تأكيد حذف الفاتورة", fontWeight = FontWeight.Bold) },
                text = {
                    Text("هل أنت متأكد من حذف الفاتورة رقم [${inv.invoiceNumber}]؟")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteInvoice(inv.id)
                            invoiceToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { invoiceToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
