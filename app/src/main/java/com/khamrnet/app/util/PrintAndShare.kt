package com.khamrnet.app.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.khamrnet.app.data.CustomerEntity
import com.khamrnet.app.data.CustomerStatementRow
import com.khamrnet.app.data.FinancialBondEntity
import com.khamrnet.app.data.InvoiceEntity
import java.io.File
import java.net.URLEncoder

object PrintAndShare {
    fun whatsapp(context: Context, customer: CustomerEntity?, invoice: InvoiceEntity) {
        val text = "عميلنا العزيز ${customer?.name ?: "النقدي"}\nعليكم فاتورة بمبلغ: ${"%.2f".format(invoice.total)}\nرصيدكم الإجمالي: ${"%.2f".format(invoice.newBalance)}\nرقم الفاتورة: ${invoice.id}"
        shareText(context, customer?.mobile, text)
    }

    fun whatsappBond(context: Context, customer: CustomerEntity, bond: FinancialBondEntity) {
        val text = "عميلنا العزيز ${customer.name}\nتم تسديد/تسجيل سند ${bond.type} بمبلغ: ${"%.2f".format(bond.amount)}\nبرقم السند: ${bond.id}\nالرصيد الحالي: ${"%.2f".format(customer.balance)}"
        shareText(context, customer.mobile, text)
    }

    fun shareStatement(context: Context, customer: CustomerEntity, rows: List<CustomerStatementRow>) {
        val sb = StringBuilder()
        sb.append("كشف حساب العميل: ${customer.name}\n")
        sb.append("الرصيد الحالي: ${customer.balance}\n\n")
        rows.forEach { row ->
            sb.append("${row.type} | مبلغ: ${row.amount} | رصيد: ${row.balanceAfter}\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب"))
    }

    private fun shareText(context: Context, phone: String?, text: String) {
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val cleanPhone = phone?.filter { it.isDigit() } ?: ""
            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
        }
    }

    fun pairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun printBluetooth(device: BluetoothDevice, invoice: InvoiceEntity, customerName: String): Result<Unit> {
        return try {
            val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            val out = socket.outputStream
            val text = "خمر نت - فاتورة مبيعات\nرقم: ${invoice.id}\nالعميل: $customerName\nالمبلغ: ${invoice.total}\nالرصيد: ${invoice.newBalance}\n\n\n"
            out.write(text.toByteArray(charset("ISO-8859-1")))
            out.flush()
            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
