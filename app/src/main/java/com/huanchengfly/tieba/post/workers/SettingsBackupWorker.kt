package com.huanchengfly.tieba.post.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.HiddenThreadDao
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.AutoBackupInterval
import com.huanchengfly.tieba.post.utils.SettingsBackupStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SettingsBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val blockDao: BlockDao,
    private val hiddenDao: HiddenThreadDao,
    private val transactionRunner: TransactionRunner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.backupSettings.first()
        val runManually = inputData.getBoolean(KEY_RUN_MANUALLY, false)
        if (!settings.autoBackupEnabled && !runManually) {
            return Result.success()
        }

        val startedAt = System.currentTimeMillis()
        return try {
            val uri = SettingsBackupStore.backup(
                context = applicationContext,
                settingsRepository = settingsRepository,
                settings = settings,
                blockDao = blockDao,
                hiddenDao = hiddenDao,
                transactionRunner = transactionRunner,
                timestamp = startedAt,
            )
            SettingsBackupStore.cleanup(
                context = applicationContext,
                directoryUri = settings.autoBackupDirectoryUri,
                keepCount = settings.autoBackupKeepCount,
            )
            settingsRepository.backupSettings.save {
                it.copy(
                    lastAutoBackupAt = startedAt,
                    lastAutoBackupSucceeded = true,
                    lastAutoBackupMessage = uri.lastPathSegment,
                )
            }
            Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, "Auto backup failed", e)
            settingsRepository.backupSettings.save {
                it.copy(
                    lastAutoBackupAt = startedAt,
                    lastAutoBackupSucceeded = false,
                    lastAutoBackupMessage = e.getErrorMessage(),
                )
            }
            when (e) {
                is IllegalArgumentException, is SecurityException -> Result.failure()
                else -> if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
            }
        }
    }

    companion object {
        const val TAG = "SettingsBackupWorker"
        private const val PERIODIC_WORK_NAME = "settings_auto_backup"
        private const val ONE_SHOT_WORK_NAME = "settings_auto_backup_now"
        private const val KEY_RUN_MANUALLY = "run_manually"
        private const val MAX_RETRIES = 3

        fun schedule(workManager: WorkManager, interval: AutoBackupInterval) {
            val request = PeriodicWorkRequestBuilder<SettingsBackupWorker>(interval.days.toLong(), TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .addTag(TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        }

        fun startNow(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<SettingsBackupWorker>()
                .setInputData(workDataOf(KEY_RUN_MANUALLY to true))
                .addTag(TAG)
                .build()
            workManager.enqueueUniqueWork(ONE_SHOT_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
