package com.yendollar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/** Home screen calculator: type an amount on the widget's keypad and see the conversion right there. */
class CalcWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    override fun onEnabled(context: Context) {
        RateWorker.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_KEY -> {
                val key = intent.data?.lastPathSegment ?: return
                CalcState.press(context, key)
                updateAll(context)
            }
            ACTION_REFRESH -> {
                RateWorker.schedule(context)
                RateWorker.refreshNow(context)
            }
        }
    }

    companion object {
        private const val ACTION_KEY = "com.yendollar.action.CALC_KEY"
        private const val ACTION_REFRESH = "com.yendollar.action.CALC_REFRESH"

        /** View id → key name sent back when it's tapped. */
        private val KEYS = listOf(
            R.id.k7 to "7", R.id.k8 to "8", R.id.k9 to "9", R.id.k_back to "back",
            R.id.k4 to "4", R.id.k5 to "5", R.id.k6 to "6", R.id.k_clear to "clear",
            R.id.k1 to "1", R.id.k2 to "2", R.id.k3 to "3", R.id.k_swap to "swap",
            R.id.k000 to "000", R.id.k0 to "0", R.id.k_dot to "dot"
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalcWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val rate = RateRepository.rate(context)
            val updated = RateRepository.updatedAt(context)
            val input = CalcState.input(context)
            val reversed = CalcState.reversed(context)
            val amount = input.toDoubleOrNull() ?: 0.0
            val result = if (reversed) amount * rate else amount / rate

            return RemoteViews(context.packageName, R.layout.widget_calc).apply {
                setTextViewText(R.id.calc_from_code, if (reversed) "USD → JPY" else "JPY → USD")
                setTextViewText(R.id.calc_from, (if (reversed) "\$" else "¥") + Fmt.input(input))
                setTextViewText(
                    R.id.calc_to,
                    if (reversed) "¥" + Fmt.num(result, 0) else "\$" + Fmt.num(result, 2)
                )
                val status = if (updated == 0L) "sample rate" else Fmt.time(updated)
                setTextViewText(R.id.calc_rate, "\$1 = ¥" + Fmt.num(rate, 2) + " · " + status)
                setTextColor(
                    R.id.k_dot,
                    ContextCompat.getColor(context, if (reversed) R.color.fg else R.color.line)
                )

                KEYS.forEachIndexed { index, (viewId, key) ->
                    val intent = Intent(context, CalcWidget::class.java)
                        .setAction(ACTION_KEY)
                        .setData(Uri.parse("yendollar://key/$key"))
                    val pending = PendingIntent.getBroadcast(
                        context, 100 + index, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    setOnClickPendingIntent(viewId, pending)
                }

                val refresh = PendingIntent.getBroadcast(
                    context, 99,
                    Intent(context, CalcWidget::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.calc_refresh, refresh)

                val open = PendingIntent.getActivity(
                    context, 98,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.calc_display, open)
            }
        }
    }
}
