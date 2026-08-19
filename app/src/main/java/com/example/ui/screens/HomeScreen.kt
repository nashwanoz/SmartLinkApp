package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.ScreenTab
import com.example.utils.Formatters

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val screen: ScreenTab,
    val requiredPermission: ((com.example.data.model.UserPermissions) -> Boolean)? = null
)

@Composable
fun HomeScreen(
    currentUser: User?,
    settings: SystemSettings,
    stats: DashboardStats,
    onNavigate: (ScreenTab) -> Unit,
    canAccess: ((com.example.data.model.UserPermissions) -> Boolean) -> Boolean
) {
    val currencyName = settings.currencySymbol.ifBlank { "YER" }

    val allActions = listOf(
        QuickActionItem(
            title = "نقطة البيع (POS)",
            subtitle = "إصدار فواتير وسداد سريع",
            icon = Icons.Default.ShoppingCart,
            color = TealPrimary,
            bgColor = TealContainer,
            screen = ScreenTab.POS,
            requiredPermission = { it.canAccessPos }
        ),
        QuickActionItem(
            title = "الأصناف والمخزون",
            subtitle = "إدارة الأصناف والأسعار",
            icon = Icons.Default.Inventory2,
            color = BlueInfo,
            bgColor = BlueContainer,
            screen = ScreenTab.PRODUCTS,
            requiredPermission = { it.canAccessProducts }
        ),
        QuickActionItem(
            title = "دليل العملاء",
            subtitle = "أرصدة وبيانات العملاء",
            icon = Icons.Default.Group,
            color = MintSecondary,
            bgColor = MintContainer,
            screen = ScreenTab.CUSTOMERS,
            requiredPermission = { it.canAccessCustomers }
        ),
        QuickActionItem(
            title = "سندات القبض والصرف",
            subtitle = "تحصيل ودفع نقدية",
            icon = Icons.Default.Receipt,
            color = EmeraldSuccess,
            bgColor = EmeraldContainer,
            screen = ScreenTab.BONDS,
            requiredPermission = { it.canAccessBonds }
        ),
        QuickActionItem(
            title = "كشوفات الحسابات",
            subtitle = "كشف حساب تفصيلي للعملاء",
            icon = Icons.Default.AccountBalance,
            color = AmberWarning,
            bgColor = AmberContainer,
            screen = ScreenTab.STATEMENTS,
            requiredPermission = { it.canAccessStatements }
        ),
        QuickActionItem(
            title = "سجل الفواتير",
            subtitle = "أرشيف وطباعة الفواتير",
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF6366F1),
            bgColor = Color(0xFFEEF2FF),
            screen = ScreenTab.INVOICES,
            requiredPermission = { it.canAccessInvoices }
        ),
        QuickActionItem(
            title = "التحويل المخزني",
            subtitle = "تغذية عهد الكاشيرات",
            icon = Icons.Default.CompareArrows,
            color = Color(0xFF0284C7),
            bgColor = Color(0xFFE0F2FE),
            screen = ScreenTab.TRANSFER,
            requiredPermission = { it.canAccessTransfers }
        ),
        QuickActionItem(
            title = "تقارير إقفال الكاشير",
            subtitle = "ملخص الوردية والمبيعات",
            icon = Icons.Default.Assessment,
            color = Color(0xFF059669),
            bgColor = Color(0xFFECFDF5),
            screen = ScreenTab.CASHIER_REPORTS
        ),
        QuickActionItem(
            title = "المستخدمين والصلاحيات",
            subtitle = "إدارة الكاشيرات والـ PIN",
            icon = Icons.Default.Assignment,
            color = Color(0xFF8B5CF6),
            bgColor = Color(0xFFF5F3FF),
            screen = ScreenTab.USERS,
            requiredPermission = { it.canAccessUsers }
        ),
        QuickActionItem(
            title = "إعدادات النظام",
            subtitle = "البيانات والنسخ الاحتياطي",
            icon = Icons.Default.Settings,
            color = Slate800,
            bgColor = Slate200,
            screen = ScreenTab.SETTINGS,
            requiredPermission = { it.canAccessSettings }
        ),
        QuickActionItem(
            title = "حول النظام والترخيص",
            subtitle = "معلومات النظام والدعم",
            icon = Icons.Default.Info,
            color = Slate600,
            bgColor = Slate100,
            screen = ScreenTab.ABOUT
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Quick POS Hero Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigate(ScreenTab.POS) }
                .testTag("hero_pos_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(TealDark, TealPrimary, MintSecondary)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "نقطة البيع السريعة (POS)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إصدار فواتير نقدية وآجلة، دعم الباركود والوحدات",
                            color = TealContainer,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "POS",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // KPI Summary Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "مبيعات اليوم",
                value = "${Formatters.formatCurrency(stats.todaySales)} $currencyName",
                subtitle = "إجمالي الفواتير الصادرة",
                icon = Icons.Default.TrendingUp,
                color = EmeraldSuccess,
                bgColor = EmeraldContainer,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenTab.INVOICES) }
            )
            StatCard(
                title = "إجمالي المديونيات",
                value = "${Formatters.formatCurrency(stats.totalDebt)} $currencyName",
                subtitle = "مستحقات على العملاء",
                icon = Icons.Default.AccountBalance,
                color = RoseError,
                bgColor = RoseContainer,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenTab.CUSTOMERS) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "سندات القبض",
                value = "${Formatters.formatCurrency(stats.totalReceipts)} $currencyName",
                subtitle = "نقدية محصلة",
                icon = Icons.Default.Receipt,
                color = MintSecondary,
                bgColor = MintContainer,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenTab.BONDS) }
            )
            StatCard(
                title = "الفواتير المسجلة",
                value = "${stats.invoicesCount} فاتورة",
                subtitle = "${stats.customersCount} عميل • ${stats.productsCount} صنف",
                icon = Icons.Default.ReceiptLong,
                color = BlueInfo,
                bgColor = BlueContainer,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenTab.INVOICES) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sections Title
        Text(
            text = "أقسام النظام والعمليات اليومية",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Slate800,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Action Items
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            allActions.forEach { action ->
                val hasAccess = if (action.requiredPermission != null) canAccess(action.requiredPermission) else true
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (hasAccess) Color.White else Slate100),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (hasAccess) 1.dp else 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (hasAccess) Slate200 else Slate200, RoundedCornerShape(16.dp))
                        .clickable(enabled = hasAccess) { onNavigate(action.screen) }
                        .testTag("action_${action.screen.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hasAccess) action.bgColor else Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.title,
                                    tint = if (hasAccess) action.color else Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = action.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasAccess) Slate900 else Slate400
                                )
                                Text(
                                    text = if (hasAccess) action.subtitle else "غير مصرح لحسابك",
                                    fontSize = 10.sp,
                                    color = if (hasAccess) Slate500 else RoseError
                                )
                            }
                        }

                        Text(
                            text = "❯",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasAccess) Slate400 else Slate300
                        )
                    }
                }
            }
        }
    }
}
