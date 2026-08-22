package com.khamrnet.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseItem(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val recipient: String,
    val paymentMethod: String,
    val date: String,
    val notes: String = ""
)

val EXPENSE_CATEGORIES = listOf(
    "إيجار المحل",
    "رواتب وأجور",
    "كهرباء ومياه",
    "صيانة وتصليح",
    "اشتراك إنترنت",
    "ضيافة ونظافة",
    "شحن ومحروقات",
    "مشتريات وبوفيه",
    "رسوم وتراخيص",
    "نثريات ومصاريف أخرى"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    settings: SystemSettingsEntity,
    currentUserName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencyName.ifEmpty { "YER" }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // In-memory expenses state list
    var expensesList by remember {
        mutableStateOf(
            listOf(
                ExpenseItem("1", "فاتورة إنترنت وتغذية خطوط", 25000.0, "اشتراك إنترنت", "شركة الاتصالات", "نقداً", sdf.format(Date(System.currentTimeMillis() - 86400000))),
                ExpenseItem("2", "صيانة سويتش وسلك شبكة", 4500.0, "صيانة وتصليح", "محل الإلكترونيات", "نقداً", sdf.format(Date(System.currentTimeMillis() - 172800000))),
                ExpenseItem("3", "ضيافة وبوفيه الكاشير", 3000.0, "ضيافة ونظافة", "البوفيه", "نقداً", sdf.format(Date(System.currentTimeMillis() - 259200000)))
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredExpenses = remember(expensesList, searchQuery, selectedCategoryFilter) {
        expensesList.filter { exp ->
            val matchesCategory = (selectedCategoryFilter == "الكل" || exp.category == selectedCategoryFilter)
            val matchesSearch = (searchQuery.isEmpty() || exp.title.contains(searchQuery, ignoreCase = true) || exp.recipient.contains(searchQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    val totalExpensesSum = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "سجل المصاريف والنثريات",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "إدارة النفقات والمصروفات التشغيلية للمحل",
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
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "إضافة مصروف", tint = Color(0xFFE11D48))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFE11D48),
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة مصروف", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Total Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFBE123C)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي المصاريف المسجلة", fontSize = 12.sp, color = Color(0xFFFECDD3))
                        Text(
                            "${numberFormat.format(totalExpensesSum)} $currency",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search and Category Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في المصاريف أو اسم المستلم...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lazy List of Expenses
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد مصاريف مطابقة", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            expense.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            "المستلم: ${expense.recipient} • ${expense.category}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Text(
                                        "${numberFormat.format(expense.amount)} $currency",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFFE11D48)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "📅 ${expense.date} • ${expense.paymentMethod}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )

                                    IconButton(
                                        onClick = {
                                            expensesList = expensesList.filter { it.id != expense.id }
                                            Toast.makeText(context, "تم حذف المصروف", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddDialog) {
        var formTitle by remember { mutableStateOf("") }
        var formAmount by remember { mutableStateOf("") }
        var formCategory by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
        var formRecipient by remember { mutableStateOf("") }
        var formNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("تسجيل مصروف جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("بيان المصروف") },
                        placeholder = { Text("مثال: تسديد فاتورة الهاتف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formAmount,
                        onValueChange = { formAmount = it },
                        label = { Text("المبلغ ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formRecipient,
                        onValueChange = { formRecipient = it },
                        label = { Text("اسم المستلم / الجهة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formNotes,
                        onValueChange = { formNotes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = formAmount.toDoubleOrNull() ?: 0.0
                        if (formTitle.isBlank() || amt <= 0) {
                            Toast.makeText(context, "يرجى كتابة البيان والمبلغ بشكل صحيح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newExp = ExpenseItem(
                            id = UUID.randomUUID().toString(),
                            title = formTitle,
                            amount = amt,
                            category = formCategory,
                            recipient = formRecipient.ifBlank { "غير محدد" },
                            paymentMethod = "نقداً",
                            date = sdf.format(Date()),
                            notes = formNotes
                        )
                        expensesList = listOf(newExp) + expensesList
                        showAddDialog = false
                        Toast.makeText(context, "✅ تم تسجيل المصروف بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("حفظ المصروف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = Color(0xFF64748B))
                }
            }
        )
    }
}
