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

    /** Typed keypad input with thousands separators, keeping a trailing "." or decimals as typed. */
    fun input(typed: String): String {
        if (typed.isEmpty()) return "0"
        val whole = num(typed.substringBefore('.').ifEmpty { "0" }.toDouble(), 0)
        return if (typed.contains('.')) whole + "." + typed.substringAfter('.') else whole
    }

    /** Redraws every home screen widget after the rate changes. */
    fun refreshWidgets(context: android.content.Context) {
        RateWidget.updateAll(context)
        CalcWidget.updateAll(context)
    }

    /** "9:41 AM" for today, otherwise "Oct 21, 2026". */
    fun time(millis: Long): String =
        DateUtils.formatSameDayTime(millis, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT).toString()
}
