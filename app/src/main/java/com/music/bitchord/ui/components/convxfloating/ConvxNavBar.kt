package com.music.bitchord.ui.components.convxfloating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.MiniPlayer
import com.music.bitchord.ui.components.MiniPlayerPull
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.player.PlayerDock
import dev.chrisbanes.haze.HazeState

/**
 * The Convx floating navigation bar from the supplied sources, adapted to
 * Kisetsu's tabs. The mini player is a separate capsule above the bar, in the
 * same column, so it never sits underneath it.
 */
@Composable
fun ConvxNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollConnection: com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    animationsActive: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean,
    onBlockedControl: () -> Unit,
    dock: PlayerDock?,
    pull: MiniPlayerPull?,
    autoMixArtwork: (@Composable (String?, Modifier) -> Unit)?,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val routes = listOf(Screens.Home, Screens.Explore, Screens.Radio, Screens.Library, Screens.Search)
    val navigation = tabs.mapIndexed { index, _ -> routes.getOrElse(index) { Screens.Library } }
    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        if (song != null) {
            MiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                hazeState = hazeState,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onExpand = onExpand,
                controlsLocked = controlsLocked,
                onBlockedControl = onBlockedControl,
                dock = dock,
                pull = pull,
                animationsActive = animationsActive,
                autoMixArtwork = autoMixArtwork,
                useGlass = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val convxScroll = rememberFloatingTabBarScrollConnection()
        AppFloatingNavBar(
            navigationItems = navigation,
            currentRoute = navigation.getOrNull(selectedIndex)?.route,
            onItemClick = { screen, _ ->
                val index = navigation.indexOfFirst { it.route == screen.route }
                if (index >= 0) onTabSelected(index)
            },
            scrollConnection = convxScroll,
            showPlayerAccessory = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
