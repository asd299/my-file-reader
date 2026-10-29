# 项目与依赖版本清单

本文件记录 FileViewer 使用的基础项目、参考项目与全部依赖的当前版本，便于上游发布新版本后快速跟进。

- 应用版本：`1.4.2`（versionCode 8），见 `app/build.gradle.kts`
- 最后核对日期：2026-10-29
- **所有依赖版本集中在 [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)**，升级时只改这一处，然后同步更新本文件。

## 基础与参考项目

| 项目 | 当前基线 | 用途 | 跟进地址 |
| --- | --- | --- | --- |
| [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer) | 已配置 `upstream` 远程，按 `upstream/main` 跟踪 | 播放器、设置页、数据层与整体架构的基础 | https://github.com/anilbeesetti/nextplayer/releases |
| [FossifyOrg/Gallery](https://github.com/FossifyOrg/Gallery) | 仅参考交互与功能设计，未引入代码 | 图片查看器的缩放、旋转、操作栏设计参考 | https://github.com/FossifyOrg/Gallery/releases |
| [anilbeesetti/nextlib](https://github.com/anilbeesetti/nextlib) | `1.11.1-0.16.0` | Media3 的 FFmpeg 解码扩展与 MediaInfo | https://github.com/anilbeesetti/nextlib |

查看上游新提交：

```bash
git fetch upstream
git log --oneline upstream/main ^HEAD   # 上游领先本分支的提交
git log --oneline HEAD ^upstream/main   # 本分支独有的提交
```

> 精简开发时删除的模块（`feature/videopicker`、`feature/network`、主页与媒体库等）在上游继续演进，同步上游时注意不要把这些模块重新合入。

## 构建工具链

| 工具 | 当前版本 | 备注 | 跟进地址 |
| --- | --- | --- | --- |
| Gradle | `9.7.1` | `gradle/wrapper/gradle-wrapper.properties` | https://gradle.org/releases/ |
| Android Gradle Plugin | `9.4.1` | `androidGradlePlugin` | https://developer.android.com/build/releases/gradle-plugin |
| Kotlin | `2.4.20` | 同时决定 Compose 编译器插件 | https://github.com/JetBrains/kotlin/releases |
| KSP | `2.3.12` | Room、Koin 注解处理 | https://github.com/google/ksp/releases |
| ktlint Gradle 插件 | `14.2.0` | `ktlintCheck` / `ktlintFormat` | https://github.com/JLLeitschuh/ktlint-gradle/releases |
| ktlint CLI | `1.8.0` | 自定义规则集编译 | https://github.com/pinterest/ktlint/releases |
| JDK | `17` | `android-jvm` | https://adoptium.net/ |
| Android SDK | compile/target `37`、min `24` | `android-compileSdk` 等 | https://developer.android.com/about/versions |

## 运行时依赖

### AndroidX / Compose

| 依赖 | 当前版本 | 用途 | 跟进地址 |
| --- | --- | --- | --- |
| Compose BOM | `2026.09.00` | 统一 `androidx.compose.*` 版本 | https://developer.android.com/develop/ui/compose/bom/bom-mapping |
| Material 3 | `1.5.0-alpha15` | 界面组件 | https://developer.android.com/jetpack/androidx/releases/compose-material3 |
| Material 3 Adaptive | `1.3.0` | 自适应布局 | https://developer.android.com/jetpack/androidx/releases/compose-material3 |
| Activity | `1.13.0` | Compose 宿主 Activity | https://developer.android.com/jetpack/androidx/releases/activity |
| AppCompat | `1.8.0` | 播放器 Activity 主题 | https://developer.android.com/jetpack/androidx/releases/appcompat |
| Core KTX | `1.19.1` | 基础扩展 | https://developer.android.com/jetpack/androidx/releases/core |
| Core PiP | `1.0.0-alpha04` | 画中画 | https://developer.android.com/jetpack/androidx/releases/core |
| Splashscreen | `1.2.0` | 启动画面 | https://developer.android.com/jetpack/androidx/releases/core |
| Biometric | `1.1.0` | 生物识别解锁设置项 | https://developer.android.com/jetpack/androidx/releases/biometric |
| Lifecycle | `2.11.0` | 生命周期与 ViewModel | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| Navigation 3 | `1.1.7` | 设置页浮层导航 | https://developer.android.com/jetpack/androidx/releases/navigation3 |
| Media3 | `1.11.1` | ExoPlayer、会话、字幕 | https://github.com/androidx/media/releases |
| DataStore | `1.2.1` | 偏好设置存储 | https://developer.android.com/jetpack/androidx/releases/datastore |
| Room | `2.8.5` | 播放进度等本地数据 | https://developer.android.com/jetpack/androidx/releases/room |
| Google Material | `1.14.0` | 原生控件主题 | https://github.com/material-components/material-components-android/releases |

### 图片与媒体

| 依赖 | 当前版本 | 用途 | 跟进地址 |
| --- | --- | --- | --- |
| Coil 3 | `3.6.3` | 缩略图与图片查看（`coil-compose`、`coil-gif`、`coil-video`、`coil-network-okhttp`） | https://github.com/coil-kt/coil/releases |
| nextlib media3ext / mediainfo | `1.11.1-0.16.0` | 软件解码器与媒体信息 | https://github.com/anilbeesetti/nextlib |

> Coil 升级后需确认 `coil3.gif.AnimatedImageDecoder` / `coil3.gif.GifDecoder` 包结构与 `AppModule` 中的注册代码仍然兼容（GIF、动态 WebP/HEIC/AVIF 支持依赖它们）。

### 依赖注入、网络与其它库

| 依赖 | 当前版本 | 用途 | 跟进地址 |
| --- | --- | --- | --- |
| Koin | `4.2.2` | 依赖注入 | https://github.com/InsertKoinIO/koin/releases |
| Koin Compiler 插件 | `1.2.1` | 注解扫描 | https://github.com/InsertKoinIO/koin-annotations/releases |
| kotlinx.coroutines | `1.11.0` | 协程 | https://github.com/Kotlin/kotlinx.coroutines/releases |
| kotlinx.serialization | `1.11.0` | JSON | https://github.com/Kotlin/kotlinx.serialization/releases |
| Reorderable | `3.1.0` | 设置页拖拽排序 | https://github.com/Calvin-LL/Reorderable/releases |
| AboutLibraries | `15.2.0` | 第三方库信息 | https://github.com/mikepenz/AboutLibraries/releases |
| Accompanist Permissions | `0.37.3` | 权限请求 | https://github.com/google/accompanist/releases |
| sshj | `0.40.0` | SFTP（上游网络存储模块遗留依赖） | https://github.com/hierynomus/sshj/releases |
| smbj / dcerpc | `0.14.0` / `0.12.13` | SMB（同上） | https://github.com/hierynomus/smbj/releases |
| commons-net | `3.13.0` | FTP（同上） | https://commons.apache.org/proper/commons-net/ |
| sardine-android | `0.9` | WebDAV（同上） | https://github.com/thegrizzlylabs/sardine-android |
| Bouncy Castle | `1.80.2` | 加密支持（同上） | https://www.bouncycastle.org/download/bouncy-castle-java/ |
| juniversalchardet | `2.5.0` | 字幕编码检测 | https://github.com/albfernandez/juniversalchardet |

### 测试

| 依赖 | 当前版本 | 跟进地址 |
| --- | --- | --- |
| JUnit 4 | `4.13.2` | https://github.com/junit-team/junit4 |
| Robolectric | `4.16.1` | https://github.com/robolectric/robolectric/releases |
| Mockito | `5.14.2` | https://github.com/mockito/mockito/releases |
| AndroidX Test Ext JUnit | `1.3.0` | https://developer.android.com/jetpack/androidx/releases/test |
| AndroidX Test Rules | `1.7.0` | https://developer.android.com/jetpack/androidx/releases/test |
| Espresso Core | `3.7.0` | https://developer.android.com/jetpack/androidx/releases/test |

## 跟进更新的流程

1. 对照上表的「跟进地址」检查新版本；AndroidX/AGP 以 Google Maven 页面为准。
2. 修改 [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) 中对应的 `[versions]` 条目（插件版本同步检查同名 key）。
3. 升级 Compose BOM 时，先核对 BOM 映射表，避免手动指定 Compose 单模块版本。
4. 升级 Coil 时额外验证：动图（GIF/动态 WebP/HEIC/AVIF）能否正常播放，见 `AppModule` 的解码器注册。
5. 升级 Media3 / nextlib 时额外验证：本地视频、字幕、软解码、画中画与后台播放。
6. 运行验证并提交：

```bash
./gradlew ktlintCheck test assembleDebug
./gradlew assembleRelease-with-debug-signing
```

7. 调整 `app/build.gradle.kts` 的 `versionName` / `versionCode`，同步更新本文件的「应用版本」与对应依赖版本，再提交发布。
8. 同步上游功能：`git fetch upstream` 后按提交逐项评估，只移植与播放器、图片查看、设置相关的部分。
