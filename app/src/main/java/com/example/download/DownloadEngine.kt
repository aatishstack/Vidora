package com.example.download

import android.content.Context
import android.util.Log
import com.example.data.database.DownloadDao
import com.example.data.database.DownloadEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class DownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(id: String) {
        if (activeJobs.containsKey(id)) return

        val job = scope.launch {
            try {
                val dbItem = downloadDao.getDownloadById(id) ?: return@launch
                
                // Transition: QUEUED -> RESOLVING
                updateStatus(dbItem, DownloadStatus.RESOLVING, progress = 0)
                delay(1200) // Simulate server resolving URL via yt-dlp

                val updatedItem = downloadDao.getDownloadById(id) ?: return@launch
                if (updatedItem.status == "CANCELLED" || updatedItem.status == "PAUSED") return@launch

                // Transition: RESOLVING -> DOWNLOADING
                updateStatus(updatedItem, DownloadStatus.DOWNLOADING, progress = updatedItem.bytesDownloaded)
                
                val totalBytes = if (updatedItem.fileBytes > 0) updatedItem.fileBytes else 45 * 1024 * 1024L
                var currentBytes = updatedItem.bytesDownloaded
                val stepSize = 1024 * 512 + (Math.random() * 512 * 1024).toLong() // 512KB - 1MB chunk steps

                while (currentBytes < totalBytes) {
                    delay(350) // High-speed network download ticker
                    
                    // Re-fetch to ensure user didn't request a Pause or Cancel midway
                    val currentItem = downloadDao.getDownloadById(id) ?: break
                    if (currentItem.status != "DOWNLOADING") {
                        Log.d("DownloadEngine", "Job $id cancelled or paused externally.")
                        return@launch
                    }

                    currentBytes += stepSize
                    if (currentBytes > totalBytes) {
                        currentBytes = totalBytes
                    }

                    // Update bytes
                    val progressEntity = currentItem.copy(
                        bytesDownloaded = currentBytes,
                        updatedAt = System.currentTimeMillis()
                    )
                    downloadDao.updateDownload(progressEntity)
                }

                // Completed
                val finalItem = downloadDao.getDownloadById(id) ?: return@launch
                if (finalItem.status == "DOWNLOADING") {
                    updateStatus(finalItem, DownloadStatus.COMPLETED, progress = totalBytes)
                }
            } catch (e: CancellationException) {
                Log.d("DownloadEngine", "Coroutine job $id was cancelled cleanly.")
            } catch (e: Exception) {
                Log.e("DownloadEngine", "Error downloading $id", e)
                val item = downloadDao.getDownloadById(id)
                if (item != null) {
                    downloadDao.updateDownload(item.copy(
                        status = DownloadStatus.FAILED.name,
                        errorCode = "NET_TIMEOUT",
                        errorMessage = e.message ?: "Unknown Network Failure",
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
    }

    fun pauseDownload(id: String) {
        val job = activeJobs.remove(id)
        job?.cancel()

        scope.launch {
            val item = downloadDao.getDownloadById(id)
            if (item != null && item.status == "DOWNLOADING") {
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
        val job = activeJobs.remove(id)
        job?.cancel()
        
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

    private suspend fun updateStatus(entity: DownloadEntity, newStatus: DownloadStatus, progress: Long) {
        val updated = entity.copy(
            status = newStatus.name,
            bytesDownloaded = progress,
            updatedAt = System.currentTimeMillis()
        )
        downloadDao.updateDownload(updated)
    }
}
