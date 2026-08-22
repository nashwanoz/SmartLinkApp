package com.khamrnet.app.ui

import com.khamrnet.app.data.model.*

data class SaleReceipt(
    val invoice: InvoiceEntity,
    val customer: CustomerEntity?
)

data class BondReceipt(
    val bond: BondEntity,
    val customer: CustomerEntity
)

data class AppStats(
    val todaySales: Double = 0.0,
    val carriedDifference: Double = 0.0
)

data class AppUiState(
    val ready: Boolean = false,
    val user: UserEntity? = null,
    val message: String? = null,
    val error: String? = null,
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val bonds: List<BondEntity> = emptyList(),
    val cashBoxes: List<CashBoxEntity> = emptyList(),
    val stock: Map<Long, Int> = emptyMap(),
    val cashBalances: Map<Long, Double> = emptyMap(),
    val customerLastMovement: Map<Long, Long> = emptyMap(),
    val stats: AppStats = AppStats(),
    val saleReceipt: SaleReceipt? = null,
    val bondReceipt: BondReceipt? = null
)
