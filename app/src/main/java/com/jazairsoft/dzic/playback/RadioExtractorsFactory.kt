package com.jazairsoft.dzic.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.ts.AdtsExtractor

/**
 * Choisit l'extracteur d'apres le Content-Type reellement renvoye par le
 * serveur, et non d'apres l'extension de l'URL.
 *
 * Pourquoi c'est indispensable pour la radio web : la majorite des stations
 * algeriennes (Chaine 1/2/3, Jil FM, El Bahdja, Adrar...) sont diffusees par
 * Infomaniak a une adresse qui se termine par ".mp3" alors que le flux est en
 * realite de l'AAC/ADTS (Content-Type: audio/aac, entete 0xFFF1).
 *
 * Par defaut ExoPlayer ordonne ses extracteurs selon l'extension : il essaie
 * donc Mp3Extractor en premier. Or le mot de synchronisation ADTS (0xFFF1)
 * satisfait aussi le masque de synchronisation MP3 (0xFFE0) : Mp3Extractor
 * "reconnait" le flux, s'y accroche et decode du vide. Resultat : le lecteur
 * annonce une lecture en cours et aucun son ne sort.
 *
 * En placant AdtsExtractor en tete des que le serveur annonce de l'AAC, le bon
 * extracteur gagne le sniffing. Les extracteurs par defaut restent en repli.
 */
@OptIn(UnstableApi::class)
class RadioExtractorsFactory : ExtractorsFactory {

    private val delegate = DefaultExtractorsFactory()

    override fun createExtractors(): Array<Extractor> = delegate.createExtractors()

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>
    ): Array<Extractor> {
        val defaults = delegate.createExtractors(uri, responseHeaders)
        val contentType = responseHeaders.entries
            .firstOrNull { it.key.equals(HEADER_CONTENT_TYPE, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()

        return when {
            // audio/aac, audio/aacp, audio/x-aac
            contentType != null && contentType.contains("aac") ->
                arrayOf<Extractor>(AdtsExtractor()) + defaults

            // audio/mpeg, audio/mp3
            contentType != null && (contentType.contains("mpeg") || contentType.contains("mp3")) ->
                arrayOf<Extractor>(Mp3Extractor()) + defaults

            contentType != null && (contentType.contains("ogg") || contentType.contains("opus")) ->
                defaults

            // Content-Type absent ou generique (application/octet-stream) :
            // en radio web c'est presque toujours de l'ADTS ou du MP3. On teste
            // l'ADTS en premier car c'est lui que le sniffing par defaut rate.
            else -> arrayOf<Extractor>(AdtsExtractor(), Mp3Extractor()) + defaults
        }
    }

    private companion object {
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
