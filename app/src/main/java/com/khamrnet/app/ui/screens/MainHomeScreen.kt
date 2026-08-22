package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.khamrnet.app.sync.SyncState
import com.khamrnet.app.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

data class MainMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    syncStatus: SyncStatus,
    onTriggerSync: () -> Unit,
    onNavigate: (screenRoute: String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val menuItems = listOf(
        MainMenuItem(
            id = "pos",
            title = "نقطة البيع (الكاشير)",
            subtitle = "إصدار فواتير نقدية وآجلة وطباعة فورية",
            icon = Icons.Default.PointOfSale,
            color = Color(0xFF0F766E)
        ),
        MainMenuItem(
            id = "invoices",
            title = "سجل الفواتير",
            subtitle = "استعراض، طباعة، ومشاركة الفواتير",
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF2563EB)
        ),
        MainMenuItem(
            id = "products",
            title = "الأصناف والمخزون",
            subtitle = "إدارة المنتجات، الوحدات، والأسعار",
            icon = Icons.Default.Inventory2,
            color = Color(0xFFD97706)
        ),
        MainMenuItem(
            id = "customers",
            title = "العملاء والحسابات",
            subtitle = "كشوف الحسابات والديون والمدفوعات",
            icon = Icons.Default.People,
            color = Color(0xFF7C3AED)
        ),
        MainMenuItem(
            id = "bonds",
            title = "السندات المالية",
            subtitle = "سندات القبض والصرف والتسديد",
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF059669)
        ),
        MainMenuItem(
            id = "reports",
            title = "التقارير والإحصائيات",
            subtitle = "أرباح اليوم، المبيعات، ونواقص المخزن",
            icon = Icons.Default.Assessment,
            color = Color(0xFFDC2626)
        ),
        MainMenuItem(
            id = "settings",
            title = "إعدادات النظام والطابعة",
            subtitle = "كود المحل، السحابة، والبلوتوث",
            icon = Icons.Default.Settings,
            color = Color(0xFF475569)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = settings.businessName.ifEmpty { "شبكة خمر" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "كود: ${settings.storeCode}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "المستخدم: $currentUserName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF99F6E4)
                        )
                    }
                },
                actions = {
                    // Sync Trigger Icon in Top Bar
                    IconButton(onClick = onTriggerSync) {
                        Icon(
                            imageVector = if (syncStatus.state == SyncState.SYNCING) Icons.Default.SyncLock else Icons.Default.CloudSync,
                            contentDescription = "مزامنة سحابية",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F766E)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Cloud & Sync Status Live Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTriggerSync() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (syncStatus.state) {
                        SyncState.SYNCING -> Color(0xFFEFF6FF)
                        SyncState.OFFLINE -> Color(0xFFFFFBEB)
                        SyncState.ERROR -> Color(0xFFFEF2F2)
                        else -> Color(0xFFF0FDF4)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (syncStatus.state) {
                        SyncState.SYNCING -> Color(0xFFBFDBFE)
                        SyncState.OFFLINE -> Color(0xFFFDE68A)
                        SyncState.ERROR -> Color(0xFFFECDD3)
                        else -> Color(0xFFBBF7D0)
                    }
                )
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when (syncStatus.state) {
                                        SyncState.SYNCING -> Color(0xFFDBEAFE)
                                        SyncState.OFFLINE -> Color(0xFFFEF3C7)
                                        SyncState.ERROR -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFDCFCE7)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (syncStatus.state) {
                                    SyncState.SYNCING -> Icons.Default.Sync
                                    SyncState.OFFLINE -> Icons.Default.CloudOff
                                    SyncState.ERROR -> Icons.Default.Warning
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = null,
                                tint = when (syncStatus.state) {
                                    SyncState.SYNCING -> Color(0xFF2563EB)
                                    SyncState.OFFLINE -> Color(0xFFD97706)
                                    SyncState.ERROR -> Color(0xFFDC2626)
                                    else -> Color(0xFF16A34A)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = when (syncStatus.state) {
                                        SyncState.SYNCING -> "جاري المزامنة السحابية..."
                                        SyncState.OFFLINE -> "الوضع غير متصل (أوفلاين - حفظ محلي)"
                                        SyncState.ERROR -> "تنبيه المزامنة السحابية"
                                        else -> "متصل ومتزامن مع السحابة"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = if (settings.lastSyncTimestamp > 0) {
                                    val timeFmt = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(settings.lastSyncTimestamp))
                                    "كود المحل: ${settings.storeCode} • آخر مزامنة: $timeFmt"
                                } else "كود المحل: ${settings.storeCode} (اضغط للمزامنة الآن)",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onTriggerSync,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (syncStatus.state) {
                                SyncState.SYNCING -> Color(0xFF2563EB)
                                SyncState.OFFLINE -> Color(0xFFD97706)
                                else -> Color(0xFF059669)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (syncStatus.state == SyncState.SYNCING) "جاري..." else "مزامنة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Main Menu Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clickable { onNavigate(item.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF64748B),
                                    maxLines = 2,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
