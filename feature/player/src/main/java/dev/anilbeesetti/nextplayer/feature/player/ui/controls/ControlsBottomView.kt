package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlaybackSpeedButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButtonBlackAlpha
import dev.anilbeesetti.nextplayer.feature.player.buttons.RotateButton
import dev.anilbeesetti.nextplayer.feature.player.extensions.drawableRes
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberChaptersState
import dev.anilbeesetti.nextplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.anilbeesetti.nextplayer.feature.player.ui.titleOrDefault
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

private const val MILLISECONDS_PER_SECOND = 1_000L

@OptIn(UnstableApi::class)
@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player?,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    videoDecoderMode: DecoderMode?,
    isPipSupported: Boolean,
    showRemainingTime: Boolean,
    onToggleTimeDisplay: () -> Unit,
    onChaptersClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val timeButtonFocusRequester = remember { FocusRequester() }
    // While the user drags the seekbar the player only reports the position after the seek is
    // applied, so the slider and the time label follow the dragged value instead.
    var scrubPositionMs by remember { mutableStateOf<Long?>(value = null) }
    Column(
        modifier = modifier
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .padding(
                bottom =
                    16.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Playback time on the left, lock in the middle and speed/rotation on the right.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                .focusGroup(),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerButton(
                    modifier = Modifier.focusRequester(focusRequester = timeButtonFocusRequester),
                    onClick = onToggleTimeDisplay,
                    containerColor = PlayerButtonBlackAlpha,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 1.dp,
                    ),
                ) {
                    val positionMs = scrubPositionMs ?: progressState.currentPositionMs
                    val wholeSeconds = positionMs / MILLISECONDS_PER_SECOND
                    val timeTextPositionMs = wholeSeconds * MILLISECONDS_PER_SECOND
                    Text(
                        text = buildString {
                            val positionText = when (showRemainingTime) {
                                true -> if (progressState.durationMs != C.TIME_UNSET) {
                                    val remainingMs = timeTextPositionMs - progressState.durationMs
                                    getStringForTime(remainingMs)
                                } else {
                                    getStringForTime(C.TIME_UNSET)
                                }

                                false -> getStringForTime(timeTextPositionMs)
                            }
                            append(positionText)
                            append(" / ")
                            append(getStringForTime(progressState.durationMs))
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (chaptersState.chapters.isNotEmpty()) {
                    PlayerButton(
                        onClick = onChaptersClick,
                        containerColor = PlayerButtonBlackAlpha,
                        contentPadding = PaddingValues(
                            vertical = 1.dp,
                            horizontal = 8.dp,
                        ).copy(end = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text =
                                    chaptersState.chapters
                                        .getOrNull(chaptersState.currentChapterIndex)
                                        ?.titleOrDefault(chaptersState.currentChapterIndex)
                                        ?: stringResource(R.string.chapters),
                                modifier = Modifier.weight(
                                    weight = 1f,
                                    fill = false,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            PlayerButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = onLockControlsClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_open),
                    contentDescription = null,
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackSpeedButton(
                    player = player,
                    onClick = onPlaybackSpeedClick,
                )

                if (!isTv) {
                    RotateButton()
                }
            }
        }
        PlayerSeekbar(
            position = (scrubPositionMs ?: progressState.currentPositionMs).toFloat(),
            duration = progressState.durationMs.toFloat(),
            chapters = chaptersState.chapters,
            onSeek = { value ->
                scrubPositionMs = value.toLong()
                onSeek(value.toLong())
            },
            onSeekFinished = {
                scrubPositionMs = null
                onSeekEnd()
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(weight = 1f)
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    alignment = controlsAlignment,
                ),
            ) {
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = null,
                    )
                }
                if (isPipSupported) {
                    PlayerButton(onClick = onPictureInPictureClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pip),
                            contentDescription = null,
                        )
                    }
                }
                PlayerButton(onClick = onDecoderClick) {
                    Text(
                        text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
                PlayerButton(onClick = onAudioClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_audio_track),
                        contentDescription = null,
                    )
                }
                PlayerButton(onClick = onSubtitleClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitle_track),
                        contentDescription = null,
                    )
                }
            }
            PlayerButton(onClick = onPlaylistClick) {
                Icon(
                    imageVector = NextIcons.Playlist,
                    contentDescription = stringResource(R.string.playlist),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsBottomViewPreview() {
    val player = rememberPreviewPlayer()
    val progressState = rememberProgressStateWithTickInterval(player = player)
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsBottomView(
                player = player,
                progressState = progressState,
                chaptersState = rememberChaptersState(
                    player = player,
                    progressState = progressState,
                ),
                controlsAlignment = Alignment.Start,
                videoContentScale = VideoContentScale.BEST_FIT,
                videoDecoderMode = DecoderMode.HARDWARE,
                isPipSupported = true,
                showRemainingTime = false,
                onToggleTimeDisplay = {},
                onChaptersClick = {},
                onVideoContentScaleClick = {},
                onVideoContentScaleLongClick = {},
                onLockControlsClick = {},
                onPictureInPictureClick = {},
                onPlaybackSpeedClick = {},
                onDecoderClick = {},
                onAudioClick = {},
                onSubtitleClick = {},
                onPlaylistClick = {},
                onSeek = {},
                onSeekEnd = {},
            )
        }
    }
}
