package com.khamrnet.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

                        val coroutineScope = rememberCoroutineScope()
                        val allProducts by repository.allProducts.collectAsState(initial = emptyList())
                        val allCustomers by repository.allCustomers.collectAsState(initial = emptyList())
                        val allInvoices by repository.allInvoices.collectAsState(initial = emptyList())
                        val allBonds by repository.allBonds.collectAsState(initial = emptyList())

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
