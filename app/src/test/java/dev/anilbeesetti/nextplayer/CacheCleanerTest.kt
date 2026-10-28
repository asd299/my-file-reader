package dev.anilbeesetti.nextplayer

import coil3.ImageLoader
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CacheCleanerTest {

    @Test
    fun clearDeletesCachedFilesButKeepsPreferences() = runBlocking {
        val application = RuntimeEnvironment.getApplication()
        val subtitle = File(
            application.cacheDir,
            "subtitles/episode.srt",
        ).apply {
            parentFile?.mkdirs()
            writeText(text = "1")
        }
        val crashReport = File(
            application.cacheDir,
            "next_player_logs.txt",
        ).apply {
            writeText(text = "logs")
        }
        val preferencesDirectory = File(
            application.filesDir,
            "datastore",
        ).apply { mkdirs() }

        CacheCleaner.clear(
            application = application,
            imageLoader = ImageLoader.Builder(application).build(),
        )

        assertFalse(subtitle.exists())
        assertFalse(crashReport.exists())
        assertTrue(application.cacheDir.listFiles()?.isEmpty() == true)
        assertTrue(preferencesDirectory.exists())
    }
}
