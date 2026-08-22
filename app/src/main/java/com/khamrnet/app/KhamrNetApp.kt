package com.khamrnet.app

import android.app.Application
import com.khamrnet.app.data.database.AppDatabase
import com.khamrnet.app.data.repository.AppRepository
import com.khamrnet.app.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class KhamrNetApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        AppRepository(
            productDao = database.productDao(),
            customerDao = database.customerDao(),
            invoiceDao = database.invoiceDao(),
            bondDao = database.bondDao(),
            systemSettingsDao = database.systemSettingsDao()
        )
    }
    val cloudSyncManager by lazy {
        CloudSyncManager(this, database)
    }
}
