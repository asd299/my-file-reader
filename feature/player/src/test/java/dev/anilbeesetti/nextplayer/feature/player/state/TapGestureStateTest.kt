package dev.anilbeesetti.nextplayer.feature.player.state

import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.anilbeesetti.nextplayer.core.model.DoubleTapGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TapGestureStateTest {
    @Test
    fun slidingUpIncreasesLongPressSpeedAndReleaseRestoresPlaybackSpeed() {
        val player = TestPlayer()
        val state = createState(player = player)

        state.handleLongPress(offset = Offset.Zero)
        assertTrue(state.isLongPressGestureInAction)
        assertEquals(
            2.0f,
            player.playbackParameters.speed,
            DELTA,
        )

        state.handleLongPressSpeedDrag(dragAmountDp = -48f)
        assertEquals(
            2.5f,
            state.currentLongPressSpeed,
            DELTA,
        )
        assertEquals(
            2.5f,
            player.playbackParameters.speed,
            DELTA,
        )

        state.handleLongPressSpeedDrag(dragAmountDp = 120f)
        assertEquals(
            1.25f,
            state.currentLongPressSpeed,
            DELTA,
        )
        assertEquals(
            1.25f,
            player.playbackParameters.speed,
            DELTA,
        )

        state.handleOnLongPressRelease()
        assertFalse(state.isLongPressGestureInAction)
        assertEquals(
            1.0f,
            player.playbackParameters.speed,
            DELTA,
        )
    }

    @Test
    fun longPressSpeedIsClampedToSupportedRange() {
        val player = TestPlayer()
        val state = createState(player = player)

        state.handleLongPress(offset = Offset.Zero)
        state.handleLongPressSpeedDrag(dragAmountDp = -2_400f)
        assertEquals(
            8.0f,
            state.currentLongPressSpeed,
            DELTA,
        )

        state.handleLongPressSpeedDrag(dragAmountDp = 4_800f)
        assertEquals(
            0.25f,
            state.currentLongPressSpeed,
            DELTA,
        )
    }

    @Test
    fun dragDoesNotChangeSpeedWithoutActiveLongPress() {
        val player = TestPlayer()
        val state = createState(player = player)

        state.handleLongPressSpeedDrag(dragAmountDp = -48f)

        assertFalse(state.isLongPressGestureInAction)
        assertEquals(
            1.0f,
            player.playbackParameters.speed,
            DELTA,
        )
        assertEquals(
            2.0f,
            state.currentLongPressSpeed,
            DELTA,
        )
    }

    private fun createState(player: Player) = TapGestureState(
        player = player,
        seekIncrementMillis = 10_000,
        useLongPressGesture = true,
        coroutineScope = CoroutineScope(context = Dispatchers.Unconfined),
        longPressSpeed = 2.0f,
        doubleTapGesture = DoubleTapGesture.NONE,
    )

    private class TestPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
        private var state = State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder().addAll(
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_SET_SPEED_AND_PITCH,
                ).build(),
            )
            .setPlaylist(
                listOf(
                    MediaItemData.Builder("video").setDurationUs(60_000_000).build(),
                ),
            )
            .setPlayWhenReady(
                true,
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
            )
            .setPlaybackState(Player.STATE_READY)
            .build()

        override fun getState(): State = state

        override fun handleSetPlaybackParameters(
            playbackParameters: PlaybackParameters,
        ): ListenableFuture<*> {
            state = state.buildUpon().setPlaybackParameters(playbackParameters).build()
            return Futures.immediateVoidFuture()
        }
    }

    private companion object {
        private const val DELTA = 0.001f
    }
}
