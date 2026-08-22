package com.khamrnet.app.util

/**
 * Operations Numbering Generator
 * Generates sequential operational numbers based on:
 * UserCode + OperationTypeCode + Sequence
 *
 * Operation Type Codes (مطابقة لنظام الويب و ERP):
 * 4: فاتورة مبيعات (Sales Invoice) e.g., Cashier 101 -> 10141, 10142...
 * 1: سند قبض (Receipt Bond) e.g., 10111, 10112...
 * 2: سند صرف (Payment Bond) e.g., 10121, 10122...
 * 3: تحويل مخزني (Stock Transfer) e.g., 10131, 10132...
 * 5: تصفية كاشير وقبض مبيعات (Cashier Settlement) e.g., 10151, 10152...
 */
object OperationNumberGenerator {

    fun generateOperationNumber(
        userCode: String,
        opTypeCode: String,
        existingNumbers: List<String>
    ): String {
        val cleanUserCode = userCode.trim().ifEmpty { "101" }
        val prefix = "$cleanUserCode$opTypeCode"

        val matchingNums = mutableListOf<Int>()

        for (rawNum in existingNumbers) {
            val strNum = rawNum.trim()
            if (strNum.startsWith(prefix)) {
                val suffix = strNum.substring(prefix.length)
                val parsed = suffix.toIntOrNull()
                if (parsed != null) {
                    matchingNums.add(parsed)
                }
            }
        }

        val nextSeq = if (matchingNums.isNotEmpty()) matchingNums.maxOrNull()!! + 1 else 1
        return "$prefix$nextSeq"
    }
}
