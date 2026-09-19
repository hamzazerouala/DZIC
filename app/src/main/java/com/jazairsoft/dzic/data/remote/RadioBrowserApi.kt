package com.jazairsoft.dzic.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    /** Stations d'un pays, par code ISO (DZ pour l'Algerie). */
    @GET("json/stations/bycountrycodeexact/{code}")
    suspend fun stationsByCountryCode(
        @Path("code") code: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 200
    ): List<StationDto>

    /** Stations diffusant dans une langue donnee. */
    @GET("json/stations/bylanguageexact/{language}")
    suspend fun stationsByLanguage(
        @Path("language") language: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 150
    ): List<StationDto>

    /** Stations portant un tag donne. */
    @GET("json/stations/bytag/{tag}")
    suspend fun stationsByTag(
        @Path("tag") tag: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 80
    ): List<StationDto>

    /** Recherche texte libre sur le nom de la station. */
    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 60
    ): List<StationDto>

    /** Liste des pays, avec le nombre de stations de chacun. */
    @GET("json/countries")
    suspend fun countries(@Query("hidebroken") hideBroken: Boolean = true): List<CountryDto>

    /** Liste des langues, avec le nombre de stations de chacune. */
    @GET("json/languages")
    suspend fun languages(@Query("hidebroken") hideBroken: Boolean = true): List<LanguageDto>

    /** Signale une ecoute a Radio Browser (statistiques communautaires). */
    @GET("json/url/{uuid}")
    suspend fun registerClick(@Path("uuid") uuid: String): Any?
}

data class CountryDto(
    @SerializedName("name") val name: String?,
    @SerializedName("iso_3166_1") val code: String?,
    @SerializedName("stationcount") val stationCount: Int?
)

data class LanguageDto(
    @SerializedName("name") val name: String?,
    @SerializedName("iso_639") val iso639: String?,
    @SerializedName("stationcount") val stationCount: Int?
)
