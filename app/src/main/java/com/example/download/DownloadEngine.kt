package com.example.download

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.database.DownloadDao
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(id: String) {
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_DOWNLOAD_ID, id)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag("download_$id")
            .build()

        workManager.enqueueUniqueWork(
            "download_$id",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun pauseDownload(id: String) {
        workManager.cancelUniqueWork("download_$id")

        scope.launch {
            val item = downloadDao.getDownloadById(id)
            if (item != null && (item.status == "DOWNLOADING" || item.status == "QUEUED" || item.status == "RESOLVING")) {
                downloadDao.updateDownload(item.copy(
                    status = DownloadStatus.PAUSED.name,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun resumeDownload(id: String) {
        scope.launch {
            val item = downloadDao.getDownloadById(id)
            if (item != null && (item.status == "PAUSED" || item.status == "QUEUED" || item.status == "FAILED")) {
                downloadDao.updateDownload(item.copy(
                    status = DownloadStatus.QUEUED.name,
                    updatedAt = System.currentTimeMillis()
                ))
                startDownload(id)
            }
        }
    }

    fun cancelDownload(id: String) {
        workManager.cancelUniqueWork("download_$id")
        
        scope.launch {
            val item = downloadDao.getDownloadById(id)
            if (item != null) {
                downloadDao.updateDownload(item.copy(
                    status = DownloadStatus.CANCELLED.name,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun retryDownload(id: String) {
        scope.launch {
            val item = downloadDao.getDownloadById(id)
            if (item != null) {
                downloadDao.updateDownload(item.copy(
                    status = DownloadStatus.QUEUED.name,
                    bytesDownloaded = 0,
                    errorCode = null,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                ))
                startDownload(id)
            }
        }
    }
}
