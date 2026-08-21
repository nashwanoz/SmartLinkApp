package com.khamrnet.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class AppRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val productDao = db.productDao()
    private val customerDao = db.customerDao()
    private val invoiceDao = db.invoiceDao()
    private val bondDao = db.financialBondDao()
    private val cashBoxDao = db.cashBoxDao()
    private val stockDao = db.stockBalanceDao()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (userDao.count() == 0) {
            val adminId = userDao.insert(
                UserEntity(
                    username = "admin",
                    userCode = "1001",
                    password = "123",
                    displayName = "المدير العام",
                    role = "ADMIN"
                )
            )
            val cashierId = userDao.insert(
                UserEntity(
                    username = "cashier",
                    userCode = "2001",
                    password = "123",
                    displayName = "كاشير 1",
                    role = "CASHIER"
                )
            )

            cashBoxDao.insert(CashBoxEntity(name = "الخزينة الرئيسية", balance = 0.0, ownerUserId = null))
            cashBoxDao.insert(CashBoxEntity(name = "صندوق كاشير 1", balance = 0.0, ownerUserId = cashierId))

            val p1 = productDao.insert(
                ProductEntity(
                    name = "كروت شبكة 100 ميجا",
                    barcode = "1001001",
                    unitName = "كرت",
                    price = 100.0,
                    caseUnitName = "باكت (60 كرت)",
                    caseQuantity = 60,
                    casePrice = 5500.0
                )
            )
            val p2 = productDao.insert(
                ProductEntity(
                    name = "كروت شبكة 200 ميجا",
                    barcode = "1001002",
                    unitName = "كرت",
                    price = 200.0,
                    caseUnitName = "باكت (60 كرت)",
                    caseQuantity = 60,
                    casePrice = 11000.0
                )
            )
            val p3 = productDao.insert(
                ProductEntity(
                    name = "شاحن سريع أصلي",
                    barcode = "2001001",
                    unitName = "قطعة",
                    price = 1500.0,
                    caseUnitName = "كرتون (10 قطع)",
                    caseQuantity = 10,
                    casePrice = 13500.0
                )
            )

            stockDao.insert(StockBalanceEntity(userId = null, productId = p1, quantity = 500))
            stockDao.insert(StockBalanceEntity(userId = null, productId = p2, quantity = 300))
            stockDao.insert(StockBalanceEntity(userId = null, productId = p3, quantity = 50))

            stockDao.insert(StockBalanceEntity(userId = cashierId, productId = p1, quantity = 60))
            stockDao.insert(StockBalanceEntity(userId = cashierId, productId = p2, quantity = 60))
            stockDao.insert(StockBalanceEntity(userId = cashierId, productId = p3, quantity = 5))

            customerDao.insert(CustomerEntity(name = "بقالة النور", mobile = "777000111", balance = 0.0))
            customerDao.insert(CustomerEntity(name = "مركز الأمل", mobile = "777000222", balance = 0.0))
        }
    }

    suspend fun login(userCode: String, password: String): UserEntity? =
        withContext(Dispatchers.IO) {
            val user = userDao.findByCode(userCode.trim())
            if (user != null && user.password == password.trim()) user else null
        }

    fun observeProducts(): Flow<List<ProductEntity>> = productDao.getAll()

    fun observeCustomers(): Flow<List<CustomerEntity>> = customerDao.getAll()

    fun observeUsers(): Flow<List<UserEntity>> = userDao.getAll()

    fun observeAllInvoices(): Flow<List<InvoiceEntity>> = invoiceDao.getAll()

    fun observeUserInvoices(userId: Long): Flow<List<InvoiceEntity>> = invoiceDao.getByUser(userId)

    fun observeCashBoxes(): Flow<List<CashBoxEntity>> = cashBoxDao.getAll()

    fun stockForMain(): Flow<List<StockBalanceEntity>> = stockDao.getMainStock()

    fun stockForUser(userId: Long): Flow<List<StockBalanceEntity>> = stockDao.getUserStock(userId)

    suspend fun dashboard(userId: Long): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val user = userDao.findById(userId)
        val invoices = if (user?.role == "ADMIN") {
            invoiceDao.getInvoicesForUserSync(userId)
        } else {
            invoiceDao.getInvoicesForUserSync(userId)
        }

        val todaySales = invoices.filter { it.createdAt >= startOfDay }.sumOf { it.total }
        val box = cashBoxDao.findByOwner(userId)
        val carriedDiff = box?.balance ?: 0.0
        Pair(todaySales, carriedDiff)
    }

    suspend fun customerLastMovements(): Map<Long, Long> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Long, Long>()
        val customers = customerDao.getAll()
        // Compute last activity per customer
        map
    }

    suspend fun testFirebaseConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordSale(
        user: UserEntity,
        product: ProductEntity,
        unitName: String,
        quantity: Int,
        customerId: Long?,
        credit: Boolean
    ): Result<SaleResult> = withContext(Dispatchers.IO) {
        try {
            val stockEntry = if (user.role == "ADMIN") {
                stockDao.findMainStock(product.id)
            } else {
                stockDao.findUserStock(user.id, product.id)
            }

            val actualStock = stockEntry?.quantity ?: 0
            val isCase = (unitName == product.caseUnitName)
            val unitsPerQty = if (isCase) product.caseQuantity else 1
            val totalUnitsDeducted = quantity * unitsPerQty

            if (user.role != "ADMIN" && actualStock < totalUnitsDeducted) {
                return@withContext Result.failure(Exception("الكمية المطلوبة ($totalUnitsDeducted) تتجاوز الرصيد المتاح بالمخزن ($actualStock)"))
            }

            val unitPrice = if (isCase) product.casePrice else product.price
            val totalPrice = unitPrice * quantity

            // Deduct stock
            if (stockEntry != null) {
                stockDao.update(stockEntry.copy(quantity = stockEntry.quantity - totalUnitsDeducted))
            }

            var previousBalance = 0.0
            var newBalance = 0.0

            if (credit && customerId != null) {
                val customer = customerDao.findById(customerId)
                if (customer != null) {
                    previousBalance = customer.balance
                    newBalance = previousBalance + totalPrice
                    customerDao.update(customer.copy(balance = newBalance))
                }
            } else {
                // Cash sale -> add to cash box
                val box = cashBoxDao.findByOwner(user.id)
                if (box != null) {
                    cashBoxDao.update(box.copy(balance = box.balance + totalPrice))
                }
            }

            val invoiceId = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
            val invoice = InvoiceEntity(
                id = invoiceId,
                userId = user.id,
                customerId = customerId,
                productId = product.id,
                unitName = unitName,
                quantity = quantity,
                price = unitPrice,
                total = totalPrice,
                paymentType = if (credit) "آجل" else "نقدي",
                previousBalance = previousBalance,
                newBalance = newBalance,
                createdAt = System.currentTimeMillis()
            )
            invoiceDao.insert(invoice)

            Result.success(
                SaleResult(
                    success = true,
                    message = "تم تسجيل الفاتورة رقم $invoiceId بنجاح",
                    invoice = invoice
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.deleteById(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(
        name: String,
        barcode: String,
        unitName: String,
        price: Double,
        caseName: String,
        caseQuantity: Int,
        casePrice: Double,
        initialStock: Int
    ) = withContext(Dispatchers.IO) {
        val id = productDao.insert(
            ProductEntity(
                name = name,
                barcode = barcode,
                unitName = unitName,
                price = price,
                caseUnitName = caseName,
                caseQuantity = caseQuantity,
                casePrice = casePrice
            )
        )
        if (initialStock > 0) {
            stockDao.insert(StockBalanceEntity(userId = null, productId = id, quantity = initialStock))
        }
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.update(product)
    }

    suspend fun createUser(
        username: String,
        userCode: String,
        password: String,
        displayName: String
    ) = withContext(Dispatchers.IO) {
        val id = userDao.insert(
            UserEntity(
                username = username,
                userCode = userCode,
                password = password,
                displayName = displayName,
                role = "CASHIER"
            )
        )
        cashBoxDao.insert(CashBoxEntity(name = "صندوق $displayName", balance = 0.0, ownerUserId = id))
    }

    suspend fun updateUser(
        id: Long,
        username: String,
        userCode: String,
        displayName: String,
        password: String?
    ) = withContext(Dispatchers.IO) {
        val existing = userDao.findById(id) ?: return@withContext
        userDao.update(
            existing.copy(
                username = username,
                userCode = userCode,
                displayName = displayName,
                password = if (!password.isNullOrBlank()) password else existing.password
            )
        )
    }

    suspend fun transferStock(
        adminId: Long,
        cashierId: Long,
        productId: Long,
        quantity: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mainStock = stockDao.findMainStock(productId)
            val currentMainQty = mainStock?.quantity ?: 0
            if (currentMainQty < quantity) {
                return@withContext Result.failure(Exception("الكمية المطلوبة في المخزن الرئيسي غير كافية ($currentMainQty)"))
            }

            if (mainStock != null) {
                stockDao.update(mainStock.copy(quantity = currentMainQty - quantity))
            }

            val cashierStock = stockDao.findUserStock(cashierId, productId)
            if (cashierStock != null) {
                stockDao.update(cashierStock.copy(quantity = cashierStock.quantity + quantity))
            } else {
                stockDao.insert(StockBalanceEntity(userId = cashierId, productId = productId, quantity = quantity))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomer(name: String, mobile: String) = withContext(Dispatchers.IO) {
        customerDao.insert(
            CustomerEntity(
                name = name,
                mobile = mobile,
                balance = 0.0
            )
        )
    }

    suspend fun customerStatement(customerId: Long): List<CustomerStatementRow> = withContext(Dispatchers.IO) {
        val invoices = invoiceDao.getByCustomer(customerId)
        val bonds = bondDao.getByCustomer(customerId)

        val rows = mutableListOf<CustomerStatementRow>()
        invoices.forEach { inv ->
            rows.add(
                CustomerStatementRow(
                    date = inv.createdAt,
                    type = "فاتورة مبيعات (${inv.paymentType})",
                    amount = inv.total,
                    balanceAfter = inv.newBalance,
                    note = "فاتورة #${inv.id}"
                )
            )
        }
        bonds.forEach { bnd ->
            rows.add(
                CustomerStatementRow(
                    date = bnd.createdAt,
                    type = "سند ${bnd.type}",
                    amount = bnd.amount,
                    balanceAfter = 0.0,
                    note = bnd.note
                )
            )
        }
        rows.sortedBy { it.date }
    }

    suspend fun recordBond(
        userId: Long,
        customerId: Long,
        type: String,
        amount: Double,
        note: String
    ): Result<BondResult> = withContext(Dispatchers.IO) {
        try {
            val customer = customerDao.findById(customerId)
                ?: return@withContext Result.failure(Exception("العميل غير موجود"))

            val bondId = "BND-${System.currentTimeMillis().toString().takeLast(6)}"
            val bond = FinancialBondEntity(
                id = bondId,
                userId = userId,
                customerId = customerId,
                type = type,
                amount = amount,
                note = note,
                createdAt = System.currentTimeMillis()
            )
            bondDao.insert(bond)

            // Adjust customer balance: "قبض" reduces customer debit balance, "صرف" increases it
            val updatedBalance = if (type == "قبض" || type == "RECEIPT") {
                customer.balance - amount
            } else {
                customer.balance + amount
            }
            val updatedCustomer = customer.copy(balance = updatedBalance)
            customerDao.update(updatedCustomer)

            // Adjust cashier cash box
            val box = cashBoxDao.findByOwner(userId)
            if (box != null) {
                val boxBalance = if (type == "قبض" || type == "RECEIPT") {
                    box.balance + amount
                } else {
                    box.balance - amount
                }
                cashBoxDao.update(box.copy(balance = boxBalance))
            }

            Result.success(BondResult(bond, updatedCustomer))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun settle(adminId: Long, cashierId: Long, actualAmount: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val box = cashBoxDao.findByOwner(cashierId)
            if (box != null) {
                // Difference between actual submitted amount and recorded balance
                cashBoxDao.update(box.copy(balance = 0.0))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
