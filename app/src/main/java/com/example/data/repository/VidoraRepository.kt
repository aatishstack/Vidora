package com.example.data.repository

import com.example.data.database.DownloadDao
import com.example.data.database.DownloadEntity
import com.example.data.model.*
import com.example.download.DownloadEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class VidoraRepository(
    private val downloadDao: DownloadDao,
    private val downloadEngine: DownloadEngine
) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getDownload(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toDomain()
    }

    suspend fun enqueueDownload(
        url: String,
        providerId: String,
        title: String,
        thumbnailUri: String?,
        durationSecs: Int,
        videoQuality: String,
        isAudioOnly: Boolean,
        fileBytes: Long,
        channelName: String = "Vidora Creator"
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val normalized = normalizeUrl(url)
        val entity = DownloadEntity(
            id = id,
            originalUrl = url,
            normalizedUrl = normalized,
            providerId = providerId,
            title = title,
            thumbnailUri = thumbnailUri,
            durationSecs = durationSecs,
            videoQuality = videoQuality,
            isAudioOnly = isAudioOnly,
            localUri = null,
            fileBytes = fileBytes,
            bytesDownloaded = 0,
            status = DownloadStatus.QUEUED.name,
            errorCode = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
            channelName = channelName,
            playCount = 0,
            isFavorite = false,
            folderName = null
        )
        downloadDao.insertDownload(entity)
        downloadEngine.startDownload(id)
        return id
    }

    fun pauseDownload(id: String) {
        downloadEngine.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadEngine.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadEngine.cancelDownload(id)
    }

    fun retryDownload(id: String) {
        downloadEngine.retryDownload(id)
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearDownloads() {
        downloadDao.clearAllDownloads()
    }

    suspend fun toggleFavorite(id: String) {
        val current = downloadDao.getDownloadById(id) ?: return
        downloadDao.updateDownload(current.copy(isFavorite = !current.isFavorite))
    }

    suspend fun updateFolder(id: String, folderName: String?) {
        val current = downloadDao.getDownloadById(id) ?: return
        downloadDao.updateDownload(current.copy(folderName = folderName))
    }

    suspend fun incrementPlay(id: String) {
        val current = downloadDao.getDownloadById(id) ?: return
        downloadDao.updateDownload(current.copy(playCount = current.playCount + 1))
    }

    val providers = listOf(
        SupportedProvider(
            id = "youtube",
            displayName = "YouTube",
            iconResName = "youtube",
            domains = listOf("youtube.com", "youtu.be", "m.youtube.com")
        ),
        SupportedProvider(
            id = "instagram",
            displayName = "Instagram",
            iconResName = "instagram",
            domains = listOf("instagram.com", "instagr.am")
        ),
        SupportedProvider(
            id = "tiktok",
            displayName = "TikTok",
            iconResName = "tiktok",
            domains = listOf("tiktok.com", "vt.tiktok.com")
        )
    )

    fun validateUrl(url: String): UrlValidationState {
        if (url.isBlank()) return UrlValidationState.Empty
        val cleanUrl = url.trim().lowercase()
        
        val matchedProvider = providers.firstOrNull { provider ->
            provider.domains.any { domain ->
                cleanUrl.contains(domain)
            }
        }

        return if (matchedProvider != null) {
            UrlValidationState.Valid(matchedProvider)
        } else {
            UrlValidationState.Invalid
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().split("?")[0]
    }

    fun fetchFormatsForProvider(providerId: String): List<MediaFormat> {
        return when (providerId) {
            "youtube" -> listOf(
                MediaFormat("yt-1080p", "1080p · MP4 · ~380 MB", "mp4", false, 1920, 1080, 380 * 1024 * 1024L, true),
                MediaFormat("yt-720p", "720p · MP4 · ~180 MB", "mp4", false, 1280, 720, 180 * 1024 * 1024L, false),
                MediaFormat("yt-480p", "480p · MP4 · ~90 MB", "mp4", false, 854, 480, 90 * 1024 * 1024L, false),
                MediaFormat("yt-mp3-320", "MP3 320kbps · Audio", "mp3", true, null, null, 12 * 1024 * 1024L, true),
                MediaFormat("yt-m4a-128", "M4A 128kbps · Audio", "m4a", true, null, null, 5 * 1024 * 1024L, false)
            )
            "instagram" -> listOf(
                MediaFormat("ig-hd", "HD Video · MP4 · ~45 MB", "mp4", false, 1080, 1920, 45 * 1024 * 1024L, true),
                MediaFormat("ig-sd", "SD Video · MP4 · ~18 MB", "mp4", false, 720, 1280, 18 * 1024 * 1024L, false),
                MediaFormat("ig-audio", "Audio (Original Track)", "m3u", true, null, null, 3 * 1024 * 1024L, false)
            )
            "tiktok" -> listOf(
                MediaFormat("tt-no-watermark", "No Watermark HD · ~25 MB", "mp4", false, 1080, 1920, 25 * 1024 * 1024L, true),
                MediaFormat("tt-watermark", "Watermarked Video · ~27 MB", "mp4", false, 1080, 1920, 27 * 1024 * 1024L, false),
                MediaFormat("tt-audio", "Background Audio · MP3", "mp3", true, null, null, 4 * 1024 * 1024L, false)
            )
            else -> listOf(
                MediaFormat("fallback-raw", "Original Source Stream · ~50 MB", "mp4", false, null, null, 50 * 1024 * 1024L, true)
            )
        }
    }

    fun getThumbnailForMockUrl(url: String, providerId: String): String {
        return when (providerId) {
            "youtube" -> {
                if (url.contains("music") || url.contains("lofi")) {
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop"
                } else if (url.contains("coding") || url.contains("tech")) {
                    "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500&auto=format&fit=crop"
                } else {
                    "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=500&auto=format&fit=crop"
                }
            }
            "instagram" -> "https://images.unsplash.com/photo-1611262588024-d12430b98920?w=500&auto=format&fit=crop"
            "tiktok" -> "https://images.unsplash.com/photo-1598128558393-70ff21433be0?w=500&auto=format&fit=crop"
            else -> "https://images.unsplash.com/photo-1516280440614-37939bbacd6a?w=500&auto=format&fit=crop"
        }
    }

    fun getTitleForMockUrl(url: String, providerId: String): String {
        return when (providerId) {
            "youtube" -> {
                if (url.contains("lofi")) "Lofi Hip Hop Radio - Beats to Study/Relax To"
                else if (url.contains("tech") || url.contains("review")) "The Ultimate M4 Ultra Review: Mind-Blowing Performance"
                else "Rick Astley - Never Gonna Give You Up (Official Video)"
            }
            "instagram" -> "Amazing Travel Reel: Midnight Sunset in Norway Fjords"
            "tiktok" -> "Viral Fusion Dance choreography to Trending Beats!"
            else -> "Media Stream Capture"
        }
    }

    fun getChannelForMockUrl(providerId: String): String {
        return when (providerId) {
            "youtube" -> "Lofi Records"
            "instagram" -> "@wanderlust_explorer"
            "tiktok" -> "@dancefusion.official"
            else -> "Web Content"
        }
    }
}
