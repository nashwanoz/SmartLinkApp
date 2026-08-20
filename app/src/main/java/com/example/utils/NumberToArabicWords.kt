package com.example.utils

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

object NumberToArabicWords {

    private val ones = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة",
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val tens = arrayOf(
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val hundreds = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    private fun convertGroup(num: Long): String {
        if (num == 0L) return ""
        val result = StringBuilder()

        val h = (num / 100).toInt()
        val remainder = (num % 100).toInt()

        if (h > 0) {
            result.append(hundreds[h])
        }

        if (remainder > 0) {
            if (result.isNotEmpty()) result.append(" و ")
            if (remainder < 20) {
                result.append(ones[remainder])
            } else {
                val t = remainder / 10
                val o = remainder % 10
                if (o > 0) {
                    result.append(ones[o]).append(" و ").append(tens[t])
                } else {
                    result.append(tens[t])
                }
            }
        }

        return result.toString()
    }

    fun convert(
        amount: Double,
        currencyName: String = "ريال يمني",
        subCurrencyName: String = "فلس"
    ): String {
        if (amount.isNaN() || amount == 0.0) {
            return "صفر $currencyName"
        }

        val isNegative = amount < 0
        val absAmount = abs(amount)

        val integerPart = floor(absAmount).toLong()
        val decimalPart = ((absAmount - integerPart) * 100).roundToLong()

        val parts = mutableListOf<String>()

        val billions = integerPart / 1_000_000_000L
        val remBillions = integerPart % 1_000_000_000L

        val millions = remBillions / 1_000_000L
        val remMillions = remBillions % 1_000_000L

        val thousands = remMillions / 1_000L
        val units = remMillions % 1_000L

        if (billions > 0) {
            when (billions) {
                1L -> parts.add("مليار")
                2L -> parts.add("ملياران")
                in 3..10 -> parts.add("${convertGroup(billions)} مليارات")
                else -> parts.add("${convertGroup(billions)} مليار")
            }
        }

        if (millions > 0) {
            when (millions) {
                1L -> parts.add("مليون")
                2L -> parts.add("مليونان")
                in 3..10 -> parts.add("${convertGroup(millions)} ملايين")
                else -> parts.add("${convertGroup(millions)} مليون")
            }
        }

        if (thousands > 0) {
            when (thousands) {
                1L -> parts.add("ألف")
                2L -> parts.add("ألفان")
                in 3..10 -> parts.add("${convertGroup(thousands)} آلاف")
                else -> parts.add("${convertGroup(thousands)} ألف")
            }
        }

        if (units > 0) {
            parts.add(convertGroup(units))
        }

        var textResult = parts.joinToString(" و ")
        if (textResult.isEmpty()) {
            textResult = "صفر"
        }

        textResult += " $currencyName"

        if (decimalPart > 0) {
            textResult += " و ${convertGroup(decimalPart)} $subCurrencyName"
        }

        return (if (isNegative) "سالب " else "") + textResult
    }
}
