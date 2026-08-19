package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY userCode ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE active = 1 ORDER BY userCode ASC")
    fun getActiveUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userCode = :code LIMIT 1")
    suspend fun getUserByCode(code: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = :newBalance WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: String, newBalance: Double)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomer(customerId: String)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoicesFlow(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY date DESC")
    fun getInvoicesForCustomer(customerId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE cashierId = :cashierId ORDER BY date DESC")
    fun getInvoicesForCashier(cashierId: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()
}

@Dao
interface BondDao {
    @Query("SELECT * FROM bonds ORDER BY date DESC")
    fun getAllBondsFlow(): Flow<List<BondEntity>>

    @Query("SELECT * FROM bonds WHERE customerId = :customerId ORDER BY date DESC")
    fun getBondsForCustomer(customerId: String): Flow<List<BondEntity>>

    @Query("SELECT * FROM bonds WHERE id = :id LIMIT 1")
    suspend fun getBondById(id: String): BondEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBond(bond: BondEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonds(bonds: List<BondEntity>)

    @Query("DELETE FROM bonds WHERE id = :id")
    suspend fun deleteBond(id: String)

    @Query("DELETE FROM bonds")
    suspend fun deleteAllBonds()
}

@Dao
interface StockTransferDao {
    @Query("SELECT * FROM stock_transfers ORDER BY date DESC")
    fun getAllTransfersFlow(): Flow<List<StockTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: StockTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfers(transfers: List<StockTransferEntity>)

    @Query("DELETE FROM stock_transfers")
    suspend fun deleteAllTransfers()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
}
