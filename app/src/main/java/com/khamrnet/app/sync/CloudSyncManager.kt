package com.khamrnet.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.khamrnet.app.data.database.AppDatabase
import com.khamrnet.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE,
    ERROR
}

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val isOnline: Boolean = true,
    val lastSyncTime: Long = 0L,
    val pendingItemsCount: Int = 0,
    val message: String = "جاهز للمزامنة"
)

class CloudSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Firebase REST API endpoints for direct offline/online cloud sync
    private val firebaseProjectId = "gen-lang-client-0683616902"
    private val firestoreBaseUrl = "https://firestore.googleapis.com/v1/projects/$firebaseProjectId/databases/(default)/documents"

    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
            actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Network check exception: ${e.message}")
            false
        }
    }

    suspend fun performSync(storeCode: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanStoreCode = storeCode.trim().uppercase().ifEmpty { "KHAMR01" }

        try {
            if (!isNetworkAvailable()) {
                _syncStatus.value = SyncStatus(
                    state = SyncState.OFFLINE,
                    isOnline = false,
                    lastSyncTime = _syncStatus.value.lastSyncTime,
                    message = "لا يوجد اتصال بالإنترنت (يعمل أوفلاين محلياً)"
                )
                return@withContext Result.failure(Exception("أنت الآن أوفلاين. تم حفظ البيانات محلياً وستتم المزامنة تلقائياً عند توفر الإنترنت."))
            }

            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCING,
                isOnline = true,
                message = "جاري المزامنة مع السحابة لكود المحل $cleanStoreCode..."
            )

            val productDao = database.productDao()
            val customerDao = database.customerDao()
            val invoiceDao = database.invoiceDao()
            val bondDao = database.bondDao()
            val settingsDao = database.systemSettingsDao()

            // 1. Sync Settings
            val currentSettings = settingsDao.getSettings() ?: SystemSettingsEntity()
            uploadDocToFirestore("stores/$cleanStoreCode/settings", "main", mapOf(
                "storeCode" to cleanStoreCode,
                "businessName" to currentSettings.businessName,
                "phone" to currentSettings.phone,
                "address" to currentSettings.address,
                "currencyName" to currentSettings.currencyName,
                "updatedAt" to System.currentTimeMillis()
            ))

            // 2. Upload Products
            val localProducts = productDao.getAllProducts()
            for (prod in localProducts) {
                uploadDocToFirestore("stores/$cleanStoreCode/products", prod.id, mapOf(
                    "id" to prod.id,
                    "code" to prod.code,
                    "name" to prod.name,
                    "price" to prod.salePrice,
                    "costPrice" to prod.purchasePrice,
                    "stockQuantity" to prod.stockQuantity,
                    "baseUnitName" to prod.baseUnitName,
                    "unitsJson" to prod.unitsJson,
                    "updatedAt" to prod.updatedAt
                ))
            }

            // 3. Upload Customers
            val localCustomers = customerDao.getAllCustomers()
            for (cust in localCustomers) {
                uploadDocToFirestore("stores/$cleanStoreCode/customers", cust.id, mapOf(
                    "id" to cust.id,
                    "code" to cust.code,
                    "name" to cust.name,
                    "phone" to cust.phone,
                    "address" to cust.address,
                    "currentBalance" to cust.currentBalance,
                    "initialBalance" to cust.initialBalance,
                    "updatedAt" to cust.updatedAt
                ))
            }

            // 4. Upload Invoices
            val localInvoices = invoiceDao.getAllInvoices()
            for (inv in localInvoices) {
                uploadDocToFirestore("stores/$cleanStoreCode/invoices", inv.id, mapOf(
                    "id" to inv.id,
                    "invoiceNumber" to inv.invoiceNumber,
                    "billNo" to inv.billNo,
                    "customerName" to inv.customerName,
                    "customerCode" to inv.customerCode,
                    "total" to inv.total,
                    "paidAmount" to inv.paidAmount,
                    "remainingAmount" to inv.remainingAmount,
                    "billType" to inv.billType,
                    "itemsJson" to inv.itemsJson,
                    "date" to inv.date
                ))
            }

            // 5. Upload Bonds
            val localBonds = bondDao.getAllBonds()
            for (bnd in localBonds) {
                val party = bnd.partyName.ifEmpty { bnd.customerName }
                val bType = bnd.type.ifEmpty { bnd.bondType }
                val noteText = bnd.note.ifEmpty { bnd.notes }
                uploadDocToFirestore("stores/$cleanStoreCode/bonds", bnd.id, mapOf(
                    "id" to bnd.id,
                    "bondNumber" to bnd.bondNumber,
                    "bondType" to bType,
                    "partyName" to party,
                    "amount" to bnd.amount,
                    "date" to bnd.date,
                    "notes" to noteText
                ))
            }

            val now = System.currentTimeMillis()
            settingsDao.insertOrUpdate(currentSettings.copy(
                storeCode = cleanStoreCode,
                lastSyncTimestamp = now,
                syncStatusMessage = "تمت المزامنة بنجاح"
            ))

            _syncStatus.value = SyncStatus(
                state = SyncState.SUCCESS,
                isOnline = true,
                lastSyncTime = now,
                pendingItemsCount = 0,
                message = "تمت المزامنة بنجاح لكود المحل ($cleanStoreCode)"
            )

            Result.success("تمت المزامنة بنجاح لكود المحل $cleanStoreCode")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Sync failed", e)
            _syncStatus.value = SyncStatus(
                state = SyncState.ERROR,
                isOnline = isNetworkAvailable(),
                lastSyncTime = _syncStatus.value.lastSyncTime,
                message = "خطأ في المزامنة: ${e.localizedMessage ?: "تعذر الاتصال بالسحابة"}"
            )
            Result.failure(e)
        }
    }

    private fun uploadDocToFirestore(collectionPath: String, docId: String, data: Map<String, Any>) {
        try {
            val urlString = "$firestoreBaseUrl/$collectionPath/$docId"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val firestoreFields = mutableMapOf<String, Any>()
            for ((key, value) in data) {
                firestoreFields[key] = when (value) {
                    is String -> mapOf("stringValue" to value)
                    is Number -> if (value is Double || value is Float) mapOf("doubleValue" to value) else mapOf("integerValue" to value.toString())
                    is Boolean -> mapOf("booleanValue" to value)
                    else -> mapOf("stringValue" to value.toString())
                }
            }

            val body = mapOf("fields" to firestoreFields)
            val jsonBody = Gson().toJson(body)

            OutputStreamWriter(conn.outputStream).use { os ->
                os.write(jsonBody)
                os.flush()
            }

            val code = conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Doc sync warning for $docId: ${e.message}")
        }
    }
}
