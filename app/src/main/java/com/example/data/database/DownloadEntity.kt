package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val providerId: String,
    val title: String,
    val thumbnailUri: String?,
    val durationSecs: Int,
    val videoQuality: String,
    val isAudioOnly: Boolean,
    val localUri: String?,
    val fileBytes: Long,
    val bytesDownloaded: Long,
    val status: String, // "QUEUED", "RESOLVING", "DOWNLOADING", "PAUSED", "COMPLETED", "FAILED"
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val channelName: String,
    val playCount: Int,
    val isFavorite: Boolean,
    val folderName: String?,
    val directUrl: String? = null
) {
    fun toDomain(): DownloadItem {
        return DownloadItem(
            id = id,
            originalUrl = originalUrl,
            normalizedUrl = normalizedUrl,
            providerId = providerId,
            title = title,
            thumbnailUri = thumbnailUri,
            durationSecs = durationSecs,
            videoQuality = videoQuality,
            isAudioOnly = isAudioOnly,
            localUri = localUri,
            fileBytes = fileBytes,
            bytesDownloaded = bytesDownloaded,
            status = try { DownloadStatus.valueOf(status) } catch (e: Exception) { DownloadStatus.FAILED },
            errorCode = errorCode,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt,
            channelName = channelName,
            playCount = playCount,
            isFavorite = isFavorite,
            folderName = folderName,
            directUrl = directUrl
        )
    }

    companion object {
        fun fromDomain(domain: DownloadItem): DownloadEntity {
            return DownloadEntity(
                id = domain.id,
                originalUrl = domain.originalUrl,
                normalizedUrl = domain.normalizedUrl,
                providerId = domain.providerId,
                title = domain.title,
                thumbnailUri = domain.thumbnailUri,
                durationSecs = domain.durationSecs,
                videoQuality = domain.videoQuality,
                isAudioOnly = domain.isAudioOnly,
                localUri = domain.localUri,
                fileBytes = domain.fileBytes,
                bytesDownloaded = domain.bytesDownloaded,
                status = domain.status.name,
                errorCode = domain.errorCode,
                errorMessage = domain.errorMessage,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                channelName = domain.channelName,
                playCount = domain.playCount,
                isFavorite = domain.isFavorite,
                folderName = domain.folderName,
                directUrl = domain.directUrl
            )
        }
    }
}
