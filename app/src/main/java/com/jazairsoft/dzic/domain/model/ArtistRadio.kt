package com.jazairsoft.dzic.domain.model

/**
 * Catalogue d'artistes pour la section "Radios par artiste".
 * Radio Browser heberge de nombreuses stations monographiques : on interroge
 * simplement la recherche par nom, ce qui evite de maintenir une liste d'URLs
 * qui se perimerait.
 */
data class ArtistRadio(val display: String, val query: String, val region: ArtistRegion)

enum class ArtistRegion { MAGHREB, ORIENT, INTERNATIONAL }

object ArtistCatalog {

    val artists: List<ArtistRadio> = listOf(
        // Algerie et Maghreb
        ArtistRadio("Cheb Khaled", "khaled", ArtistRegion.MAGHREB),
        ArtistRadio("Idir", "idir", ArtistRegion.MAGHREB),
        ArtistRadio("Matoub Lounès", "matoub", ArtistRegion.MAGHREB),
        ArtistRadio("Dahmane El Harrachi", "harrachi", ArtistRegion.MAGHREB),
        ArtistRadio("Warda", "warda", ArtistRegion.MAGHREB),
        ArtistRadio("Cheikha Rimitti", "rimitti", ArtistRegion.MAGHREB),
        ArtistRadio("Cheb Mami", "mami", ArtistRegion.MAGHREB),
        ArtistRadio("Souad Massi", "souad massi", ArtistRegion.MAGHREB),
        ArtistRadio("Raï", "rai", ArtistRegion.MAGHREB),
        ArtistRadio("Chaabi", "chaabi", ArtistRegion.MAGHREB),
        ArtistRadio("Andalous", "andalous", ArtistRegion.MAGHREB),

        // Monde arabe
        ArtistRadio("Fairuz", "fairuz", ArtistRegion.ORIENT),
        ArtistRadio("Umm Kulthum", "oum kalthoum", ArtistRegion.ORIENT),
        ArtistRadio("Abdel Halim Hafez", "abdel halim", ArtistRegion.ORIENT),
        ArtistRadio("Mohammed Abdel Wahab", "abdel wahab", ArtistRegion.ORIENT),
        ArtistRadio("Amr Diab", "amr diab", ArtistRegion.ORIENT),
        ArtistRadio("Nancy Ajram", "nancy ajram", ArtistRegion.ORIENT),
        ArtistRadio("Kadim Al Sahir", "kadim", ArtistRegion.ORIENT),
        ArtistRadio("Tarab", "tarab", ArtistRegion.ORIENT),

        // International
        ArtistRadio("The Beatles", "beatles", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Elvis Presley", "elvis", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Michael Jackson", "michael jackson", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Queen", "queen", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Pink Floyd", "pink floyd", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Bob Marley", "bob marley", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Frank Sinatra", "sinatra", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Édith Piaf", "piaf", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Jacques Brel", "brel", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Bob Dylan", "dylan", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Mozart", "mozart", ArtistRegion.INTERNATIONAL),
        ArtistRadio("Beethoven", "beethoven", ArtistRegion.INTERNATIONAL)
    )
}
