package com.dawahayah.app.data.sync

import android.content.Context
import androidx.work.*
import com.dawahayah.app.DawaHayahApplication
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.storage.AppLanguage
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Small published metadata only. Large assets always require an explicit download. */
class KnowledgeSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val app=applicationContext as? DawaHayahApplication ?: return Result.failure()
        val c=app.container
        val settings=c.preferencesDataStore.settingsFlow.first()
        if(settings.wifiOnlyDownloads && !c.connectivityObserver.currentStatus().isUnmeteredWifi) return Result.retry()
        c.seedInitializer.ensureSeededIfEmpty()
        return when(c.publishedSynchronizer.refresh()) {
            is AppResult.Error -> if(runAttemptCount<3)Result.retry() else Result.failure()
            is AppResult.Success -> {
                c.knowledgeRepository.discoverCloudPacks(if(settings.language==AppLanguage.ARABIC) "ar" else "en")
                Result.success()
            }
        }
    }
    companion object {
        fun schedule(context:Context) {
            val request=PeriodicWorkRequestBuilder<KnowledgeSyncWorker>(24,TimeUnit.HOURS)
                .setInitialDelay(12,TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,15,TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("published-knowledge-daily",ExistingPeriodicWorkPolicy.KEEP,request)
        }
    }
}
