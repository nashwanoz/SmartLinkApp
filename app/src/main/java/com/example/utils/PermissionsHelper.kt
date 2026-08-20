package com.example.utils

import com.example.data.model.User
import com.example.data.model.UserPermissions
import com.example.data.model.UserRole

object PermissionsHelper {

    val DEFAULT_ADMIN_PERMISSIONS = UserPermissions(
        canAccessPos = true,
        canSellNegativeStock = true,
        canAccessProducts = true,
        canAccessCustomers = true,
        canSetOpeningBalance = true,
        canAccessStatements = true,
        canAccessInvoices = true,
        canAccessBonds = true,
        canAccessTransfers = true,
        canAccessUsers = true,
        canAccessSettings = true
    )

    val DEFAULT_CASHIER_PERMISSIONS = UserPermissions(
        canAccessPos = true,
        canSellNegativeStock = false,
        canAccessProducts = true,
        canAccessCustomers = true,
        canSetOpeningBalance = false,
        canAccessStatements = false,
        canAccessInvoices = true,
        canAccessBonds = true,
        canAccessTransfers = false,
        canAccessUsers = false,
        canAccessSettings = false
    )

    fun hasPermission(user: User?, permissionExtractor: (UserPermissions) -> Boolean): Boolean {
        if (user == null) return false
        if (user.role == UserRole.ADMIN) return true
        return permissionExtractor(user.permissions)
    }
}
