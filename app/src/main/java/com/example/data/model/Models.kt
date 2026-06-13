package com.example.data.model

data class DownloadItem(
    val id: String,
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
    val status: DownloadStatus,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val channelName: String = "Vidora Creator",
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val folderName: String? = null
)

enum class DownloadStatus {
    QUEUED, RESOLVING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class MediaFormat(
    val formatId: String,
    val label: String,
    val ext: String,
    val isAudioOnly: Boolean,
    val width: Int?,
    val height: Int?,
    val fileSizeBytes: Long?,
    val isRecommended: Boolean = false
)

data class SupportedProvider(
    val id: String,
    val displayName: String,
    val iconResName: String, // e.g. "ic_youtube"
    val domains: List<String>
)

sealed class UrlValidationState {
    object Empty : UrlValidationState()
    data class Valid(val provider: SupportedProvider) : UrlValidationState()
    object Invalid : UrlValidationState()
    object Loading : UrlValidationState()
}
