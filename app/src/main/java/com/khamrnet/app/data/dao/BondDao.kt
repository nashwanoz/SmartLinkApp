package com.khamrnet.app.data.dao

import androidx.room.*
import com.khamrnet.app.data.model.BondEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BondDao {
    @Query("SELECT * FROM bonds ORDER BY date DESC")
    fun getAllBondsFlow(): Flow<List<BondEntity>>

    @Query("SELECT * FROM bonds ORDER BY date DESC")
    suspend fun getAllBonds(): List<BondEntity>

    @Query("SELECT * FROM bonds WHERE id = :id LIMIT 1")
    suspend fun getBondById(id: String): BondEntity?

    @Query("SELECT * FROM bonds WHERE customerId = :customerId ORDER BY date DESC")
    suspend fun getBondsByCustomerId(customerId: String): List<BondEntity>

    @Query("SELECT COUNT(*) FROM bonds")
    suspend fun getBondsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBond(bond: BondEntity)

    @Update
    suspend fun updateBond(bond: BondEntity)

    @Delete
    suspend fun deleteBond(bond: BondEntity)
}
