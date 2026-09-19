package com.jazairsoft.dzic.di

import android.content.Context
import androidx.room.Room
import com.jazairsoft.dzic.BuildConfig
import com.jazairsoft.dzic.data.local.DzicDatabase
import com.jazairsoft.dzic.data.local.FavoriteDao
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

    /**
     * Radio Browser impose un User-Agent identifiant l'application.
     * all.api.radio-browser.info est un DNS round-robin sur les miroirs.
     */
    private const val RADIO_BROWSER_BASE = "https://all.api.radio-browser.info/"
    private const val USER_AGENT = "DZIC/0.1 (Android; Jazairsoft)"

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

    @Provides
    @Singleton
    fun provideRadioBrowserApi(client: OkHttpClient): RadioBrowserApi = Retrofit.Builder()
        .baseUrl(RADIO_BROWSER_BASE)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RadioBrowserApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DzicDatabase =
        Room.databaseBuilder(context, DzicDatabase::class.java, "dzic.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(database: DzicDatabase): FavoriteDao = database.favoriteDao()
}
