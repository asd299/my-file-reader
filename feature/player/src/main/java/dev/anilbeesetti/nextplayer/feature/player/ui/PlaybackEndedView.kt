package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton

/** Tracks whether playback reached the end of the last media item. */
@Composable
fun rememberIsPlaybackEnded(player: Player?): Boolean {
    var isEnded by remember(player) {
        mutableStateOf(value = player?.playbackState == Player.STATE_ENDED)
    }
    DisposableEffect(key1 = player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isEnded = playbackState == Player.STATE_ENDED
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    return isEnded
}

/** Floating replay action shown in the center of the player once playback has finished. */
@Composable
fun PlaybackEndedReplayButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val contentDescription = stringResource(id = R.string.replay_video)
    PlayerButton(
        modifier = modifier.size(72.dp),
        containerColor = Color.Black.copy(alpha = 0.5f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_replay),
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
        )
    }
}
