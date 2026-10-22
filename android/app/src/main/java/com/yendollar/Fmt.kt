package com.yendollar

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

object Fmt {
    private fun nf(decimals: Int) = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }

    fun num(value: Double, decimals: Int): String = nf(decimals).format(value)

    fun usd(value: Double): String = "\$" + num(value, 2)

    fun yen(value: Double): String = "¥" + num(value, 0)

    /** "9:41 AM" for today, otherwise "Oct 21, 2026". */
    fun time(millis: Long): String =
        DateUtils.formatSameDayTime(millis, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT).toString()
}
