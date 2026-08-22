package com.khamrnet.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.text.NumberFormat
import java.util.*

data class CardProfile(
    val id: String,
    val name: String,
    val price: Double,
    val validity: String,
    val speedLimit: String
)

data class GeneratedCard(
    val username: String,
    val pin: String,
    val code: String
)

val DEFAULT_CARD_PROFILES = listOf(
    CardProfile("1", "باقة 100 ميجا - 24 ساعة", 100.0, "1 يوم", "2M/512K"),
    CardProfile("2", "باقة 300 ميجا - 3 أيام", 200.0, "3 أيام", "3M/1M"),
    CardProfile("3", "باقة 1 جيجا - 7 أيام", 500.0, "7 أيام", "4M/2M"),
    CardProfile("4", "باقة 3 جيجا - شهر كامل", 1500.0, "30 يوم", "5M/2M"),
    CardProfile("5", "باقة مفتوحة - 24 ساعة", 1000.0, "1 يوم", "10M/4M")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardGenerationScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencyName.ifEmpty { "YER" }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var selectedProfile by remember { mutableStateOf(DEFAULT_CARD_PROFILES[0]) }
    var cardCountText by remember { mutableStateOf("12") }
    var prefix by remember { mutableStateOf("KM-") }
    var isGenerating by remember { mutableStateOf(false) }

    var generatedCards by remember {
        mutableStateOf(
            listOf(
                GeneratedCard("KM-839201", "839201", "839201"),
                GeneratedCard("KM-492014", "492014", "492014"),
                GeneratedCard("KM-910283", "910283", "910283"),
                GeneratedCard("KM-374829", "374829", "374829"),
                GeneratedCard("KM-582910", "582910", "582910"),
                GeneratedCard("KM-629401", "629401", "629401")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "توليد وطباعة كروت المايكروتك",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "إنشاء كروت شبكات ميكروتك والتحكم بالسرعات",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Setup & Parameters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "اختيار باقة الشبكة والكمية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )

                    // Profile Selector Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DEFAULT_CARD_PROFILES.take(3).forEach { profile ->
                            val isSelected = profile.id == selectedProfile.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                                    .clickable { selectedProfile = profile }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        profile.name.take(12) + "...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${profile.price.toInt()} $currency",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cardCountText,
                            onValueChange = { cardCountText = it },
                            label = { Text("عدد الكروت") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("بادئة الكود (Prefix)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val count = cardCountText.toIntOrNull() ?: 10
                            isGenerating = true
                            val generated = (1..count).map {
                                val code = (100000..999999).random().toString()
                                GeneratedCard(
                                    username = "$prefix$code",
                                    pin = code,
                                    code = code
                                )
                            }
                            generatedCards = generated
                            isGenerating = false
                            Toast.makeText(context, "✅ تم توليد $count كرت بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد الكروت الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Cards Preview Grid Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "الكروت المولدة (${generatedCards.size} كرت):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )

                TextButton(
                    onClick = {
                        val textToCopy = generatedCards.joinToString("\n") { "${it.username} - PIN: ${it.pin}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Cards", textToCopy))
                        Toast.makeText(context, "تم نسخ جميع الكروت للحافظة", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ الكل", fontSize = 12.sp)
                }
            }

            // Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(generatedCards) { card ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                settings.storeName.ifEmpty { "شبكة خمر نت" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                card.username,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "PIN: ${card.pin}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${selectedProfile.price.toInt()} $currency • ${selectedProfile.validity}",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}
