package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BondType
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentMethod
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val userCode: String,
    val name: String,
    val role: UserRole,
    val username: String,
    val pin: String,
    val active: Boolean,
    val assignedBranch: String,
    val permissions: UserPermissions
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val cCode: String,
    val name: String,
    val mobile: String,
    val balance: Double,
    val address: String,
    val createdAt: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val barcode: String,
    val unitName: String,
    val price: Double,
    val caseUnitName: String,
    val caseQuantity: Double,
    val casePrice: Double,
    val stockMain: Double,
    val stockCashier: Map<String, Double>,
    val category: String
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val customerMobile: String,
    val cashierId: String,
    val cashierCode: String,
    val cashierName: String,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val paidAmount: Double,
    val remainingAmount: Double,
    val prevCustomerBalance: Double,
    val newCustomerBalance: Double,
    val date: String,
    val notes: String
)

@Entity(tableName = "bonds")
data class BondEntity(
    @PrimaryKey val id: String,
    val bondNumber: String,
    val type: BondType,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val customerMobile: String,
    val cashierId: String,
    val cashierCode: String,
    val cashierName: String,
    val amount: Double,
    val note: String,
    val prevCustomerBalance: Double,
    val newCustomerBalance: Double,
    val date: String
)

@Entity(tableName = "stock_transfers")
data class StockTransferEntity(
    @PrimaryKey val id: String,
    val transferNumber: String,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val quantity: Double,
    val fromLocation: String,
    val toCashierId: String,
    val toCashierName: String,
    val toCashierCode: String,
    val adminId: String,
    val adminName: String,
    val date: String,
    val note: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String,
    val tagline: String,
    val address: String,
    val phone: String,
    val currency: String,
    val currencySymbol: String,
    val logoUrl: String,
    val taxNumber: String,
    val whatsappMode: String,
    val autoPrintAfterInvoice: Boolean
)
