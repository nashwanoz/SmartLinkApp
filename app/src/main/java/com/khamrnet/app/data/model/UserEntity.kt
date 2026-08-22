package com.khamrnet.app.data.model

data class UserPermissions(
    val canAccessPos: Boolean = true,
    val canSellNegativeStock: Boolean = false,
    val canAccessProducts: Boolean = true,
    val canAccessCustomers: Boolean = true,
    val canSetOpeningBalance: Boolean = false,
    val canAccessInvoices: Boolean = true,
    val canViewAllInvoices: Boolean = false,
    val canAccessBonds: Boolean = true,
    val canViewAllBonds: Boolean = false,
    val canAccessTransfers: Boolean = false,
    val canAccessSettlements: Boolean = false,
    val canAccessExpenses: Boolean = true,
    val canAccessCardGeneration: Boolean = false,
    val canAccessLedger: Boolean = false,
    val canAccessPrinterSettings: Boolean = false,
    val canAccessUsers: Boolean = false,
    val canAccessSettings: Boolean = false
)

data class CashDrawer(
    val id: String,
    val code: String,
    val name: String,
    val isMain: Boolean = false,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Warehouse(
    val id: String,
    val code: String,
    val name: String,
    val isMain: Boolean = false,
    val location: String? = null,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppUser(
    val id: String,
    val userCode: String,
    val name: String,
    val role: String = "CASHIER", // "ADMIN" or "CASHIER"
    val username: String,
    val pin: String = "1234",
    val active: Boolean = true,
    val assignedBranch: String = "الفرع الرئيسي",
    val assignedDrawerCode: String = "CASH-101",
    val assignedDrawerName: String = "صندوق كاشير",
    val assignedWarehouseCode: String = "WH-01",
    val assignedWarehouseName: String = "المستودع العام الرئيسي",
    val permissions: UserPermissions = UserPermissions()
)

data class UserEntity(
    val id: Long = 1L,
    val username: String = "admin",
    val userCode: String = "101",
    val displayName: String = "المدير",
    val role: String = "ADMIN",
    val password: String = "1234"
)

object UserDefaults {
    val DEFAULT_ADMIN_PERMISSIONS = UserPermissions(
        canAccessPos = true,
        canSellNegativeStock = true,
        canAccessProducts = true,
        canAccessCustomers = true,
        canSetOpeningBalance = true,
        canAccessInvoices = true,
        canViewAllInvoices = true,
        canAccessBonds = true,
        canViewAllBonds = true,
        canAccessTransfers = true,
        canAccessSettlements = true,
        canAccessExpenses = true,
        canAccessCardGeneration = true,
        canAccessLedger = true,
        canAccessPrinterSettings = true,
        canAccessUsers = true,
        canAccessSettings = true
    )

    val DEFAULT_CASHIER_PERMISSIONS = UserPermissions(
        canAccessPos = true,
        canSellNegativeStock = false,
        canAccessProducts = true,
        canAccessCustomers = true,
        canSetOpeningBalance = false,
        canAccessInvoices = true,
        canViewAllInvoices = false,
        canAccessBonds = true,
        canViewAllBonds = false,
        canAccessTransfers = false,
        canAccessSettlements = false,
        canAccessExpenses = true,
        canAccessCardGeneration = false,
        canAccessLedger = false,
        canAccessPrinterSettings = false,
        canAccessUsers = false,
        canAccessSettings = false
    )

    val DEFAULT_CASH_DRAWERS = listOf(
        CashDrawer(
            id = "drawer_main",
            code = "BOX-001",
            name = "الصندوق الرئيسي (الخزينة العامة)",
            isMain = true,
            assignedUserId = "user_101",
            assignedUserName = "المدير العام (نشوان)",
            notes = "الخزينة المركزية للإدارة"
        ),
        CashDrawer(
            id = "drawer_cashier_102",
            code = "CASH-102",
            name = "صندوق كاشير 1 (نقطة السوق)",
            isMain = false,
            assignedUserId = "user_102",
            assignedUserName = "كاشير 1 - نقطة السوق",
            notes = "عهدة نقدية يومية"
        ),
        CashDrawer(
            id = "drawer_cashier_103",
            code = "CASH-103",
            name = "صندوق كاشير 2 (النقطة الشرقية)",
            isMain = false,
            assignedUserId = "user_103",
            assignedUserName = "كاشير 2 - النقطة الشرقية",
            notes = "عهدة نقدية يومية"
        )
    )

    val DEFAULT_WAREHOUSES = listOf(
        Warehouse(
            id = "wh_main",
            code = "WH-01",
            name = "المستودع العام الرئيسي",
            isMain = true,
            location = "المقر الرئيسي - الإدارة",
            assignedUserId = "user_101",
            assignedUserName = "المدير العام (نشوان)",
            notes = "المخزن المركزي لجميع البضائع والكروت"
        ),
        Warehouse(
            id = "wh_pos_1",
            code = "WH-02",
            name = "مخزن نقطة بيع السوق",
            isMain = false,
            location = "فرع السوق العام",
            assignedUserId = "user_102",
            assignedUserName = "كاشير 1 - نقطة السوق",
            notes = "مخزن فرعي لنقطة البيع"
        )
    )

    val DEFAULT_USERS = listOf(
        AppUser(
            id = "user_101",
            userCode = "101",
            name = "المدير العام (نشوان)",
            role = "ADMIN",
            username = "admin",
            pin = "101",
            active = true,
            assignedDrawerCode = "BOX-001",
            assignedDrawerName = "الصندوق الرئيسي (الخزينة العامة)",
            assignedWarehouseCode = "WH-01",
            assignedWarehouseName = "المستودع العام الرئيسي",
            permissions = DEFAULT_ADMIN_PERMISSIONS
        ),
        AppUser(
            id = "user_102",
            userCode = "102",
            name = "كاشير 1 - نقطة السوق",
            role = "CASHIER",
            username = "cashier1",
            pin = "102",
            active = true,
            assignedDrawerCode = "CASH-102",
            assignedDrawerName = "صندوق كاشير 1 (نقطة السوق)",
            assignedWarehouseCode = "WH-02",
            assignedWarehouseName = "مخزن نقطة بيع السوق",
            permissions = DEFAULT_CASHIER_PERMISSIONS
        ),
        AppUser(
            id = "user_103",
            userCode = "103",
            name = "كاشير 2 - النقطة الشرقية",
            role = "CASHIER",
            username = "cashier2",
            pin = "103",
            active = true,
            assignedDrawerCode = "CASH-103",
            assignedDrawerName = "صندوق كاشير 2 (النقطة الشرقية)",
            assignedWarehouseCode = "WH-01",
            assignedWarehouseName = "المستودع العام الرئيسي",
            permissions = DEFAULT_CASHIER_PERMISSIONS
        )
    )
}

