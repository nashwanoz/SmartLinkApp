package com.khamrnet.app.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.khamrnet.app.data.model.InvoiceEntity
import com.khamrnet.app.data.model.InvoiceItem
import com.khamrnet.app.data.model.SystemSettingsEntity
import com.khamrnet.app.util.ArabicNumberConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-Speed Direct Bluetooth Thermal Printer Controller (ESC/POS)
 * Sends raw bytes directly to the thermal printer in under 1 second.
 */
class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        // Standard ESC/POS Commands
        val CMD_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
        val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val CMD_FEED_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Cut paper
        val CMD_FEED_3_LINES = byteArrayOf(0x1B, 0x64, 0x03)
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val paired = bluetoothAdapter?.bondedDevices ?: emptySet()
        return paired.filter { device ->
            val name = device.name ?: ""
            name.contains("POS", ignoreCase = true) ||
            name.contains("RP", ignoreCase = true) ||
            name.contains("Printer", ignoreCase = true) ||
            name.contains("MPT", ignoreCase = true) ||
            name.contains("XP", ignoreCase = true) ||
            name.contains("ZJ", ignoreCase = true) ||
            name.contains("58", ignoreCase = true) ||
            name.contains("80", ignoreCase = true)
        }.ifEmpty { paired.toList() }
    }

    /**
     * Prints invoice silently and instantly to thermal printer via Bluetooth Socket
     */
    @SuppressLint("MissingPermission")
    suspend fun printInvoiceSilent(
        printerMacAddress: String,
        invoice: InvoiceEntity,
        settings: SystemSettingsEntity
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = bluetoothAdapter?.getRemoteDevice(printerMacAddress)
                ?: return@withContext Result.failure(Exception("الطابعة غير موجودة"))

            bluetoothAdapter?.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // Parse items
            val itemType = object : TypeToken<List<InvoiceItem>>() {}.type
            val items: List<InvoiceItem> = try {
                Gson().fromJson(invoice.itemsJson, itemType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            // 1. Initialize
            outputStream.write(CMD_INIT)

            // 2. Header (Center)
            outputStream.write(CMD_ALIGN_CENTER)
            outputStream.write(CMD_BOLD_ON)
            sendArabicText(outputStream, "================================")
            sendArabicText(outputStream, settings.businessName)
            outputStream.write(CMD_BOLD_OFF)
            if (settings.phone.isNotBlank()) sendArabicText(outputStream, "هاتف: ${settings.phone}")
            if (settings.address.isNotBlank()) sendArabicText(outputStream, settings.address)
            sendArabicText(outputStream, "================================")

            // 3. Invoice Info (Right/Center)
            outputStream.write(CMD_ALIGN_RIGHT)
            val dateFormat = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar"))
            val formattedDate = dateFormat.format(Date(invoice.date))
            sendArabicText(outputStream, "رقم الفاتورة: #${invoice.billNo.ifEmpty { invoice.invoiceNumber }}")
            sendArabicText(outputStream, "التاريخ: $formattedDate")
            sendArabicText(outputStream, "العميل: ${invoice.customerName} [${invoice.customerCode}]")
            sendArabicText(outputStream, "--------------------------------")

            // 4. Items Table
            sendArabicText(outputStream, "الصنف              الكمية     السعر     الإجمالي")
            sendArabicText(outputStream, "--------------------------------")
            for (item in items) {
                val line = String.format(
                    Locale.US,
                    "%-15s %4.0f %-3s %6.0f %8.0f",
                    item.productName.take(15),
                    item.quantity,
                    item.unitName.take(3),
                    item.unitPrice,
                    item.total
                )
                sendArabicText(outputStream, line)
            }
            sendArabicText(outputStream, "--------------------------------")

            // 5. Totals & Tafqeet
            outputStream.write(CMD_BOLD_ON)
            sendArabicText(outputStream, "إجمالي الفاتورة: ${invoice.total} ${settings.currencyName}")
            outputStream.write(CMD_BOLD_OFF)
            
            val totalWords = ArabicNumberConverter.convertToArabicWords(invoice.total, settings.currencyName)
            sendArabicText(outputStream, "فقط وقدره: $totalWords لا غير")

            if (invoice.billType == 4 || invoice.paymentMethod == "CREDIT") {
                sendArabicText(outputStream, "المدفوع: ${invoice.paidAmount} ${settings.currencyName}")
                sendArabicText(outputStream, "المتبقي آجل: ${invoice.remainingAmount} ${settings.currencyName}")
                sendArabicText(outputStream, "الرصيد السابق: ${invoice.previousCustomerBalance} ${settings.currencyName}")
                outputStream.write(CMD_BOLD_ON)
                sendArabicText(outputStream, "الرصيد الإجمالي: ${invoice.newCustomerBalance} ${settings.currencyName}")
                outputStream.write(CMD_BOLD_OFF)
                val balWords = ArabicNumberConverter.convertToArabicWords(invoice.newCustomerBalance, settings.currencyName)
                sendArabicText(outputStream, "فقط وقدره: $balWords لا غير")
            }

            // 6. Footer
            sendArabicText(outputStream, "--------------------------------")
            outputStream.write(CMD_ALIGN_CENTER)
            sendArabicText(outputStream, "فاتورة مبيعات إلكترونية معتمدة")
            sendArabicText(outputStream, settings.footerText)
            outputStream.write(CMD_FEED_3_LINES)

            outputStream.flush()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun sendArabicText(outputStream: OutputStream, text: String) {
        try {
            // Encode as CP864 / Windows-1256 / UTF-8
            val bytes = text.toByteArray(charset("windows-1256"))
            outputStream.write(bytes)
            outputStream.write(0x0A) // Line Feed
        } catch (e: Exception) {
            outputStream.write(text.toByteArray())
            outputStream.write(0x0A)
        }
    }
}
