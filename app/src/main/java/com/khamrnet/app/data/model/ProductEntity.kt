package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Product Unit (e.g., Piece, Box, Carton) with its conversion factor and custom prices
 */
data class ProductUnit(
    val id: String = "",
    val name: String = "حبة",
    val factor: Double = 1.0, // Conversion to base unit (Base unit factor = 1.0)
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val barcode: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val name: String,
    val barcode: String = "",
    val category: String = "عام",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockLimit: Double = 0.0,
    val baseUnitName: String = "حبة",
    val unitsJson: String = "[]", // Serialized List<ProductUnit>
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val price: Double get() = salePrice
    val costPrice: Double get() = purchasePrice
    val subUnitsJson: String get() = unitsJson
}
