package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

data class CustomerStatementRow(
    val date: String = "",
    val type: String = "",
    val reference: String = "",
    val notes: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val name: String,
    val phone: String = "",
    val mobile: String = "",
    val address: String = "",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
