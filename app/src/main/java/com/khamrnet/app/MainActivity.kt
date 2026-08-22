package com.khamrnet.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.khamrnet.app.sync.SyncStatus
import com.khamrnet.app.ui.KhamrTheme
import com.khamrnet.app.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as KhamrNetApp
        val repository = app.repository
        val syncManager = app.cloudSyncManager

        setContent {
            // Force RTL Layout for complete Arabic support across all Android devices
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KhamrTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val settingsState by repository.systemSettings.collectAsState(initial = null)
                        val settings = settingsState ?: SystemSettingsEntity()

                        val syncStatus by syncManager.syncStatus.collectAsState()

                        var isLoggedIn by remember { mutableStateOf(false) }
                        var currentUserName by remember { mutableStateOf("المدير") }
                        var currentScreen by remember { mutableStateOf("home") }

                        var showExitDialog by remember { mutableStateOf(false) }
                        var lastBackPressTime by remember { mutableLongStateOf(0L) }

                        val coroutineScope = rememberCoroutineScope()
                        val allProducts by repository.allProducts.collectAsState(initial = emptyList())
                        val allCustomers by repository.allCustomers.collectAsState(initial = emptyList())
                        val allInvoices by repository.allInvoices.collectAsState(initial = emptyList())
                        val allBonds by repository.allBonds.collectAsState(initial = emptyList())

                        // Handle Back Button with double press check and exit confirmation
                        BackHandler(enabled = true) {
                            if (isLoggedIn && currentScreen != "home") {
                                currentScreen = "home"
                            } else {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastBackPressTime < 2500L) {
                                    showExitDialog = true
                                } else {
                                    lastBackPressTime = currentTime
                                    Toast.makeText(this@MainActivity, "اضغط مرة أخرى لتأكيد الخروج من التطبيق", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // Exit Confirmation Dialog
                        if (showExitDialog) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                AlertDialog(
                                    onDismissRequest = { showExitDialog = false },
                                    title = {
                                        Text(
                                            text = "تأكيد الخروج",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "هل تريد الخروج من التطبيق وإغلاقه؟",
                                            fontSize = 14.sp,
                                            color = Color(0xFF334155)
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showExitDialog = false
                                                finish()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                        ) {
                                            Text("نعم، خروج", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(onClick = { showExitDialog = false }) {
                                            Text("إلغاء", color = Color(0xFF64748B))
                                        }
                                    }
                                )
                            }
                        }

                        // Auto sync on start or resume safely
                        LaunchedEffect(isLoggedIn, settings.storeCode) {
                            if (isLoggedIn && settings.storeCode.isNotEmpty()) {
                                try {
                                    syncManager.performSync(settings.storeCode)
                                } catch (_: Exception) {}
                            }
                        }

                        Crossfade(targetState = isLoggedIn, label = "AuthFlow") { loggedIn ->
                            if (!loggedIn) {
                                LoginScreen(
                                    settings = settings,
                                    onLoginSuccess = { name, _ ->
                                        currentUserName = name
                                        isLoggedIn = true
                                    }
                                )
                            } else {
                                when (currentScreen) {
                                    "home" -> {
                                        MainHomeScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            invoices = allInvoices,
                                            bonds = allBonds,
                                            products = allProducts,
                                            customers = allCustomers,
                                            syncStatus = syncStatus,
                                            onTriggerSync = {
                                                coroutineScope.launch {
                                                    val res = syncManager.performSync(settings.storeCode)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(this@MainActivity, "✅ تمت المزامنة بنجاح", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "تنبيه المزامنة", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onUpdateStoreCode = { newCode ->
                                                coroutineScope.launch {
                                                    val updated = settings.copy(storeCode = newCode)
                                                    repository.updateSettings(updated)
                                                    val res = syncManager.registerOrConnectStore(newCode, settings.businessName)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(this@MainActivity, "✅ تم ربط كود المحل $newCode ومزامنة البيانات بنجاح", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "تنبيه الربط", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onNavigate = { route ->
                                                currentScreen = route
                                            },
                                            onLogout = {
                                                isLoggedIn = false
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "pos" -> {
                                        PosScreen(
                                            settings = settings,
                                            products = allProducts,
                                            customers = allCustomers,
                                            currentUserName = currentUserName,
                                            onSaveInvoice = { invoice, items ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.insertInvoice(invoice, items)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "invoices" -> {
                                        InvoicesScreen(
                                            settings = settings,
                                            invoices = allInvoices,
                                            onCancelInvoice = { invoice, items ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.cancelInvoice(invoice, items)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "products" -> {
                                        ProductsScreen(
                                            settings = settings,
                                            products = allProducts,
                                            onSaveProduct = { product ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.insertProduct(product)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onDeleteProduct = { product ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.deleteProduct(product)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "customers" -> {
                                        CustomersScreen(
                                            settings = settings,
                                            customers = allCustomers,
                                            onSaveCustomer = { customer ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.insertCustomer(customer)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onDeleteCustomer = { customer ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.deleteCustomer(customer)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "bonds" -> {
                                        BondsScreen(
                                            settings = settings,
                                            bonds = allBonds,
                                            customers = allCustomers,
                                            invoices = allInvoices,
                                            currentUserName = currentUserName,
                                            syncStatus = syncStatus,
                                            onTriggerSync = {
                                                coroutineScope.launch {
                                                    val res = syncManager.performSync(settings.storeCode)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(this@MainActivity, "✅ تمت المزامنة السحابية بنجاح", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "خطأ في المزامنة", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onUpdateStoreCode = { newCode ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val updated = settings.copy(storeCode = newCode)
                                                    repository.updateSettings(updated)
                                                    syncManager.performSync(newCode)
                                                }
                                            },
                                            onSaveBond = { bond ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.insertBond(bond)
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigate = { route ->
                                                currentScreen = route
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            },
                                            onLogout = {
                                                currentScreen = "login"
                                            }
                                        )
                                    }
                                    "transfer" -> {
                                        StockTransferScreen(
                                            settings = settings,
                                            products = allProducts,
                                            currentUserName = currentUserName,
                                            onSaveTransfer = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "settlements" -> {
                                        CashierSettlementScreen(
                                            settings = settings,
                                            invoices = allInvoices,
                                            bonds = allBonds,
                                            products = allProducts,
                                            currentUserName = currentUserName,
                                            onSaveSettlement = { amt, cashier, notes ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "expenses" -> {
                                        ExpensesScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "card-generation" -> {
                                        CardGenerationScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "users" -> {
                                        UsersManagementScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "ledger" -> {
                                        AccountingLedgerScreen(
                                            settings = settings,
                                            invoices = allInvoices,
                                            bonds = allBonds,
                                            customers = allCustomers,
                                            currentUserName = currentUserName,
                                            syncStatus = syncStatus,
                                            onTriggerSync = {
                                                coroutineScope.launch {
                                                    val res = syncManager.performSync(settings.storeCode)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(this@MainActivity, "✅ تمت المزامنة السحابية بنجاح", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "خطأ في المزامنة", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onUpdateStoreCode = { newCode ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val updated = settings.copy(storeCode = newCode)
                                                    repository.updateSettings(updated)
                                                    syncManager.performSync(newCode)
                                                }
                                            },
                                            onNavigate = { route ->
                                                currentScreen = route
                                            },
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            },
                                            onLogout = {
                                                currentScreen = "login"
                                            }
                                        )
                                    }
                                    "about" -> {
                                        AboutScreen(
                                            settings = settings,
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "reports" -> {
                                        ReportsScreen(
                                            settings = settings,
                                            invoices = allInvoices,
                                            products = allProducts,
                                            bonds = allBonds,
                                            onNavigateBack = {
                                                currentScreen = "home"
                                            }
                                        )
                                    }
                                    "settings" -> {
                                        SettingsScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            syncStatus = syncStatus,
                                            products = allProducts,
                                            customers = allCustomers,
                                            invoices = allInvoices,
                                            bonds = allBonds,
                                            onSaveSettings = { updatedSettings ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.updateSettings(updatedSettings)
                                                    syncManager.performSync(updatedSettings.storeCode)
                                                }
                                            },
                                            onTriggerSync = {
                                                coroutineScope.launch {
                                                    val res = syncManager.performSync(settings.storeCode)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(this@MainActivity, "✅ تمت المزامنة السحابية بنجاح", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "خطأ في المزامنة", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onUpdateStoreCode = { newCode ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val updated = settings.copy(storeCode = newCode)
                                                    repository.updateSettings(updated)
                                                    syncManager.performSync(newCode)
                                                }
                                            },
                                            onResetToDefaults = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val defaultSettings = SystemSettingsEntity()
                                                    repository.updateSettings(defaultSettings)
                                                }
                                            },
                                            onClearAllData = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    repository.clearAllData()
                                                }
                                            },
                                            onNavigate = { route ->
                                                currentScreen = route
                                            },
                                            onLogout = {
                                                currentScreen = "login"
                                            }
                                        )
                                    }
                                    else -> {
                                        MainHomeScreen(
                                            settings = settings,
                                            currentUserName = currentUserName,
                                            syncStatus = syncStatus,
                                            onTriggerSync = {
                                                coroutineScope.launch {
                                                    syncManager.performSync(settings.storeCode)
                                                }
                                            },
                                            onNavigate = { route -> currentScreen = route },
                                            onLogout = { isLoggedIn = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
