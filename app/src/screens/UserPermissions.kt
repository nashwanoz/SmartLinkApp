package com.smartlink.erp.data.local.entity

data class UserPermissions(
    val canAccessPos: Boolean = false,
    val canAccessProducts: Boolean = false,
    val canAccessCustomers: Boolean = false,
    val canSetOpeningBalance: Boolean = false,
    val canAccessStatements: Boolean = false,
    val canAccessInvoices: Boolean = false,
    val canAccessBonds: Boolean = false,
    val canAccessTransfers: Boolean = false,
    val canAccessUsers: Boolean = false,
    val canAccessSettings: Boolean = false
)
