package com.khamrnet.app.data.dao

import androidx.room.*
import com.khamrnet.app.data.model.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE code = :code LIMIT 1")
    suspend fun getCustomerByCode(code: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET currentBalance = currentBalance + :amount WHERE id = :customerId")
    suspend fun increaseBalance(customerId: String, amount: Double)

    @Query("UPDATE customers SET currentBalance = currentBalance - :amount WHERE id = :customerId")
    suspend fun decreaseBalance(customerId: String, amount: Double)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}
