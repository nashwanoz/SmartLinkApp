package com.khamrnet.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsBottomNavBar(
    currentRoute: String,
    onNavigate: (route: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. الإعدادات (Settings)
            SettingsBottomItem(
                icon = Icons.Default.Settings,
                label = "الإعدادات",
                selected = currentRoute == "settings",
                onClick = { onNavigate("settings") }
            )

            // 2. السندات (Bonds)
            SettingsBottomItem(
                icon = Icons.Default.ReceiptLong,
                label = "السندات",
                selected = currentRoute == "bonds",
                onClick = { onNavigate("bonds") }
            )

            // 3. العملاء (Customers)
            SettingsBottomItem(
                icon = Icons.Default.People,
                label = "العملاء",
                selected = currentRoute == "customers",
                onClick = { onNavigate("customers") }
            )

            // 4. الأصناف (Products)
            SettingsBottomItem(
                icon = Icons.Default.Inventory2,
                label = "الأصناف",
                selected = currentRoute == "products",
                onClick = { onNavigate("products") }
            )

            // 5. نقطة البيع (POS)
            SettingsBottomItem(
                icon = Icons.Default.ShoppingCart,
                label = "نقطة البيع",
                selected = currentRoute == "pos",
                onClick = { onNavigate("pos") }
            )

            // 6. الرئيسية (Home)
            SettingsBottomItem(
                icon = Icons.Default.Home,
                label = "الرئيسية",
                selected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )
        }
    }
}

@Composable
fun SettingsBottomItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF0F766E) else Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) Color(0xFF0F766E) else Color(0xFF64748B)
        )
    }
}
