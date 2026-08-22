package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonds")
data class BondEntity(
    @PrimaryKey
    val id: String,
    val bondNumber: String,
    val type: String = "RECEIPT", // "RECEIPT" (قبض), "PAYMENT" (صرف)
    val bondType: String = "RECEIPT", // For backwards compatibility
    val date: Long = System.currentTimeMillis(),
    val customerId: String = "",
    val customerCode: String = "",
    val customerName: String = "",
    val partyName: String = "",
    val partyType: String = "CUSTOMER",
    val partyId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val previousBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val note: String = "",
    val notes: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)

// Typealias so FinancialBondEntity works everywhere
typealias FinancialBondEntity = BondEntity
