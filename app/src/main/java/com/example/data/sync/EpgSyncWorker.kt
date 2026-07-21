package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.IPTVDatabase
import com.example.data.preference.PreferencesManager
import com.example.data.repository.IPTVRepository
import java.util.concurrent.TimeUnit

class EpgSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("EpgSyncWorker", "Background EPG sync task started")
        val context = applicationContext
        val preferences = PreferencesManager(context)
        val epgUrl = preferences.epgUrl

        if (epgUrl.isEmpty()) {
            Log.d("EpgSyncWorker", "EPG URL is empty. Skipping background sync.")
            return Result.success()
        }

        Log.d("EpgSyncWorker", "Fetching EPG in background from: $epgUrl")
        return try {
            val database = IPTVDatabase.getDatabase(context)
            val repository = IPTVRepository(database.iptvDao(), context)
            val success = repository.importEpg(epgUrl)
            if (success) {
                Log.d("EpgSyncWorker", "Background EPG sync succeeded")
                Result.success()
            } else {
                Log.e("EpgSyncWorker", "Background EPG sync failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("EpgSyncWorker", "Background EPG sync error", e)
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "epg_periodic_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Periodically run every 12 hours to keep EPG data updated
            val syncRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d("EpgSyncWorker", "EPG periodic background sync work scheduled")
        }
    }
}
