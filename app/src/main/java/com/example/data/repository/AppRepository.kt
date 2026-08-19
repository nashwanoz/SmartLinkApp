package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BondEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.StockTransferEntity
import com.example.data.local.UserEntity
import com.example.data.model.BackupPayload
import com.example.data.model.Bond
import com.example.data.model.BondType
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.StockTransfer
import com.example.data.model.SystemSettings
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max

class AppRepository(private val db: AppDatabase) {

    // --- Users ---
    val usersFlow: Flow<List<User>> = db.userDao().getAllUsersFlow().map { entities ->
        entities.map { it.toModel() }
    }

    val activeUsersFlow: Flow<List<User>> = db.userDao().getActiveUsersFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveUser(user: User) {
        db.userDao().insertUser(user.toEntity())
    }

    suspend fun deleteUser(userId: String) {
        db.userDao().deleteUser(userId)
    }

    // --- Customers ---
    val customersFlow: Flow<List<Customer>> = db.customerDao().getAllCustomersFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveCustomer(customer: Customer) {
        db.customerDao().insertCustomer(customer.toEntity())
    }

    suspend fun deleteCustomer(customerId: String) {
        db.customerDao().deleteCustomer(customerId)
    }

    // --- Products ---
    val productsFlow: Flow<List<Product>> = db.productDao().getAllProductsFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveProduct(product: Product) {
        db.productDao().insertProduct(product.toEntity())
    }

    suspend fun deleteProduct(productId: Long) {
        db.productDao().deleteProduct(productId)
    }

    // --- Invoices ---
    val invoicesFlow: Flow<List<Invoice>> = db.invoiceDao().getAllInvoicesFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveInvoice(invoice: Invoice) {
        // 1. Insert Invoice
        db.invoiceDao().insertInvoice(invoice.toEntity())

        // 2. Adjust Customer Balance (remaining amount added to customer debt)
        val customerEntity = db.customerDao().getCustomerById(invoice.customerId)
        if (customerEntity != null) {
            val newBalance = customerEntity.balance + invoice.remainingAmount
            db.customerDao().updateCustomerBalance(invoice.customerId, newBalance)
        }

        // 3. Deduct Stock from Products
        for (item in invoice.items) {
            val productEntity = db.productDao().getProductById(item.productId)
            if (productEntity != null) {
                val currentCashierQty = productEntity.stockCashier[invoice.cashierId] ?: 0.0
                val updatedStockCashier = productEntity.stockCashier.toMutableMap()
                var updatedMainStock = productEntity.stockMain

                if (currentCashierQty >= item.convertedMinorQty) {
                    updatedStockCashier[invoice.cashierId] = currentCashierQty - item.convertedMinorQty
                } else {
                    val remToDeduct = item.convertedMinorQty - currentCashierQty
                    updatedStockCashier[invoice.cashierId] = 0.0
                    updatedMainStock = max(0.0, updatedMainStock - remToDeduct)
                }

                val updatedProduct = productEntity.copy(
                    stockMain = updatedMainStock,
                    stockCashier = updatedStockCashier
                )
                db.productDao().updateProduct(updatedProduct)
            }
        }
    }

    suspend fun deleteInvoice(invoiceId: String) {
        db.invoiceDao().deleteInvoice(invoiceId)
    }

    // --- Bonds ---
    val bondsFlow: Flow<List<Bond>> = db.bondDao().getAllBondsFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveBond(bond: Bond) {
        // 1. Insert Bond
        db.bondDao().insertBond(bond.toEntity())

        // 2. Update Customer Balance
        // RECEIPT (سند قبض): decreases debt (- amount)
        // PAYMENT (سند صرف): increases debt (+ amount)
        val customerEntity = db.customerDao().getCustomerById(bond.customerId)
        if (customerEntity != null) {
            val delta = if (bond.type == BondType.RECEIPT) -bond.amount else bond.amount
            val newBalance = customerEntity.balance + delta
            db.customerDao().updateCustomerBalance(bond.customerId, newBalance)
        }
    }

    suspend fun deleteBond(bondId: String) {
        val bondEntity = db.bondDao().getBondById(bondId)
        if (bondEntity != null) {
            // Reverse customer balance effect
            val customerEntity = db.customerDao().getCustomerById(bondEntity.customerId)
            if (customerEntity != null) {
                val reverseDelta = if (bondEntity.type == BondType.RECEIPT) bondEntity.amount else -bondEntity.amount
                val newBalance = customerEntity.balance + reverseDelta
                db.customerDao().updateCustomerBalance(bondEntity.customerId, newBalance)
            }
            db.bondDao().deleteBond(bondId)
        }
    }

