package com.khamrnet.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val userCode: String,
    val password: String,
    val displayName: String,
    val role: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val unitName: String = "حبة",
    val price: Double = 0.0,
    val caseUnitName: String = "كرت",
    val caseQuantity: Int = 60,
    val casePrice: Double = 0.0
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String = "",
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val userId: Long,
    val customerId: Long?,
    val productId: Long,
    val unitName: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val paymentType: String,
    val previousBalance: Double,
    val newBalance: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_bonds")
data class FinancialBondEntity(
    @PrimaryKey val id: String,
    val userId: Long,
    val customerId: Long,
    val type: String,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_boxes")
data class CashBoxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long? = null,
    val name: String,
    val balance: Double = 0.0
)

@Entity(tableName = "stock_balances")
data class StockBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val productId: Long,
    val quantity: Int = 0
)

data class CustomerStatementRow(
    val date: Long,
    val type: String,
    val amount: Double,
    val balanceAfter: Double,
    val note: String = ""
)

data class SaleResult(
    val success: Boolean,
    val message: String,
    val invoice: InvoiceEntity
)

data class BondResult(
    val bond: FinancialBondEntity,
    val customer: CustomerEntity
)
