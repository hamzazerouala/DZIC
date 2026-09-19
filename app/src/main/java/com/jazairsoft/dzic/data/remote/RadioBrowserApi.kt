package com.jazairsoft.dzic.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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

    /** Signale une ecoute a Radio Browser (statistiques communautaires). */
    @GET("json/url/{uuid}")
    suspend fun registerClick(@Path("uuid") uuid: String): Any?

    /** Liste des miroirs disponibles (utilise pour choisir un hote joignable). */
    @GET
    suspend fun servers(@Url url: String): List<ServerDto>
}
