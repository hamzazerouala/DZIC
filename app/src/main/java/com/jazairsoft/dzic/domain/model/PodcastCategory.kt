package com.jazairsoft.dzic.domain.model

import androidx.annotation.StringRes
import com.jazairsoft.dzic.R

/**
 * Categories de contenus parles autorisees a l'affichage (whitelist).
 * [allowedGenres] sert au second filtrage cote client : la categorisation
 * declaree par les createurs n'est pas fiable.
 *
 * [isAudiobook] bascule la source vers LibriVox (domaine public) au lieu
 * du catalogue de podcasts.
 */
enum class PodcastCategory(
    val key: String,
    @StringRes val labelRes: Int,
    val query: String,
    val allowedGenres: List<String>,
    val isAudiobook: Boolean = false
) {
    AUDIOBOOKS("books", R.string.pod_cat_books, "", emptyList(), isAudiobook = true),
    LANGUAGES(
        "languages", R.string.pod_cat_languages, "apprendre les langues",
        listOf("language", "langues", "apprentissage", "education", "éducation", "courses")
    ),
    MEDICINE(
        "medicine", R.string.pod_cat_medicine, "medecine sante",
        listOf("medicine", "médecine", "health", "santé", "forme", "fitness", "science")
    ),
    TECH(
        "tech", R.string.pod_cat_tech, "informatique developpement programmation",
        listOf("technology", "technologies", "tech", "informatique", "science")
    ),
    THEATRE(
        "theatre", R.string.pod_cat_theatre, "theatre fiction radiophonique",
        listOf("arts", "fiction", "drama", "théâtre", "performing arts", "books", "livres")
    ),
    SCIENCE(
        "science", R.string.pod_cat_science, "science vulgarisation",
        listOf("science", "sciences", "nature", "education", "éducation")
    ),
    HISTORY(
        "history", R.string.pod_cat_history, "histoire",
        listOf("history", "histoire", "education", "éducation", "society", "culture")
    ),
    SPORT(
        "sport", R.string.pod_cat_sport, "sport",
        listOf("sport", "sports", "football", "fitness")
    ),
    COMEDY(
        "comedy", R.string.pod_cat_comedy, "humour",
        listOf("comedy", "humour", "comédie", "arts")
    ),
    CULTURE(
        "culture", R.string.pod_cat_culture, "societe culture",
        listOf("society", "culture", "société", "arts", "books", "livres")
    );

    companion object {
        /**
         * Exclusions imposees par le cahier des charges, appliquees quel que
         * soit le point d'entree : accueil, categorie ou recherche.
         */
        val blockedGenres = listOf(
            "religion", "spirituality", "spiritualité", "spiritualite", "islam",
            "christianity", "christianisme", "buddhism", "judaism", "hinduism",
            "politics", "politique", "government", "gouvernement"
        )

        val blockedKeywords = listOf(
            "coran", "qur'an", "quran", "islam", "bible", "eglise", "église",
            "priere", "prière", "prayer", "sermon", "hadith", "tafsir", "cheikh",
            "politique", "election", "élection", "gouvernement", "president", "président",
            "erotic", "erotique", "érotique", "xxx"
        )

        fun fromKey(key: String?): PodcastCategory? = entries.firstOrNull { it.key == key }
    }
}

/** Une emission ou un livre : conteneur d'episodes / de chapitres. */
data class PodcastShow(
    val id: String,
    val title: String,
    val author: String?,
    val artworkUrl: String?,
    val feedUrl: String?,
    val genres: List<String>,
    val isAudiobook: Boolean = false,
    val description: String? = null
)
