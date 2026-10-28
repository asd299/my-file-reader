package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import dev.anilbeesetti.nextplayer.core.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BITS_PER_KBIT = 1_000
private const val MILLIHERTZ_PER_HERTZ = 1_000

/** Read only overview of the currently playing media, shown at the right of the controls. */
@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MediaDetailsView(show: Boolean, player: Player, title: String) {
    var currentTracks by remember(player) { mutableStateOf(value = player.currentTracks) }
    DisposableEffect(key1 = player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    val videoFormat = currentTracks.selectedFormat(type = C.TRACK_TYPE_VIDEO)
    val audioFormat = currentTracks.selectedFormat(type = C.TRACK_TYPE_AUDIO)
    val bitrate = videoFormat?.bitrate?.takeIf { it > 0 } ?: audioFormat?.bitrate

    OverlayView(
        show = show,
        title = stringResource(R.string.media_details),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailsRow(
                label = stringResource(R.string.details_title),
                value = title.takeIf { it.isNotBlank() } ?: player.mediaMetadata.title?.toString(),
            )
            DetailsRow(
                label = stringResource(R.string.details_duration),
                value = getStringForTime(player.duration),
            )
            videoFormat?.let { format ->
                if (format.width > 0 && format.height > 0) {
                    DetailsRow(
                        label = stringResource(R.string.details_resolution),
                        value = "${format.width} × ${format.height}",
                    )
                }
                if (format.frameRate > 0) {
                    DetailsRow(
                        label = stringResource(R.string.details_frame_rate),
                        value = format.frameRate.toInt().toString() + " fps",
                    )
                }
                format.sampleMimeType?.let { mimeType ->
                    DetailsRow(
                        label = stringResource(R.string.details_video_codec),
                        value = mimeType,
                    )
                }
            }
            audioFormat?.let { format ->
                format.sampleMimeType?.let { mimeType ->
                    DetailsRow(
                        label = stringResource(R.string.details_audio_codec),
                        value = mimeType,
                    )
                }
                if (format.channelCount > 0) {
                    DetailsRow(
                        label = stringResource(R.string.details_channels),
                        value = format.channelCount.toString(),
                    )
                }
                if (format.sampleRate > 0) {
                    DetailsRow(
                        label = stringResource(R.string.details_sample_rate),
                        value = (format.sampleRate / MILLIHERTZ_PER_HERTZ).toString() + " kHz",
                    )
                }
            }
            if (bitrate != null) {
                DetailsRow(
                    label = stringResource(R.string.details_bitrate),
                    value = (bitrate / BITS_PER_KBIT).toString() + " kbps",
                )
            }
            DetailsRow(
                label = stringResource(R.string.details_path),
                value = rememberLocalFilePath(
                    uri = player.currentMediaItem?.localConfiguration?.uri,
                ),
            )
        }
    }
}

/**
 * Content URIs handed over by file managers and gallery apps do not contain a file system path,
 * so ask the provider for it; fall back to the URI itself when it is not resolvable.
 */
@Composable
private fun rememberLocalFilePath(uri: Uri?): String? {
    val context = LocalContext.current
    var path by remember(uri) {
        mutableStateOf(value = uri?.let(::rawPathOf) ?: uri?.toString())
    }
    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        path = withContext(Dispatchers.IO) {
            resolveLocalPath(
                context,
                uri,
            ) ?: rawPathOf(uri) ?: uri.toString()
        }
    }
    return path
}

private fun rawPathOf(uri: Uri): String? =
    uri.path.takeIf { uri.scheme == ContentResolver.SCHEME_FILE }

private fun resolveLocalPath(context: Context, uri: Uri): String? {
    if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.path
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
    val projection = arrayOf(
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DISPLAY_NAME,
    )
    return runCatching {
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val data = dataIndex.takeIf { it >= 0 }?.let(cursor::getString)
            data?.takeIf { it.isNotBlank() } ?: cursor
                .getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
        }
    }.getOrNull()
}

@Composable
private fun DetailsRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The value is the part that can be arbitrarily long (paths, codec names), so it is the
        // only one that takes the remaining width; the label keeps its intrinsic size and never
        // gets squeezed to zero width.
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4,
        )
    }
}

@OptIn(UnstableApi::class)
private fun Tracks.selectedFormat(type: Int): Format? {
    val group = groups.firstOrNull { candidate ->
        candidate.type == type && candidate.isSelected
    } ?: return null
    val index = (0 until group.length).firstOrNull { index ->
        group.isTrackSelected(index)
    } ?: 0
    return group.getTrackFormat(index)
}