    // --- Stock Transfers ---
    val transfersFlow: Flow<List<StockTransfer>> = db.stockTransferDao().getAllTransfersFlow().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun executeTransfer(
        productId: Long,
        quantity: Double,
        toCashierId: String,
        toCashierName: String,
        toCashierCode: String,
        adminId: String,
        adminName: String,
        transferNumber: String,
        date: String,
        note: String
    ) {
        val productEntity = db.productDao().getProductById(productId) ?: return

        // 1. Deduct from main stock, add to cashier stock
        val currentCashierQty = productEntity.stockCashier[toCashierId] ?: 0.0
        val updatedCashierMap = productEntity.stockCashier.toMutableMap()
        updatedCashierMap[toCashierId] = currentCashierQty + quantity

        val updatedProduct = productEntity.copy(
            stockMain = max(0.0, productEntity.stockMain - quantity),
            stockCashier = updatedCashierMap
        )
        db.productDao().updateProduct(updatedProduct)

        // 2. Record transfer log
        val transferEntity = StockTransferEntity(
            id = "tr_${System.currentTimeMillis()}_${(100..999).random()}",
            transferNumber = transferNumber,
            productId = productId,
            productName = productEntity.name,
            unitName = productEntity.unitName,
            quantity = quantity,
            fromLocation = "المخزن الرئيسي",
            toCashierId = toCashierId,
            toCashierName = toCashierName,
            toCashierCode = toCashierCode,
            adminId = adminId,
            adminName = adminName,
            date = date,
            note = note.ifBlank { "تحويل وتغذية عهدة كاشير" }
        )
        db.stockTransferDao().insertTransfer(transferEntity)
    }

    // --- Settings ---
    val settingsFlow: Flow<SystemSettings> = db.settingsDao().getSettingsFlow().map { entity ->
        entity?.toModel() ?: SystemSettings()
    }

    suspend fun saveSettings(settings: SystemSettings) {
        db.settingsDao().insertSettings(settings.toEntity())
    }

    // --- Reset and Backups ---
    suspend fun clearAllData() {
        db.customerDao().deleteAllCustomers()
        db.productDao().deleteAllProducts()
        db.invoiceDao().deleteAllInvoices()
        db.bondDao().deleteAllBonds()
        db.stockTransferDao().deleteAllTransfers()
    }

    suspend fun resetToDefaults() {
        clearAllData()
        db.userDao().deleteAllUsers()
        AppDatabase.seedInitialData(db)
    }

    suspend fun restoreBackup(payload: BackupPayload) {
        payload.settings?.let { saveSettings(it) }
        if (payload.users.isNotEmpty()) {
            db.userDao().insertUsers(payload.users.map { it.toEntity() })
        }
        if (payload.customers.isNotEmpty()) {
            db.customerDao().insertCustomers(payload.customers.map { it.toEntity() })
        }
        if (payload.products.isNotEmpty()) {
            db.productDao().insertProducts(payload.products.map { it.toEntity() })
        }
        if (payload.invoices.isNotEmpty()) {
            db.invoiceDao().insertInvoices(payload.invoices.map { it.toEntity() })
        }
        if (payload.bonds.isNotEmpty()) {
            db.bondDao().insertBonds(payload.bonds.map { it.toEntity() })
        }
        if (payload.transfers.isNotEmpty()) {
            db.stockTransferDao().insertTransfers(payload.transfers.map { it.toEntity() })
        }
    }

    // --- Mapping Extensions ---
    private fun UserEntity.toModel() = User(
        id = id,
        userCode = userCode,
        name = name,
        role = role,
        username = username,
        pin = pin,
        active = active,
        assignedBranch = assignedBranch,
        permissions = permissions
    )

    private fun User.toEntity() = UserEntity(
        id = id,
        userCode = userCode,
        name = name,
        role = role,
        username = username,
        pin = pin,
        active = active,
        assignedBranch = assignedBranch,
        permissions = permissions
    )

    private fun CustomerEntity.toModel() = Customer(
        id = id,
        cCode = cCode,
        name = name,
        mobile = mobile,
        balance = balance,
        address = address,
        createdAt = createdAt
    )

    private fun Customer.toEntity() = CustomerEntity(
        id = id,
        cCode = cCode,
        name = name,
        mobile = mobile,
        balance = balance,
        address = address,
        createdAt = createdAt
    )

