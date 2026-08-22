package com.khamrnet.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single record settings
    val businessName: String = "شبكة خمر اللاسلكيه",
    val phone: String = "783888185",
    val address: String = "خمر - السوق العام",
    val footerText: String = "شكراً لتعاملكم معنا",
    val invoiceFooterMessage: String = "شكراً لتعاملكم معنا",
    val currencyName: String = "YER",
    val thermalPaperWidth: String = "80mm", // "80mm", "58mm", "72mm"
    val thermalPrintScale: Int = 100,
    val thermalFontSize: String = "medium", // "small", "medium", "large"
    val defaultPrinterAddress: String = "",
    val defaultPrinterName: String = "",
    val defaultPrinterMac: String = "",
    val autoPrintOnSave: Boolean = false,
    val silentBluetoothPrint: Boolean = true,
    val storeCode: String = "KHAMR01", // كود المحل / السحابة للمزامنة
    val isCloudSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val syncStatusMessage: String = "جاهز للمزامنة",
    val updatedAt: Long = System.currentTimeMillis()
)
