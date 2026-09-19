package com.jazairsoft.dzic.data.remote

import com.jazairsoft.dzic.domain.model.PodcastShow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transporte l'emission selectionnee vers l'ecran de detail.
 * Passer l'objet complet dans la route de navigation obligerait a le
 * serialiser dans l'URL ; un porteur partage est plus simple et plus sur.
 */
@Singleton
class PodcastSelection @Inject constructor() {
    @Volatile
    var current: PodcastShow? = null
}
