package com.khamrnet.app.util

object ArabicNumberConverter {
    private val ONES = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    private val TEENS = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    private val TENS = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    private val HUNDREDS = arrayOf("", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    fun convertToArabicWords(amount: Double, currency: String = "YER", subUnit: String = "فلس"): String {
        val total = Math.abs(amount)
        val integerPart = total.toLong()
        val fractionalPart = Math.round((total - integerPart) * 100).toInt()

        val intWords = convertGroup(integerPart)
        val fracWords = if (fractionalPart > 0) convertGroup(fractionalPart.toLong()) else ""

        val currName = when (currency.uppercase()) {
            "YER", "ريال يمني" -> "ريال يمني"
            "SAR", "ريال سعودي" -> "ريال سعودي"
            "USD", "دولار" -> "دولار"
            else -> currency
        }

        var result = if (intWords.isNotEmpty()) "$intWords $currName" else "صفر $currName"
        if (fracWords.isNotEmpty()) {
            result += " و $fracWords $subUnit"
        }
        return result
    }

    private fun convertGroup(number: Long): String {
        if (number == 0L) return ""
        if (number < 10) return ONES[number.toInt()]
        if (number < 20) return TEENS[(number - 10).toInt()]
        if (number < 100) {
            val one = (number % 10).toInt()
            val ten = (number / 10).toInt()
            return if (one == 0) TENS[ten] else "${ONES[one]} و ${TENS[ten]}"
        }
        if (number < 1000) {
            val hundred = (number / 100).toInt()
            val rem = number % 100
            val remStr = convertGroup(rem)
            return if (rem == 0L) HUNDREDS[hundred] else "${HUNDREDS[hundred]} و $remStr"
        }
        if (number < 1000000) {
            val thousand = number / 1000
            val rem = number % 1000
            val thouStr = when (thousand) {
                1L -> "ألف"
                2L -> "ألفان"
                in 3..10 -> "${convertGroup(thousand)} آلاف"
                else -> "${convertGroup(thousand)} ألف"
            }
            val remStr = convertGroup(rem)
            return if (rem == 0L) thouStr else "$thouStr و $remStr"
        }
        if (number < 1000000000) {
            val million = number / 1000000
            val rem = number % 1000000
            val milStr = when (million) {
                1L -> "مليون"
                2L -> "مليونان"
                in 3..10 -> "${convertGroup(million)} ملايين"
                else -> "${convertGroup(million)} مليون"
            }
            val remStr = convertGroup(rem)
            return if (rem == 0L) milStr else "$milStr و $remStr"
        }
        return number.toString()
    }
}
