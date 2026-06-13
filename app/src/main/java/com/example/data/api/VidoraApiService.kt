package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class MediaLookupRequest(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class ApiMediaFormat(
    @Json(name = "format_id") val formatId: String,
    @Json(name = "url") val url: String?,
    @Json(name = "ext") val ext: String,
    @Json(name = "quality_label") val qualityLabel: String?,
    @Json(name = "width") val width: Int?,
    @Json(name = "height") val height: Int?,
    @Json(name = "approx_bytes") val approxBytes: Long?
)

@JsonClass(generateAdapter = true)
data class MediaLookupResponse(
    @Json(name = "title") val title: String,
    @Json(name = "duration") val duration: Double?,
    @Json(name = "channel") val channel: String?,
    @Json(name = "thumbnail") val thumbnail: String?,
    @Json(name = "formats") val formats: List<ApiMediaFormat>?
)

interface VidoraApiService {
    @POST("v1/media/info")
    suspend fun getMediaInfo(
        @Body request: MediaLookupRequest
    ): MediaLookupResponse
}
