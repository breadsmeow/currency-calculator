package com.yendollar

import android.content.Context
import java.util.Locale
import kotlin.math.roundToLong

/** What's typed into the keypad widget. Saved on the phone so it survives restarts. */
object CalcState {
    private const val PREFS = "calc_widget"
    private const val KEY_INPUT = "input"
    private const val KEY_REVERSED = "reversed"
    private const val MAX_DIGITS = 12

    fun input(context: Context): String = prefs(context).getString(KEY_INPUT, "10000") ?: "10000"

    /** false = typing yen, showing dollars. true = typing dollars, showing yen. */
    fun reversed(context: Context): Boolean = prefs(context).getBoolean(KEY_REVERSED, false)

    fun press(context: Context, key: String) {
        var input = input(context)
        var reversed = reversed(context)
        val rate = RateRepository.rate(context)

        when (key) {
            "back" -> input = input.dropLast(1)
            "clear" -> input = ""
            "dot" -> if (reversed && !input.contains('.')) input = input.ifEmpty { "0" } + "."
            "swap" -> {
                val amount = input.toDoubleOrNull() ?: 0.0
                val converted = if (reversed) amount * rate else amount / rate
                reversed = !reversed
                input = if (reversed) {
                    String.format(Locale.US, "%.2f", converted).trimEnd('0').trimEnd('.')
                } else {
                    converted.roundToLong().toString()
                }
                if (input == "0") input = ""
            }
            "000" -> if (input.isNotEmpty() && input != "0") repeat(3) { input = appendDigit(input, '0') }
            else -> if (key.length == 1 && key[0].isDigit()) input = appendDigit(input, key[0])
        }

        prefs(context).edit()
            .putString(KEY_INPUT, input)
            .putBoolean(KEY_REVERSED, reversed)
            .apply()
    }

    private fun appendDigit(input: String, digit: Char): String {
        if (input.contains('.') && input.substringAfter('.').length >= 2) return input
        if (input.replace(".", "").length >= MAX_DIGITS) return input
        if (input == "0") return digit.toString()
        return input + digit
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
