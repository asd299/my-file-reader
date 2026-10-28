package dev.anilbeesetti.nextplayer.feature.player.extensions

import android.app.Activity
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SystemBrightnessTest {

    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get()

    @Test
    fun aospBrightnessScaleIsNormalizedToWindowRange() {
        Settings.System.putFloat(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            51f,
        )

        assertEquals(
            0.2f,
            activity.systemBrightness,
            TOLERANCE,
        )
    }

    @Test
    fun widerOemScaleIsNotClampedToFullBrightness() {
        Settings.System.putFloat(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            2_040f,
        )

        val brightness = activity.systemBrightness

        assertTrue(
            "expected brightness in 0..1 but below 1, was $brightness",
            brightness in 0f..1f && brightness < 1f,
        )
    }

    @Test
    fun windowOverrideTakesPrecedenceOverSystemSetting() {
        Settings.System.putFloat(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            51f,
        )
        val attributes = activity.window.attributes
        attributes.screenBrightness = 0.3f
        activity.window.attributes = attributes

        assertEquals(
            0.3f,
            activity.currentBrightness,
            TOLERANCE,
        )
    }

    private companion object {
        private const val TOLERANCE = 0.01f
    }
}
