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
        // Newer Android versions keep a 0..1 copy of the level under a separate key.
        val floatValue = Settings.System.getFloat(
            contentResolver,
            SETTING_SCREEN_BRIGHTNESS_FLOAT,
            INVALID_BRIGHTNESS,
        )
        if (floatValue >= BRIGHTNESS_OVERRIDE_OFF && floatValue <= BRIGHTNESS_OVERRIDE_FULL) {
            return floatValue
        }

        val systemValue = try {
            Settings.System.getFloat(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
            )
        } catch (_: Settings.SettingNotFoundException) {
            return DEFAULT_BRIGHTNESS
        }
        return (systemValue / brightnessScaleFor(rawValue = systemValue)).coerceIn(
            minimumValue = BRIGHTNESS_OVERRIDE_OFF,
            maximumValue = BRIGHTNESS_OVERRIDE_FULL,
        )
    }

/**
 * AOSP stores [Settings.System.SCREEN_BRIGHTNESS] on a 0..255 scale, but several ROMs use a
 * wider one. Dividing those values by 255 clamps them to 1.0, which made every brightness
 * gesture start at 100%, so pick the smallest 2^n - 1 scale that still fits the raw value.
 */
private fun brightnessScaleFor(rawValue: Float): Float {
    var scale = SYSTEM_BRIGHTNESS_MAX
    while (scale < rawValue && scale < WIDEST_SYSTEM_BRIGHTNESS) {
        scale = scale * 2f + 1f
    }
    return scale
}

val Activity.brightnessPercentage: Int
    get() = (
        currentBrightness.coerceIn(
            0f,
            BRIGHTNESS_OVERRIDE_FULL,
        ) * 100
        ).roundToInt()

private const val SETTING_SCREEN_BRIGHTNESS_FLOAT = "screen_brightness_float"

private const val INVALID_BRIGHTNESS = -1f

private const val SYSTEM_BRIGHTNESS_MAX = 255f

private const val WIDEST_SYSTEM_BRIGHTNESS = 65535f

private const val DEFAULT_BRIGHTNESS = 0.5f
