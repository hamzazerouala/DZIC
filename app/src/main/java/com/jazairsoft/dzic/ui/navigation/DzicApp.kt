package com.jazairsoft.dzic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.AboutDialog
import com.jazairsoft.dzic.ui.components.DzicTopBar
import com.jazairsoft.dzic.ui.components.FullPlayer
import com.jazairsoft.dzic.ui.components.MiniPlayer
import com.jazairsoft.dzic.ui.screens.library.LibraryScreen
import com.jazairsoft.dzic.ui.screens.library.PlaylistDetailScreen
import com.jazairsoft.dzic.ui.screens.music.MusicScreen
import com.jazairsoft.dzic.ui.screens.podcasts.PodcastDetailScreen
import com.jazairsoft.dzic.ui.screens.podcasts.PodcastsScreen
import com.jazairsoft.dzic.ui.screens.radios.RadiosScreen
import com.jazairsoft.dzic.ui.screens.settings.SettingsScreen

private enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    RADIOS("radios", R.string.tab_radios, Icons.Filled.Radio),
    MUSIC("music", R.string.tab_music, Icons.Filled.MusicNote),
    PODCASTS("podcasts", R.string.tab_podcasts, Icons.AutoMirrored.Filled.LibraryBooks),
    LIBRARY("library", R.string.tab_library_short, Icons.Filled.LibraryMusic),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private const val PLAYLIST_ROUTE = "playlist/{playlistId}"
private const val SHOW_ROUTE = "show"

@Composable
fun DzicApp(viewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playerState.errorMessage) {
        val message = playerState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { DzicTopBar(onInfoClick = { showAbout = true }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                MiniPlayer(
                    state = playerState,
                    onExpand = { playerExpanded = true },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::next
                )
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.RADIOS.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Destination.RADIOS.route) { RadiosScreen() }
            composable(Destination.MUSIC.route) { MusicScreen() }
            composable(Destination.PODCASTS.route) {
                PodcastsScreen(onOpenShow = { show ->
                    viewModel.selectShow(show)
                    navController.navigate(SHOW_ROUTE)
                })
            }
            composable(Destination.LIBRARY.route) {
                LibraryScreen(onOpenPlaylist = { id -> navController.navigate("playlist/$id") })
            }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
            composable(SHOW_ROUTE) {
                PodcastDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = PLAYLIST_ROUTE,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) {
                PlaylistDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    AnimatedVisibility(
        visible = playerExpanded && playerState.hasContent,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        FullPlayer(
            state = playerState,
            isFavorite = playerState.station?.id?.let { favoriteIds.contains(it) } == true,
            onCollapse = { playerExpanded = false },
            onTogglePlayPause = viewModel::togglePlayPause,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onSeekTo = viewModel::seekTo,
            onStop = {
                viewModel.stop()
                playerExpanded = false
            },
            onToggleFavorite = { viewModel.toggleFavoriteCurrent() }
        )
    }
}
