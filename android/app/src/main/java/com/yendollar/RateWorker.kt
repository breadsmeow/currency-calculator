package com.yendollar

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Refreshes the rate in the background and redraws the home screen widget. */
class RateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ok = RateRepository.refresh(applicationContext)
        Fmt.refreshWidgets(applicationContext)
        return if (ok || runAttemptCount >= 3) Result.success() else Result.retry()
    }

    companion object {
        private val online = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Every 3 hours while the phone is online. Safe to call repeatedly. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RateWorker>(3, TimeUnit.HOURS)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("rate-periodic", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** One refresh as soon as the phone is online (the widget's ↻ button). */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RateWorker>()
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("rate-now", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
