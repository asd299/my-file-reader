package dev.anilbeesetti.nextplayer.feature.player.state

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import dev.anilbeesetti.nextplayer.feature.player.extensions.brightnessPercentage
import dev.anilbeesetti.nextplayer.feature.player.extensions.currentBrightness
import kotlin.math.roundToInt

@Composable
fun rememberBrightnessState(): BrightnessState {
    val activity = LocalActivity.current
    val brightnessState = remember { BrightnessState(activity = activity as PlayerActivity) }
    DisposableEffect(key1 = activity) {
        brightnessState.handleListeners(disposableEffectScope = this)
    }
    return brightnessState
}

@Stable
class BrightnessState(private val activity: PlayerActivity) {
    val maxBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    var currentBrightness: Float by mutableFloatStateOf(activity.currentBrightness)
        private set

    var brightnessPercentage: Int by mutableIntStateOf(activity.brightnessPercentage)
        private set

    /** Applies [percentage] of the full brightness range, keeping the value continuous. */
    fun updateBrightnessPercentage(percentage: Float) {
        setBrightness(
            brightness =
                percentage.coerceIn(
                    minimumValue = 0f,
                    maximumValue = PERCENTAGE_MAX,
                ) / PERCENTAGE_MAX * maxBrightness,
        )
    }

    fun setBrightness(brightness: Float) {
        val clampedBrightness = brightness.coerceIn(
            minimumValue = 0f,
            maximumValue = maxBrightness,
        )
        val windowAttributes = activity.window.attributes
        windowAttributes.screenBrightness = clampedBrightness
        activity.window.attributes = windowAttributes
        currentBrightness = clampedBrightness
        brightnessPercentage = (clampedBrightness * PERCENTAGE_MAX).roundToInt()
    }

    /**
     * Pins the window brightness to the level currently shown on screen so that the brightness
     * gesture starts from the real value instead of the unspecified window default.
     */
    fun applyCurrentBrightness() {
        setBrightness(brightness = activity.currentBrightness)
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult =
        with(receiver = disposableEffectScope) {
            val windowAttributesChangedListener: Consumer<WindowManager.LayoutParams?> = Consumer {
                currentBrightness = activity.currentBrightness
                brightnessPercentage = activity.brightnessPercentage
            }
            activity.addOnWindowAttributesChangedListener(
                listener = windowAttributesChangedListener,
            )

            onDispose {
                activity.removeOnWindowAttributesChangedListener(
                    listener = windowAttributesChangedListener,
                )
            }
        }

    private companion object {
        private const val PERCENTAGE_MAX = 100f
    }
}
