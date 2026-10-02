package com.jazairsoft.dzic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.data.local.DownloadState
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station

/**
 * Tuile de media : pochette carree, titre, sous-titre.
 * C'est le format des plateformes de streaming, et il rend la pochette
 * exploitable comme reperage visuel, ce qu'une liste ne fait pas.
 */
@Composable
fun MediaCard(
    station: Station,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    downloadState: String? = null,
    isUnavailable: Boolean = false,
    onDownload: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Artwork(
                station = station,
                modifier = Modifier.fillMaxSize(),
                dimmed = isUnavailable
            )

            if (isUnavailable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.station_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB4A8)
                    )
                }
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Pastille de telechargement, en haut a gauche
            if (downloadState != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (downloadState == DownloadState.DONE.name)
                            Icons.Filled.CheckCircle else Icons.Filled.Downloading,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.unlike else R.string.like),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (station.subtitle.isNotBlank()) {
                    Text(
                        text = station.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDownload != null || onAddToPlaylist != null || onRemove != null) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.more_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_playlist)) },
                                onClick = { menuOpen = false; onAddToPlaylist() }
                            )
                        }
                        // Une radio est un flux sans fin : rien a telecharger.
                        if (onDownload != null && station.kind != MediaKind.RADIO) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.download)) },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                onClick = { menuOpen = false; onDownload() }
                            )
                        }
                        if (onRemove != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove)) },
                                onClick = { menuOpen = false; onRemove() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beaucoup de stations et de titres libres n'ont pas de pochette.
 * Plutot qu'un gris uniforme qui donne une grille deprimante, on genere
 * un degrade stable derive de l'identifiant, avec l'initiale.
 */
@Composable
fun Artwork(station: Station, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val palette = placeholderColors(station.id)
    Box(
        modifier = modifier.background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center
    ) {
        if (!station.faviconUrl.isNullOrBlank()) {
            AsyncImage(
                model = station.faviconUrl,
                contentDescription = station.name,
                // Une radio fournit un logo, pas une photo : le rogner coupe le
                // texte du logo. Une pochette de morceau, elle, doit remplir.
                contentScale = if (station.kind == MediaKind.RADIO) ContentScale.Fit
                else ContentScale.Crop,
                modifier = if (station.kind == MediaKind.RADIO)
                    Modifier.fillMaxSize().padding(14.dp) else Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = station.initial(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
        if (dimmed) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        }
    }
}

/**
 * Premiere lettre reelle du nom. Beaucoup d'entrees Radio Browser commencent
 * par un point, un espace ou un signe : prendre take(1) affichait "!" ou ".".
 */
private fun Station.initial(): String =
    name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "\u266A"

/** Couleur stable pour un identifiant donne : la grille reste variee mais jamais aleatoire. */
internal fun placeholderColors(id: String): List<Color> {
    val hues = listOf(
        listOf(Color(0xFF1E6F5C), Color(0xFF0E3A33)),
        listOf(Color(0xFF2B5876), Color(0xFF17263B)),
        listOf(Color(0xFF6D3B5E), Color(0xFF331C2E)),
        listOf(Color(0xFF7A5230), Color(0xFF38241440)),
        listOf(Color(0xFF34697A), Color(0xFF16323A)),
        listOf(Color(0xFF4A5A2B), Color(0xFF232C13))
    )
    val index = (id.hashCode().toLong() and 0xFFFFFFFFL) % hues.size
    return hues[index.toInt()]
}

/** Tuile d'emission ou de livre : meme grammaire visuelle que [MediaCard]. */
@Composable
fun ShowCard(
    title: String,
    author: String?,
    artworkUrl: String?,
    identifier: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(placeholderPalette(identifier))),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = title.trim().take(1).uppercase(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (!author.isNullOrBlank()) {
            Text(
                text = author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun placeholderPalette(id: String): List<Color> = placeholderColors(id)
