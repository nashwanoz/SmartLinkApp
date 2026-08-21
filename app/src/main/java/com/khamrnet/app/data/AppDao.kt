package com.khamrnet.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username AND userCode = :userCode LIMIT 1")
    suspend fun findByUserAndCode(username: String, userCode: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY createdAt DESC")
    fun getByUser(userId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY createdAt ASC")
    suspend fun getByCustomer(customerId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE userId = :userId")
    suspend fun getInvoicesForUserSync(userId: Long): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int
}

@Dao
interface FinancialBondDao {
    @Query("SELECT * FROM financial_bonds ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FinancialBondEntity>>

    @Query("SELECT * FROM financial_bonds WHERE customerId = :customerId ORDER BY createdAt ASC")
    suspend fun getByCustomer(customerId: Long): List<FinancialBondEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bond: FinancialBondEntity)

    @Query("SELECT COUNT(*) FROM financial_bonds")
    suspend fun count(): Int
}

@Dao
interface CashBoxDao {
    @Query("SELECT * FROM cash_boxes")
    fun getAll(): Flow<List<CashBoxEntity>>

    @Query("SELECT * FROM cash_boxes WHERE ownerUserId = :userId LIMIT 1")
    suspend fun findByOwner(userId: Long): CashBoxEntity?

    @Query("SELECT * FROM cash_boxes WHERE ownerUserId IS NULL LIMIT 1")
    suspend fun getMainBox(): CashBoxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(box: CashBoxEntity): Long

    @Update
    suspend fun update(box: CashBoxEntity)

    @Query("SELECT COUNT(*) FROM cash_boxes")
    suspend fun count(): Int
}

@Dao
interface StockBalanceDao {
    @Query("SELECT * FROM stock_balances WHERE userId IS NULL")
    fun getMainStock(): Flow<List<StockBalanceEntity>>

    @Query("SELECT * FROM stock_balances WHERE userId = :userId")
    fun getUserStock(userId: Long): Flow<List<StockBalanceEntity>>

    @Query("SELECT * FROM stock_balances WHERE userId IS NULL AND productId = :productId LIMIT 1")
    suspend fun findMainStock(productId: Long): StockBalanceEntity?

    @Query("SELECT * FROM stock_balances WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun findUserStock(userId: Long, productId: Long): StockBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(balance: StockBalanceEntity): Long

    @Update
    suspend fun update(balance: StockBalanceEntity)
}
