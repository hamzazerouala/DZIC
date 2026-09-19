package com.jazairsoft.dzic.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.AboutDialog
import com.jazairsoft.dzic.ui.components.DzicLogo
import com.jazairsoft.dzic.ui.theme.DzicGreen
import com.jazairsoft.dzic.ui.theme.DzicNight
import com.jazairsoft.dzic.ui.theme.DzicNightElevated
import com.jazairsoft.dzic.ui.theme.DzicSand
import kotlinx.coroutines.delay

/**
 * Page d'accueil affichee au lancement.
 * Elle s'efface seule apres [AUTO_DISMISS_MS], ou immediatement si on la touche.
 * Ouvrir "A propos" suspend la fermeture automatique.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "splashAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(durationMillis = 650),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(AUTO_DISMISS_MS)
        if (!showAbout) onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DzicNightElevated, DzicNight, DzicNight)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!showAbout) onFinished() }
    ) {
        IconButton(
            onClick = { showAbout = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.about_action),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha)
                .scale(contentScale)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DzicLogo(showTagline = false)

            Spacer(modifier = Modifier.height(22.dp))

            // La promesse du produit, en grand : c'est ce qui le distingue.
            Text(
                text = stringResource(R.string.splash_promise),
                fontSize = 26.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = DzicSand,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))
            Equalizer()
        }

        Text(
            text = stringResource(R.string.splash_publisher),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 26.dp)
                .alpha(contentAlpha)
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = {
            showAbout = false
            onFinished()
        })
    }
}

/** Trois barres qui respirent : signale que l'app est un lecteur audio. */
@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(32.dp)
    ) {
        listOf(0, 180, 360).forEachIndexed { index, offset ->
            val height by transition.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 620 + offset, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((32 * height).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DzicGreen)
            )
        }
    }
}

private const val AUTO_DISMISS_MS = 2600L
