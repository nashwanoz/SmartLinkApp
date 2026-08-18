package com.smartlink.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartlink.erp.data.local.DEFAULT_SETTINGS
import com.smartlink.erp.data.local.DEFAULT_USERS
import com.smartlink.erp.data.local.entity.*
import com.smartlink.erp.data.local.loadCustomers
import com.smartlink.erp.data.local.loadProducts
import com.smartlink.erp.data.local.loadInvoices
import com.smartlink.erp.data.local.loadBonds
import com.smartlink.erp.data.local.loadTransfers
import com.smartlink.erp.data.local.loadSettings
import com.smartlink.erp.data.local.loadUsers
import com.smartlink.erp.data.local.saveCustomers
import com.smartlink.erp.data.local.saveProducts
import com.smartlink.erp.data.local.saveInvoices
import com.smartlink.erp.data.local.saveBonds
import com.smartlink.erp.data.local.saveTransfers
import com.smartlink.erp.data.local.saveSettings
import com.smartlink.erp.screens.*
import com.smartlink.erp.utils.hasPermission
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartLinkERPApp()
        }
    }
}

@Composable
fun SmartLinkERPApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Global State with LocalStorage Persistence
    var settings by remember { mutableStateOf(context.loadSettings()) }
    var users by remember { mutableStateOf(context.loadUsers()) }
    var currentUser by remember { mutableStateOf(users.firstOrNull() ?: DEFAULT_USERS.first()) }
    var customers by remember { mutableStateOf(context.loadCustomers()) }
    var products by remember { mutableStateOf(context.loadProducts()) }
    var invoices by remember { mutableStateOf(context.loadInvoices()) }
    var bonds by remember { mutableStateOf(context.loadBonds()) }
    var transfers by remember { mutableStateOf(context.loadTransfers()) }
    
    // Multi-tenant Store state
    var storeCode by remember { mutableStateOf<String?>(null) }
    var storeMetadata by remember { mutableStateOf<StoreMetadata?>(null) }
    var isActivationModalOpen by remember { mutableStateOf(false) }
    
    // Active Screen / Tab
    var activeTab by remember { mutableStateOf("home") }
    
    // Inter-screen navigation parameters
    var preselectedCustomerForStatement by remember { mutableStateOf<String?>(null) }
    var preselectedCustomerForBond by remember { mutableStateOf<Customer?>(null) }
    var preselectedProductForPos by remember { mutableStateOf<Product?>(null) }
    var selectedUserForPermissions by remember { mutableStateOf<User?>(null) }
    
    // Quick User Switcher modal
    var showUserSwitcher by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Permission access checker for current user
    fun canAccess(key: (UserPermissions) -> Boolean): Boolean {
        return hasPermission(currentUser, key)
    }
    
    // Save permissions for user
    fun handleSaveUserPermissions(userId: String, updatedPermissions: UserPermissions) {
        users = users.map { u ->
            if (u.id == userId) u.copy(permissions = updatedPermissions) else u
        }
        if (currentUser.id == userId) {
            currentUser = currentUser.copy(permissions = updatedPermissions)
        }
    }
    
    // Save to LocalStorage on changes
    LaunchedEffect(settings) {
        context.saveSettings(settings)
    }
    
    LaunchedEffect(users) {
        context.saveUsers(users)
    }
    
    LaunchedEffect(customers) {
        context.saveCustomers(customers)
    }
    
    LaunchedEffect(products) {
        context.saveProducts(products)
    }
    
    LaunchedEffect(invoices) {
        context.saveInvoices(invoices)
    }
    
    LaunchedEffect(bonds) {
        context.saveBonds(bonds)
    }
    
    LaunchedEffect(transfers) {
        context.saveTransfers(transfers)
    }
    
    // Handle Save Product
    fun handleSaveProduct(productData: Product, isEdit: Boolean) {
        if (isEdit) {
            products = products.map { p ->
                if (p.id == productData.id) productData else p
            }
        } else {
            products = products + productData
        }
    }
    
    fun handleDeleteProduct(productId: Long) {
        products = products.filter { it.id != productId }
    }
    
    // Handle Save Customer
    fun handleSaveCustomer(customerData: Customer, isEdit: Boolean) {
        if (isEdit) {
            customers = customers.map { c ->
                if (c.id == customerData.id) customerData else c
            }
        } else {
            customers = customers + customerData
        }
    }
    
    fun handleDeleteCustomer(customerId: String) {
        customers = customers.filter { it.id != customerId }
    }
    
    // Handle Save User
    fun handleSaveUser(userData: User, isEdit: Boolean) {
        if (isEdit) {
            users = users.map { u ->
                if (u.id == userData.id) userData else u
            }
            if (currentUser.id == userData.id) {
                currentUser = userData
            }
        } else {
            users = users + userData
        }
    }
    
    fun handleDeleteUser(userId: String) {
        users = users.filter { it.id != userId }
    }
    
    // Execute Stock Transfer (Admin only)
    fun handleExecuteTransfer(
        productId: Long,
        toCashierId: String,
        quantity: Double,
        note: String?
    ) {
        val targetProduct = products.find { it.id == productId }
        val targetCashier = users.find { it.id == toCashierId }
        if (targetProduct == null || targetCashier == null) return
        
        // Deduct from main stock, add to cashier stock
        products = products.map { p ->
            if (p.id == productId) {
                val currentCashierQty = p.stockCashier?.get(toCashierId) ?: 0.0
                p.copy(
                    stockMain = (p.stockMain - quantity).coerceAtLeast(0.0),
                    stockCashier = (p.stockCashier ?: mutableMapOf()).toMutableMap().apply {
                        put(toCashierId, currentCashierQty + quantity)
                    }
                )
            } else {
                p
            }
        }
        
        // Record transfer log
        val transferNumber = "TR-${String.format("%03d", transfers.size + 101)}"
        val newTransfer = StockTransfer(
            id = "tr_${System.currentTimeMillis()}",
            transferNumber = transferNumber,
            productId = productId,
            productName = targetProduct.name,
            unitName = targetProduct.unitName,
            quantity = quantity,
            fromLocation = "المخزن الرئيسي",
            toCashierId = toCashierId,
            toCashierName = targetCashier.name,
            toCashierCode = targetCashier.userCode,
            adminId = currentUser.id,
            adminName = currentUser.name,
            date = System.currentTimeMillis(),
            note = note ?: "تحويل مخزني"
        )
        
        transfers = transfers + newTransfer
    }
    
    // Save Invoice (deduct stock, adjust customer balance)
    fun handleSaveInvoice(newInvoice: Invoice) {
        // 1. Add to invoices
        invoices = invoices + newInvoice
        
        // 2. Update customer balance (add remaining unpaid amount to customer debt)
        customers = customers.map { c ->
            if (c.id == newInvoice.customerId) {
                c.copy(balance = c.balance + newInvoice.remainingAmount)
            } else {
                c
            }
        }
        
        // 3. Deduct inventory from cashier stock (or main stock if admin)
        products = products.map { p ->
            val item = newInvoice.items.find { it.productId == p.id }
            if (item == null) return@map p
            
            val currentCashierQty = p.stockCashier?.get(newInvoice.cashierId) ?: 0.0
            if (currentCashierQty >= item.convertedMinorQty) {
                p.copy(
                    stockCashier = (p.stockCashier ?: mutableMapOf()).toMutableMap().apply {
                        put(newInvoice.cashierId, currentCashierQty - item.convertedMinorQty)
                    }
                )
            } else {
                val remToDeduct = item.convertedMinorQty - currentCashierQty
                p.copy(
                    stockMain = (p.stockMain - remToDeduct).coerceAtLeast(0.0),
                    stockCashier = (p.stockCashier ?: mutableMapOf()).toMutableMap().apply {
                        put(newInvoice.cashierId, 0.0)
                    }
                )
            }
        }
    }
    
    // Save Bond (Receipt / Payment)
    fun handleSaveBond(newBond: Bond) {
        bonds = bonds + newBond
        
        // Update customer balance:
        // RECEIPT: decreases customer debt (- amount)
        // PAYMENT: increases customer debt (+ amount)
        customers = customers.map { c ->
            if (c.id == newBond.customerId) {
                val delta = if (newBond.type == "RECEIPT") -newBond.amount else newBond.amount
                c.copy(balance = c.balance + delta)
            } else {
                c
            }
        }
    }
    
    // Delete Bond (Manager Only) - Reverses customer balance adjustment
    fun handleDeleteBond(bondId: String) {
        val bond = bonds.find { it.id == bondId }
        if (bond == null) return
        
        // 1. Remove bond from list
        bonds = bonds.filter { it.id != bondId }
        
        // 2. Reverse customer balance
        customers = customers.map { c ->
            if (c.id == bond.customerId) {
                val reverseDelta = if (bond.type == "RECEIPT") bond.amount else -bond.amount
                c.copy(balance = c.balance + reverseDelta)
            } else {
                c
            }
        }
    }
    
    // Reset and Clear all dummy / test data
    fun handleClearAllData() {
        customers = emptyList()
        products = emptyList()
        invoices = emptyList()
        bonds = emptyList()
        transfers = emptyList()
    }
    
    // Reset to Defaults
    fun handleResetToDefaults() {
        settings = DEFAULT_SETTINGS
        users = DEFAULT_USERS
        currentUser = DEFAULT_USERS.first()
        handleClearAllData()
    }
    
    // Main UI
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = settings.businessName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                    )
                    Text(
                        text = "${settings.address} • ${settings.currencySymbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // User Switcher
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentUser.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    IconButton(onClick = { showUserSwitcher = true }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        )
        
        // Main Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                "home" -> HomeScreen(
                    currentUser = currentUser,
                    settings = settings,
                    products = products,
                    customers = customers,
                    invoices = invoices,
                    bonds = bonds,
                    onNavigate = { tab -> activeTab = tab },
                    onOpenPos = { activeTab = "pos" }
                )
                
                "pos" -> PosScreen(
                    currentUser = currentUser,
                    products = products,
                    customers = customers,
                    settings = settings,
                    onSaveInvoice = { invoice -> handleSaveInvoice(invoice) },
                    preselectedProduct = preselectedProductForPos,
                    onClearPreselectedProduct = { preselectedProductForPos = null }
                )
                
                "products" -> ProductsScreen(
                    products = products,
                    currentUser = currentUser,
                    settings = settings,
                    onSaveProduct = { product, isEdit -> handleSaveProduct(product, isEdit) },
                    onDeleteProduct = { productId -> handleDeleteProduct(productId) },
                    onSelectForPos = { product ->
                        preselectedProductForPos = product
                        activeTab = "pos"
                    }
                )
                
                "customers" -> CustomersScreen(
                    customers = customers,
                    currentUser = currentUser,
                    settings = settings,
                    onSaveCustomer = { customer, isEdit -> handleSaveCustomer(customer, isEdit) },
                    onDeleteCustomer = { customerId -> handleDeleteCustomer(customerId) },
                    onOpenBondModal = { customer ->
                        preselectedCustomerForBond = customer
                        activeTab = "bonds"
                    },
                    onOpenStatement = { customer ->
                        preselectedCustomerForStatement = customer.id
                        activeTab = "statements"
                    }
                )
                
                "statements" -> StatementsScreen(
                    customers = customers,
                    users = users,
                    products = products,
                    invoices = invoices,
                    bonds = bonds,
                    settings = settings,
                    preselectedCustomerId = preselectedCustomerForStatement
                )
                
                "transfer" -> StockTransferScreen(
                    products = products,
                    users = users,
                    currentUser = currentUser,
                    settings = settings,
                    transfers = transfers,
                    onExecuteTransfer = { productId, toCashierId, quantity, note ->
                        handleExecuteTransfer(productId, toCashierId, quantity, note)
                    }
                )
                
                "invoices" -> InvoicesScreen(
                    invoices = invoices,
                    customers = customers,
                    currentUser = currentUser,
                    settings = settings
                )
                
                "bonds" -> BondsScreen(
                    bonds = bonds,
                    customers = customers,
                    currentUser = currentUser,
                    settings = settings,
                    onSaveBond = { bond -> handleSaveBond(bond) },
                    onDeleteBond = { bondId -> handleDeleteBond(bondId) },
                    preselectedCustomer = preselectedCustomerForBond,
                    onClearPreselectedCustomer = { preselectedCustomerForBond = null }
                )
                
                "users" -> UsersScreen(
                    users = users,
                    currentUser = currentUser,
                    settings = settings,
                    onSaveUser = { user, isEdit -> handleSaveUser(user, isEdit) },
                    onDeleteUser = { userId -> handleDeleteUser(userId) },
                    onOpenPermissions = { user ->
                        selectedUserForPermissions = user
                        activeTab = "permissions"
                    }
                )
                
                "permissions" -> selectedUserForPermissions?.let { user ->
                    PermissionsScreen(
                        targetUser = user,
                        currentUser = currentUser,
                        onSavePermissions = { userId, perms -> handleSaveUserPermissions(userId, perms) },
                        onBack = { activeTab = "users" }
                    )
                }
                
                "settings" -> SettingsScreen(
                    settings = settings,
                    currentUser = currentUser,
                    onSaveSettings = { newSettings ->
                        settings = newSettings
                    },
                    onResetToDefaults = { handleResetToDefaults() },
                    onClearAllData = { handleClearAllData() }
                )
                
                "about" -> AboutScreen(
                    settings = settings,
                    currentUser = currentUser,
                    onBack = { activeTab = "home" },
                    storeCode = storeCode,
                    onOpenStoreSync = { isActivationModalOpen = true }
                )
            }
        }
        
        // Bottom Navigation Bar
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("الرئيسية") },
                selected = activeTab == "home",
                onClick = { activeTab = "home" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                label = { Text("نقطة البيع") },
                selected = activeTab == "pos",
                onClick = { activeTab = "pos" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                label = { Text("الأصناف") },
                selected = activeTab == "products",
                onClick = { activeTab = "products" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.People, contentDescription = null) },
                label = { Text("العملاء") },
                selected = activeTab == "customers",
                onClick = { activeTab = "customers" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                label = { Text("كشف حساب") },
                selected = activeTab == "statements",
                onClick = { activeTab = "statements" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                label = { Text("السندات") },
                selected = activeTab == "bonds",
                onClick = { activeTab = "bonds" }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("الإعدادات") },
                selected = activeTab == "settings",
                onClick = { activeTab = "settings" }
            )
        }
    }
    
    // Quick User Switcher Modal
    if (showUserSwitcher) {
        UserSwitcherModal(
            users = users,
            currentUser = currentUser,
            onDismiss = { showUserSwitcher = false },
            onSelectUser = { user ->
                currentUser = user
                showUserSwitcher = false
            }
        )
    }
    
    // Store Activation Modal
    if (isActivationModalOpen) {
        StoreActivationModal(
            currentStoreCode = storeCode,
            businessName = settings.businessName,
            isOpen = true,
            onClose = { isActivationModalOpen = false },
            onActivated = { newCode, metadata ->
                storeCode = newCode
                if (metadata != null) storeMetadata = metadata
            }
        )
    }
}

@Composable
private fun UserSwitcherModal(
    users: List<User>,
    currentUser: User,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تبديل المستخدم الحالي") },
        text = {
            Column {
                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = user.userCode,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (user.role == "ADMIN") "مدير" else "كاشير",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

// Data class for Store Metadata
data class StoreMetadata(
    val storeCode: String,
    val businessName: String,
    val createdAt: Long,
    val isActive: Boolean = true,
    val suspendedMessage: String? = null
)
