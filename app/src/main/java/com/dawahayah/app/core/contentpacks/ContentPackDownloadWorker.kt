package com.dawahayah.app.core.contentpacks

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dawahayah.app.DawaHayahApplication
import com.dawahayah.app.core.errors.AppResult
import com.dawahayah.app.core.errors.AppError

/**
 * WorkManager background worker for resilient, battery-aware content pack installation.
 */
class ContentPackDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val packId = inputData.getString(KEY_PACK_ID) ?: return Result.failure()
        val app = applicationContext as? DawaHayahApplication ?: return Result.failure()
        return when (val result = app.container.contentPackManager.installPack(packId)) {
            is AppResult.Success -> Result.success()
            is AppResult.Error -> if (runAttemptCount < 3 &&
                (result.error == AppError.NetworkUnavailable || result.error is AppError.DownloadInterrupted)
            ) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_PACK_ID = "pack_id"

        fun enqueuePackDownload(
            context: Context,
            packId: String,
            wifiOnly: Boolean
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresStorageNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<ContentPackDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_PACK_ID to packId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_$packId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
