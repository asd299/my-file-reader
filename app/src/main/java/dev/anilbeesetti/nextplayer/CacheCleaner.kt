package dev.anilbeesetti.nextplayer

import android.app.Application
import coil3.ImageLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Removes everything the app can regenerate on demand: the picture caches plus the files kept in
 * the internal and external cache directories (subtitle copies, shared crash reports, temporary
 * media). It runs on every process start so these never pile up, and it never touches settings,
 * playback history or the player database.
 */
internal object CacheCleaner {
    suspend fun clear(application: Application, imageLoader: ImageLoader) =
        withContext(Dispatchers.IO) {
            imageLoader.memoryCache?.clear()
            runCatching { imageLoader.diskCache?.clear() }

            val cacheDirectories = listOfNotNull(
                application.cacheDir,
                application.externalCacheDir,
            )
            cacheDirectories.forEach { directory: File ->
                directory.listFiles()?.forEach { file ->
                    runCatching { file.deleteRecursively() }
                }
            }
        }
}
