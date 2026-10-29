package dev.anilbeesetti.nextplayer

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.re.docviewer.DocViewerBootstrap
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

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // 离线文档查看器：解压 split、挂资源、注入 dex（Quark 的 Qigsaw 同款链路）
        DocViewerBootstrap.init(base)
    }

    /**
     * split 里的控件常用 application context 造 View，这里每次取用都确保
     * ucoffice / pdfviewer 的资源路径已挂到当前 AssetManager 上。
     */
    override fun getResources(): Resources {
        val resources = super.getResources()
        DocViewerBootstrap.installSplitResources(resources)
        return resources
    }

    override fun getAssets(): AssetManager {
        DocViewerBootstrap.installSplitResources(super.getResources())
        return super.getAssets()
    }

    /**
     * split 里的 Activity（QuarkOfficeActivity 等）继承自宿主进程，必须在创建前换主题、
     * 创建后补挂资源，否则 ucoffice 的布局找不到自己的 drawable / string。
     * API 29 以下没有 onActivityPreCreated，只能靠 onActivityCreated 兜底。
     */
    private fun registerDocViewerLifecycle() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                    DocViewerBootstrap.onActivityPreCreated(activity)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    DocViewerBootstrap.onActivityCreated(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    override fun onCreate() {
        super.onCreate()
        registerDocViewerLifecycle()
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
