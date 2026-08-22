package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
