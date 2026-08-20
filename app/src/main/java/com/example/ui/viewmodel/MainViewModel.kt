package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BackupPayload
import com.example.data.model.Bond
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StockTransfer
import com.example.data.model.SystemSettings
import com.example.data.model.User
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole
import com.example.data.repository.AppRepository
import com.example.utils.Formatters
import com.example.utils.PermissionsHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    LOGIN,
    HOME,
    POS,
    PRODUCTS,
    CUSTOMERS,
    BONDS,
    INVOICES,
    STATEMENTS,
    TRANSFER,
    CASHIER_REPORTS,
    USERS,
    PERMISSIONS,
    SETTINGS,
    ABOUT
}

data class CartItem(
    val product: Product,
    val unitType: String, // "minor" or "major"
    val unitName: String,
    val quantity: Double,
    val unitPrice: Double,
    val convertedMinorQty: Double
) {
    val total: Double get() = quantity * unitPrice
}

data class DashboardStats(
    val todaySales: Double = 0.0,
    val totalDebt: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalPayments: Double = 0.0,
    val invoicesCount: Int = 0,
    val customersCount: Int = 0,
    val productsCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db)

    // State Flows
    val users = repository.usersFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val customers = repository.customersFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val products = repository.productsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val invoices = repository.invoicesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bonds = repository.bondsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val transfers = repository.transfersFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = repository.settingsFlow.stateIn(viewModelScope, SharingStarted.Lazily, SystemSettings())

    // Active session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScreenTab.LOGIN)
    val currentScreen: StateFlow<ScreenTab> = _currentScreen.asStateFlow()

    // Inter-screen navigation parameters
    private val _selectedCustomerForStatement = MutableStateFlow<String?>(null)
    val selectedCustomerForStatement: StateFlow<String?> = _selectedCustomerForStatement.asStateFlow()

    private val _selectedCustomerForBond = MutableStateFlow<Customer?>(null)
    val selectedCustomerForBond: StateFlow<Customer?> = _selectedCustomerForBond.asStateFlow()

    private val _selectedProductForPos = MutableStateFlow<Product?>(null)
    val selectedProductForPos: StateFlow<Product?> = _selectedProductForPos.asStateFlow()

    private val _selectedUserForPermissions = MutableStateFlow<User?>(null)
    val selectedUserForPermissions: StateFlow<User?> = _selectedUserForPermissions.asStateFlow()

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _posSelectedCustomer = MutableStateFlow<Customer?>(null)
    val posSelectedCustomer: StateFlow<Customer?> = _posSelectedCustomer.asStateFlow()

    private val _posDiscount = MutableStateFlow(0.0)
    val posDiscount: StateFlow<Double> = _posDiscount.asStateFlow()

    private val _posPaidAmount = MutableStateFlow(0.0)
    val posPaidAmount: StateFlow<Double> = _posPaidAmount.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val posPaymentMethod: StateFlow<PaymentMethod> = _posPaymentMethod.asStateFlow()

    // Toast/Message events
    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    // Computed Dashboard Stats
    val dashboardStats: StateFlow<DashboardStats> = combine(
        invoices,
        customers,
        bonds,
        products
    ) { invList, custList, bondList, prodList ->
        val todayStr = Formatters.formatDateOnly(Formatters.currentIsoDate())
        val todayInvoices = invList.filter { Formatters.formatDateOnly(it.date) == todayStr }
        val todaySales = todayInvoices.sumOf { it.total }

        val totalDebt = custList.filter { it.balance > 0 }.sumOf { it.balance }
        val totalReceipts = bondList.filter { it.type == com.example.data.model.BondType.RECEIPT }.sumOf { it.amount }
        val totalPayments = bondList.filter { it.type == com.example.data.model.BondType.PAYMENT }.sumOf { it.amount }

        DashboardStats(
            todaySales = todaySales,
            totalDebt = totalDebt,
            totalReceipts = totalReceipts,
            totalPayments = totalPayments,
            invoicesCount = invList.size,
            customersCount = custList.size,
            productsCount = prodList.size
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardStats())

    init {
        // Check if database needs first-time initialization check
        viewModelScope.launch {
            repository.saveSettings(settings.value)
        }
    }

    // --- Authentication & User Switcher ---
    fun login(user: User, pin: String): Boolean {
        if (user.pin.isBlank() || user.pin == pin.trim()) {
            _currentUser.value = user
            _currentScreen.value = ScreenTab.HOME
            return true
        }
        return false
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = ScreenTab.LOGIN
        clearPosCart()
    }

    fun switchUser(targetUser: User, pin: String): Boolean {
        if (targetUser.pin.isBlank() || targetUser.pin == pin.trim()) {
            _currentUser.value = targetUser
            return true
        }
        return false
    }

    fun navigateTo(screen: ScreenTab) {
        _currentScreen.value = screen
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _messageEvents.emit(msg)
        }
    }

    // --- Permission Checker ---
    fun canAccess(permissionExtractor: (UserPermissions) -> Boolean): Boolean {
        return PermissionsHelper.hasPermission(_currentUser.value, permissionExtractor)
    }

    // --- POS Operations ---
    fun addToCart(product: Product, unitType: String = "minor") {
        val user = _currentUser.value ?: return
        val currentCashierStock = product.stockCashier[user.id] ?: 0.0
        val isMajor = unitType == "major"
        val unitName = if (isMajor) product.caseUnitName else product.unitName
        val unitPrice = if (isMajor) product.casePrice else product.price
        val convertedQtyPerUnit = if (isMajor) product.caseQuantity else 1.0

        val currentCart = _cartItems.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id && it.unitType == unitType }

        val canSellNegative = canAccess { it.canSellNegativeStock }

        if (existingIndex != -1) {
            val existing = currentCart[existingIndex]
            val newQty = existing.quantity + 1.0
            val newConverted = newQty * convertedQtyPerUnit

            if (!canSellNegative && currentCashierStock > 0 && newConverted > (currentCashierStock + product.stockMain)) {
                showToast("الكمية المطلوبة تتجاوز المخزون المتاح!")
                return
            }

            currentCart[existingIndex] = existing.copy(
                quantity = newQty,
                convertedMinorQty = newConverted
            )
        } else {
            if (!canSellNegative && currentCashierStock <= 0 && product.stockMain <= 0) {
                showToast("تنبيه: الرصيد المخزني من هذا الصنف صفر!")
                return
            }

            currentCart.add(
                CartItem(
                    product = product,
                    unitType = unitType,
                    unitName = unitName,
                    quantity = 1.0,
                    unitPrice = unitPrice,
                    convertedMinorQty = convertedQtyPerUnit
                )
            )
        }
        _cartItems.value = currentCart
    }

    fun updateCartItemQuantity(productId: Long, unitType: String, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeFromCart(productId, unitType)
            return
        }
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId && it.unitType == unitType }
        if (index != -1) {
            val item = currentCart[index]
            val convRatio = if (item.unitType == "major") item.product.caseQuantity else 1.0
            currentCart[index] = item.copy(
                quantity = newQuantity,
                convertedMinorQty = newQuantity * convRatio
            )
            _cartItems.value = currentCart
        }
    }

    fun updateCartItemPrice(productId: Long, unitType: String, newPrice: Double) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId && it.unitType == unitType }
        if (index != -1) {
            val item = currentCart[index]
            currentCart[index] = item.copy(unitPrice = newPrice)
            _cartItems.value = currentCart
        }
    }

    fun removeFromCart(productId: Long, unitType: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId && it.unitType == unitType }
    }

    fun clearPosCart() {
        _cartItems.value = emptyList()
        _posDiscount.value = 0.0
        _posPaidAmount.value = 0.0
        _posPaymentMethod.value = PaymentMethod.CASH
        _posSelectedCustomer.value = null
    }

    fun setPosCustomer(customer: Customer?) {
        _posSelectedCustomer.value = customer
    }

    fun setPosDiscount(discount: Double) {
        _posDiscount.value = discount
    }

    fun setPosPaidAmount(paid: Double) {
        _posPaidAmount.value = paid
    }

    fun setPosPaymentMethod(method: PaymentMethod) {
        _posPaymentMethod.value = method
    }

    fun checkoutInvoice(notes: String = ""): Invoice? {
        val user = _currentUser.value ?: return null
        val items = _cartItems.value
        if (items.isEmpty()) {
            showToast("سلة المشتريات فارغة!")
            return null
        }

        val subtotal = items.sumOf { it.total }
        val discount = _posDiscount.value
        val total = maxOf(0.0, subtotal - discount)

        val customer = _posSelectedCustomer.value ?: Customer(
            id = "walk_in_customer",
            cCode = "0000",
            name = "عميل نقدي عام",
            balance = 0.0
        )

        val method = _posPaymentMethod.value
        val paidAmount = when (method) {
            PaymentMethod.CASH -> total
            PaymentMethod.CREDIT -> 0.0
            PaymentMethod.PARTIAL -> minOf(total, _posPaidAmount.value)
        }
        val remainingAmount = maxOf(0.0, total - paidAmount)

        val prevBalance = customer.balance
        val newBalance = prevBalance + remainingAmount

        val invoiceNumber = Formatters.generateOpNumber(
            userCode = user.userCode,
            opTypeCode = "4",
            existingNumbers = invoices.value.map { it.invoiceNumber }
        )

        val invoiceItems = items.map {
            InvoiceItem(
                productId = it.product.id,
                productName = it.product.name,
                unitType = it.unitType,
                unitName = it.unitName,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                total = it.total,
                convertedMinorQty = it.convertedMinorQty
            )
        }

        val invoice = Invoice(
            id = "inv_${System.currentTimeMillis()}_${(100..999).random()}",
            invoiceNumber = invoiceNumber,
            customerId = customer.id,
            customerCode = customer.cCode,
            customerName = customer.name,
            customerMobile = customer.mobile,
            cashierId = user.id,
            cashierCode = user.userCode,
            cashierName = user.name,
            items = invoiceItems,
            subtotal = subtotal,
            discount = discount,
            total = total,
            paymentMethod = method,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            prevCustomerBalance = prevBalance,
            newCustomerBalance = newBalance,
            date = Formatters.currentIsoDate(),
            notes = notes
        )

        viewModelScope.launch {
            repository.saveInvoice(invoice)
            clearPosCart()
            showToast("تم حفظ الفاتورة رقم [$invoiceNumber] بنجاح")
        }

        return invoice
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice.id)
            showToast("تم حذف الفاتورة رقم [${invoice.invoiceNumber}]")
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
            showToast("تم حذف الفاتورة")
        }
    }

    // --- Products Operations ---
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showToast("تم حفظ الصنف بنجاح")
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            showToast("تم حذف الصنف")
        }
    }

    // --- Customers Operations ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showToast("تم حفظ العميل بنجاح")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            showToast("تم حذف العميل")
        }
    }

    // --- Bonds Operations ---
    fun createBond(
        type: com.example.data.model.BondType,
        customer: Customer,
        amount: Double,
        note: String
    ): Bond? {
        val user = _currentUser.value ?: return null
        if (amount <= 0) {
            showToast("يرجى إدخال مبلغ صحيح للسند")
            return null
        }

        val opCode = if (type == com.example.data.model.BondType.RECEIPT) "1" else "2"
        val bondNumber = Formatters.generateOpNumber(
            userCode = user.userCode,
            opTypeCode = opCode,
            existingNumbers = bonds.value.map { it.bondNumber }
        )

        val prevBalance = customer.balance
        val delta = if (type == com.example.data.model.BondType.RECEIPT) -amount else amount
        val newBalance = prevBalance + delta

        val bond = Bond(
            id = "bond_${System.currentTimeMillis()}_${(100..999).random()}",
            bondNumber = bondNumber,
            type = type,
            customerId = customer.id,
            customerCode = customer.cCode,
            customerName = customer.name,
            customerMobile = customer.mobile,
            cashierId = user.id,
            cashierCode = user.userCode,
            cashierName = user.name,
            amount = amount,
            note = note,
            prevCustomerBalance = prevBalance,
            newCustomerBalance = newBalance,
            date = Formatters.currentIsoDate()
        )

        viewModelScope.launch {
            repository.saveBond(bond)
            showToast("تم حفظ ${if (type == com.example.data.model.BondType.RECEIPT) "سند القبض" else "سند الصرف"} رقم [$bondNumber]")
        }

        return bond
    }

    fun deleteBond(bondId: String) {
        viewModelScope.launch {
            repository.deleteBond(bondId)
            showToast("تم حذف السند وتحديث رصيد العميل")
        }
    }

    // --- Stock Transfer Operations ---
    fun executeTransfer(
        productId: Long,
        quantity: Double,
        toCashier: User,
        note: String
    ) {
        val user = _currentUser.value ?: return
        val transferNumber = Formatters.generateOpNumber(
            userCode = user.userCode,
            opTypeCode = "3",
            existingNumbers = transfers.value.map { it.transferNumber }
        )

        viewModelScope.launch {
            repository.executeTransfer(
                productId = productId,
                quantity = quantity,
                toCashierId = toCashier.id,
                toCashierName = toCashier.name,
                toCashierCode = toCashier.userCode,
                adminId = user.id,
                adminName = user.name,
                transferNumber = transferNumber,
                date = Formatters.currentIsoDate(),
                note = note
            )
            showToast("تم تحويل $quantity من المخزن إلى ${toCashier.name} بنجاح")
        }
    }

    // --- Users & Permissions Operations ---
    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
            showToast("تم حفظ بيانات المستخدم بنجاح")
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            showToast("تم حذف المستخدم")
        }
    }

    fun saveUserPermissions(userId: String, permissions: UserPermissions) {
        viewModelScope.launch {
            val target = users.value.find { it.id == userId }
            if (target != null) {
                val updated = target.copy(permissions = permissions)
                repository.saveUser(updated)
                if (_currentUser.value?.id == userId) {
                    _currentUser.value = updated
                }
                showToast("تم تحديث الصلاحيات بنجاح")
            }
        }
    }

    // --- Settings & Reset Operations ---
    fun saveSettings(newSettings: SystemSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            showToast("تم حفظ الإعدادات بنجاح")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearPosCart()
            showToast("تم تصفير البيانات بنجاح")
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            clearPosCart()
            showToast("تم استعادة الإعدادات الافتراضية بنجاح")
        }
    }

    fun exportBackupJson(): String {
        val payload = BackupPayload(
            settings = settings.value,
            users = users.value,
            customers = customers.value,
            products = products.value,
            invoices = invoices.value,
            bonds = bonds.value,
            transfers = transfers.value,
            exportDate = Formatters.currentIsoDate()
        )
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(BackupPayload::class.java)
        return adapter.toJson(payload)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(jsonString)
            if (payload != null) {
                viewModelScope.launch {
                    repository.restoreBackup(payload)
                    showToast("تم استيراد النسخة الاحتياطية بنجاح")
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            showToast("فشل استيراد النسخة الاحتياطية: ملف غير صالح")
            false
        }
    }

    // --- Navigation Selection Setters ---
    fun openStatementForCustomer(customerId: String) {
        _selectedCustomerForStatement.value = customerId
        _currentScreen.value = ScreenTab.STATEMENTS
    }

    fun openBondForCustomer(customer: Customer) {
        _selectedCustomerForBond.value = customer
        _currentScreen.value = ScreenTab.BONDS
    }

    fun openPosForProduct(product: Product) {
        _selectedProductForPos.value = product
        addToCart(product, "minor")
        _currentScreen.value = ScreenTab.POS
    }

    fun openPermissionsForUser(user: User) {
        _selectedUserForPermissions.value = user
        _currentScreen.value = ScreenTab.PERMISSIONS
    }
}
