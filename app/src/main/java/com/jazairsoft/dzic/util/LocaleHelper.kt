package com.jazairsoft.dzic.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applique la langue choisie par l'utilisateur au Context de l'Activity.
 *
 * On n'utilise pas AppCompatDelegate.setApplicationLocales : cela imposerait
 * la dependance AppCompat et une AppCompatActivity, alors que l'app est en
 * Compose pur sur une ComponentActivity. Envelopper le Context dans
 * attachBaseContext est l'approche native et suffit a partir de l'API 24.
 */
object LocaleHelper {

    fun wrap(context: Context): Context {
        val locale = AppPreferences.resolveLocale(AppPreferences.language(context))
            ?: return context
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
