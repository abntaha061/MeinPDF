package com.mohammed.pdfreader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mohammed.pdfreader.data.repository.PdfRepository
import com.mohammed.pdfreader.utils.FileManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ScanLibraryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileManager: FileManager,
    private val repository: PdfRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val scannedFiles = fileManager.scanAllPdfs()
            var addedCount = 0

            scannedFiles.forEach { scanned ->
                val uri = scanned.uri
                if (fileManager.isUriAccessible(uri)) {
                    val added = repository.addFile(uri)
                    if (added != null) addedCount++
                }
            }

            Result.success(
                workDataOf("added_count" to addedCount, "total_scanned" to scannedFiles.size)
            )
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val WORK_NAME = "scan_library_worker"

        // ===== Schedule one-time scan =====
        fun schedule(context: Context): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ScanLibraryWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()
        }

        // ===== Schedule periodic scan (daily) =====
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScanLibraryWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
