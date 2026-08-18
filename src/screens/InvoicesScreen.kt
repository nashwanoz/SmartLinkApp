package com.smartlink.erp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.utils.WhatsAppUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoicesScreen(
    invoices: List<Invoice>,
    customers: List<Customer>,
    currentUser: User,
    settings: SystemSettings
) {
    val context = LocalContext.current  // ✅ الحصول على Context
    
    var searchTerm by remember { mutableStateOf("") }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var copiedInvId by remember { mutableStateOf<String?>(null) }
    var limitCount by remember { mutableStateOf(10) }
    
    val filtered by remember {
        derivedStateOf {
            val s = searchTerm.lowercase().trim()
            if (s.isEmpty()) invoices
            else invoices.filter { inv ->
                inv.invoiceNumber.lowercase().contains(s) ||
                inv.customerName.lowercase().contains(s) ||
                inv.customerCode.contains(s) ||
                inv.cashierName.lowercase().contains(s)
            }
        }
    }
    
    val displayedInvoices by remember {
        derivedStateOf {
            filtered.reversed().take(limitCount)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ... (نفس الكود السابق - Header, Search) ...
        
        // Invoices List
        items(displayedInvoices, key = { it.id }) { inv ->
            InvoiceItem(
                invoice = inv,
                settings = settings,
                context = context,  // ✅ تمرير Context
                onPreviewClick = { selectedInvoice = inv }
            )
        }
        
        // ... (باقي الكود) ...
    }
    
    // DETAILED INVOICE MODAL PREVIEW
    selectedInvoice?.let { invoice ->
        InvoicePreviewModal(
            invoice = invoice,
            settings = settings,
            context = context,  // ✅ تمرير Context
            onClose = { selectedInvoice = null },
            onCopied = { copiedInvId = it }
        )
    }
}

@Composable
private fun InvoiceItem(
    invoice: Invoice,
    settings: SystemSettings,
    context: Context,  // ✅ إضافة Context
    onPreviewClick: () -> Unit
) {
    // ... (نفس الكود السابق) ...
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... (زر المعاينة) ...
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ✅ زر مشاركة PDF + نص
            Button(
                onClick = {
                    // TODO: Generate PDF first, then share
                    Toast.makeText(context, "جاري إنشاء PDF...", Toast.LENGTH_SHORT).show()
                    // sharePdfWithText(context, pdfFile, invoice.customerMobile ?: "", message)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669)
                ),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "مشاركة PDF + نص",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            // ... (زر الطباعة) ...
            
            // ✅ زر واتساب (نص فقط)
            Button(
                onClick = {
                    val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(invoice, settings)
                    WhatsAppUtils.sendTextOnly(context, invoice.customerMobile ?: "", message)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669)
                ),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "واتساب",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InvoicePreviewModal(
    invoice: Invoice,
    settings: SystemSettings,
    context: Context,  // ✅ إضافة Context
    onClose: () -> Unit,
    onCopied: (String) -> Unit
) {
    var copiedInvId by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onClose) {
        Card(
            // ... (نفس الكود السابق) ...
        ) {
            Column(
                // ...
            ) {
                // ... (Header) ...
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ زر مشاركة PDF + نص
                    Button(
                        onClick = {
                            // TODO: Generate PDF first
                            Toast.makeText(context, "جاري إنشاء PDF...", Toast.LENGTH_SHORT).show()
                            // val pdfFile = generateInvoicePdf(invoice, settings, context)
                            // val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(invoice, settings)
                            // WhatsAppUtils.sharePdfWithText(context, pdfFile, invoice.customerMobile ?: "", message)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "مشاركة الفاتورة (ملف PDF + النص) للجوال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ... (زر الطباعة) ...
                        
                        // ✅ زر واتساب (نص فقط)
                        Button(
                            onClick = {
                                val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(invoice, settings)
                                WhatsAppUtils.sendTextOnly(context, invoice.customerMobile ?: "", message)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "نص واتساب فقط",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // ✅ زر نسخ النص
                    Button(
                        onClick = {
                            val message = WhatsAppUtils.formatInvoiceWhatsAppMessage(invoice, settings)
                            val success = WhatsAppUtils.copyToClipboard(context, message)
                            if (success) {
                                copiedInvId = "modal-preview"
                                onCopied("modal-preview")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (copiedInvId == "modal-preview") {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "تم نسخ نص الفاتورة بنجاح ✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        } else {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "نسخ نص الفاتورة للحافظة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }
    }
}
