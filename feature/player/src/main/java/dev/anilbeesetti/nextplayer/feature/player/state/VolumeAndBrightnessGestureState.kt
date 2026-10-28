package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVolumeAndBrightnessGestureState(
    volumeState: VolumeState,
    brightnessState: BrightnessState,
    enableVolumeGesture: Boolean,
    enableBrightnessGesture: Boolean,
    volumeGestureSensitivity: Float,
    brightnessGestureSensitivity: Float,
): VolumeAndBrightnessGestureState {
    val coroutineScope = rememberCoroutineScope()
    val volumeAndBrightnessGestureState = remember(
        volumeState,
        brightnessState,
        enableVolumeGesture,
        enableBrightnessGesture,
        volumeGestureSensitivity,
        brightnessGestureSensitivity,
    ) {
        VolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = enableVolumeGesture,
            enableBrightnessGesture = enableBrightnessGesture,
            volumeGestureSensitivity = volumeGestureSensitivity,
            brightnessGestureSensitivity = brightnessGestureSensitivity,
            coroutineScope = coroutineScope,
        )
    }
    return volumeAndBrightnessGestureState
}

@Stable
class VolumeAndBrightnessGestureState(
    private val volumeState: VolumeState,
    private val brightnessState: BrightnessState,
    private val enableVolumeGesture: Boolean = true,
    private val enableBrightnessGesture: Boolean = true,
    private val volumeGestureSensitivity: Float,
    private val brightnessGestureSensitivity: Float,
    private val coroutineScope: CoroutineScope,
) {
    var activeGesture: VerticalGesture? by mutableStateOf(value = null)
        private set

    /** Continuous volume level in percent while the volume gesture is active. */
    var volumePercentage: Float by mutableFloatStateOf(value = 0f)
        private set

    /** Continuous brightness level in percent while the brightness gesture is active. */
    var brightnessPercentage: Float by mutableFloatStateOf(value = 0f)
        private set

    private var startingY = 0f
    private var startVolumePercentage = 0f
    private var startBrightnessPercentage = 0f
    private var job: Job? = null

    fun onDragStart(offset: Offset, size: IntSize) {
        val viewCenterX = size.width / 2
        job?.cancel()
        activeGesture = when {
            offset.x < viewCenterX -> VerticalGesture.BRIGHTNESS.takeIf { enableBrightnessGesture }
            else -> VerticalGesture.VOLUME.takeIf { enableVolumeGesture }
        }
        startingY = offset.y
        // Only sync the state the current gesture controls: pinning brightness while adjusting
        // volume made the screen jump to the brightness reported by the system.
        when (activeGesture) {
            VerticalGesture.VOLUME -> volumeState.syncWithSystem()
            VerticalGesture.BRIGHTNESS -> brightnessState.applyCurrentBrightness()
            null -> Unit
        }
        startVolumePercentage = volumeState.preciseVolumePercentage
        startBrightnessPercentage = brightnessState.brightnessPercentage.toFloat()
        volumePercentage = startVolumePercentage
        brightnessPercentage = startBrightnessPercentage
    }

    fun onDrag(change: PointerInputChange) {
        val activeGesture = activeGesture ?: return
        if (change.isConsumed) return

        val dragDistance = startingY - change.position.y

        when (activeGesture) {
            VerticalGesture.VOLUME -> {
                val newVolume = (
                    startVolumePercentage +
                        dragDistance * (volumeGestureSensitivity / SENSITIVITY_DIVISOR)
                    ).coerceIn(
                    minimumValue = 0f,
                    maximumValue = volumeState.maxVolumePercentage.toFloat(),
                )
                volumePercentage = newVolume
                volumeState.updateVolumePercentage(percentage = newVolume)
            }

            VerticalGesture.BRIGHTNESS -> {
                val newBrightness = (
                    startBrightnessPercentage +
                        dragDistance * (brightnessGestureSensitivity / SENSITIVITY_DIVISOR)
                    ).coerceIn(
                    minimumValue = 0f,
                    maximumValue = MAX_BRIGHTNESS_PERCENTAGE,
                )
                brightnessPercentage = newBrightness
                brightnessState.updateBrightnessPercentage(percentage = newBrightness)
            }
        }
    }

    fun onDragEnd() {
        startingY = 0f
        startVolumePercentage = 0f
        startBrightnessPercentage = 0f

        job?.cancel()
        job = coroutineScope.launch {
            delay(1.seconds)
            activeGesture = null
        }
    }

    companion object {
        private const val SENSITIVITY_DIVISOR = 10f
        private const val MAX_BRIGHTNESS_PERCENTAGE = 100f
    }
}

enum class VerticalGesture {
    VOLUME,
    BRIGHTNESS,
}
