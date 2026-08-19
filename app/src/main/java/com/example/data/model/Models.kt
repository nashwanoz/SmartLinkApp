package com.example.data.model

enum class UserRole {
    ADMIN,
    CASHIER
}

data class UserPermissions(
    val canAccessPos: Boolean = true,
    val canSellNegativeStock: Boolean = false,
    val canAccessProducts: Boolean = true,
    val canAccessCustomers: Boolean = true,
    val canSetOpeningBalance: Boolean = false,
    val canAccessStatements: Boolean = false,
    val canAccessInvoices: Boolean = true,
    val canAccessBonds: Boolean = true,
    val canAccessTransfers: Boolean = false,
    val canAccessUsers: Boolean = false,
    val canAccessSettings: Boolean = false
)

data class User(
    val id: String,
    val userCode: String,
    val name: String,
    val role: UserRole,
    val username: String,
    val pin: String = "101",
    val active: Boolean = true,
    val assignedBranch: String = "",
    val permissions: UserPermissions = UserPermissions()
)

data class Customer(
    val id: String,
    val cCode: String,
    val name: String,
    val mobile: String = "",
    val balance: Double = 0.0, // Positive: Customer owes (عليه), Negative: Customer credit (له)
    val address: String = "",
    val createdAt: String = ""
)

data class Product(
    val id: Long,
    val name: String,
    val barcode: String,
    val unitName: String = "حبة",
    val price: Double = 0.0,
    val caseUnitName: String = "كرتون",
    val caseQuantity: Double = 1.0,
    val casePrice: Double = 0.0,
    val stockMain: Double = 0.0,
    val stockCashier: Map<String, Double> = emptyMap(),
    val category: String = "عام"
)

data class InvoiceItem(
    val productId: Long,
    val productName: String,
    val unitType: String, // "minor" | "major"
    val unitName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val convertedMinorQty: Double
)

enum class PaymentMethod {
    CASH,
    CREDIT,
    PARTIAL
}

data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val customerMobile: String = "",
    val cashierId: String,
    val cashierCode: String,
    val cashierName: String,
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val prevCustomerBalance: Double = 0.0,
    val newCustomerBalance: Double = 0.0,
    val date: String,
    val notes: String = ""
)

enum class BondType {
    RECEIPT, // سند قبض (Cash In - decreases customer debt)
    PAYMENT  // سند صرف (Cash Out - increases customer debt)
}

data class Bond(
    val id: String,
    val bondNumber: String,
    val type: BondType,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val customerMobile: String = "",
    val cashierId: String,
    val cashierCode: String,
    val cashierName: String,
    val amount: Double,
    val note: String = "",
    val prevCustomerBalance: Double = 0.0,
    val newCustomerBalance: Double = 0.0,
    val date: String
)

data class StockTransfer(
    val id: String,
    val transferNumber: String,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val quantity: Double,
    val fromLocation: String = "المخزن الرئيسي",
    val toCashierId: String,
    val toCashierName: String,
    val toCashierCode: String,
    val adminId: String,
    val adminName: String,
    val date: String,
    val note: String = ""
)

data class SystemSettings(
    val businessName: String = "شبكة خمر نت اللاسلكية",
    val tagline: String = "خدمات الشبكات والأنظمة ونقاط البيع",
    val address: String = "خمر - السوق العام",
    val phone: String = "783888185",
    val currency: String = "ريال يمني",
    val currencySymbol: String = "YER",
    val logoUrl: String = "",
    val taxNumber: String = "",
    val whatsappMode: String = "text", // "text" or "receipt"
    val autoPrintAfterInvoice: Boolean = false
)

data class BackupPayload(
    val settings: SystemSettings? = null,
    val users: List<User> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val products: List<Product> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val transfers: List<StockTransfer> = emptyList(),
    val exportDate: String = ""
)
