package dev.anilbeesetti.nextplayer.feature.player.extensions

import android.app.Activity
import android.provider.Settings
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

/**
* Must call this function after any configuration done to activity to keep system bars behaviour
*/
fun Activity.swipeToShowStatusBars() {
    WindowCompat.getInsetsController(
        window,
        window.decorView,
    ).systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

/**
 * Toggles system bars visibility
 * @param showBars true to show system bars, false to hide
 * @param types [Type.InsetsType] system bars to toggle default is all system bars
 */
fun Activity.toggleSystemBars(showBars: Boolean, @Type.InsetsType types: Int = Type.systemBars()) {
    WindowCompat.getInsetsController(
        window,
        window.decorView,
    ).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (showBars) show(types) else hide(types)
    }
}

val Activity.currentBrightness: Float
    get() = when (val brightness = window.attributes.screenBrightness) {
        in BRIGHTNESS_OVERRIDE_OFF..BRIGHTNESS_OVERRIDE_FULL ->
            brightness

        else -> systemBrightness
    }

/**
 * Brightness currently reported by the system, normalized to the 0..1 window range.
 * Falls back to [DEFAULT_BRIGHTNESS] when the setting is not available.
 */
val Activity.systemBrightness: Float
    get() {
        val systemValue = try {
            Settings.System.getFloat(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
            )
        } catch (_: Settings.SettingNotFoundException) {
            DEFAULT_BRIGHTNESS * SYSTEM_BRIGHTNESS_MAX
        }
        return (systemValue / SYSTEM_BRIGHTNESS_MAX).coerceIn(
            minimumValue = BRIGHTNESS_OVERRIDE_OFF,
            maximumValue = BRIGHTNESS_OVERRIDE_FULL,
        )
    }

val Activity.brightnessPercentage: Int
    get() = (
        currentBrightness.coerceIn(
            0f,
            BRIGHTNESS_OVERRIDE_FULL,
        ) * 100
        ).roundToInt()

private const val SYSTEM_BRIGHTNESS_MAX = 255f

private const val DEFAULT_BRIGHTNESS = 0.5f
