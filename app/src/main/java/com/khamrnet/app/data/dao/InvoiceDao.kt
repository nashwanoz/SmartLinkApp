package com.khamrnet.app.data.dao

import androidx.room.*
import com.khamrnet.app.data.model.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE isCancelled = 0 ORDER BY date DESC")
    fun getAllInvoicesFlow(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE isCancelled = 0 ORDER BY date DESC")
    suspend fun getAllInvoices(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND isCancelled = 0 ORDER BY date DESC")
    suspend fun getInvoicesByCustomerId(customerId: String): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getInvoicesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET isCancelled = 1 WHERE id = :id")
    suspend fun cancelInvoice(id: String)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()
}
