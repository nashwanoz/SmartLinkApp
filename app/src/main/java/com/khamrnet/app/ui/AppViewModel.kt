package com.khamrnet.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khamrnet.app.KhamrNetApp
import com.khamrnet.app.data.model.*
import com.khamrnet.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as KhamrNetApp).repository
    private val syncManager = (application as KhamrNetApp).cloudSyncManager

    private val _state = MutableStateFlow(AppUiState(ready = true))
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                repository.allProducts.collectLatest { products ->
                    _state.update { it.copy(products = products) }
                }
            }
            launch {
                repository.allCustomers.collectLatest { customers ->
                    _state.update { it.copy(customers = customers) }
                }
            }
            launch {
                repository.allInvoices.collectLatest { invoices ->
                    _state.update { it.copy(invoices = invoices) }
                }
            }
            launch {
                repository.allBonds.collectLatest { bonds ->
                    _state.update { it.copy(bonds = bonds) }
                }
            }
        }
    }

    fun login(username: String, userCode: String, password: String) {
        if (password.isNotBlank()) {
            val user = UserEntity(
                id = 1L,
                username = username,
                userCode = userCode,
                displayName = if (username.contains("admin", true)) "المدير العام" else username,
                role = if (username.contains("admin", true)) "ADMIN" else "CASHIER",
                password = password
            )
            _state.update { it.copy(user = user, error = null) }
        } else {
            _state.update { it.copy(error = "يرجى إدخال كلمة المرور") }
        }
    }

    fun logout() {
        _state.update { it.copy(user = null) }
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

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.insertProduct(product)
                _state.update { it.copy(message = "تم حفظ الصنف بنجاح") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _state.update { it.copy(message = "تم حذف الصنف بنجاح") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                repository.insertCustomer(customer)
                _state.update { it.copy(message = "تم حفظ العميل بنجاح") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveBond(bond: BondEntity) {
        viewModelScope.launch {
            try {
                repository.insertBond(bond)
                _state.update { it.copy(message = "تم حفظ السند بنجاح") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun syncData(storeCode: String) {
        viewModelScope.launch {
            try {
                syncManager.performSync(storeCode)
                _state.update { it.copy(message = "تمت المزامنة السحابية بنجاح") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "فشلت المزامنة") }
            }
        }
    }
}
