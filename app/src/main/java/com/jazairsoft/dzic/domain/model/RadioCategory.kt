package com.jazairsoft.dzic.domain.model

import androidx.annotation.StringRes
import com.jazairsoft.dzic.R

/**
 * Categories d'affichage maison. On ne montre jamais les tags bruts de
 * Radio Browser (heterogenes et non moderes) : chaque categorie mappe
 * un ensemble de tags autorises.
 *
 * [queryTags] = tags reellement interroges cote API (les plus peuplees).
 * [matchTags] = tags servant a rattacher une station (dont les stations DZ)
 *               a la categorie cote client.
 */
enum class RadioCategory(
    val key: String,
    @StringRes val labelRes: Int,
    val queryTags: List<String>,
    val matchTags: List<String>
) {
    SPORT(
        "sport", R.string.cat_sport,
        listOf("sport", "workout", "dance"),
        listOf("sport", "sports", "workout", "fitness", "dance", "edm", "hits")
    ),
    CHILL(
        "chill", R.string.cat_chill,
        listOf("chillout", "lounge", "ambient"),
        listOf("chillout", "chill", "lounge", "ambient", "relax", "easy listening", "smooth")
    ),
    ELECTRO(
        "electro", R.string.cat_electro,
        listOf("electronic", "house", "techno"),
        listOf("electronic", "electro", "edm", "house", "techno", "trance", "deep house")
    ),
    JAZZ(
        "jazz", R.string.cat_jazz,
        listOf("jazz", "blues", "soul"),
        listOf("jazz", "blues", "soul", "funk", "lounge")
    ),
    POP(
        "pop", R.string.cat_pop,
        listOf("pop", "top 40", "hits"),
        listOf("pop", "top 40", "hits", "charts", "40s", "variety")
    ),
    ROCK(
        "rock", R.string.cat_rock,
        listOf("rock", "classic rock", "indie"),
        listOf("rock", "classic rock", "indie", "metal", "alternative")
    ),
    CLASSICAL(
        "classical", R.string.cat_classical,
        listOf("classical", "opera"),
        listOf("classical", "opera", "orchestra", "baroque", "piano")
    ),
    WORLD(
        "world", R.string.cat_world,
        listOf("arabic", "rai", "world music"),
        listOf("arabic", "rai", "raï", "chaabi", "kabyle", "andalous", "oriental", "world music", "world", "maghreb", "berber")
    ),
    NEWS(
        "news", R.string.cat_news, 
        listOf("news", "talk"),
        listOf("news", "talk", "information", "actualite")
    );

    companion object {
        fun fromKey(key: String?): RadioCategory? = entries.firstOrNull { it.key == key }
    }
}
