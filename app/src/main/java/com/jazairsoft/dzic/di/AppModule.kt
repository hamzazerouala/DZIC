package com.jazairsoft.dzic.di

import android.content.Context
import androidx.room.Room
import com.jazairsoft.dzic.BuildConfig
import com.jazairsoft.dzic.data.local.DzicDatabase
import com.jazairsoft.dzic.data.local.DownloadDao
import com.jazairsoft.dzic.data.local.EpisodeProgressDao
import com.jazairsoft.dzic.data.local.FavoriteDao
import com.jazairsoft.dzic.data.local.HistoryDao
import com.jazairsoft.dzic.data.local.PlaylistDao
import com.jazairsoft.dzic.data.remote.CcMixterApi
import com.jazairsoft.dzic.data.remote.ItunesApi
import com.jazairsoft.dzic.data.remote.LibriVoxApi
import com.jazairsoft.dzic.data.remote.OpenverseApi
import com.jazairsoft.dzic.data.remote.RadioBrowserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val RADIO_BROWSER_BASE = "https://all.api.radio-browser.info/"
    private const val OPENVERSE_BASE = "https://api.openverse.org/"
    private const val CCMIXTER_BASE = "https://ccmixter.org/"
    private const val ITUNES_BASE = "https://itunes.apple.com/"
    private const val LIBRIVOX_BASE = "https://librivox.org/"
    private const val USER_AGENT = "DZIC/0.3 (Android; Jazairsoft)"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return builder.build()
    }

    private fun retrofit(client: OkHttpClient, baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideRadioBrowserApi(client: OkHttpClient): RadioBrowserApi =
        retrofit(client, RADIO_BROWSER_BASE).create(RadioBrowserApi::class.java)

    @Provides
    @Singleton
    fun provideOpenverseApi(client: OkHttpClient): OpenverseApi =
        retrofit(client, OPENVERSE_BASE).create(OpenverseApi::class.java)

    @Provides
    @Singleton
    fun provideCcMixterApi(client: OkHttpClient): CcMixterApi =
        retrofit(client, CCMIXTER_BASE).create(CcMixterApi::class.java)

    @Provides
    @Singleton
    fun provideItunesApi(client: OkHttpClient): ItunesApi =
        retrofit(client, ITUNES_BASE).create(ItunesApi::class.java)

    @Provides
    @Singleton
    fun provideLibriVoxApi(client: OkHttpClient): LibriVoxApi =
        retrofit(client, LIBRIVOX_BASE).create(LibriVoxApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DzicDatabase =
        Room.databaseBuilder(context, DzicDatabase::class.java, "dzic.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(database: DzicDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaylistDao(database: DzicDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideHistoryDao(database: DzicDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideEpisodeProgressDao(database: DzicDatabase): EpisodeProgressDao =
        database.episodeProgressDao()

    @Provides
    fun provideDownloadDao(database: DzicDatabase): DownloadDao = database.downloadDao()
}
