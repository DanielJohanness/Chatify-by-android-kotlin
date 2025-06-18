
// ========== ChatDateUtils.kt ==========
package com.student.chatify.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatReadableDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> "Hari ini"

        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> "Kemarin"

        else -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(date.time)
    }
}
