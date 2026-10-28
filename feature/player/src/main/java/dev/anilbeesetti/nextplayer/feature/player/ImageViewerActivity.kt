package dev.anilbeesetti.nextplayer.feature.player

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.ui.image.ImageViewerScreen

/**
 * Opens images that other apps hand over with "Open with" / "Share". Playback is untouched, this
 * activity only displays still images; several images shared at once can be swiped through.
 */
class ImageViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uris = imageUrisFrom(intent = intent)
        if (uris.isEmpty()) {
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            NextPlayerTheme(darkTheme = true) {
                ImageViewerScreen(
                    uris = uris,
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // "Open with" while already open: restart with the new image instead of stacking screens.
        setIntent(intent)
        recreate()
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
