package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VerticalGesture
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
    ),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Shows the cumulative amount skipped by repeated D-pad left/right seeks while the controls are
 * hidden, along with the resulting position. Fades out shortly after the last seek.
 */
@Composable
internal fun BoxScope.DpadSeekIndicator(visible: Boolean, offsetMs: Long, positionMs: Long) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.6f),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(degrees = if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}${abs(
                            n = offsetMs,
                        ).milliseconds.inWholeSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerGestureIndicators(tapGestureState: TapGestureState) {
    DoubleTapIndicator(tapGestureState = tapGestureState)

    AnimatedVisibility(
        modifier = Modifier
            .padding(top = 24.dp)
            .align(Alignment.TopCenter),
        visible = tapGestureState.isLongPressGestureInAction,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(shape = CircleShape) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                Text(
                    text = stringResource(
                        coreUiR.string.fast_playback_speed,
                        tapGestureState.currentLongPressSpeed,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.PlayerVerticalGestureIndicators(
    activeGesture: VerticalGesture?,
    volumePercentage: Int,
    maxVolumePercentage: Int,
    brightnessPercentage: Int,
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(
                systemBarsPadding.copy(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
            )
            .padding(24.dp),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterStart),
            visible = activeGesture == VerticalGesture.VOLUME,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VerticalProgressView(
                value = volumePercentage,
                maxValue = maxVolumePercentage,
                icon = painterResource(coreUiR.drawable.ic_volume),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = activeGesture == VerticalGesture.BRIGHTNESS,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VerticalProgressView(
                value = brightnessPercentage,
                icon = painterResource(coreUiR.drawable.ic_brightness),
            )
        }
    }
}

/**
 * Shows the seek gesture on a timeline of the whole video: what is being skipped, where playback
 * will land and how far that is through the video.
 */
@Composable
internal fun SeekGestureIndicator(positionMs: Long, durationMs: Long, seekAmountMs: Long) {
    val totalMs = durationMs.takeIf { it > 0 }
    val progress = if (totalMs != null) {
        (positionMs.toFloat() / totalMs).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        )
    } else {
        0f
    }
    val sign = if (seekAmountMs < 0) "-" else "+"

    Surface(
        modifier = Modifier.widthIn(
            min = 240.dp,
            max = 380.dp,
        ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.75f),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = sign + abs(n = seekAmountMs).milliseconds.formatted(),
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Text(
                    text = positionMs.coerceAtLeast(minimumValue = 0L).milliseconds.formatted() +
                        " / " +
                        (totalMs?.milliseconds?.formatted() ?: "--:--"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 16.dp),
            ) {
                val strokeWidth = 6.dp.toPx()
                val edgeInset = strokeWidth / 2 + 2.dp.toPx()
                val centerY = size.height / 2
                val startX = edgeInset
                val endX = size.width - edgeInset
                val thumbX = startX + (endX - startX) * progress
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(
                        x = startX,
                        y = centerY,
                    ),
                    end = Offset(
                        x = endX,
                        y = centerY,
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                if (progress > 0f) {
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            x = startX,
                            y = centerY,
                        ),
                        end = Offset(
                            x = thumbX,
                            y = centerY,
                        ),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(
                        x = thumbX,
                        y = centerY,
                    ),
                )
            }
        }
    }
}
