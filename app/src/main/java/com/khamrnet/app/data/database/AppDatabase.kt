package com.khamrnet.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.khamrnet.app.data.dao.*
import com.khamrnet.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        InvoiceEntity::class,
        BondEntity::class,
        SystemSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun bondDao(): BondDao
    abstract fun systemSettingsDao(): SystemSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khamrnet_pos.db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Seed default system settings
                        database.systemSettingsDao().insertOrUpdate(
                            SystemSettingsEntity(
                                id = 1,
                                businessName = "شبكة خمر اللاسلكيه",
                                phone = "783888185",
                                address = "خمر - السوق العام",
                                currencyName = "YER",
                                thermalPaperWidth = "80mm",
                                silentBluetoothPrint = true
                            )
                        )
                        // Seed default general customer
                        database.customerDao().insertCustomer(
                            CustomerEntity(
                                id = "cust-1",
                                code = "1001",
                                name = "عميل نقدي عام",
                                phone = "",
                                address = "محلي",
                                initialBalance = 0.0,
                                currentBalance = 0.0
                            )
                        )
                    }
                }
            }
        }
    }
}
