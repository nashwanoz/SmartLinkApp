package com.khamrnet.app.data.model

data class CashBoxEntity(
    val id: Long = 1L,
    val name: String = "الصندوق الرئيسي",
    val balance: Double = 0.0,
    val ownerUserId: Long? = null
)

data class StockBalance(
    val productId: Long = 0L,
    val quantity: Int = 0
)

data class SaleResult(
    val message: String = "تمت عملية البيع بنجاح",
    val invoice: InvoiceEntity = InvoiceEntity(id = "", invoiceNumber = "", customerId = "", customerCode = "", customerName = "")
)

data class BondResult(
    val bond: BondEntity = BondEntity(id = "", bondNumber = ""),
    val customer: CustomerEntity = CustomerEntity(id = "", code = "", name = "")
)
