package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        InvoiceEntity::class,
        BondEntity::class,
        StockTransferEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun bondDao(): BondDao
    abstract fun stockTransferDao(): StockTransferDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khamernet_pos_db"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    seedInitialData(database)
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            // Default System Settings
            database.settingsDao().insertSettings(
                SettingsEntity(
                    id = 1,
                    businessName = "شبكة خمر نت اللاسلكية",
                    tagline = "خدمات الشبكات والأنظمة ونقاط البيع",
                    address = "خمر - السوق العام",
                    phone = "783888185",
                    currency = "ريال يمني",
                    currencySymbol = "YER",
                    logoUrl = "",
                    taxNumber = "",
                    whatsappMode = "text",
                    autoPrintAfterInvoice = false
                )
            )

            // Default Users
            val defaultUsers = listOf(
                UserEntity(
                    id = "user_101",
                    userCode = "101",
                    name = "المدير العام (نشوان)",
                    role = UserRole.ADMIN,
                    username = "admin",
                    pin = "101",
                    active = true,
                    assignedBranch = "الفرع الرئيسي",
                    permissions = UserPermissions(
                        canAccessPos = true,
                        canSellNegativeStock = true,
                        canAccessProducts = true,
                        canAccessCustomers = true,
                        canSetOpeningBalance = true,
                        canAccessStatements = true,
                        canAccessInvoices = true,
                        canAccessBonds = true,
                        canAccessTransfers = true,
                        canAccessUsers = true,
                        canAccessSettings = true
                    )
                ),
                UserEntity(
                    id = "user_102",
                    userCode = "102",
                    name = "كاشير 1 - نقطة السوق",
                    role = UserRole.CASHIER,
                    username = "cashier1",
                    pin = "102",
                    active = true,
                    assignedBranch = "نقطة السوق",
                    permissions = UserPermissions(
                        canAccessPos = true,
                        canSellNegativeStock = false,
                        canAccessProducts = true,
                        canAccessCustomers = true,
                        canSetOpeningBalance = false,
                        canAccessStatements = false,
                        canAccessInvoices = true,
                        canAccessBonds = true,
                        canAccessTransfers = false,
                        canAccessUsers = false,
                        canAccessSettings = false
                    )
                ),
                UserEntity(
                    id = "user_103",
                    userCode = "103",
                    name = "كاشير 2 - النقطة الشرقية",
                    role = UserRole.CASHIER,
                    username = "cashier2",
                    pin = "103",
                    active = true,
                    assignedBranch = "النقطة الشرقية",
                    permissions = UserPermissions(
                        canAccessPos = true,
                        canSellNegativeStock = false,
                        canAccessProducts = true,
                        canAccessCustomers = true,
                        canSetOpeningBalance = false,
                        canAccessStatements = false,
                        canAccessInvoices = true,
                        canAccessBonds = true,
                        canAccessTransfers = false,
                        canAccessUsers = false,
                        canAccessSettings = false
                    )
                )
            )
            database.userDao().insertUsers(defaultUsers)
        }
    }
}
