package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonds")
data class BondEntity(
    @PrimaryKey
    val id: String,
    val bondNumber: String,
    val type: String, // "RECEIPT" (قبض), "PAYMENT" (صرف)
    val date: Long = System.currentTimeMillis(),
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val previousBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val note: String = "",
    val createdBy: String = "المدير",
    val createdAt: Long = System.currentTimeMillis()
)
