![Next player banner](fastlane/metadata/android/en-US/images/featureGraphic.png)

# Next Player（中文定制版）

Next Player 是一款使用 Kotlin 编写的原生 Android 视频播放器，界面简洁、易于使用，基于 [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer) 二次开发。

**本仓库为个人定制分支，已针对播放器的手势体验和播放结束行为做了专门调整，并精简了设置入口。项目仍在持续开发中，如遇到问题欢迎在 [Issues](https://github.com/asd299/nextplayer/issues) 中反馈。**

## 本分支的定制内容

- **长按上下滑动实时调速**：长按屏幕后上下滑动即可连续调整播放速度（0.25x～8x，步进 0.25x），松手后自动恢复原速。
- **播放结束停留最后一帧**：视频播放完毕后不再自动关闭播放器或清空队列，画面停留在最后一帧。
- **中央重播按钮**：播放结束后在画面中央显示悬浮“重播”按钮，点击即可从头重新播放。
- **默认不自动播放下一个**：默认关闭自动连播，需要手动切换或重播。
- **默认竖屏播放**：进入播放器默认竖屏，仍可通过控制栏的旋转按钮切换方向。
- **设置入口移至播放器右上角**：设置以全屏浮层的形式在播放器内打开，无需退出播放；原媒体库、播放列表、网络页面及“更多”页中的设置图标与设置路由已删除，代码更精简。
- **音量调节更顺滑**：音量手势改为连续值控制，并对系统音量离散步进做去抖处理，指示器跟随手指平滑变化。
- **亮度调节修复**：亮度不再从 100% 开始，进入播放器时会以系统当前真实亮度为起点，手势过程连续可调。

## 功能特性

- 原生 Android 应用，界面简洁易用
- 完全免费、开源，无广告、无多余权限
- 支持 h264 与 hevc 软件解码
- 音轨 / 字幕轨选择
- 左侧上下滑动调节亮度，右侧上下滑动调节音量
- 左右滑动快进 / 快退
- 支持 [Material 3 (You)](https://m3.material.io/) 动态取色
- 媒体选择器支持目录树、文件夹、文件三种视图
- 播放网络 URL 视频
- 通过存储访问框架（Android 文档选择器）播放视频
- 播放速度调节
- 外挂字幕支持
- 缩放手势
- 画中画模式
- 后台播放
- Android TV 版本
- 搜索功能
- 字幕延迟调节
- 网络存储支持（SMB / FTP / SFTP / WebDAV）

## 支持的格式

- **视频**：H.263、H.264 AVC（Baseline Profile；Android 6+ 支持 Main Profile）、H.265 HEVC、MPEG-4 SP、VP8、VP9、AV1
    - 实际支持情况取决于 Android 设备
- **音频**：Vorbis、Opus、FLAC、ALAC、PCM/WAVE（μ-law、A-law）、MP1、MP2、MP3、AMR（NB、WB）、AAC（LC、ELD、HE；Android 9+ 支持 xHE）、AC-3、E-AC-3、DTS、DTS-HD、TrueHD
    - 由 ExoPlayer FFmpeg 扩展提供支持
- **字幕**：SRT、SSA、ASS、TTML、VTT、DVB
    - SSA/ASS 的样式支持有限，详见[该问题](https://github.com/google/ExoPlayer/issues/8435)

## 截图

### 手机

<div style="width:100%; display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px;">

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=19% alt="主页文件夹">](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=19% alt="文件夹视频">](fastlane/metadata/android/en-US/images/phoneScreenshots/2.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=19% alt="快捷设置">](fastlane/metadata/android/en-US/images/phoneScreenshots/3.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=19% alt="网格视图">](fastlane/metadata/android/en-US/images/phoneScreenshots/4.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width=19% alt="搜索结果">](fastlane/metadata/android/en-US/images/phoneScreenshots/6.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width=19% alt="添加网络连接">](fastlane/metadata/android/en-US/images/phoneScreenshots/7.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width=19% alt="外观设置">](fastlane/metadata/android/en-US/images/phoneScreenshots/8.png)
</div>

### 播放器界面

<div style="width:100%; display:flex; justify-content:space-between;">

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width=49% alt="手机播放器控制栏">](fastlane/metadata/android/en-US/images/phoneScreenshots/5.png)
[<img src="fastlane/metadata/android/en-US/images/tvScreenshots/3.png" width=49% alt="TV 播放器控制栏">](fastlane/metadata/android/en-US/images/tvScreenshots/3.png)
</div>

### Android TV

<div style="width:100%; display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px;">

[<img src="fastlane/metadata/android/en-US/images/tvScreenshots/1.png" width=49% alt="TV 主页文件夹">](fastlane/metadata/android/en-US/images/tvScreenshots/1.png)
[<img src="fastlane/metadata/android/en-US/images/tvScreenshots/2.png" width=49% alt="TV 文件夹视频">](fastlane/metadata/android/en-US/images/tvScreenshots/2.png)
[<img src="fastlane/metadata/android/en-US/images/tvScreenshots/4.png" width=49% alt="TV 外观设置">](fastlane/metadata/android/en-US/images/tvScreenshots/4.png)
</div>

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

- `app/`：应用入口与顶层导航
- `core/`：可复用基础层（`common`、`data`、`domain`、`media`、`model`、`ui`、`datastore`）
- `feature/player`：播放器、手势、亮度与音量控制
- `feature/settings`：设置页面（现在作为播放器内的全屏浮层复用）
- `feature/videopicker`、`feature/playlist`、`feature/network`：媒体选择、播放列表与网络存储

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
