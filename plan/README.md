# 我的文件阅读器 · 融合计划

把 **FileViewer**（Kotlin/Compose 视频播放 + 图片查看）与 **quark-doc-viewer 的离线文档查看器**
（Java 壳 + 夸克 ucoffice / pdfviewer split）合并为一个应用。以 FileViewer 的主题与架构为主，
文档查看能力作为新模块接入，仍保持「无桌面图标、由其他应用通过打开方式调用」的形态。

## 融合后的能力

| 类型 | 承载界面 | 来源 |
| --- | --- | --- |
| 视频 | `PlayerActivity`（exoplayer + Compose） | FileViewer |
| 图片 | `ImageViewerActivity`（缩放/旋转/幻灯片/删除） | FileViewer |
| PDF（内嵌 pdfium） | `PdfViewerActivity` + split `com.uc.pdf.PDFViewer` | quark-doc-viewer |
| Office（doc/docx/xls/xlsx/ppt/pptx/txt/rtf） | `OfficeActivity` + split `com.uc.office.ui.QuarkOfficeActivity` | quark-doc-viewer |

所有入口都由 `VIEW` / `SEND` 意图触发，不注册桌面启动图标（与 FileViewer 一致）。

## 使用的项目地址与版本

最后核对日期：2026-10-29

| 项目 | 地址 | 当前使用版本 | 跟进地址 |
| --- | --- | --- | --- |
| FileViewer（融合基座） | https://github.com/asd299/FileViewer | `1.4.2`（versionCode 8） | https://github.com/asd299/FileViewer/releases |
| quark-doc-viewer（离线文档查看器） | https://github.com/asd299/quark-doc-viewer | `0.8.6`（versionCode 21） | https://github.com/asd299/quark-doc-viewer/releases |
| nextplayer（FileViewer 上游） | https://github.com/anilbeesetti/nextplayer | 按 `upstream/main` 跟踪 | https://github.com/anilbeesetti/nextplayer/releases |
| nextlib（Media3 软解扩展） | https://github.com/anilbeesetti/nextlib | `1.11.1-0.16.0` | https://github.com/anilbeesetti/nextlib |
| 夸克浏览器（split 来源） | 本地逆向，无公开源 | `10.19.6.1171` | 夸克官网版本页 |
| ucoffice split | 包名 `com.quark.browser.ucoffice` | 随夸克 10.19.6.1171 | — |
| pdfviewer split | 包名 `com.quark.browser.pdfviewer` | 随夸克 10.19.6.1171 | — |
| 融合产物（本仓库） | https://github.com/asd299/my-file-reader | `1.7.0`（versionCode 15，package `com.re.docviewer`，应用名「轻阅」，minSdk 23） | 本仓库 Releases |

> 构建环境：Android SDK 的平台包从 36 起带小版本号（`platforms;android-37.0`），
> `splitstubs` 会按 `android-37*` 前缀自动查找 `android.jar`。

### 构建工具链（沿用 FileViewer）

| 工具 | 版本 | 位置 |
| --- | --- | --- |
| Gradle | `9.7.1` | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | `9.4.1` | `gradle/libs.versions.toml` |
| Kotlin | `2.4.20` | 同上 |
| JDK | `17` | `android-jvm` |
| Android SDK | compile/target `37`、min `23` | `android-compileSdk` 等 |

详细依赖版本见 [dependencies/README.md](../dependencies/README.md) 与
[gradle/libs.versions.toml](../gradle/libs.versions.toml)；融合新增的依赖记录在下方「融合增量」。

## 模块结构（融合后）

```
app/                 应用入口：Application 引导、清单、FileProvider、主题
core/…               原 FileViewer 基础层（common/data/domain/media/model/ui/…）
feature/player       播放器 + 图片查看（原 FileViewer）
feature/settings     设置页（原 FileViewer）
feature/docviewer    新增：离线文档查看器（Java 壳 + split 资源）
splitstubs           新增：编译期桩（不打包，供覆盖 split 同名类）
plan/                本目录：进度、版本与跟进说明
```

## 融合增量（新增依赖）

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| constraintlayout | `2.2.1` | pdfviewer split 布局使用 |
| recyclerview | `1.4.0` | ucoffice split 列表 |
| documentfile | `1.1.0` | SAF 文档访问 |
| commons-logging | `1.3.5` | split 内部日志门面 |
| desugar_jdk_libs | `2.0.4` | split dex 的 `j$.` 脱糖类 |
| androidx.collection | `1.5.0` | split 依赖 |

## 进度

当前状态与每一步的结果记录在 [progress.md](progress.md)。

## 后续跟进

1. **上游 FileViewer**：`git fetch origin && git log --oneline origin/main ^HEAD`，只移植播放器 / 图片 / 设置相关改动；
   不要引入 `feature/videopicker`、`feature/network` 与主页、媒体库。
2. **quark-doc-viewer 更新**：对照其 Release 变更，把 `feature/docviewer` 里的 Java 壳与
   `assets/ucoffice-master.apk`、`assets/pdfviewer-master.apk`、`jniLibs/arm64-v8a/*` 同步过来；
   资源版本号在 `DocViewerBootstrap.SPLIT_ASSET_VERSION` 递增，否则已解压的旧副本不会失效。
3. **夸克版本更新**：split 包若随夸克升级，需要重新 dump 资源 id（`R.java` 桩）与类字段，
   并复核 `ParseErrorView` / `OfficeMenuDialog` 覆盖类的方法描述符。
4. **工具链升级**：只改 `gradle/libs.versions.toml`，同步 `dependencies/README.md`，
   跑 `./gradlew ktlintCheck test assembleDebug`。
5. **品牌资源**：图标源图在 [brand/qingyue-icon.jpg](brand/qingyue-icon.jpg)，改图后执行
   `python3 plan/brand/make_icons.py` 重新生成 mdpi~xxxhdpi 的
   `ic_launcher` / `ic_launcher_round` / `ic_launcher_foreground` / `ic_launcher_monochrome`；
   应用名在 `core/ui/src/main/res/values/strings.xml` 的 `app_name`（debug 变体另有一份）。
