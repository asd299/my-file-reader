![Next player banner](fastlane/metadata/android/en-US/images/featureGraphic.png)

# Next Player（播放器精简定制版）

一个只保留播放器的原生 Android 视频播放器，基于 [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer) 二次开发。

本分支做了一次彻底精简：**删除播放器之外的所有界面**（主页、媒体库、播放列表、网络存储管理、更多/历史/回收站等），**不提供桌面启动图标**，只能由其他应用通过“打开方式 / 分享”等方式调用播放器。

## 使用方式

其他应用（文件管理器、相册、浏览器、聊天工具等）以 `VIEW` 意图打开视频时即可调用本应用，支持：

- 本地文件与 `content://` 视频
- `http/https`、`rtsp/rtp/rtmp` 等流媒体地址
- `smb`、`ftp`、`sftp`、`webdav` 网络存储地址

由于没有主界面，安装后在桌面不会出现图标，需要通过其他应用的“打开方式”来使用。

## 本分支的定制内容

- **只保留播放器**：除播放器外的所有页面与导航均已删除，应用入口只有 `PlayerActivity`。
- **播放结束停留最后一帧**：播放完毕后不关闭播放器、不清空队列，画面停留在最后一帧。
- **中央悬浮重播按钮**：播放结束后画面中央显示悬浮“重播”按钮，点击从头重播；即使自动连播关闭、播放器在最后一帧暂停也能正确显示。
- **默认不自动播放下一个**：默认关闭自动连播。
- **默认竖屏播放**：进入播放器默认竖屏，仍可通过控制栏的旋转按钮切换方向。
- **设置入口在播放器右上角**：设置以全屏浮层在播放器内打开，无需退出播放。
- **长按上下滑动实时调速**：长按画面后上下滑动连续调整速度（0.25x～8x），松手恢复原速。
- **音量调节更顺滑**：音量手势改为连续值控制，并对系统音量离散步进做去抖处理。
- **亮度调节修复**：亮度不再从 100% 开始，进入播放器时以系统当前真实亮度为起点。

## 功能特性

- 完全免费、开源，无广告
- 支持 h264 与 hevc 软件解码
- 音轨 / 字幕轨选择
- 左侧上下滑动调节亮度，右侧上下滑动调节音量
- 左右滑动快进 / 快退
- 播放速度调节（含长按滑动调速）
- 外挂字幕与字幕延迟调节
- 缩放手势、画中画模式、后台播放
- Android TV 版本（由其他应用或系统调用播放器）

## 支持的格式

- **视频**：H.263、H.264 AVC、H.265 HEVC、MPEG-4 SP、VP8、VP9、AV1（实际支持情况取决于设备）
- **音频**：Vorbis、Opus、FLAC、ALAC、PCM/WAVE、MP1/MP2/MP3、AMR、AAC、AC-3、E-AC-3、DTS、DTS-HD、TrueHD（由 ExoPlayer FFmpeg 扩展提供）
- **字幕**：SRT、SSA、ASS、TTML、VTT、DVB（SSA/ASS 样式支持有限）

## 构建

环境要求：JDK 17、Android SDK（`ANDROID_HOME` 指向 SDK 目录）。

```bash
# 构建所有 debug 变体
./gradlew assembleDebug

# 构建可安装的发布包（使用 debug 签名，便于本地安装体验）
./gradlew :app:assembleRelease-with-debug-signing

# 运行本地单元测试
./gradlew test

# 检查 Kotlin 代码风格
./gradlew ktlintCheck

# 自动修复代码风格
./gradlew ktlintFormat
```

`release-with-debug-signing` 变体在 `app/build/outputs/apk/release-with-debug-signing/` 下产出通用包与按 ABI 拆分的安装包（arm64-v8a、armeabi-v7a、x86、x86_64）。

## 项目结构

- `app/`：应用入口（依赖注入、崩溃处理、文件共享），不再包含任何页面
- `core/`：可复用基础层（`common`、`data`、`domain`、`media`、`model`、`ui`、`datastore`、`database`）
- `feature/player`：播放器界面、手势、亮度与音量控制、播放服务
- `feature/settings`：设置页面，作为播放器内的全屏浮层复用

## 参与贡献

欢迎提交贡献。开发前请先阅读 [AGENTS.md](AGENTS.md)，提交前确保 `assembleDebug`、`test`、`ktlintCheck` 均通过。

## 致谢

### 开源项目

- [Findroid](https://github.com/jarnedemeulemeester/findroid)
- [Just (Video) Player](https://github.com/moneytoo/Player)
- [LibreTube](https://github.com/libre-tube/LibreTube)
- [ReadYou](https://github.com/Ashinch/ReadYou)
- [Seal](https://github.com/JunkFood02/Seal)

### 特别感谢

[<img src="https://hosted.weblate.org/widgets/next-player/-/287x66-white.png"  width="200"/>](https://hosted.weblate.org/engage/next-player/)

感谢 **Weblate** 为项目提供免费的翻译托管服务。

## 许可证

Next Player 基于 GNU 通用公共许可证 v3.0 发布，详见 [LICENSE](LICENSE) 文件。
