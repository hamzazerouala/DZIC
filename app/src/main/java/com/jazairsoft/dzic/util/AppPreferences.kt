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

    const val LANGUAGE_SYSTEM = "system"

    /** Langues proposees, dans l'ordre d'affichage. */
    val supportedLanguages = listOf(LANGUAGE_SYSTEM, "fr", "ar", "en")

    fun language(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun resolveLocale(language: String): Locale? =
        if (language == LANGUAGE_SYSTEM) null else Locale.forLanguageTag(language)
}
