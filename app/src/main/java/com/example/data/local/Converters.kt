package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BondType
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentMethod
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Invoice Items
    private val invoiceItemListType = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
    private val invoiceItemAdapter = moshi.adapter<List<InvoiceItem>>(invoiceItemListType)

    @TypeConverter
    fun fromInvoiceItemList(value: List<InvoiceItem>?): String {
        return invoiceItemAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toInvoiceItemList(value: String?): List<InvoiceItem> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            invoiceItemAdapter.fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Stock Cashier Map (CashierId -> Double)
    private val stockCashierMapType = Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
    private val stockCashierAdapter = moshi.adapter<Map<String, Double>>(stockCashierMapType)

    @TypeConverter
    fun fromStockCashierMap(value: Map<String, Double>?): String {
        return stockCashierAdapter.toJson(value ?: emptyMap())
    }

    @TypeConverter
    fun toStockCashierMap(value: String?): Map<String, Double> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            stockCashierAdapter.fromJson(value) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // UserPermissions
    private val permissionsAdapter = moshi.adapter(UserPermissions::class.java)

    @TypeConverter
    fun fromPermissions(value: UserPermissions?): String {
        return permissionsAdapter.toJson(value ?: UserPermissions())
    }

    @TypeConverter
    fun toPermissions(value: String?): UserPermissions {
        if (value.isNullOrBlank()) return UserPermissions()
        return try {
            permissionsAdapter.fromJson(value) ?: UserPermissions()
        } catch (_: Exception) {
            UserPermissions()
        }
    }

    // Enums
    @TypeConverter
    fun fromUserRole(value: UserRole?): String = value?.name ?: UserRole.CASHIER.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole {
        return try {
            UserRole.valueOf(value ?: UserRole.CASHIER.name)
        } catch (_: Exception) {
            UserRole.CASHIER
        }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String = value?.name ?: PaymentMethod.CASH.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod {
        return try {
            PaymentMethod.valueOf(value ?: PaymentMethod.CASH.name)
        } catch (_: Exception) {
            PaymentMethod.CASH
        }
    }

    @TypeConverter
    fun fromBondType(value: BondType?): String = value?.name ?: BondType.RECEIPT.name

    @TypeConverter
    fun toBondType(value: String?): BondType {
        return try {
            BondType.valueOf(value ?: BondType.RECEIPT.name)
        } catch (_: Exception) {
            BondType.RECEIPT
        }
    }
}
