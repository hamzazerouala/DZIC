package com.jazairsoft.dzic.domain.model

/**
 * Une station de radio, normalisee depuis l'API Radio Browser
 * ou relue depuis la base locale (favoris).
 */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: List<String>,
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val codec: String?,
    val bitrate: Int,
    val votes: Int,
    val clickCount: Int
) {
    val isAlgerian: Boolean get() = countryCode.equals("DZ", ignoreCase = true)

    val subtitle: String
        get() = buildList {
            country?.takeIf { it.isNotBlank() }?.let { add(it) }
            tags.take(2).forEach { add(it.replaceFirstChar { c -> c.uppercaseChar() }) }
            if (bitrate > 0) add("${bitrate}k")
        }.joinToString(" · ")
}
