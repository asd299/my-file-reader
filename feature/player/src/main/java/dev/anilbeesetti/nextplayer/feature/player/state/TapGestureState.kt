package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.common.extensions.round
import dev.anilbeesetti.nextplayer.core.model.DoubleTapGesture
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberTapGestureState(
    player: Player,
    doubleTapGesture: DoubleTapGesture,
    seekIncrementMillis: Long,
    useLongPressGesture: Boolean,
    longPressSpeed: Float,
): TapGestureState {
    val coroutineScope = rememberCoroutineScope()
    val tapGestureState = remember(
        player,
        doubleTapGesture,
        seekIncrementMillis,
        useLongPressGesture,
        longPressSpeed,
    ) {
        TapGestureState(
            player = player,
            doubleTapGesture = doubleTapGesture,
            seekIncrementMillis = seekIncrementMillis,
            useLongPressGesture = useLongPressGesture,
            longPressSpeed = longPressSpeed,
            coroutineScope = coroutineScope,
        )
    }
    return tapGestureState
}

@Stable
class TapGestureState(
    private val player: Player,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 2.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    var seekMillis by mutableLongStateOf(value = 0L)
    var isLongPressGestureInAction by mutableStateOf(value = false)
    var currentLongPressSpeed by mutableFloatStateOf(value = longPressSpeed)
        private set

    private var resetJob: Job? = null
    private var currentSpeed: Float = player.playbackParameters.speed
    private var longPressSpeedDragDp: Float = 0f

    fun handleDoubleTap(offset: Offset, size: IntSize) {
        if (!player.isCurrentMediaItemSeekable) return

        val action = when (doubleTapGesture) {
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                val viewCenterX = size.width / 2
                when {
                    offset.x < viewCenterX -> DoubleTapAction.SEEK_BACKWARD
                    else -> DoubleTapAction.SEEK_FORWARD
                }
            }

            DoubleTapGesture.BOTH -> {
                val eventPositionX = offset.x / size.width
                when {
                    eventPositionX < 0.35 -> DoubleTapAction.SEEK_BACKWARD
                    eventPositionX > 0.65 -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }
            }

            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE

            DoubleTapGesture.NONE -> return
        }

        when (action) {
            DoubleTapAction.SEEK_BACKWARD -> {
                player.seekTo(player.currentPosition - seekIncrementMillis)
                if (seekMillis > 0L) {
                    seekMillis = 0L
                }
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }

            DoubleTapAction.SEEK_FORWARD -> {
                player.seekTo(player.currentPosition + seekIncrementMillis)
                if (seekMillis < 0L) {
                    seekMillis = 0L
                }
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }

            DoubleTapAction.PLAY_PAUSE -> {
                when (player.isPlaying) {
                    true -> player.pause()
                    false -> player.play()
                }
            }
        }
        resetDoubleTapSeekState()
    }

    /**
     * Starts the temporary long-press playback speed, which can be adjusted by vertical dragging.
     */
    fun handleLongPress(offset: Offset) {
        if (!useLongPressGesture) return

        isLongPressGestureInAction = true
        currentSpeed = player.playbackParameters.speed
        currentLongPressSpeed = longPressSpeed
        longPressSpeedDragDp = 0f
        player.setPlaybackSpeed(longPressSpeed)
    }

    /**
     * Adjusts the temporary long-press playback speed while the finger slides vertically.
     * Sliding up increases the speed, sliding down decreases it.
     */
    fun handleLongPressSpeedDrag(dragAmountDp: Float) {
        if (!isLongPressGestureInAction) return

        longPressSpeedDragDp += dragAmountDp
        val speedSteps = (longPressSpeedDragDp / DRAG_DP_PER_SPEED_STEP).toInt()
        val newSpeed = (longPressSpeed - speedSteps * SPEED_STEP)
            .coerceIn(
                minimumValue = MIN_LONG_PRESS_SPEED,
                maximumValue = MAX_LONG_PRESS_SPEED,
            )
            .round(decimalPlaces = 2)

        if (newSpeed == currentLongPressSpeed) return

        currentLongPressSpeed = newSpeed
        player.setPlaybackSpeed(newSpeed)
    }

    fun handleOnLongPressRelease() {
        if (isLongPressGestureInAction) {
            isLongPressGestureInAction = false
            longPressSpeedDragDp = 0f
            player.setPlaybackSpeed(currentSpeed)
        }
    }

    private fun resetDoubleTapSeekState() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            delay(750.milliseconds)
            seekMillis = 0L
        }
    }

    companion object {
        private const val SPEED_STEP = 0.25f
        private const val DRAG_DP_PER_SPEED_STEP = 24f
        private const val MIN_LONG_PRESS_SPEED = 0.25f
        private const val MAX_LONG_PRESS_SPEED = 8.0f
    }
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}
