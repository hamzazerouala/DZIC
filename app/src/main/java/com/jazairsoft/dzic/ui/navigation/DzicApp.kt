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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
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
import com.jazairsoft.dzic.ui.components.FullPlayer
import com.jazairsoft.dzic.ui.components.MiniPlayer
import com.jazairsoft.dzic.ui.screens.library.LibraryScreen
import com.jazairsoft.dzic.ui.screens.library.PlaylistDetailScreen
import com.jazairsoft.dzic.ui.screens.radios.RadiosScreen
import com.jazairsoft.dzic.ui.screens.search.SearchScreen
import com.jazairsoft.dzic.ui.screens.settings.SettingsScreen

private enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    RADIOS("radios", R.string.tab_radios, Icons.Filled.Radio),
    SEARCH("search", R.string.tab_search, Icons.Filled.Search),
    LIBRARY("library", R.string.tab_library, Icons.Filled.LibraryMusic),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private const val PLAYLIST_ROUTE = "playlist/{playlistId}"

@Composable
fun DzicApp(viewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Une erreur de lecture silencieuse est indebogable pour l'utilisateur :
    // on la remonte toujours a l'ecran.
    LaunchedEffect(playerState.errorMessage) {
        val message = playerState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    Scaffold(
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
                            label = { Text(stringResource(destination.labelRes)) }
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
            composable(Destination.SEARCH.route) { SearchScreen() }
            composable(Destination.LIBRARY.route) {
                LibraryScreen(onOpenPlaylist = { id -> navController.navigate("playlist/$id") })
            }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
            composable(
                route = PLAYLIST_ROUTE,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) {
                PlaylistDetailScreen(onBack = { navController.popBackStack() })
            }
        }
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
            onStop = {
                viewModel.stop()
                playerExpanded = false
            },
            onToggleFavorite = { viewModel.toggleFavoriteCurrent() }
        )
    }
}
