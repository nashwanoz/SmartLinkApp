package com.smartlink.erp.utils

/**
 * Arabic Number to Words Converter (Tafqeet / تفقيط مالي)
 * Handles integers and decimals (e.g. 94506.50 -> أربعة وتسعون ألفاً وخمسمائة وستة ريال يمني وخمسون فلساً)
 */

private val ones = arrayOf(
    "",
    "واحد",
    "اثنان",
    "ثلاثة",
    "أربعة",
    "خمسة",
    "ستة",
    "سبعة",
    "ثمانية",
    "تسعة",
    "عشرة",
    "أحد عشر",
    "اثنا عشر",
    "ثلاثة عشر",
    "أربعة عشر",
    "خمسة عشر",
    "ستة عشر",
    "سبعة عشر",
    "ثمانية عشر",
    "تسعة عشر"
)

private val tens = arrayOf(
    "",
    "",
    "عشرون",
    "ثلاثون",
    "أربعون",
    "خمسون",
    "ستون",
    "سبعون",
    "ثمانون",
    "تسعون"
)

private val hundreds = arrayOf(
    "",
    "مائة",
    "مائتان",
    "ثلاثمائة",
    "أربعمائة",
    "خمسمائة",
    "ستمائة",
    "سبعمائة",
    "ثمانمائة",
    "تسعمائة"
)

/**
 * Convert a group of 1-3 digits to Arabic words
 */
private fun convertGroup(num: Int): String {
    if (num == 0) return ""
    
    val result = StringBuilder()
    
    val h = num / 100
    val remainder = num % 100
    
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

/**
 * Convert amount to Arabic words with currency
 * @param amount The numeric amount (e.g. 94506.50)
 * @param currencyName The currency name (e.g. "ريال يمني")
 * @param subCurrencyName The sub-currency name (e.g. "فلس")
 * @return Arabic text representation (e.g. "أربعة وتسعون ألفاً وخمسمائة وستة ريال يمني وخمسون فلساً")
 */
fun numberToArabicWords(
    amount: Double,
    currencyName: String = "ريال يمني",
    subCurrencyName: String = "فلس"
): String {
    if (amount.isNaN() || amount == 0.0) {
        return "صفر $currencyName"
    }
    
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount)
    
    val integerPart = absAmount.toLong()
    val decimalPart = ((absAmount - integerPart) * 100).roundToInt()
    
    val parts = mutableListOf<String>()
    
    // Billions
    val billions = integerPart / 1_000_000_000
    var rem = integerPart % 1_000_000_000
    
    // Millions
    val millions = rem / 1_000_000
    rem = rem % 1_000_000
    
    // Thousands
    val thousands = rem / 1_000
    val units = rem % 1_000
    
    // Add billions
    if (billions > 0) {
        when {
            billions == 1L -> parts.add("مليار")
            billions == 2L -> parts.add("ملياران")
            else -> parts.add("${convertGroup(billions.toInt())} مليار")
        }
    }
    
    // Add millions
    if (millions > 0) {
        when {
            millions == 1L -> parts.add("مليون")
            millions == 2L -> parts.add("مليونان")
            millions in 3..10 -> parts.add("${convertGroup(millions.toInt())} ملايين")
            else -> parts.add("${convertGroup(millions.toInt())} مليون")
        }
    }
    
    // Add thousands
    if (thousands > 0) {
        when {
            thousands == 1L -> parts.add("ألف")
            thousands == 2L -> parts.add("ألفان")
            thousands in 3..10 -> parts.add("${convertGroup(thousands.toInt())} آلاف")
            else -> parts.add("${convertGroup(thousands.toInt())} ألف")
        }
    }
    
    // Add units
    if (units > 0) {
        parts.add(convertGroup(units.toInt()))
    }
    
    var textResult = parts.joinToString(" و ")
    
    if (textResult.isEmpty()) {
        textResult = "صفر"
    }
    
    textResult += " $currencyName"
    
    // Add decimal part (fils)
    if (decimalPart > 0) {
        textResult += " و ${convertGroup(decimalPart)} $subCurrencyName"
    }
    
    return if (isNegative) "سالب $textResult" else textResult
}

/**
 * Simplified version for quick usage (integer only)
 */
fun numberToArabicWordsSimple(amount: Long): String {
    return numberToArabicWords(amount.toDouble(), "ريال", "فلس")
}

/**
 * Test function to verify conversion
 */
fun testNumberToArabicWords() {
    val testCases = listOf(
        0.0 to "صفر ريال يمني",
        1.0 to "واحد ريال يمني",
        10.0 to "عشرة ريال يمني",
        99.0 to "تسعة وتسعون ريال يمني",
        100.0 to "مائة ريال يمني",
        101.0 to "مائة وواحد ريال يمني",
        999.0 to "تسعمائة وتسعة وتسعون ريال يمني",
        1000.0 to "ألف ريال يمني",
        1001.0 to "ألف وواحد ريال يمني",
        1100.0 to "ألف ومائة ريال يمني",
        9999.0 to "تسعة آلاف وتسعمائة وتسعة وتسعون ريال يمني",
        10000.0 to "عشرة آلاف ريال يمني",
        100000.0 to "مائة ألف ريال يمني",
        1000000.0 to "مليون ريال يمني",
        1000000000.0 to "مليار ريال يمني",
        94506.50 to "أربعة وتسعون ألفاً وخمسمائة وستة ريال يمني وخمسون فلساً",
        -500.0 to "سالب خمسمائة ريال يمني"
    )
    
    testCases.forEach { (amount, expected) ->
        val result = numberToArabicWords(amount)
        println("Amount: $amount")
        println("Expected: $expected")
        println("Result: $result")
        println("Match: ${result == expected}")
        println("---")
    }
}
