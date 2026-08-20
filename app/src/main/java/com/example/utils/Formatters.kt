package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {

    private val numberFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val decimalFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            numberFormat.format(amount.roundToLong())
        } else {
            decimalFormat.format(amount)
        }
    }

    fun currentIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return sdf.format(Date())
    }

    fun formatDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) {
            val sdf = SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a", Locale.ENGLISH)
            return sdf.format(Date())
        }
        return try {
            val inputFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            )
            var parsedDate: Date? = null
            for (format in inputFormats) {
                try {
                    parsedDate = format.parse(isoString)
                    if (parsedDate != null) break
                } catch (_: Exception) { }
            }

            if (parsedDate != null) {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a", Locale.ENGLISH)
                outputFormat.format(parsedDate)
            } else {
                isoString
            }
        } catch (_: Exception) {
            isoString
        }
    }

    fun formatDateOnly(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val parts = isoString.split("T")
            if (parts.isNotEmpty()) parts[0] else isoString
        } catch (_: Exception) {
            isoString
        }
    }

    /**
     * Generate operation numbers (e.g. 10141 for invoice, 10111 for receipt bond, 10121 for payment bond, 10131 for transfer)
     */
    fun generateOpNumber(
        userCode: String,
        opTypeCode: String,
        existingNumbers: List<String>
    ): String {
        val cleanCode = if (userCode.isBlank()) "101" else userCode.trim()
        val prefix = "$cleanCode$opTypeCode"

        val seqs = mutableListOf<Int>()
        for (num in existingNumbers) {
            val cleanNum = num.trim()
            if (cleanNum.startsWith(prefix)) {
                val suffix = cleanNum.substring(prefix.length)
                val parsed = suffix.toIntOrNull()
                if (parsed != null) {
                    seqs.add(parsed)
                }
            }
        }

        val nextSeq = if (seqs.isNotEmpty()) seqs.maxOrNull()!! + 1 else 1
        return "$prefix$nextSeq"
    }

    /**
     * Normalize Arabic/Persian digits to standard digits
     */
    fun normalizeDigits(input: String): String {
        val arabicMap = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
            '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
        )
        return input.map { arabicMap[it] ?: it }.joinToString("")
    }

    fun normalizePhoneNumber(phone: String): String {
        val converted = normalizeDigits(phone)
        var digitsOnly = converted.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        if (digitsOnly.startsWith("00")) {
            digitsOnly = digitsOnly.substring(2)
        }

        return when {
            digitsOnly.startsWith("07") && digitsOnly.length == 10 -> "967" + digitsOnly.substring(1)
            (digitsOnly.startsWith("70") || digitsOnly.startsWith("71") || digitsOnly.startsWith("73") ||
                    digitsOnly.startsWith("77") || digitsOnly.startsWith("78")) && digitsOnly.length == 9 -> "967$digitsOnly"
            digitsOnly.startsWith("0") && digitsOnly.length >= 9 -> "967" + digitsOnly.substring(1)
            else -> digitsOnly
        }
    }
}
