package com.khamrnet.app.data.dao

import androidx.room.*
import com.khamrnet.app.data.model.SystemSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemSettingsDao {
    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SystemSettingsEntity?>

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SystemSettingsEntity?

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    fun getSettingsSync(): SystemSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SystemSettingsEntity)
}
