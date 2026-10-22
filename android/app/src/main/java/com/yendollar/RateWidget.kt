package com.yendollar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home screen widget: common yen amounts in dollars, live rate, tap to open the calculator. */
class RateWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Only draw from the saved rate here. Starting background work from onUpdate can make
        // some launchers redraw the widget in a loop; the scheduled worker handles fetching.
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    override fun onEnabled(context: Context) {
        RateWorker.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            RateWorker.schedule(context)
            RateWorker.refreshNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.yendollar.action.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RateWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val rate = RateRepository.rate(context)
            val updated = RateRepository.updatedAt(context)
            fun usdFor(yen: Int) = Fmt.usd(yen / rate)

            return RemoteViews(context.packageName, R.layout.widget_rate).apply {
                setTextViewText(R.id.main_value, usdFor(10_000))
                setTextViewText(R.id.v1000, usdFor(1_000))
                setTextViewText(R.id.v5000, usdFor(5_000))
                setTextViewText(R.id.v50000, usdFor(50_000))
                setTextViewText(R.id.rate_line, "\$1 = ¥" + Fmt.num(rate, 2))
                setTextViewText(
                    R.id.updated,
                    if (updated == 0L) context.getString(R.string.widget_not_loaded)
                    else context.getString(R.string.widget_updated, Fmt.time(updated))
                )

                val open = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.widget_root, open)

                val refresh = PendingIntent.getBroadcast(
                    context, 1,
                    Intent(context, RateWidget::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.refresh, refresh)
            }
        }
    }
}
