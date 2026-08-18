package com.smartlink.erp.utils

import com.smartlink.erp.data.local.entity.User

/**
 * User Permissions Data Class
 */
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

/**
 * Default Admin Permissions (Full Access)
 */
val DEFAULT_ADMIN_PERMISSIONS = UserPermissions(
    canAccessPos = true,
    canAccessProducts = true,
    canAccessCustomers = true,
    canSetOpeningBalance = true, // Only Admin
    canAccessStatements = true,
    canAccessInvoices = true,
    canAccessBonds = true,
    canAccessTransfers = true,
    canAccessUsers = true,
    canAccessSettings = true
)

/**
 * Default Cashier Permissions (Limited Access)
 */
val DEFAULT_CASHIER_PERMISSIONS = UserPermissions(
    canAccessPos = true,
    canAccessProducts = true,
    canAccessCustomers = true,
    canSetOpeningBalance = false, // Hidden & forbidden for Cashier
    canAccessStatements = false,
    canAccessInvoices = true,
    canAccessBonds = true,
    canAccessTransfers = false,
    canAccessUsers = false,
    canAccessSettings = false
)

/**
 * Get user permissions based on role
 */
fun getUserPermissions(user: User): UserPermissions {
    return if (user.role == "ADMIN") {
        DEFAULT_ADMIN_PERMISSIONS.copy(
            canAccessPos = user.permissions?.canAccessPos ?: true,
            canAccessProducts = user.permissions?.canAccessProducts ?: true,
            canAccessCustomers = user.permissions?.canAccessCustomers ?: true,
            canSetOpeningBalance = user.permissions?.canSetOpeningBalance ?: true,
            canAccessStatements = user.permissions?.canAccessStatements ?: true,
            canAccessInvoices = user.permissions?.canAccessInvoices ?: true,
            canAccessBonds = user.permissions?.canAccessBonds ?: true,
            canAccessTransfers = user.permissions?.canAccessTransfers ?: true,
            canAccessUsers = user.permissions?.canAccessUsers ?: true,
            canAccessSettings = user.permissions?.canAccessSettings ?: true
        )
    } else {
        DEFAULT_CASHIER_PERMISSIONS.copy(
            canAccessPos = user.permissions?.canAccessPos ?: true,
            canAccessProducts = user.permissions?.canAccessProducts ?: true,
            canAccessCustomers = user.permissions?.canAccessCustomers ?: true,
            canSetOpeningBalance = user.permissions?.canSetOpeningBalance ?: false,
            canAccessStatements = user.permissions?.canAccessStatements ?: false,
            canAccessInvoices = user.permissions?.canAccessInvoices ?: true,
            canAccessBonds = user.permissions?.canAccessBonds ?: true,
            canAccessTransfers = user.permissions?.canAccessTransfers ?: false,
            canAccessUsers = user.permissions?.canAccessUsers ?: false,
            canAccessSettings = user.permissions?.canAccessSettings ?: false
        )
    }
}

/**
 * Check if user has specific permission
 */
fun hasPermission(user: User, key: (UserPermissions) -> Boolean): Boolean {
    if (user.role == "ADMIN") return true
    
    val perms = getUserPermissions(user)
    return key(perms)
}

/**
 * Extension functions for permission checks
 */
fun User.canAccessPos(): Boolean = hasPermission(this) { it.canAccessPos }
fun User.canAccessProducts(): Boolean = hasPermission(this) { it.canAccessProducts }
fun User.canAccessCustomers(): Boolean = hasPermission(this) { it.canAccessCustomers }
fun User.canSetOpeningBalance(): Boolean = hasPermission(this) { it.canSetOpeningBalance }
fun User.canAccessStatements(): Boolean = hasPermission(this) { it.canAccessStatements }
fun User.canAccessInvoices(): Boolean = hasPermission(this) { it.canAccessInvoices }
fun User.canAccessBonds(): Boolean = hasPermission(this) { it.canAccessBonds }
fun User.canAccessTransfers(): Boolean = hasPermission(this) { it.canAccessTransfers }
fun User.canAccessUsers(): Boolean = hasPermission(this) { it.canAccessUsers }
fun User.canAccessSettings(): Boolean = hasPermission(this) { it.canAccessSettings }

/**
 * Check if user is admin
 */
fun User.isAdmin(): Boolean = this.role == "ADMIN"

/**
 * Check if user is cashier
 */
fun User.isCashier(): Boolean = this.role == "CASHIER"
