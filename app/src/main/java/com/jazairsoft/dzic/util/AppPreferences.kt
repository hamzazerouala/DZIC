package com.jazairsoft.dzic.util

import android.content.Context
import java.util.Locale

/**
 * Preferences lues de maniere synchrone : la langue doit etre connue dans
 * attachBaseContext(), bien avant qu'une coroutine puisse s'executer.
 * DataStore, asynchrone par nature, ne convient pas ici.
 */
object AppPreferences {

    private const val FILE = "dzic_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_DATA_SAVER = "data_saver"
    private const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"

    const val LANGUAGE_SYSTEM = "system"

    /** Langues proposees, dans l'ordre d'affichage. */
    val supportedLanguages = listOf(LANGUAGE_SYSTEM, "fr", "ar", "en")

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun language(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun resolveLocale(language: String): Locale? =
        if (language == LANGUAGE_SYSTEM) null else Locale.forLanguageTag(language)

    /**
     * Economie de donnees : privilegie les flux radio a faible debit.
     * Une radio a 128 kbps consomme environ 56 Mo par heure, a 64 kbps environ
     * 28 Mo. Sur un forfait 4G algerien, l'ecart compte.
     */
    fun dataSaver(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DATA_SAVER, false)

    fun setDataSaver(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
    }

    fun downloadWifiOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOWNLOAD_WIFI_ONLY, true)

    fun setDownloadWifiOnly(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOWNLOAD_WIFI_ONLY, enabled).apply()
    }
}
