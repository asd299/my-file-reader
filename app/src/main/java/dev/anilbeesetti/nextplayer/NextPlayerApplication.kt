package dev.anilbeesetti.nextplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.common.di.DiQualifiers
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.media.network.keys.SshKeyStore
import dev.anilbeesetti.nextplayer.crash.CrashActivity
import dev.anilbeesetti.nextplayer.crash.GlobalExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.core.qualifier.named
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class NextPlayerApplication :
    Application(),
    SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    private val networkConnectionRepository: NetworkConnectionRepository by inject()

    private val sshKeyStore: SshKeyStore by inject()

    private val preferencesRepository: PreferencesRepository by inject()

    private val applicationScope: CoroutineScope by inject(
        qualifier = named(DiQualifiers.APPLICATION_SCOPE),
    )

    override fun onCreate() {
        super.onCreate()
        startKoin<NextPlayerApplication> {
            allowOverride(override = false)
            androidContext(androidContext = this@NextPlayerApplication)
        }
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(
                context = applicationContext,
                CrashActivity::class.java,
            ),
        )
        applicationScope.launch {
            CacheCleaner.clear(
                application = this@NextPlayerApplication,
                imageLoader = imageLoader,
            )
        }
        applicationScope.launch {
            runCatching {
                initializeSshKeyStore(
                    repository = networkConnectionRepository,
                    sshKeyStore = sshKeyStore,
                )
            }.onFailure { error ->
                Logger.logError(
                    tag = TAG,
                    message = "Couldn't reconcile SSH keys: ${error.message}",
                )
            }
        }
        disableAutoPictureInPictureOnce()
    }

    /**
     * Auto-enter picture in picture used to be enabled by default, so every install upgraded from
     * an older build has it stored as `true` even though the user never asked for it. Reset it once
     * to the new default; the player setting can still turn it back on afterwards.
     */
    private fun disableAutoPictureInPictureOnce() {
        val migrations = getSharedPreferences(
            MIGRATIONS_PREFERENCES,
            MODE_PRIVATE,
        )
        if (
            migrations.getBoolean(
                AUTO_PIP_DISABLED_KEY,
                false,
            )
        ) {
            return
        }

        migrations.edit()
            .putBoolean(
                AUTO_PIP_DISABLED_KEY,
                true,
            )
            .apply()
        applicationScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(autoPip = false) }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    private companion object {
        const val TAG = "NextPlayerApplication"
        const val MIGRATIONS_PREFERENCES = "migrations"
        const val AUTO_PIP_DISABLED_KEY = "auto_pip_disabled"
    }
}
