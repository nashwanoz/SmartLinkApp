package com.smartlink.erp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.smartlink.erp.data.local.entity.Bond
import com.smartlink.erp.data.local.entity.Customer
import com.smartlink.erp.data.local.entity.Invoice
import com.smartlink.erp.data.local.entity.Product
import com.smartlink.erp.data.local.entity.StockTransfer
import com.smartlink.erp.data.local.entity.SystemSettings
import com.smartlink.erp.data.local.entity.User
import com.smartlink.erp.data.local.entity.UserPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Default System Settings
 */
val DEFAULT_SETTINGS = SystemSettings(
    businessName = "شبكة خمر نت اللاسلكية",
    tagline = "خدمات الشبكات والأنظمة ونقاط البيع",
    address = "خمر - السوق العام",
    phone = "783888185",
    currency = "ريال يمني",
    currencySymbol = "YER",
    logoUrl = "",
    whatsappMode = "text",
    autoPrintAfterInvoice = false
)

/**
 * Default Users (Admin + Cashiers)
 */
val DEFAULT_USERS = listOf(
    User(
        id = "user_101",
        userCode = "101",
        name = "المدير ا
