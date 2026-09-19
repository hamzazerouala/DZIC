package com.jazairsoft.dzic.domain.model

import androidx.annotation.StringRes
import com.jazairsoft.dzic.R

/**
 * Categories musicales maison. [query] alimente Openverse (catalogue Jamendo)
 * et [ccTag] ccMixter. On n'expose jamais les tags bruts des plateformes.
 */
enum class MusicCategory(
    val key: String,
    @StringRes val labelRes: Int,
    val query: String,
    val ccTag: String?
) {
    SPORT("sport", R.string.music_cat_sport, "workout energetic", "uptempo"),
    FOCUS("focus", R.string.music_cat_focus, "lofi study calm", "instrumental"),
    AMBIENT("ambient", R.string.music_cat_ambient, "ambient atmospheric", "ambient"),
    ELECTRO("electro", R.string.music_cat_electro, "electronic house", "electronic"),
    CHILL("chill", R.string.music_cat_chill, "chillout relax downtempo", "chill"),
    CINEMATIC("cinematic", R.string.music_cat_cinematic, "cinematic epic orchestral", "orchestral"),
    ACOUSTIC("acoustic", R.string.music_cat_acoustic, "acoustic guitar piano", "acoustic"),
    WORLD("world", R.string.music_cat_world, "oriental world ethnic", "world");

    companion object {
        fun fromKey(key: String?): MusicCategory? = entries.firstOrNull { it.key == key }
    }
}
