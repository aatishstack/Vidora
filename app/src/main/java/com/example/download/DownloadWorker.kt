package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.database.VidoraDatabase
import com.example.data.database.DownloadEntity
import com.example.data.model.DownloadStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val downloadDao = VidoraDatabase.getDatabase(context).downloadDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        
        var dbItem = downloadDao.getDownloadById(id) ?: return Result.failure()
        
        // Generate unique notification ID from the hash of the string, or stable formula
        val notificationId = NOTIFICATION_ID_BASE + (id.hashCode() % 10000)

        createNotificationChannel()
        
        // Start foreground service immediately for Android 12+ compatibility
        setForeground(getForegroundInfo(id, dbItem.title, 0))

        try {
            // Update state: QUEUED -> RESOLVING
            updateStatusInDb(dbItem, DownloadStatus.RESOLVING, dbItem.bytesDownloaded)
            
            // Re-fetch directUrl or check if directUrl is present.
            // If directUrl is blank, try to fetch it via API
            var directUrl = dbItem.directUrl
            if (directUrl.isNullOrBlank()) {
                val response = try {
                    com.example.data.api.RetrofitClient.apiService.getMediaInfo(
                        com.example.data.api.MediaLookupRequest(dbItem.originalUrl)
                    )
                } catch (e: Exception) {
                    null
                }
                
                val apiFormat = response?.formats?.firstOrNull { 
                    it.formatId == dbItem.videoQuality || (it.height == null && dbItem.isAudioOnly)
                } ?: response?.formats?.firstOrNull()
                
                directUrl = apiFormat?.url
                if (directUrl.isNullOrBlank()) {
                    // Fallback to original URL
                    directUrl = dbItem.originalUrl
                } else {
                    // Save the fetched directUrl for reuse
                    downloadDao.updateDownload(dbItem.copy(directUrl = directUrl))
                }
            }

            dbItem = downloadDao.getDownloadById(id) ?: return Result.failure()
            if (dbItem.status == "CANCELLED" || dbItem.status == "PAUSED") {
                return Result.success()
            }

            // Update state: RESOLVING -> DOWNLOADING
            updateStatusInDb(dbItem, DownloadStatus.DOWNLOADING, dbItem.bytesDownloaded)

            val bytesAlreadyDownloaded = dbItem.bytesDownloaded
            val extension = if (dbItem.isAudioOnly) "mp3" else "mp4"
            val targetFile = File(context.getExternalFilesDir(null), "download_${id}.$extension")

            // Setup okhttp request with resume range header
            val requestBuilder = Request.Builder().url(directUrl)
            if (bytesAlreadyDownloaded > 0 && targetFile.exists()) {
                requestBuilder.addHeader("Range", "bytes=$bytesAlreadyDownloaded-")
            } else {
                // If local file was deleted but database says we downloaded something, reset it
                if (!targetFile.exists() && bytesAlreadyDownloaded > 0) {
                    updateStatusInDb(dbItem, DownloadStatus.DOWNLOADING, 0)
                    downloadDao.updateDownload(dbItem.copy(bytesDownloaded = 0))
                }
            }

            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw IOException("Server responded with code $code")
            }

            val body = response.body ?: throw IOException("Response body was null")
            val totalContentBytes = body.contentLength()
            val expectedTotalBytes = if (bytesAlreadyDownloaded > 0) {
                // For range queries, Content-Length is the length of the remaining chunk.
                // Total bytes of the full file is bytesAlreadyDownloaded + remaining chunk length.
                bytesAlreadyDownloaded + totalContentBytes
            } else {
                totalContentBytes
            }

            // If we have total length, update db item with it
            if (expectedTotalBytes > 0 && expectedTotalBytes != dbItem.fileBytes) {
                dbItem = dbItem.copy(fileBytes = expectedTotalBytes)
                downloadDao.updateDownload(dbItem)
            }

            // Write stream
            body.byteStream().use { inputStream ->
                // Open file in append mode if we have existing bytes, otherwise normal write
                val appendMode = bytesAlreadyDownloaded > 0 && targetFile.exists()
                FileOutputStream(targetFile, appendMode).use { outputStream ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer
                    var bytesRead: Int
                    var currentDownloaded = if (appendMode) bytesAlreadyDownloaded else 0L
                    var lastDbUpdateTime = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            // Cancelled by WorkManager (or paused)
                            response.close()
                            return Result.success()
                        }
                        
                        outputStream.write(buffer, 0, bytesRead)
                        currentDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        // Throttle database persistence to every 400ms to maintain performance
                        if (now - lastDbUpdateTime > 400) {
                            dbItem = dbItem.copy(
                                bytesDownloaded = currentDownloaded,
                                status = DownloadStatus.DOWNLOADING.name,
                                updatedAt = now
                            )
                            downloadDao.updateDownload(dbItem)
                            
                            val progressPercent = if (expectedTotalBytes > 0) {
                                ((currentDownloaded * 100) / expectedTotalBytes).toInt()
                            } else {
                                0
                            }
                            
                            notificationManager.notify(
                                notificationId,
                                getNotification(id, dbItem.title, progressPercent)
                            )
                            lastDbUpdateTime = now
                        }
                    }
                }
            }

            // Done downloading!
            dbItem = downloadDao.getDownloadById(id) ?: return Result.failure()
            dbItem = dbItem.copy(
                status = DownloadStatus.COMPLETED.name,
                bytesDownloaded = expectedTotalBytes,
                localUri = targetFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )
            downloadDao.updateDownload(dbItem)
            
            // Update finished notification
            notificationManager.notify(
                notificationId,
                getFinishedNotification(dbItem.title)
            )
            return Result.success()

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error downloading format $id", e)
            
            // On failure, if the worker was stopped/cancelled externally, we don't save FAILED status
            if (isStopped) {
                return Result.success()
            }
            
            val item = downloadDao.getDownloadById(id)
            if (item != null) {
                downloadDao.updateDownload(item.copy(
                    status = DownloadStatus.FAILED.name,
                    errorCode = "NET_FAILURE",
                    errorMessage = e.message ?: "Network stream disconnected",
                    updatedAt = System.currentTimeMillis()
                ))
            }
            
            notificationManager.notify(
                notificationId,
                getFailedNotification(dbItem.title, e.message ?: "Network error occurred")
            )
            return Result.failure()
        }
    }

    private suspend fun updateStatusInDb(entity: DownloadEntity, newStatus: DownloadStatus, progress: Long) {
        val updated = entity.copy(
            status = newStatus.name,
            bytesDownloaded = progress,
            updatedAt = System.currentTimeMillis()
        )
        downloadDao.updateDownload(updated)
    }

    // WorkManager foreground capability API setup
    private fun getForegroundInfo(id: String, title: String, progress: Int): ForegroundInfo {
        val notificationId = NOTIFICATION_ID_BASE + (id.hashCode() % 10000)
        return ForegroundInfo(notificationId, getNotification(id, title, progress))
    }

    private fun getNotification(id: String, title: String, progress: Int) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading Media")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(100, progress, progress == 0)
            .build()

    private fun getFinishedNotification(title: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Finished")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

    private fun getFailedNotification(title: String, error: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Active Sync Channels",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress for downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
