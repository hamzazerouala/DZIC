package com.jazairsoft.dzic.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jazairsoft.dzic.BuildConfig
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.AboutDialog
import com.jazairsoft.dzic.util.AppPreferences

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(AppPreferences.language(context)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Selecteur de langue : chips compactes en ligne, pas de liste
        // deroulante pleine largeur avec libelle au-dessus.
        SettingsCard(title = stringResource(R.string.settings_language)) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppPreferences.supportedLanguages.forEach { tag ->
                    FilterChip(
                        selected = language == tag,
                        onClick = {
                            if (language != tag) {
                                language = tag
                                AppPreferences.setLanguage(context, tag)
                                context.findActivity()?.recreate()
                            }
                        },
                        label = { Text(languageLabel(tag)) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        SettingsCard(
            title = stringResource(R.string.settings_battery),
            description = stringResource(R.string.settings_battery_desc),
            actionLabel = stringResource(R.string.settings_battery_action),
            onAction = { openBatterySettings(context) }
        )

        SettingsCard(
            title = stringResource(R.string.settings_storage_title),
            description = stringResource(R.string.settings_storage_desc)
        )

        SettingsCard(
            title = stringResource(R.string.settings_about),
            description = "Jazairsoft · www.jazairsoft.com\n" +
                "${stringResource(R.string.about_developer)} : Hamza ZEROUALA\n" +
                "${stringResource(R.string.about_version)} ${BuildConfig.VERSION_NAME}",
            actionLabel = stringResource(R.string.about_action),
            onAction = { showAbout = true }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    AppPreferences.LANGUAGE_SYSTEM -> stringResource(R.string.language_system)
    "fr" -> "Francais"
    "ar" -> "العربية"
    "en" -> "English"
    else -> tag
}

@Composable
private fun SettingsCard(
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            content?.invoke()
            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text(actionLabel) }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openBatterySettings(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val ignoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    val intent = if (ignoring) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    runCatching { context.startActivity(intent) }
}
