package com.jazairsoft.dzic.data.remote

import com.google.gson.annotations.SerializedName
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station
import retrofit2.http.GET
import retrofit2.http.Query

// ---------- Openverse : agregateur de medias sous licence ouverte ----------
// Une seule API donne acces a Jamendo, Wikimedia Commons et Freesound,
// avec la licence de chaque piste. Aucune cle requise.

interface OpenverseApi {
    @GET("v1/audio/")
    suspend fun search(
        @Query("q") query: String,
        @Query("source") source: String = "jamendo",
        @Query("license_type") licenseType: String = "commercial,modification",
        @Query("page_size") pageSize: Int = 40,
        @Query("page") page: Int = 1
    ): OpenverseResponse
}

data class OpenverseResponse(
    @SerializedName("result_count") val resultCount: Int?,
    @SerializedName("results") val results: List<OpenverseAudioDto>?
)

data class OpenverseAudioDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("creator") val creator: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("license") val license: String?,
    @SerializedName("provider") val provider: String?,
    @SerializedName("genres") val genres: List<String>?
) {
    fun toStation(): Station? {
        val identifier = id?.takeIf { it.isNotBlank() } ?: return null
        val stream = url?.takeIf { it.startsWith("http") } ?: return null
        val label = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return Station(
            id = "ov:$identifier",
            name = label,
            streamUrl = stream,
            faviconUrl = thumbnail?.takeIf { it.startsWith("http") },
            tags = genres.orEmpty().map { it.lowercase() },
            country = null,
            countryCode = null,
            language = null,
            codec = "MP3",
            bitrate = 0,
            votes = 0,
            clickCount = 0,
            kind = MediaKind.TRACK,
            artist = creator?.trim(),
            durationMs = duration ?: 0L,
            sourceLabel = buildString {
                append(provider?.replaceFirstChar { it.uppercaseChar() } ?: "Openverse")
                license?.takeIf { it.isNotBlank() }?.let { append(" · CC ").append(it.uppercase()) }
            }
        )
    }
}

// ---------- ccMixter : remixes et instrumentaux Creative Commons ----------

interface CcMixterApi {
    @GET("api/query")
    suspend fun query(
        @Query("f") format: String = "json",
        @Query("tags") tags: String? = null,
        @Query("limit") limit: Int = 40,
        @Query("sinceday") sinceDay: Int? = null
    ): List<CcMixterUploadDto>
}

data class CcMixterUploadDto(
    @SerializedName("upload_id") val uploadId: Long?,
    @SerializedName("upload_name") val uploadName: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("license_name") val licenseName: String?,
    @SerializedName("files") val files: List<CcMixterFileDto>?
) {
    fun toStation(): Station? {
        val identifier = uploadId ?: return null
        val label = uploadName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val audio = files.orEmpty().firstOrNull { file ->
            file.downloadUrl?.lowercase()?.endsWith(".mp3") == true
        } ?: return null
        val stream = audio.downloadUrl ?: return null
        return Station(
            id = "cc:$identifier",
            name = label,
            streamUrl = stream,
            faviconUrl = null,
            tags = emptyList(),
            country = null,
            countryCode = null,
            language = null,
            codec = "MP3",
            bitrate = 0,
            votes = 0,
            clickCount = 0,
            kind = MediaKind.TRACK,
            artist = userName?.trim(),
            durationMs = 0L,
            sourceLabel = "ccMixter" + (licenseName?.let { " · $it" } ?: "")
        )
    }
}

data class CcMixterFileDto(
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("download_url") val downloadUrl: String?
)
