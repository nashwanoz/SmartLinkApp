package com.smartlink.erp.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    currentUser: User,
    settings: SystemSettings,
    products: List<Product>,
    customers: List<Customer>,
    invoices: List<Invoice>,
    bonds: List<Bond>,
    onNavigate: (String) -> Unit,
    onOpenPos: () -> Unit
) {
    // Compute key stats
    val totalSales by remember {
        derivedStateOf {
            invoices.sumOf { it.total }
        }
    }
    
    val totalCashCollected by remember {
        derivedStateOf {
            invoices.sumOf { it.paidAmount } +
            bonds.filter { it.type.name == "RECEIPT" }.sumOf { it.amount }
        }
    }
    
    val totalCustomerDebts by remember {
        derivedStateOf {
            customers.sumOf { c -> if (c.balance > 0) c.balance else 0.0 }
        }
    }
    
    // Total inventory units in all warehouses
    val totalStockUnits by remember {
        derivedStateOf {
            products.sumOf { p ->
                val cashierUnits = p.stockCashier?.values?.sumOf { it ?: 0.0 } ?: 0.0
                p.stockMain + cashierUnits
            }
        }
    }
    
    // Recent 5 activities (invoices + bonds combined)
    val recentInvoices by remember {
        derivedStateOf {
            invoices.takeLast(3).reversed()
        }
    }
    
    val recentBonds by remember {
        derivedStateOf {
            bonds.takeLast(3).reversed()
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick POS Launch Button
        if (hasPermission(currentUser, "canAccessPos")) {
            item {
                Button(
                    onClick = onOpenPos,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F766E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    text = "فتح نقطة البيع (POS)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "إصدار فاتورة نقدية أو آجلة وسداد جزئي",
                                    fontSize = 11.sp,
                                    color = Color(0xCCFFFFFF)
                                )
                            }
                        }
                        
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "دخول البيع ⚡",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Stats Overview - 2 items (Total Sales & Collected)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "إجمالي المبيعات",
                    value = String.format("%,.2f", totalSales),
                    currency = settings.currencySymbol,
                    subtitle = "${invoices.size} فواتير مسجلة",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "المبالغ المقبوضة",
                    value = String.format("%,.2f", totalCashCollected),
                    currency = settings.currencySymbol,
                    subtitle = "فواتير + سندات",
                    icon = Icons.Default.CreditCard,
                    iconColor = Color(0xFF059669),
                    valueColor = Color(0xFF047857),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Main Screen Navigation Grid - 3 columns per row
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "شاشات وأقسام النظام المتاحة لك",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF475569)
                )
                
                NavigationGrid(
                    currentUser = currentUser,
                    onNavigate = onNavigate
                )
            }
        }
        
        // Recent Activity Mini-List (Last 3 Invoices)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "آخر الفواتير المسجلة (${recentInvoices.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        
                        TextButton(
                            onClick = { onNavigate("invoices") }
                        ) {
                            Text(
                                text = "عرض الكل في صفحة الفواتير ←",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }
                    
                    if (recentInvoices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد فواتير مسجلة بعد",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentInvoices.forEach { inv ->
                                RecentInvoiceItem(
                                    invoice = inv,
                                    settings = settings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    currency: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    valueColor: Color = Color(0xFF1E293B),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            
            Text(
                text = "$value $currency",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun NavigationGrid(
    currentUser: User,
    onNavigate: (String) -> Unit
) {
    val navItems = listOf(
        NavItem("products", "بيانات الاصناف", Icons.Default.Inventory, Color(0xFF0F766E), Color(0xFFD1FAE5), "canAccessProducts"),
        NavItem("customers", "العملاء", Icons.Default.People, Color(0xFF2563EB), Color(0xFFDBEAFE), "canAccessCustomers"),
        NavItem("statements", "كشف حساب", Icons.Default.List, Color(0xFF92400E), Color(0xFFFEF3C7), "canAccessStatements"),
        NavItem("invoices", "سجل الفواتير", Icons.Default.ReceiptLong, Color(0xFF7C3AED), Color(0xFFEDE9FE), "canAccessInvoices"),
        NavItem("bonds", "السندات", Icons.Default.Description, Color(0xFF047857), Color(0xFFECFDF5), "canAccessBonds"),
        NavItem("transfer", "التحويل المخزني", Icons.Default.SwapHoriz, Color(0xFFEA580C), Color(0xFFFFF7ED), "canAccessTransfers"),
        NavItem("users", "الكاشير", Icons.Default.PersonCheck, Color(0xFF4F46E5), Color(0xFFE0E7FF), "canAccessUsers"),
        NavItem("settings", "إعدادات النظام", Icons.Default.Settings, Color(0xFF475569), Color(0xFFF1F5F9), "canAccessSettings"),
        NavItem("about", "حول البرنامج", Icons.Default.Info, Color(0xFF0F766E), Color(0xFFD1FAE5), null)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        navItems.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    if (item.permission == null || hasPermission(currentUser, item.permission)) {
                        NavigationButton(
                            item = item,
                            onClick = { onNavigate(item.id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationButton(
    item: NavItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = item.iconBgColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, item.iconBgColor.copy(alpha = 0.5f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1
            )
        }
    }
}

private data class NavItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val permission: String?
)

@Composable
private fun RecentInvoiceItem(
    invoice: Invoice,
    settings: SystemSettings
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFCCFBF1),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = invoice.invoiceNumber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF134E4A),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = invoice.customerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = formatDateTime(invoice.date),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${String.format("%,.2f", invoice.total)} ${settings.currencySymbol}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
    return sdf.format(Date(timestamp))
}

private fun hasPermission(user: User, permission: String): Boolean {
    return user.role == "ADMIN" || user.permissions?.contains(permission) == true
}
