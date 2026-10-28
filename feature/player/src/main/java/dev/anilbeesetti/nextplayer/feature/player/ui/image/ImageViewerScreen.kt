package dev.anilbeesetti.nextplayer.feature.player.ui.image

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Full screen image viewer with pinch to zoom, panning and swiping between shared images. */
@Composable
fun ImageViewerScreen(uris: List<Uri>, onClose: () -> Unit) {
    val pagerState = rememberPagerState { uris.size }
    var isZoomed by remember { mutableStateOf(value = false) }
    var chromeVisible by remember { mutableStateOf(value = true) }

    // Zooming in must win over paging, so paging is only enabled once the image is back to fit.
    LaunchedEffect(pagerState.currentPage) { isZoomed = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed,
        ) { page ->
            ZoomableImage(
                uri = uris[page],
                onZoomChanged = { isZoomed = it },
                onTap = { chromeVisible = !chromeVisible },
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(alignment = Alignment.TopCenter),
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ImageViewerTopBar(
                uris = uris,
                index = pagerState.currentPage,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun ImageViewerTopBar(uris: List<Uri>, index: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val title = rememberDisplayName(
        context = context,
        uri = uris[index],
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = NextIcons.Close,
                contentDescription = stringResource(R.string.image_viewer_close),
                tint = Color.White,
            )
        }
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(end = 12.dp),
            text = "${index + 1} / ${uris.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ZoomableImage(uri: Uri, onZoomChanged: (Boolean) -> Unit, onTap: () -> Unit) {
    var scale by remember(uri) { mutableFloatStateOf(value = MIN_SCALE) }
    var offset by remember(uri) { mutableStateOf(value = Offset.Zero) }
    var size by remember { mutableStateOf(value = IntSize.Zero) }

    fun clamp(newOffset: Offset, newScale: Float): Offset {
        val maxX = ((newScale - MIN_SCALE) * size.width / 2f).coerceAtLeast(minimumValue = 0f)
        val maxY = ((newScale - MIN_SCALE) * size.height / 2f).coerceAtLeast(minimumValue = 0f)
        return Offset(
            x = newOffset.x.coerceIn(
                minimumValue = -maxX,
                maximumValue = maxX,
            ),
            y = newOffset.y.coerceIn(
                minimumValue = -maxY,
                maximumValue = maxY,
            ),
        )
    }

    LaunchedEffect(scale) { onZoomChanged(scale > MIN_SCALE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        val newScale = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                        scale = newScale
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(
                        MIN_SCALE,
                        MAX_SCALE,
                    )
                    offset = if (newScale == MIN_SCALE) {
                        Offset.Zero
                    } else {
                        clamp(
                            newOffset = offset + pan,
                            newScale = newScale,
                        )
                    }
                    scale = newScale
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

@Composable
private fun rememberDisplayName(context: Context, uri: Uri): String {
    var name by remember(uri) {
        mutableStateOf(value = uri.lastPathSegment.orEmpty())
    }
    LaunchedEffect(uri) {
        name = withContext(Dispatchers.IO) {
            queryDisplayName(
                context = context,
                uri = uri,
            )
        }
            ?: name
    }
    return name
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver
        .query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}.getOrNull()?.takeIf { it.isNotBlank() }
