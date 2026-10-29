package dev.anilbeesetti.nextplayer.feature.player.ui.image

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.ui.resolveLocalPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val ROTATION_STEP = 90
private const val SLIDESHOW_INTERVAL_MS = 3_000L

/** Full screen image viewer with zoom, rotation, sharing, deletion, slideshow and details. */
@Composable
fun ImageViewerScreen(uris: List<Uri>, onDelete: (Uri) -> Unit, onSettingsClick: () -> Unit) {
    if (uris.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black),
        )
        return
    }
    val context = LocalContext.current
    val pagerState = rememberPagerState { uris.size }
    var isZoomed by remember { mutableStateOf(value = false) }
    var chromeVisible by remember { mutableStateOf(value = true) }
    var slideshow by remember { mutableStateOf(value = false) }
    var showDetails by remember { mutableStateOf(value = false) }
    val rotations = remember { mutableStateMapOf<Uri, Int>() }

    // Zooming in must win over paging, so paging is only enabled once the image is back to fit.
    LaunchedEffect(pagerState.currentPage) { isZoomed = false }

    LaunchedEffect(
        slideshow,
        isZoomed,
        pagerState.currentPage,
        uris.size,
    ) {
        if (!slideshow || isZoomed || uris.size < 2) return@LaunchedEffect
        delay(timeMillis = SLIDESHOW_INTERVAL_MS)
        pagerState.animateScrollToPage(page = (pagerState.currentPage + 1) % uris.size)
    }

    // Deleting images can leave a single one behind, where a slideshow no longer makes sense.
    LaunchedEffect(uris.size) {
        if (uris.size < 2) slideshow = false
    }

    val currentPage = pagerState.currentPage.coerceIn(
        minimumValue = 0,
        maximumValue = uris.lastIndex,
    )
    val currentUri = uris[currentPage]

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
                rotation = rotations[uris[page]] ?: 0,
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
                index = currentPage,
                onShowDetails = { showDetails = true },
                onSettingsClick = onSettingsClick,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(alignment = Alignment.BottomCenter),
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            ImageViewerActions(
                showSlideshow = uris.size > 1,
                slideshow = slideshow,
                onShare = {
                    shareImage(
                        context = context,
                        uri = currentUri,
                    )
                },
                onRotate = {
                    rotations[currentUri] = ((rotations[currentUri] ?: 0) + ROTATION_STEP) % 360
                },
                onToggleSlideshow = { slideshow = !slideshow },
                onDelete = { onDelete(currentUri) },
            )
        }
    }

    if (showDetails) {
        ImageDetailsDialog(
            uri = currentUri,
            index = currentPage,
            count = uris.size,
            onDismiss = { showDetails = false },
        )
    }
}

@Composable
private fun ImageViewerTopBar(
    uris: List<Uri>,
    index: Int,
    onShowDetails: () -> Unit,
    onSettingsClick: () -> Unit,
) {
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
        IconButton(onClick = onShowDetails) {
            Icon(
                imageVector = NextIcons.Info,
                contentDescription = stringResource(R.string.info),
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
            text = "${index + 1} / ${uris.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = NextIcons.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ImageViewerActions(
    showSlideshow: Boolean,
    slideshow: Boolean,
    onShare: () -> Unit,
    onRotate: () -> Unit,
    onToggleSlideshow: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f),
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ImageViewerAction(
            icon = NextIcons.Share,
            label = stringResource(R.string.share),
            onClick = onShare,
        )
        ImageViewerAction(
            icon = NextIcons.Rotate90,
            label = stringResource(R.string.image_viewer_rotate),
            onClick = onRotate,
        )
        if (showSlideshow) {
            ImageViewerAction(
                icon = if (slideshow) NextIcons.Pause else NextIcons.Play,
                label = stringResource(
                    if (slideshow) {
                        R.string.image_viewer_stop_slideshow
                    } else {
                        R.string.image_viewer_start_slideshow
                    },
                ),
                onClick = onToggleSlideshow,
            )
        }
        ImageViewerAction(
            icon = NextIcons.Delete,
            label = stringResource(R.string.delete),
            onClick = onDelete,
        )
    }
}

@Composable
private fun ImageViewerAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.size(size = 26.dp),
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
        )
    }
}

@Composable
private fun ImageDetailsDialog(uri: Uri, index: Int, count: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var details by remember(uri) { mutableStateOf(value = ImageDetails()) }

    LaunchedEffect(uri) {
        details = loadImageDetails(
            context = context,
            uri = uri,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
                DetailsRow(
                    label = stringResource(R.string.image_viewer_name),
                    value = details.name,
                )
                DetailsRow(
                    label = stringResource(R.string.details_resolution),
                    value = details.dimensions(),
                )
                DetailsRow(
                    label = stringResource(R.string.image_viewer_size),
                    value = details.sizeBytes?.let {
                        Formatter.formatShortFileSize(
                            context,
                            it,
                        )
                    },
                )
                DetailsRow(
                    label = stringResource(R.string.details_path),
                    value = details.path,
                )
                DetailsRow(
                    label = stringResource(R.string.details_index),
                    value = "${index + 1} / $count",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.image_viewer_close))
            }
        },
    )
}

@Composable
private fun DetailsRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ZoomableImage(
    uri: Uri,
    rotation: Int,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    var scale by remember(uri) { mutableFloatStateOf(value = MIN_SCALE) }
    var offset by remember(uri) { mutableStateOf(value = Offset.Zero) }
    var size by remember { mutableStateOf(value = IntSize.Zero) }
    val density = LocalDensity.current
    val quarterTurn = rotation % (ROTATION_STEP * 2) != 0

    // Rotating swaps the drawing area so the turned image still fits the screen.
    val contentModifier = if (quarterTurn && size != IntSize.Zero) {
        with(density) {
            Modifier.size(
                width = size.height.toDp(),
                height = size.width.toDp(),
            )
        }
    } else {
        Modifier.fillMaxSize()
    }

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
    LaunchedEffect(rotation) {
        scale = MIN_SCALE
        offset = Offset.Zero
    }

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
                        minimumValue = MIN_SCALE,
                        maximumValue = MAX_SCALE,
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
        Box(
            modifier = contentModifier.graphicsLayer {
                rotationZ = rotation.toFloat()
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private data class ImageDetails(
    val name: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
    val path: String? = null,
) {
    fun dimensions(): String? = if (width != null && height != null) "$width × $height" else null
}

private suspend fun loadImageDetails(context: Context, uri: Uri): ImageDetails = runCatching {
    withContext(Dispatchers.IO) {
        var name: String? = null
        var size: Long? = null
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
        }
        val bounds = runCatching {
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        this,
                    )
                }
            }
        }.getOrNull()
        if (size == null) {
            size = runCatching {
                context.contentResolver.openAssetFileDescriptor(
                    uri,
                    "r",
                )?.use { it.length }
            }.getOrNull()?.takeIf { it > 0 }
        }
        ImageDetails(
            name = name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment,
            width = bounds?.outWidth?.takeIf { it > 0 },
            height = bounds?.outHeight?.takeIf { it > 0 },
            sizeBytes = size,
            path = resolveLocalPath(
                context = context,
                uri = uri,
            ),
        )
    }
}.getOrElse { ImageDetails(name = uri.lastPathSegment) }

private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(
            Intent.EXTRA_STREAM,
            uri,
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.share),
        ),
    )
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
