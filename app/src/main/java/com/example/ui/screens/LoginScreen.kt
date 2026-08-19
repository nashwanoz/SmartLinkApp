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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldSuccess
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
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary

@Composable
fun LoginScreen(
    users: List<User>,
    settings: SystemSettings,
    onLoginSuccess: (User, String) -> Boolean
) {
    var selectedUser by remember { mutableStateOf<User?>(users.firstOrNull()) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Slate900, Slate800, TealDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // System Logo & Branding
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(TealDark, TealPrimary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Smart ERP",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = settings.businessName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = settings.tagline,
                    fontSize = 11.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Selection Dropdown/Chips
                Text(
                    text = "اختر المستخدم لتسجيل الدخول:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    users.filter { it.active }.forEach { user ->
                        val isSelected = (selectedUser?.id == user.id)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) TealContainer else Slate50,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) TealPrimary else Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUser = user
                                    pinInput = ""
                                    errorMessage = ""
                                }
                                .testTag("user_item_${user.userCode}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) TealPrimary else Slate700),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.userCode,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = if (user.role == UserRole.ADMIN) "مدير النظام" else "كاشير",
                                            fontSize = 10.sp,
                                            color = if (user.role == UserRole.ADMIN) TealDark else Slate500,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(TealPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PIN Input Field
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = ""
                    },
                    label = { Text("رمز PIN الخاص بالمستخدم") },
                    placeholder = { Text("مثال: 101 للمدير، 102 للكاشير") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input")
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = RoseError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numeric Keypad for fast touchscreen POS login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "2", "3").forEach { digit ->
                        KeypadButton(digit, modifier = Modifier.weight(1f)) {
                            pinInput += digit
                            errorMessage = ""
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("4", "5", "6").forEach { digit ->
                        KeypadButton(digit, modifier = Modifier.weight(1f)) {
                            pinInput += digit
                            errorMessage = ""
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("7", "8", "9").forEach { digit ->
                        KeypadButton(digit, modifier = Modifier.weight(1f)) {
                            pinInput += digit
                            errorMessage = ""
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate100,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                pinInput = ""
                                errorMessage = ""
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("مسح", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }
                    }

                    KeypadButton("0", modifier = Modifier.weight(1f)) {
                        pinInput += "0"
                        errorMessage = ""
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RoseContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "حذف",
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Submit Button
                Button(
                    onClick = {
                        val user = selectedUser ?: users.firstOrNull()
                        if (user != null) {
                            val ok = onLoginSuccess(user, pinInput)
                            if (!ok) {
                                errorMessage = "رمز PIN غير صحيح! جرب ${user.userCode}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    Text(
                        text = "تسجيل الدخول للنظام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate100,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
    }
}
