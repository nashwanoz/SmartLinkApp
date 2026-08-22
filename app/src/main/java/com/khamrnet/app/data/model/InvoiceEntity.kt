package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Invoice Item entity (Lines inside each invoice)
 */
data class InvoiceItem(
    val id: String,
    val productId: String,
    val productCode: String,
    val productName: String,
    val unitId: String = "",
    val unitName: String = "حبة",
    val unitFactor: Double = 1.0,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val billNo: String = "",
    val billType: Int = 1, // 1: Cash, 4: Credit (آجل)
    val paymentMethod: String = "CASH", // "CASH", "CREDIT"
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val previousCustomerBalance: Double = 0.0,
    val newCustomerBalance: Double = 0.0,
    val itemsJson: String = "[]", // Serialized List<InvoiceItem>
    val notes: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis(),
    val isCancelled: Boolean = false
)