    private fun ProductEntity.toModel() = Product(
        id = id,
        name = name,
        barcode = barcode,
        unitName = unitName,
        price = price,
        caseUnitName = caseUnitName,
        caseQuantity = caseQuantity,
        casePrice = casePrice,
        stockMain = stockMain,
        stockCashier = stockCashier,
        category = category
    )

    private fun Product.toEntity() = ProductEntity(
        id = id,
        name = name,
        barcode = barcode,
        unitName = unitName,
        price = price,
        caseUnitName = caseUnitName,
        caseQuantity = caseQuantity,
        casePrice = casePrice,
        stockMain = stockMain,
        stockCashier = stockCashier,
        category = category
    )

    private fun InvoiceEntity.toModel() = Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        customerId = customerId,
        customerCode = customerCode,
        customerName = customerName,
        customerMobile = customerMobile,
        cashierId = cashierId,
        cashierCode = cashierCode,
        cashierName = cashierName,
        items = items,
        subtotal = subtotal,
        discount = discount,
        total = total,
        paymentMethod = paymentMethod,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        prevCustomerBalance = prevCustomerBalance,
        newCustomerBalance = newCustomerBalance,
        date = date,
        notes = notes
    )

    private fun Invoice.toEntity() = InvoiceEntity(
        id = id,
        invoiceNumber = invoiceNumber,
        customerId = customerId,
        customerCode = customerCode,
        customerName = customerName,
        customerMobile = customerMobile,
        cashierId = cashierId,
        cashierCode = cashierCode,
        cashierName = cashierName,
        items = items,
        subtotal = subtotal,
        discount = discount,
        total = total,
        paymentMethod = paymentMethod,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        prevCustomerBalance = prevCustomerBalance,
        newCustomerBalance = newCustomerBalance,
        date = date,
        notes = notes
    )

    private fun BondEntity.toModel() = Bond(
        id = id,
        bondNumber = bondNumber,
        type = type,
        customerId = customerId,
        customerCode = customerCode,
        customerName = customerName,
        customerMobile = customerMobile,
        cashierId = cashierId,
        cashierCode = cashierCode,
        cashierName = cashierName,
        amount = amount,
        note = note,
        prevCustomerBalance = prevCustomerBalance,
        newCustomerBalance = newCustomerBalance,
        date = date
    )

    private fun Bond.toEntity() = BondEntity(
        id = id,
        bondNumber = bondNumber,
        type = type,
        customerId = customerId,
        customerCode = customerCode,
        customerName = customerName,
        customerMobile = customerMobile,
        cashierId = cashierId,
        cashierCode = cashierCode,
        cashierName = cashierName,
        amount = amount,
        note = note,
        prevCustomerBalance = prevCustomerBalance,
        newCustomerBalance = newCustomerBalance,
        date = date
    )

    private fun StockTransferEntity.toModel() = StockTransfer(
        id = id,
        transferNumber = transferNumber,
        productId = productId,
        productName = productName,
        unitName = unitName,
        quantity = quantity,
        fromLocation = fromLocation,
        toCashierId = toCashierId,
        toCashierName = toCashierName,
        toCashierCode = toCashierCode,
        adminId = adminId,
        adminName = adminName,
        date = date,
        note = note
    )

    private fun StockTransfer.toEntity() = StockTransferEntity(
        id = id,
        transferNumber = transferNumber,
        productId = productId,
        productName = productName,
        unitName = unitName,
        quantity = quantity,
        fromLocation = fromLocation,
        toCashierId = toCashierId,
        toCashierName = toCashierName,
        toCashierCode = toCashierCode,
        adminId = adminId,
        adminName = adminName,
        date = date,
        note = note
    )

    private fun SettingsEntity.toModel() = SystemSettings(
        businessName = businessName,
        tagline = tagline,
        address = address,
        phone = phone,
        currency = currency,
        currencySymbol = currencySymbol,
        logoUrl = logoUrl,
        taxNumber = taxNumber,
        whatsappMode = whatsappMode,
        autoPrintAfterInvoice = autoPrintAfterInvoice
    )

    private fun SystemSettings.toEntity() = SettingsEntity(
        id = 1,
        businessName = businessName,
        tagline = tagline,
        address = address,
        phone = phone,
        currency = currency,
        currencySymbol = currencySymbol,
        logoUrl = logoUrl,
        taxNumber = taxNumber,
        whatsappMode = whatsappMode,
        autoPrintAfterInvoice = autoPrintAfterInvoice
    )
}
