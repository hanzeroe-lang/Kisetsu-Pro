package com.music.bitchord.ui.components.convxnav

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.MiniPlayer
import com.music.bitchord.ui.components.MiniPlayerPull
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.player.PlayerDock
import dev.chrisbanes.haze.HazeState

/**
 * The Convx iOS-style floating tab bar, vendored verbatim as [IosTabBar] and
 * adapted to Kisetsu's tabs. Standard destinations share the glass capsule;
 * Search is its own trailing circle; the mini player floats above as the
 * accessory capsule.
 */
@Composable
fun ConvxNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
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
    val items = tabs.mapIndexed { index, tab ->
        IosTab(
            title = tab.label,
            icon = { tint ->
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            },
            role = if (tab.compact) IosTabRole.Search else IosTabRole.Standard,
        )
    }
    IosTabBar(
        tabs = items,
        selectedIndex = selectedIndex,
        onSelect = onTabSelected,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        state = rememberIosTabBarState(IosTabBarMinimizeBehavior.Never),
        accessory = if (song != null) {
            {
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
                    useGlass = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else null,
    )
}
