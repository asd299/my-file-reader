package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton

private const val END_TOLERANCE_MS = 800L

/**
 * The player is paused at the end of the queue, either because it reached STATE_ENDED or because
 * [Player.getPauseAtEndOfMediaItems] paused it on the last frame (which keeps STATE_READY).
 */
private fun Player.isAtEndOfPlayback(): Boolean {
    if (playbackState == Player.STATE_ENDED) return true
    if (playWhenReady || hasNextMediaItem()) return false
    if (playbackState != Player.STATE_READY) return false
    val duration = duration
    return duration > 0 &&
        currentPosition >= duration - END_TOLERANCE_MS
}

/** Tracks whether playback reached the end of the last media item. */
@Composable
fun rememberIsPlaybackEnded(player: Player?): Boolean {
    var isEnded by remember(player) {
        mutableStateOf(value = player?.isAtEndOfPlayback() == true)
    }
    DisposableEffect(key1 = player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isEnded = player?.isAtEndOfPlayback() == true
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                val pausedAtEnd = !playWhenReady &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
                if (pausedAtEnd) {
                    // Pausing at the end of the item keeps STATE_READY, so the state alone is not enough.
                    isEnded = true
                } else if (playWhenReady) {
                    isEnded = false
                } else {
                    isEnded = player?.isAtEndOfPlayback() == true
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isEnded = false
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (player?.playWhenReady != true) {
                    isEnded = player?.isAtEndOfPlayback() == true
                }
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
        contentPadding = PaddingValues(12.dp),
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_replay),
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
        )
    }
}
