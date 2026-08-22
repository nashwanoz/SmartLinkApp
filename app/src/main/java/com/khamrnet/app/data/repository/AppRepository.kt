package com.khamrnet.app.data.repository

import com.khamrnet.app.data.dao.*
import com.khamrnet.app.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val bondDao: BondDao,
    private val systemSettingsDao: SystemSettingsDao
) {
    // --- Products ---
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProductsFlow()
    suspend fun getProductById(id: String) = productDao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String) = productDao.getProductByBarcode(barcode)
    suspend fun insertProduct(product: ProductEntity) = productDao.insertProduct(product)
    suspend fun insertProducts(products: List<ProductEntity>) = productDao.insertProducts(products)
    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    // --- Customers ---
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomersFlow()
    suspend fun getCustomerById(id: String) = customerDao.getCustomerById(id)
    suspend fun getCustomerByCode(code: String) = customerDao.getCustomerByCode(code)
    suspend fun insertCustomer(customer: CustomerEntity) = customerDao.insertCustomer(customer)
    suspend fun insertCustomers(customers: List<CustomerEntity>) = customerDao.insertCustomers(customers)
    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    // --- Invoices ---
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoicesFlow()
    suspend fun getInvoiceById(id: String) = invoiceDao.getInvoiceById(id)
    suspend fun getInvoicesCount() = invoiceDao.getInvoicesCount()

    /**
     * Save new invoice with automatic stock decrement and customer balance update
     */
    suspend fun insertInvoice(invoice: InvoiceEntity, items: List<InvoiceItem>) {
        invoiceDao.insertInvoice(invoice)

        // Decrease stock for each sold item
        for (item in items) {
            val deductQty = item.quantity * item.unitFactor
            productDao.decreaseStock(item.productId, deductQty)
        }

        // Adjust customer balance if credit invoice
        if (invoice.remainingAmount > 0 || invoice.paymentMethod == "CREDIT" || invoice.billType == 4) {
            val creditAmount = invoice.remainingAmount.takeIf { it > 0 } ?: invoice.total
            customerDao.increaseBalance(invoice.customerId, creditAmount)
        }
    }

    suspend fun cancelInvoice(invoice: InvoiceEntity, items: List<InvoiceItem>) {
        invoiceDao.cancelInvoice(invoice.id)

        // Return stock
        for (item in items) {
            val returnQty = item.quantity * item.unitFactor
            productDao.increaseStock(item.productId, returnQty)
        }

        // Revert customer balance
        if (invoice.remainingAmount > 0 || invoice.paymentMethod == "CREDIT" || invoice.billType == 4) {
            val creditAmount = invoice.remainingAmount.takeIf { it > 0 } ?: invoice.total
            customerDao.decreaseBalance(invoice.customerId, creditAmount)
        }
    }

    // --- Bonds ---
    val allBonds: Flow<List<BondEntity>> = bondDao.getAllBondsFlow()
    suspend fun getBondById(id: String) = bondDao.getBondById(id)
    suspend fun getBondsCount() = bondDao.getBondsCount()

    suspend fun insertBond(bond: BondEntity) {
        bondDao.insertBond(bond)
        // Receipt (قبض) reduces debt / Payment (صرف) increases debt
        if (bond.customerId.isNotEmpty()) {
            if (bond.type == "RECEIPT" || bond.bondType == "RECEIPT") {
                customerDao.decreaseBalance(bond.customerId, bond.amount)
            } else {
                customerDao.increaseBalance(bond.customerId, bond.amount)
            }
        }
    }

    // --- Settings ---
    val systemSettings: Flow<SystemSettingsEntity?> = systemSettingsDao.getSettingsFlow()
    suspend fun getSettings() = systemSettingsDao.getSettings()
    suspend fun updateSettings(settings: SystemSettingsEntity) = systemSettingsDao.insertOrUpdate(settings)

    suspend fun clearAllData() {
        invoiceDao.deleteAllInvoices()
        bondDao.deleteAllBonds()
        productDao.deleteAllProducts()
        customerDao.deleteAllCustomers()
    }

    /**
     * Execute fiscal year closing:
     * 1. Rollover each customer's final balance to become their new opening balance (initialBalance).
     * 2. Rollover current inventory quantities as opening stock.
     * 3. Clear/archive operational invoices and bonds to reset transactions for the new year.
     */
    suspend fun executeFiscalYearClosing(
        currentCustomers: List<CustomerEntity>,
        currentProducts: List<ProductEntity>,
        targetYear: Int
    ) {
        // 1. Rollover customer balances
        val rolledOverCustomers = currentCustomers.map { c ->
            val finalBalance = if (c.currentBalance != 0.0) c.currentBalance else c.balance
            c.copy(
                initialBalance = finalBalance,
                currentBalance = finalBalance,
                balance = finalBalance,
                updatedAt = System.currentTimeMillis()
            )
        }
        if (rolledOverCustomers.isNotEmpty()) {
            customerDao.insertCustomers(rolledOverCustomers)
        }

        // 2. Rollover product stock
        if (currentProducts.isNotEmpty()) {
            productDao.insertProducts(currentProducts)
        }

        // 3. Clear operational invoices and bonds
        invoiceDao.deleteAllInvoices()
        bondDao.deleteAllBonds()
    }

    suspend fun restoreAllData(
        products: List<ProductEntity>,
        customers: List<CustomerEntity>,
        invoices: List<InvoiceEntity>,
        bonds: List<BondEntity>,
        settings: SystemSettingsEntity?
    ) {
        clearAllData()
        if (products.isNotEmpty()) productDao.insertProducts(products)
        if (customers.isNotEmpty()) customerDao.insertCustomers(customers)
        for (inv in invoices) {
            invoiceDao.insertInvoice(inv)
        }
        for (bond in bonds) {
            bondDao.insertBond(bond)
        }
        if (settings != null) {
            systemSettingsDao.insertOrUpdate(settings)
        }
    }
}
