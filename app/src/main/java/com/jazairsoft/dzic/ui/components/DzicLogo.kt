package com.jazairsoft.dzic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.theme.DzicGreen
import com.jazairsoft.dzic.ui.theme.DzicSand

/** Marque DZIC : pastille + mot-symbole + accroche. Reutilisee accueil / a propos. */
@Composable
fun DzicLogo(
    modifier: Modifier = Modifier,
    markSize: Int = 108,
    wordmarkSize: Int = 46,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(markSize.dp)
                .clip(RoundedCornerShape((markSize / 3.4f).dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_dzic_mark),
                contentDescription = null,
                modifier = Modifier.size((markSize * 1.15f).dp)
            )
        }

        Text(
            text = "DZIC",
            fontSize = wordmarkSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (wordmarkSize * 0.14f).sp,
            color = DzicGreen,
            modifier = Modifier.padding(top = (markSize / 5).dp, start = (wordmarkSize * 0.14f).dp)
        )

        if (showTagline) {
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = DzicSand,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
