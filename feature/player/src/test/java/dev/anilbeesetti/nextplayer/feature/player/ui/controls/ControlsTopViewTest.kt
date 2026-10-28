package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ControlsTopViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun enteringGroupFocusesDecoderInsteadOfARemovedBackButton() {
        val groupFocusRequester = FocusRequester()
        composeRule.setContent {
            NextPlayerTheme {
                ControlsTopView(
                    modifier = Modifier.focusRequester(focusRequester = groupFocusRequester),
                    title = "Video",
                    videoDecoderMode = null,
                )
            }
        }
        val decoder = composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.select_decoders),
        )

        composeRule.runOnIdle { groupFocusRequester.requestFocus() }

        decoder.assertIsFocused()
    }
}
