package dev.anilbeesetti.nextplayer.feature.player

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerSettingsOverlay
import dev.anilbeesetti.nextplayer.feature.player.ui.image.ImageViewerScreen

/**
 * Opens images that other apps hand over with "Open with" / "Share". Playback is untouched, this
 * activity only displays still images; several images shared at once can be swiped through.
 */
class ImageViewerActivity : ComponentActivity() {

    private var imageUris by mutableStateOf<List<Uri>>(emptyList())
    private var pendingDeleteUri: Uri? = null

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val deletedUri = pendingDeleteUri
        pendingDeleteUri = null
        if (deletedUri != null && result.resultCode == RESULT_OK) {
            removeImage(uri = deletedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageUris = imageUrisFrom(intent = intent)
        if (imageUris.isEmpty()) {
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            NextPlayerTheme(darkTheme = true) {
                var showSettings by rememberSaveable { mutableStateOf(value = false) }
                ImageViewerScreen(
                    uris = imageUris,
                    onClose = ::finish,
                    onDelete = ::requestDelete,
                    onSettingsClick = { showSettings = true },
                )
                if (showSettings) {
                    PlayerSettingsOverlay(onDismiss = { showSettings = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // "Open with" while already open: restart with the new image instead of stacking screens.
        setIntent(intent)
        recreate()
    }

    /** Deletes the image, asking the system for confirmation when it manages the file. */
    private fun requestDelete(uri: Uri) {
        val request = mediaStoreDeleteRequest(uri = uri)
        if (request == null) {
            deleteImageFile(uri = uri)
            return
        }
        pendingDeleteUri = uri
        runCatching {
            deleteLauncher.launch(IntentSenderRequest.Builder(request).build())
        }.onFailure {
            pendingDeleteUri = null
            deleteImageFile(uri = uri)
        }
    }

    private fun mediaStoreDeleteRequest(uri: Uri) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            uri.authority == MediaStore.AUTHORITY
        ) {
            runCatching {
                MediaStore.createDeleteRequest(
                    contentResolver,
                    listOf(uri),
                ).intentSender
            }.getOrNull()
        } else {
            null
        }

    private fun deleteImageFile(uri: Uri) {
        val deleted = runCatching {
            if (DocumentsContract.isDocumentUri(
                    this,
                    uri,
                )
            ) {
                DocumentsContract.deleteDocument(
                    contentResolver,
                    uri,
                )
            } else {
                contentResolver.delete(
                    uri,
                    null,
                    null,
                ) > 0
            }
        }.getOrDefault(false)
        if (deleted) {
            removeImage(uri = uri)
        } else {
            Toast.makeText(
                this,
                R.string.image_viewer_delete_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun removeImage(uri: Uri) {
        val remaining = imageUris.filterNot { it == uri }
        if (remaining.size == imageUris.size) return
        imageUris = remaining
        if (remaining.isEmpty()) finish()
    }
}

internal fun imageUrisFrom(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    val clipDataUris = intent.clipData?.let { clipData ->
        (0 until clipData.itemCount).mapNotNull { index -> clipData.getItemAt(index).uri }
    }.orEmpty()
    val streamUris = when (val stream = intent.extras?.get(Intent.EXTRA_STREAM)) {
        is Uri -> listOf(stream)
        is List<*> -> stream.filterIsInstance<Uri>()
        is Array<*> -> stream.filterIsInstance<Uri>()
        else -> emptyList()
    }
    return (listOfNotNull(intent.data) + clipDataUris + streamUris).distinct()
}
