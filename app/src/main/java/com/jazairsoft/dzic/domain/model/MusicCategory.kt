package com.jazairsoft.dzic.domain.model

import androidx.annotation.StringRes
import com.jazairsoft.dzic.R

/**
 * Categories musicales maison.
 *
 * [archiveSubject] interroge Internet Archive, qui est la source principale :
 * sans cle, sans limitation agressive, et chaque sujet ci-dessous a ete verifie
 * comme renvoyant au moins un millier d'items.
 *
 * [openverseQuery] n'est qu'un complement opportuniste (catalogue Jamendo) :
 * l'API Openverse anonyme passe derriere Cloudflare et refuse une requete sur
 * deux, donc elle ne doit jamais etre seule a remplir un onglet.
 */
enum class MusicCategory(
    val key: String,
    @StringRes val labelRes: Int,
    val archiveSubject: String,
    val openverseQuery: String
) {
    ELECTRO("electro", R.string.music_cat_electro, "electronic", "electronic"),
    SPORT("sport", R.string.music_cat_sport, "dance", "workout"),
    FOCUS("focus", R.string.music_cat_focus, "ambient", "lofi"),
    CHILL("chill", R.string.music_cat_chill, "lounge", "chill"),
    JAZZ("jazz", R.string.music_cat_jazz, "jazz", "jazz"),
    ROCK("rock", R.string.music_cat_rock, "rock", "rock"),
    CINEMATIC("cinematic", R.string.music_cat_cinematic, "soundtrack", "cinematic"),
    ACOUSTIC("acoustic", R.string.music_cat_acoustic, "folk", "acoustic"),
    ORIENTAL("oriental", R.string.music_cat_oriental, "arabic", "arabic"),
    WORLD("world", R.string.music_cat_world, "world music", "world"),
    CLASSICAL("classical", R.string.music_cat_classical, "classical", "classical");

    companion object {
        fun fromKey(key: String?): MusicCategory? = entries.firstOrNull { it.key == key }
    }
}
