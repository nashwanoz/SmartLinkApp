package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.UserSwitcherModal
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BondsScreen
import com.example.ui.screens.CashierReportsScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatementsScreen
import com.example.ui.screens.StockTransferScreen
import com.example.ui.screens.UsersScreen
import com.example.ui.theme.KhamerNetPOSTheme
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current

            // Listen for toast events
            LaunchedEffect(Unit) {
                viewModel.messageEvents.collect { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            KhamerNetPOSTheme {
                // Force Right-to-Left (RTL) Layout for Arabic UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AccountingApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun AccountingApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val users by viewModel.users.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val posCustomer by viewModel.posSelectedCustomer.collectAsState()
    val posDiscount by viewModel.posDiscount.collectAsState()
    val posPaidAmount by viewModel.posPaidAmount.collectAsState()
    val posPaymentMethod by viewModel.posPaymentMethod.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val bonds by viewModel.bonds.collectAsState()
    val transfers by viewModel.transfers.collectAsState()

    val selectedCustomerForStatement by viewModel.selectedCustomerForStatement.collectAsState()
    val selectedCustomerForBond by viewModel.selectedCustomerForBond.collectAsState()
    val selectedUserForPermissions by viewModel.selectedUserForPermissions.collectAsState()

    var showUserSwitcherModal by remember { mutableStateOf(false) }

    // If no user logged in, display Login Screen
    if (currentUser == null || currentScreen == ScreenTab.LOGIN) {
        LoginScreen(
            users = users,
            settings = settings,
            onLoginSuccess = { user, pin ->
                viewModel.login(user, pin)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            AppHeader(
                currentUser = currentUser,
                settings = settings,
                onHomeClick = { viewModel.navigateTo(ScreenTab.HOME) },
                onSwitchUserClick = { showUserSwitcherModal = true },
                onLogoutClick = { viewModel.logout() }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = Slate50,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            when (currentScreen) {
                ScreenTab.LOGIN, ScreenTab.HOME -> {
                    HomeScreen(
                        currentUser = currentUser,
                        settings = settings,
                        stats = stats,
                        onNavigate = { viewModel.navigateTo(it) },
                        canAccess = { viewModel.canAccess(it) }
                    )
                }

                ScreenTab.POS -> {
                    PosScreen(
                        currentUser = currentUser,
                        products = products,
                        customers = customers,
                        cartItems = cartItems,
                        selectedCustomer = posCustomer,
                        discount = posDiscount,
                        paidAmount = posPaidAmount,
                        paymentMethod = posPaymentMethod,
                        settings = settings,
                        onAddToCart = { prod, unitType -> viewModel.addToCart(prod, unitType) },
                        onUpdateCartQty = { id, unitType, qty -> viewModel.updateCartItemQuantity(id, unitType, qty) },
                        onRemoveFromCart = { id, unitType -> viewModel.removeFromCart(id, unitType) },
                        onClearCart = { viewModel.clearPosCart() },
                        onSelectCustomer = { viewModel.setPosCustomer(it) },
                        onSetDiscount = { viewModel.setPosDiscount(it) },
                        onSetPaidAmount = { viewModel.setPosPaidAmount(it) },
                        onSetPaymentMethod = { viewModel.setPosPaymentMethod(it) },
                        onCheckoutInvoice = { notes -> viewModel.checkoutInvoice(notes) }
                    )
                }

                ScreenTab.PRODUCTS -> {
                    ProductsScreen(
                        products = products,
                        currentUser = currentUser,
                        settings = settings,
                        onSaveProduct = { viewModel.saveProduct(it) },
                        onDeleteProduct = { viewModel.deleteProduct(it) },
                        onSelectForPos = { viewModel.openPosForProduct(it) }
                    )
                }

                ScreenTab.CUSTOMERS -> {
                    CustomersScreen(
                        customers = customers,
                        currentUser = currentUser,
                        settings = settings,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onOpenBondModal = { viewModel.openBondForCustomer(it) },
                        onOpenStatement = { viewModel.openStatementForCustomer(it.id) }
                    )
                }

                ScreenTab.BONDS -> {
                    BondsScreen(
                        bonds = bonds,
                        customers = customers,
                        currentUser = currentUser,
                        settings = settings,
                        preselectedCustomer = selectedCustomerForBond,
                        onSaveBond = { type, cust, amount, note ->
                            viewModel.createBond(type, cust, amount, note)
                        },
                        onDeleteBond = { viewModel.deleteBond(it) },
                        onClearPreselectedCustomer = { viewModel.openBondForCustomer(customers.firstOrNull() ?: return@BondsScreen) }
                    )
                }

                ScreenTab.INVOICES -> {
                    InvoicesScreen(
                        invoices = invoices,
                        currentUser = currentUser,
                        settings = settings,
                        onDeleteInvoice = { viewModel.deleteInvoice(it) }
                    )
                }

                ScreenTab.STATEMENTS -> {
                    StatementsScreen(
                        customers = customers,
                        invoices = invoices,
                        bonds = bonds,
                        settings = settings,
                        preselectedCustomerId = selectedCustomerForStatement
                    )
                }

                ScreenTab.TRANSFER -> {
                    StockTransferScreen(
                        transfers = transfers,
                        products = products,
                        users = users,
                        currentUser = currentUser,
                        settings = settings,
                        onExecuteTransfer = { prodId, qty, cashier, note ->
                            viewModel.executeTransfer(prodId, qty, cashier, note)
                        }
                    )
                }

                ScreenTab.CASHIER_REPORTS -> {
                    CashierReportsScreen(
                        users = users,
                        invoices = invoices,
                        bonds = bonds,
                        settings = settings,
                        currentUser = currentUser
                    )
                }

                ScreenTab.USERS -> {
                    UsersScreen(
                        users = users,
                        currentUser = currentUser,
                        onSaveUser = { viewModel.saveUser(it) },
                        onDeleteUser = { viewModel.deleteUser(it) },
                        onOpenPermissions = { viewModel.openPermissionsForUser(it) }
                    )
                }

                ScreenTab.PERMISSIONS -> {
                    PermissionsScreen(
                        user = selectedUserForPermissions ?: currentUser,
                        onSavePermissions = { uId, perms ->
                            viewModel.saveUserPermissions(uId, perms)
                        },
                        onBack = { viewModel.navigateTo(ScreenTab.USERS) }
                    )
                }

                ScreenTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onSaveSettings = { viewModel.saveSettings(it) },
                        onClearAllData = { viewModel.clearAllData() },
                        onResetToDefaults = { viewModel.resetToDefaults() },
                        onExportBackupJson = { viewModel.exportBackupJson() },
                        onImportBackupJson = { viewModel.importBackupJson(it) }
                    )
                }

                ScreenTab.ABOUT -> {
                    AboutScreen(settings = settings)
                }
            }
        }
    }

    // User Switcher Dialog
    if (showUserSwitcherModal) {
        UserSwitcherModal(
            users = users,
            currentUser = currentUser,
            onDismiss = { showUserSwitcherModal = false },
            onUserSwitched = { user, pin ->
                val ok = viewModel.switchUser(user, pin)
                if (ok) showUserSwitcherModal = false
                ok
            }
        )
    }
}

@Composable
fun AppBottomNavigationBar(
    currentScreen: ScreenTab,
    onNavigate: (ScreenTab) -> Unit
) {
    val navItems = listOf(
        Triple("الرئيسية", Icons.Default.Home, ScreenTab.HOME),
        Triple("نقطة البيع", Icons.Default.PointOfSale, ScreenTab.POS),
        Triple("الأصناف", Icons.Default.Inventory2, ScreenTab.PRODUCTS),
        Triple("العملاء", Icons.Default.Group, ScreenTab.CUSTOMERS),
        Triple("السندات", Icons.Default.Receipt, ScreenTab.BONDS)
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (label, icon, screen) ->
                val isSelected = currentScreen == screen
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) TealContainer else Color.Transparent)
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("nav_tab_${screen.name}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) TealPrimary else Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TealDark else Slate600
                    )
                }
            }
        }
    }
}
