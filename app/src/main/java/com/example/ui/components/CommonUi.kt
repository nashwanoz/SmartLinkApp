package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Invoice
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.utils.Formatters
import com.example.utils.NumberToArabicWords
import com.example.utils.WhatsAppHelper
import kotlin.math.abs

@Composable
fun AppHeader(
    currentUser: User?,
    settings: SystemSettings,
    onHomeClick: () -> Unit,
    onSwitchUserClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Business info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onHomeClick() }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(TealDark, TealPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SL",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = settings.businessName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${settings.address} • ${settings.currencySymbol}",
                            fontSize = 10.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active user & actions
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // User Switcher Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .clickable { onSwitchUserClick() }
                                .testTag("user_switcher_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TealPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.userCode,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = currentUser.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (currentUser.role == UserRole.ADMIN) "مدير النظام" else "كاشير",
                                        fontSize = 9.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Logout button
                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseContainer)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "تسجيل خروج",
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // System Status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldContainer)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "النظام المحاسبي الذكي جاهز ومتصل محلياً",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
                Text(
                    text = "v2.5 Pro",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun UserSwitcherModal(
    users: List<User>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onUserSwitched: (User, String) -> Boolean
) {
    var selectedTargetUser by remember { mutableStateOf<User?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تبديل المستخدم النشط",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedTargetUser == null) {
                    Text(
                        text = "اختر المستخدم الذي تريد التبديل إليه:",
                        fontSize = 12.sp,
                        color = Slate500,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    users.filter { it.active }.forEach { user ->
                        val isCurrent = user.id == currentUser?.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) TealContainer else Slate50,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) TealPrimary else Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (user.pin.isBlank()) {
                                        onUserSwitched(user, "")
                                    } else {
                                        selectedTargetUser = user
                                        pinInput = ""
                                        errorMessage = ""
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) TealPrimary else Slate700),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.userCode,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = if (user.role == UserRole.ADMIN) "مدير" else "كاشير",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "الحالي",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Enter PIN for target user
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "أدخل رمز PIN للمستخدم: ${selectedTargetUser?.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                errorMessage = ""
                            },
                            label = { Text("رمز PIN (كلمة المرور)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_pin_input")
                        )
                        if (errorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage,
                                color = RoseError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTargetUser != null) {
                Button(
                    onClick = {
                        val success = onUserSwitched(selectedTargetUser!!, pinInput)
                        if (!success) {
                            errorMessage = "رمز PIN غير صحيح!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("دخول")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedTargetUser != null) {
                        selectedTargetUser = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (selectedTargetUser != null) "رجوع" else "إلغاء")
            }
        }
    )
}

@Composable
fun InvoiceReceiptDialog(
    invoice: Invoice,
    settings: SystemSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyName = settings.currency.ifBlank { "ريال يمني" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "معاينة فاتورة مبيعات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = settings.businessName,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    Text(
                        text = settings.tagline,
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = "${settings.address} • هاتف: ${settings.phone}",
                        fontSize = 9.sp,
                        color = Slate400
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "رقم الفاتورة: ${invoice.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Formatters.formatDateTime(invoice.date),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "العميل: ${invoice.customerName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "الكاشير: ${invoice.cashierName}",
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    // Items table
                    Column(modifier = Modifier.fillMaxWidth()) {
                        invoice.items.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${idx + 1}. ${item.productName} (${Formatters.formatCurrency(item.quantity)} ${item.unitName})",
                                    fontSize = 11.sp,
                                    color = Slate800,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${Formatters.formatCurrency(item.total)} $currencyName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Slate900
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    // Totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي:", fontSize = 11.sp, color = Slate600)
                        Text("${Formatters.formatCurrency(invoice.subtotal)} $currencyName", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (invoice.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الخصم:", fontSize = 11.sp, color = RoseError)
                            Text("-${Formatters.formatCurrency(invoice.discount)} $currencyName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoseError)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الصافي المستحق:", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("${Formatters.formatCurrency(invoice.total)} $currencyName", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TealPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المدفوع نقداً:", fontSize = 11.sp, color = EmeraldSuccess)
                        Text("${Formatters.formatCurrency(invoice.paidAmount)} $currencyName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المتبقي كدين:", fontSize = 11.sp, color = AmberWarning)
                        Text("${Formatters.formatCurrency(invoice.remainingAmount)} $currencyName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    // Balance status
                    val debtStatus = if (invoice.newCustomerBalance >= 0) "عليه" else "له"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رصيد العميل الحالي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${Formatters.formatCurrency(abs(invoice.newCustomerBalance))} $currencyName [$debtStatus]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (invoice.newCustomerBalance > 0) RoseError else EmeraldSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "فقط: " + NumberToArabicWords.convert(invoice.newCustomerBalance, currencyName, "فلس"),
                        fontSize = 10.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val msg = WhatsAppHelper.formatInvoiceWhatsAppMessage(invoice, settings)
                    WhatsAppHelper.sendWhatsApp(context, invoice.customerMobile, msg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال واتساب")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val msg = WhatsAppHelper.formatInvoiceWhatsAppMessage(invoice, settings)
                    WhatsAppHelper.shareText(context, msg, "طباعة / مشاركة الفاتورة")
                }
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة النص")
            }
        }
    )
}

@Composable
fun BondReceiptDialog(
    bond: Bond,
    settings: SystemSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyName = settings.currency.ifBlank { "ريال يمني" }
    val isReceipt = bond.type == BondType.RECEIPT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isReceipt) "معاينة سند قبض" else "معاينة سند صرف",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = settings.businessName,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    Text(
                        text = if (isReceipt) "سند قبض مالي (توريد نقدية)" else "سند صرف مالي (صرف نقدية)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceipt) EmeraldSuccess else RoseError
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم السند: [${bond.bondNumber}]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(Formatters.formatDateTime(bond.date), fontSize = 10.sp, color = Slate500)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("اسم العميل: ${bond.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("المحرر: ${bond.cashierName}", fontSize = 10.sp, color = Slate500)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ المدفوع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${Formatters.formatCurrency(bond.amount)} $currencyName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isReceipt) EmeraldSuccess else RoseError
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "فقط: " + NumberToArabicWords.convert(bond.amount, currencyName, "فلس"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        textAlign = TextAlign.Center
                    )

                    if (bond.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "البيان: ${bond.note}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate300)

                    val status = if (bond.newCustomerBalance >= 0) "عليه" else "له"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رصيد العميل بعد السند:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${Formatters.formatCurrency(abs(bond.newCustomerBalance))} $currencyName [$status]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (bond.newCustomerBalance > 0) RoseError else EmeraldSuccess
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val msg = WhatsAppHelper.formatBondWhatsAppMessage(bond, settings)
                    WhatsAppHelper.sendWhatsApp(context, bond.customerMobile, msg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال واتساب")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val msg = WhatsAppHelper.formatBondWhatsAppMessage(bond, settings)
                    WhatsAppHelper.shareText(context, msg, "مشاركة السند")
                }
            ) {
                Text("مشاركة")
            }
        }
    )
}
