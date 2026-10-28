package dev.anilbeesetti.nextplayer

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.common.di.DispatchersModule
import dev.anilbeesetti.nextplayer.core.data.DataModule
import dev.anilbeesetti.nextplayer.feature.player.PlayerModule
import dev.anilbeesetti.nextplayer.settings.SettingsModule
import okio.FileSystem
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(
    includes = [
        DispatchersModule::class,
        DataModule::class,
        PlayerModule::class,
        SettingsModule::class,
    ],
)
class AppModule {

    @Single
    fun provideImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context = context)
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache(
            DiskCache.Builder()
                .fileSystem(FileSystem.SYSTEM)
                .directory(context.filesDir.resolve(relative = "thumbnails"))
                .maxSizePercent(percent = 1.0)
                .build(),
        )
        .crossfade(enable = true)
        .build()
}
