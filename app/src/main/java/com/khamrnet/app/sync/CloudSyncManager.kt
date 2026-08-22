package com.khamrnet.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.khamrnet.app.data.database.AppDatabase
import com.khamrnet.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
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

    // Firestore REST API endpoints for direct cloud synchronization
    private val baseUrls = listOf(
        "https://firestore.googleapis.com/v1/projects/smart-erp-link/databases/(default)/documents",
        "https://firestore.googleapis.com/v1/projects/gen-lang-client-0683616902/databases/ai-studio-8090c1e0-0eae-440d-b8b2-ecd27caab93c/documents",
        "https://firestore.googleapis.com/v1/projects/gen-lang-client-0683616902/databases/(default)/documents"
    )

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
                    message = "لا يوجد اتصال بالإنترنت (يعمل محلياً)"
                )
                return@withContext Result.failure(Exception("أنت الآن أوفلاين. تم حفظ البيانات محلياً وسيتم تحديثها عند توفر الإنترنت."))
            }

            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCING,
                isOnline = true,
                message = "جاري جلب ومزامنة البيانات مع السحابة لكود المحل $cleanStoreCode..."
            )

            val productDao = database.productDao()
            val customerDao = database.customerDao()
            val invoiceDao = database.invoiceDao()
            val bondDao = database.bondDao()
            val settingsDao = database.systemSettingsDao()

            var totalPulledItems = 0

            // -------------------------------------------------------------
            // 1. PULL / DOWNLOAD FROM FIRESTORE TO LOCAL ROOM DATABASE
            // -------------------------------------------------------------

            // 1.1 Download Products
            val remoteProducts = fetchCollectionFromFirestore("stores/$cleanStoreCode/products")
            if (remoteProducts.isNotEmpty()) {
                val parsedProducts = remoteProducts.map { (docId, fields) ->
                    val id = getString(fields, "id", docId)
                    ProductEntity(
                        id = id,
                        code = getString(fields, "code", getString(fields, "barcode", id)),
                        name = getString(fields, "name", getString(fields, "productName", "صنف")),
                        barcode = getString(fields, "barcode", ""),
                        category = getString(fields, "category", "عام"),
                        purchasePrice = getDouble(fields, "purchasePrice", getDouble(fields, "costPrice", 0.0)),
                        salePrice = getDouble(fields, "salePrice", getDouble(fields, "price", 0.0)),
                        wholesalePrice = getDouble(fields, "wholesalePrice", getDouble(fields, "casePrice", 0.0)),
                        stockQuantity = getDouble(fields, "stockQuantity", getDouble(fields, "stockMain", 0.0)),
                        minStockLimit = getDouble(fields, "minStockLimit", 5.0),
                        baseUnitName = getString(fields, "baseUnitName", getString(fields, "unitName", "حبة")),
                        unitsJson = getString(fields, "unitsJson", getString(fields, "subUnitsJson", "[]")),
                        isActive = getBoolean(fields, "isActive", true),
                        createdAt = getLong(fields, "createdAt", System.currentTimeMillis()),
                        updatedAt = getLong(fields, "updatedAt", System.currentTimeMillis())
                    )
                }
                productDao.insertProducts(parsedProducts)
                totalPulledItems += parsedProducts.size
            }

            // 1.2 Download Customers
            val remoteCustomers = fetchCollectionFromFirestore("stores/$cleanStoreCode/customers")
            if (remoteCustomers.isNotEmpty()) {
                val parsedCustomers = remoteCustomers.map { (docId, fields) ->
                    val id = getString(fields, "id", docId)
                    CustomerEntity(
                        id = id,
                        code = getString(fields, "code", getString(fields, "cCode", "1001")),
                        name = getString(fields, "name", "عميل"),
                        phone = getString(fields, "phone", getString(fields, "mobile", "")),
                        mobile = getString(fields, "mobile", getString(fields, "phone", "")),
                        address = getString(fields, "address", ""),
                        initialBalance = getDouble(fields, "initialBalance", 0.0),
                        currentBalance = getDouble(fields, "currentBalance", getDouble(fields, "balance", 0.0)),
                        balance = getDouble(fields, "balance", getDouble(fields, "currentBalance", 0.0)),
                        creditLimit = getDouble(fields, "creditLimit", 0.0),
                        notes = getString(fields, "notes", ""),
                        isActive = getBoolean(fields, "isActive", true),
                        createdAt = getLong(fields, "createdAt", System.currentTimeMillis()),
                        updatedAt = getLong(fields, "updatedAt", System.currentTimeMillis())
                    )
                }
                customerDao.insertCustomers(parsedCustomers)
                totalPulledItems += parsedCustomers.size
            }

            // 1.3 Download Invoices
            val remoteInvoices = fetchCollectionFromFirestore("stores/$cleanStoreCode/invoices")
            if (remoteInvoices.isNotEmpty()) {
                for ((docId, fields) in remoteInvoices) {
                    val id = getString(fields, "id", docId)
                    val invoice = InvoiceEntity(
                        id = id,
                        invoiceNumber = getString(fields, "invoiceNumber", getString(fields, "billNo", id)),
                        billNo = getString(fields, "billNo", getString(fields, "invoiceNumber", id)),
                        billType = getLong(fields, "billType", 1L).toInt(),
                        paymentMethod = getString(fields, "paymentMethod", "CASH"),
                        customerId = getString(fields, "customerId", ""),
                        customerCode = getString(fields, "customerCode", ""),
                        customerName = getString(fields, "customerName", "عميل نقدي"),
                        date = getLong(fields, "date", System.currentTimeMillis()),
                        subtotal = getDouble(fields, "subtotal", getDouble(fields, "total", 0.0)),
                        discount = getDouble(fields, "discount", 0.0),
                        total = getDouble(fields, "total", 0.0),
                        paidAmount = getDouble(fields, "paidAmount", 0.0),
                        remainingAmount = getDouble(fields, "remainingAmount", 0.0),
                        previousCustomerBalance = getDouble(fields, "previousCustomerBalance", 0.0),
                        newCustomerBalance = getDouble(fields, "newCustomerBalance", getDouble(fields, "newBalance", 0.0)),
                        newBalance = getDouble(fields, "newBalance", getDouble(fields, "newCustomerBalance", 0.0)),
                        itemsJson = getString(fields, "itemsJson", "[]"),
                        notes = getString(fields, "notes", ""),
                        createdBy = getString(fields, "createdBy", "المدير"),
                        createdAt = getLong(fields, "createdAt", System.currentTimeMillis()),
                        isCancelled = getBoolean(fields, "isCancelled", false)
                    )
                    invoiceDao.insertInvoice(invoice)
                    totalPulledItems++
                }
            }

            // 1.4 Download Bonds
            val remoteBonds = fetchCollectionFromFirestore("stores/$cleanStoreCode/bonds")
            if (remoteBonds.isNotEmpty()) {
                for ((docId, fields) in remoteBonds) {
                    val id = getString(fields, "id", docId)
                    val bond = BondEntity(
                        id = id,
                        bondNumber = getString(fields, "bondNumber", id),
                        type = getString(fields, "type", getString(fields, "bondType", "RECEIPT")),
                        bondType = getString(fields, "bondType", getString(fields, "type", "RECEIPT")),
                        date = getLong(fields, "date", System.currentTimeMillis()),
                        customerId = getString(fields, "customerId", ""),
                        customerCode = getString(fields, "customerCode", ""),
                        customerName = getString(fields, "customerName", getString(fields, "partyName", "")),
                        partyName = getString(fields, "partyName", getString(fields, "customerName", "")),
                        partyType = getString(fields, "partyType", "CUSTOMER"),
                        amount = getDouble(fields, "amount", 0.0),
                        paymentMethod = getString(fields, "paymentMethod", "CASH"),
                        previousBalance = getDouble(fields, "previousBalance", 0.0),
                        currentBalance = getDouble(fields, "currentBalance", 0.0),
                        note = getString(fields, "note", getString(fields, "notes", "")),
                        notes = getString(fields, "notes", getString(fields, "note", "")),
                        createdBy = getString(fields, "createdBy", "المدير"),
                        createdAt = getLong(fields, "createdAt", System.currentTimeMillis())
                    )
                    bondDao.insertBond(bond)
                    totalPulledItems++
                }
            }

            // 1.5 Download Settings
            val remoteSettings = fetchDocumentFromFirestore("stores/$cleanStoreCode/settings/main")
            val currentSettings = settingsDao.getSettings() ?: SystemSettingsEntity()
            if (remoteSettings != null) {
                val updatedSettings = currentSettings.copy(
                    storeCode = cleanStoreCode,
                    businessName = getString(remoteSettings, "businessName", currentSettings.businessName),
                    phone = getString(remoteSettings, "phone", currentSettings.phone),
                    address = getString(remoteSettings, "address", currentSettings.address),
                    currencyName = getString(remoteSettings, "currencyName", currentSettings.currencyName),
                    footerText = getString(remoteSettings, "footerText", getString(remoteSettings, "invoiceFooterMessage", currentSettings.footerText)),
                    invoiceFooterMessage = getString(remoteSettings, "invoiceFooterMessage", getString(remoteSettings, "footerText", currentSettings.invoiceFooterMessage)),
                    lastSyncTimestamp = System.currentTimeMillis(),
                    syncStatusMessage = "تم جلب البيانات وتحديثها بنجاح"
                )
                settingsDao.insertOrUpdate(updatedSettings)
            }

            // -------------------------------------------------------------
            // 2. PUSH / UPLOAD LOCAL ENTITIES TO CLOUD (Two-Way Sync)
            // -------------------------------------------------------------

            // 2.1 Upload Local Products
            val localProducts = productDao.getAllProducts()
            for (prod in localProducts) {
                uploadDocToFirestore("stores/$cleanStoreCode/products", prod.id, mapOf(
                    "id" to prod.id,
                    "code" to prod.code,
                    "name" to prod.name,
                    "barcode" to prod.barcode,
                    "price" to prod.salePrice,
                    "salePrice" to prod.salePrice,
                    "costPrice" to prod.purchasePrice,
                    "purchasePrice" to prod.purchasePrice,
                    "stockQuantity" to prod.stockQuantity,
                    "stockMain" to prod.stockQuantity,
                    "baseUnitName" to prod.baseUnitName,
                    "unitName" to prod.baseUnitName,
                    "unitsJson" to prod.unitsJson,
                    "updatedAt" to prod.updatedAt
                ))
            }

            // 2.2 Upload Local Customers
            val localCustomers = customerDao.getAllCustomers()
            for (cust in localCustomers) {
                uploadDocToFirestore("stores/$cleanStoreCode/customers", cust.id, mapOf(
                    "id" to cust.id,
                    "code" to cust.code,
                    "cCode" to cust.code,
                    "name" to cust.name,
                    "phone" to cust.phone,
                    "mobile" to cust.phone,
                    "address" to cust.address,
                    "currentBalance" to cust.currentBalance,
                    "balance" to cust.currentBalance,
                    "initialBalance" to cust.initialBalance,
                    "updatedAt" to cust.updatedAt
                ))
            }

            // 2.3 Upload Local Invoices
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

            // 2.4 Upload Local Bonds
            val localBonds = bondDao.getAllBonds()
            for (bnd in localBonds) {
                val party = bnd.partyName.ifEmpty { bnd.customerName }
                val bType = bnd.type.ifEmpty { bnd.bondType }
                val noteText = bnd.note.ifEmpty { bnd.notes }
                uploadDocToFirestore("stores/$cleanStoreCode/bonds", bnd.id, mapOf(
                    "id" to bnd.id,
                    "bondNumber" to bnd.bondNumber,
                    "bondType" to bType,
                    "type" to bType,
                    "partyName" to party,
                    "customerName" to party,
                    "amount" to bnd.amount,
                    "date" to bnd.date,
                    "notes" to noteText,
                    "note" to noteText
                ))
            }

            val now = System.currentTimeMillis()
            val finalSettings = settingsDao.getSettings() ?: SystemSettingsEntity()
            val syncSuccessMessage = if (totalPulledItems > 0) {
                "تم جلب ومزامنة $totalPulledItems عنصراً بنجاح من السحابة لكود ($cleanStoreCode)"
            } else {
                "تمت المزامنة السحابية بنجاح لكود المحل ($cleanStoreCode)"
            }

            settingsDao.insertOrUpdate(finalSettings.copy(
                storeCode = cleanStoreCode,
                lastSyncTimestamp = now,
                syncStatusMessage = syncSuccessMessage
            ))

            _syncStatus.value = SyncStatus(
                state = SyncState.SUCCESS,
                isOnline = true,
                lastSyncTime = now,
                pendingItemsCount = 0,
                message = syncSuccessMessage
            )

            Result.success(syncSuccessMessage)
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

    private fun fetchCollectionFromFirestore(collectionPath: String): List<Pair<String, JsonObject>> {
        val result = mutableListOf<Pair<String, JsonObject>>()
        for (baseUrl in baseUrls) {
            try {
                val urlString = "$baseUrl/$collectionPath"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 7000
                conn.readTimeout = 7000

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JsonParser.parseString(responseText).asJsonObject
                    if (json.has("documents")) {
                        val docsArray = json.getAsJsonArray("documents")
                        for (i in 0 until docsArray.size()) {
                            val docObj = docsArray[i].asJsonObject
                            val name = docObj.get("name")?.asString ?: ""
                            val docId = name.substringAfterLast("/")
                            val fields = if (docObj.has("fields")) docObj.getAsJsonObject("fields") else JsonObject()
                            result.add(Pair(docId, fields))
                        }
                    }
                    conn.disconnect()
                    if (result.isNotEmpty()) break
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w("CloudSyncManager", "Fetch error for $collectionPath on $baseUrl: ${e.message}")
            }
        }
        return result
    }

    private fun fetchDocumentFromFirestore(documentPath: String): JsonObject? {
        for (baseUrl in baseUrls) {
            try {
                val urlString = "$baseUrl/$documentPath"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 7000
                conn.readTimeout = 7000

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JsonParser.parseString(responseText).asJsonObject
                    conn.disconnect()
                    return if (json.has("fields")) json.getAsJsonObject("fields") else null
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w("CloudSyncManager", "Doc fetch error for $documentPath on $baseUrl: ${e.message}")
            }
        }
        return null
    }

    private fun uploadDocToFirestore(collectionPath: String, docId: String, data: Map<String, Any>) {
        for (baseUrl in baseUrls) {
            try {
                val urlString = "$baseUrl/$collectionPath/$docId"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 7000
                conn.readTimeout = 7000

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
                if (code in 200..299) break
            } catch (e: Exception) {
                Log.w("CloudSyncManager", "Doc sync warning for $docId on $baseUrl: ${e.message}")
            }
        }
    }

    private fun getString(fields: JsonObject?, key: String, default: String = ""): String {
        if (fields == null || !fields.has(key)) return default
        val obj = fields.getAsJsonObject(key) ?: return default
        if (obj.has("stringValue")) return obj.get("stringValue").asString
        if (obj.has("integerValue")) return obj.get("integerValue").asString
        if (obj.has("doubleValue")) return obj.get("doubleValue").asString
        if (obj.has("booleanValue")) return obj.get("booleanValue").asString
        return default
    }

    private fun getDouble(fields: JsonObject?, key: String, default: Double = 0.0): Double {
        if (fields == null || !fields.has(key)) return default
        val obj = fields.getAsJsonObject(key) ?: return default
        if (obj.has("doubleValue")) return obj.get("doubleValue").asDouble
        if (obj.has("integerValue")) return obj.get("integerValue").asString.toDoubleOrNull() ?: default
        if (obj.has("stringValue")) return obj.get("stringValue").asString.toDoubleOrNull() ?: default
        return default
    }

    private fun getLong(fields: JsonObject?, key: String, default: Long = 0L): Long {
        if (fields == null || !fields.has(key)) return default
        val obj = fields.getAsJsonObject(key) ?: return default
        if (obj.has("integerValue")) return obj.get("integerValue").asString.toLongOrNull() ?: default
        if (obj.has("doubleValue")) return obj.get("doubleValue").asDouble.toLong()
        if (obj.has("stringValue")) return obj.get("stringValue").asString.toLongOrNull() ?: default
        return default
    }

    private fun getBoolean(fields: JsonObject?, key: String, default: Boolean = true): Boolean {
        if (fields == null || !fields.has(key)) return default
        val obj = fields.getAsJsonObject(key) ?: return default
        if (obj.has("booleanValue")) return obj.get("booleanValue").asBoolean
        if (obj.has("stringValue")) return obj.get("stringValue").asString.toBoolean()
        return default
    }
}
