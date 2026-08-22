package com.khamrnet.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khamrnet.app.data.AppRepository
import com.khamrnet.app.data.CustomerEntity
import com.khamrnet.app.data.CustomerStatementRow
import com.khamrnet.app.data.ProductEntity
import com.khamrnet.app.data.UserEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initialize()
            _state.update { it.copy(ready = true) }
        }
    }

    fun login(username: String, userCode: String, password: String) {
        viewModelScope.launch {
            val user = repository.login(username, userCode, password)
            if (user != null) {
                _state.update { it.copy(user = user, error = null) }
                startObserving(user)
            } else {
                _state.update { it.copy(error = "بيانات الدخول غير صحيحة") }
            }
        }
    }

    fun logout() {
        observeJob?.cancel()
        _state.update { AppUiState(ready = true) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun clearSaleReceipt() {
        _state.update { it.copy(saleReceipt = null) }
    }

    fun clearBondReceipt() {
        _state.update { it.copy(bondReceipt = null) }
    }

    private fun startObserving(user: UserEntity) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            launch {
                repository.observeProducts().collectLatest { products ->
                    _state.update { it.copy(products = products) }
                }
            }
            launch {
                repository.observeCustomers().collectLatest { customers ->
                    _state.update { it.copy(customers = customers) }
                    loadCustomerMovements()
                }
            }
            launch {
                repository.observeUsers().collectLatest { users ->
                    _state.update { it.copy(users = users) }
                }
            }
            launch {
                if (user.role == "ADMIN") {
                    repository.observeAllInvoices().collectLatest { invoices ->
                        _state.update { it.copy(invoices = invoices) }
                        refreshDashboard(user.id)
                    }
                } else {
                    repository.observeUserInvoices(user.id).collectLatest { invoices ->
                        _state.update { it.copy(invoices = invoices) }
                        refreshDashboard(user.id)
                    }
                }
            }
            launch {
                repository.observeCashBoxes().collectLatest { boxes ->
                    val map = boxes.filter { it.ownerUserId != null }.associate { it.ownerUserId!! to it.balance }
                    _state.update { it.copy(cashBoxes = boxes, cashBalances = map) }
                }
            }
            launch {
                val stockFlow = if (user.role == "ADMIN") repository.stockForMain() else repository.stockForUser(user.id)
                stockFlow.collectLatest { balances ->
                    val map = balances.associate { it.productId to it.quantity }
                    _state.update { it.copy(stock = map) }
                }
            }
        }
    }

    private suspend fun refreshDashboard(userId: Long) {
        val (todaySales, carriedDiff) = repository.dashboard(userId)
        _state.update {
            it.copy(
                stats = AppStats(
                    todaySales = todaySales,
                    carriedDifference = carriedDiff
                )
            )
        }
    }

    private suspend fun loadCustomerMovements() {
        val movements = repository.customerLastMovements()
        _state.update { it.copy(customerLastMovement = movements) }
    }

    fun testFirebaseConnection() {
        viewModelScope.launch {
            val result = repository.testFirebaseConnection()
            if (result.isSuccess) {
                _state.update { it.copy(message = "تم الاتصال بـ Firebase بنجاح!") }
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "فشل الاتصال بـ Firebase") }
            }
        }
    }

    fun sell(product: ProductEntity, unitName: String, quantity: Int, customerId: Long?, credit: Boolean) {
        val currentUser = _state.value.user ?: return
        viewModelScope.launch {
            val result = repository.recordSale(currentUser, product, unitName, quantity, customerId, credit)
            result.onSuccess { saleResult ->
                val customer = customerId?.let { id -> _state.value.customers.firstOrNull { it.id == id } }
                _state.update {
                    it.copy(
                        message = saleResult.message,
                        saleReceipt = SaleReceipt(saleResult.invoice, customer)
                    )
                }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            val result = repository.deleteProduct(productId)
            result.onSuccess {
                _state.update { it.copy(message = "تم حذف الصنف بنجاح") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun createProduct(
        name: String,
        barcode: String,
        unitName: String,
        price: Double,
        caseName: String,
        caseQuantity: Int,
        casePrice: Double,
        initialStock: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createProduct(name, barcode, unitName, price, caseName, caseQuantity, casePrice, initialStock)
            }.onSuccess {
                _state.update { it.copy(message = "تم إنشاء الصنف بنجاح") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            runCatching {
                repository.updateProduct(product)
            }.onSuccess {
                _state.update { it.copy(message = "تم تحديث بيانات الصنف") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun createUser(username: String, userCode: String, password: String, displayName: String) {
        viewModelScope.launch {
            runCatching {
                repository.createUser(username, userCode, password, displayName)
            }.onSuccess {
                _state.update { it.copy(message = "تم إضافة الكاشير بنجاح") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun updateUser(user: UserEntity, username: String, userCode: String, password: String?, displayName: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateUser(user.id, username, userCode, displayName, password)
            }.onSuccess {
                _state.update { it.copy(message = "تم تحديث بيانات المستخدم") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun transferStock(cashierId: Long, productId: Long, quantity: Int) {
        val admin = _state.value.user ?: return
        viewModelScope.launch {
            val result = repository.transferStock(admin.id, cashierId, productId, quantity)
            result.onSuccess {
                _state.update { it.copy(message = "تم تحويل الكمية إلى مخزن الكاشير") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun addCustomer(name: String, mobile: String) {
        viewModelScope.launch {
            runCatching {
                repository.addCustomer(name, mobile)
            }.onSuccess {
                _state.update { it.copy(message = "تم إضافة العميل بنجاح") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun loadCustomerStatement(customerId: Long, onResult: (List<CustomerStatementRow>) -> Unit) {
        viewModelScope.launch {
            val rows = repository.customerStatement(customerId)
            onResult(rows)
        }
    }

    fun bond(customerId: Long, type: String, amount: Double, note: String) {
        val currentUser = _state.value.user ?: return
        viewModelScope.launch {
            val result = repository.recordBond(currentUser.id, customerId, type, amount, note)
            result.onSuccess { bondResult ->
                _state.update {
                    it.copy(
                        message = "تم حفظ السند بنجاح",
                        bondReceipt = BondReceipt(bondResult.bond, bondResult.customer)
                    )
                }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }

    fun settle(cashierId: Long, actualAmount: Double) {
        val admin = _state.value.user ?: return
        viewModelScope.launch {
            val result = repository.settle(admin.id, cashierId, actualAmount)
            result.onSuccess {
                _state.update { it.copy(message = "تم اعتماد التصفية وحفظ الفرق") }
            }.onFailure { err ->
                _state.update { it.copy(error = err.message) }
            }
        }
    }
}
