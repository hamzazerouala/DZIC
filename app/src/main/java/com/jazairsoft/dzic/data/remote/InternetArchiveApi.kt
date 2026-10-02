package com.jazairsoft.dzic.data.remote

import com.google.gson.annotations.SerializedName
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internet Archive : plus de 500 000 enregistrements audio en acces libre,
 * sans cle d'API. Source principale de l'onglet Musique.
 *
 * L'URL de lecture n'est pas dans la reponse de recherche : il faut un second
 * appel sur /metadata/{id}. On ne le fait donc qu'au moment du clic, sinon
 * afficher une grille de 40 titres couterait 40 requetes.
 */
interface InternetArchiveApi {

    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("rows") rows: Int = 40,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json",
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "creator")
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}")
    suspend fun metadata(@Path("identifier") identifier: String): ArchiveMetadataResponse
}

data class ArchiveSearchResponse(
    @SerializedName("response") val response: ArchiveResponse?
)

data class ArchiveResponse(
    @SerializedName("numFound") val numFound: Int?,
    @SerializedName("docs") val docs: List<ArchiveDoc>?
)

data class ArchiveDoc(
    @SerializedName("identifier") val identifier: String?,
    // Internet Archive renvoie parfois une chaine, parfois un tableau,
    // pour le meme champ : on type en Any et on normalise.
    @SerializedName("title") val title: Any?,
    @SerializedName("creator") val creator: Any?
) {
    fun toStation(): Station? {
        val id = identifier?.takeIf { it.isNotBlank() } ?: return null
        val label = title.asText()?.takeIf { it.isNotBlank() } ?: return null
        return Station(
            id = "ia:$id",
            name = label,
            // Marqueur : l'URL reelle sera resolue au moment de la lecture.
            streamUrl = "ia:$id",
            faviconUrl = "https://archive.org/services/img/$id",
            tags = emptyList(),
            country = null,
            countryCode = null,
            language = null,
            codec = "MP3",
            bitrate = 0,
            votes = 0,
            clickCount = 0,
            kind = MediaKind.TRACK,
            artist = creator.asText(),
            durationMs = 0L,
            sourceLabel = "Internet Archive",
            needsResolution = true
        )
    }
}

private fun Any?.asText(): String? = when (this) {
    null -> null
    is String -> trim()
    is List<*> -> firstOrNull()?.toString()?.trim()
    else -> toString().trim()
}

data class ArchiveMetadataResponse(
    @SerializedName("files") val files: List<ArchiveFile>?
)

data class ArchiveFile(
    @SerializedName("name") val name: String?,
    @SerializedName("format") val format: String?,
    @SerializedName("length") val length: String?
)
