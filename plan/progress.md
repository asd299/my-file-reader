# 融合进度

## 2026-10-29

### 融合（1.5.0 / code 9）

- [x] 克隆 FileViewer 作为融合基座（v1.4.2 / code 8）
- [x] 建立 plan 目录，记录项目地址、版本与跟进方式
- [x] 迁移离线文档查看器源码到 `feature/docviewer`（v0.8.6 / code 21）
- [x] 迁移 split 编译期桩到 `splitstubs`
- [x] 迁移 split 资源与 so：`feature/docviewer/src/main/assets`、`jniLibs/arm64-v8a`
- [x] 清单合并：文档 Activity、CAMERA 权限、FileProvider 路径（含 external-cache）
- [x] Application 引导合并：`DocViewerBootstrap.init` / 资源补挂 / Activity 生命周期回调（主题切换）
- [x] 文档类 VIEW 入口改用 `Theme.NextPlayer`；保持无桌面图标
- [x] ABI 收窄为 arm64-v8a（文档引擎只提供 arm64 so），不再产出其它 ABI 的空壳包
- [x] 构建通过并产出 APK（Gradle 9.7.1 / AGP 9.4.1 / Kotlin 2.4.20 / JDK 17 / SDK 37）

### 品牌与关于页（1.6.0 / code 10）

- [x] 应用名改为「轻阅」（`app_name`，debug 变体为「轻阅 (Debug)」）
- [x] 用 seedream 重新生成图标（深青→海蓝渐变 + 白色折角纸张 + 播放/图片符号）
- [x] 图标落地：旧版圆角 `ic_launcher(.webp)`、圆形 `ic_launcher_round`、自适应前景
      `ic_launcher_foreground`、Android 13 主题单色层 `ic_launcher_monochrome`（mdpi~xxxhdpi）
- [x] 图标生成可复现：源图 `plan/brand/qingyue-icon.jpg`，脚本 `plan/brand/make_icons.py`
- [x] 关于页：仓库链接改为 my-file-reader，新增「Document engine」条目指向 quark-doc-viewer
- [x] 文案：`app_tagline` 改为 "Video, images and documents in one place"，权限说明去掉 Next Player 字样
- [x] 构建通过并产出 APK（package `com.re.docviewer.debug`，1.6.0 / 10）

### 修复 PDF 崩溃（1.6.1 / code 11）

- [x] 定位：`pdfviewer` split dex 直接引用 `j$/util/concurrent/ConcurrentHashMap`、
      `j$/util/Objects`，而 `minSdk=24` 时 L8 判定这些 Java 8 回填类不需要，base dex 里没有，
      打开 PDF 时 `PdfiumSDK.<init>` 抛 `NoClassDefFoundError`
- [x] 试过在 `feature/docviewer` 里自建 `j$` 桩：D8 禁止 `j$.` 类与普通类合并到同一 dex，方案不可行（已回退）
- [x] 采用原壳同款配置：`android-minSdk = 23`，让 L8 打包完整脱糖库
      （旧版离线文档查看器同为 23，dex 内含 1008 个 `j$` 描述符）
- [x] `core-pip` 声明 minSdk 24，在 app 清单用 `tools:overrideLibrary="androidx.core.pip"` 放开
      （仅画中画受影响，API 26 以下本就不可用）
- [x] 校验：融合 APK 含 1388 个 `j$` 描述符，两个 split 引用的 `j$` 类全部可解析（缺失集为空）
- [x] 构建通过并产出 APK（1.6.1 / 11，`app-arm64-v8a-debug.apk` 52,999,561 B）

### 构建结果

```
./gradlew assembleDebug
BUILD SUCCESSFUL
app-arm64-v8a-debug.apk   52,999,561 B  (1.6.1 / 11)
```

产物校验（aapt2）：
- label = 轻阅 (Debug)，versionName = 1.6.1，versionCode = 11，minSdk = 23
- application-icon 指向 `mipmap-anydpi-v26/ic_launcher.xml`（自适应图标）
- 已注册 `OfficeActivity`（VIEW：pdf/msword/ms-excel/ms-powerpoint/ooxml）
- 已注册 `SaveFileActivity`、`PdfViewerActivity`、`QuarkOfficeActivity`、`QuarkOfficeTextViewActivity`
- APK 内含 `assets/ucoffice/ucoffice-master.apk`、`assets/pdfviewer/pdfviewer-master.apk`
  与 `lib/arm64-v8a/libpdfium.so`、`libpdfsdk_jni.so`、`libtiff*.so`

### 待办 / 已知限制

- [ ] PDF / Office 界面本身来自夸克 split，无法换成 Compose 主题；只有壳页面（加载页、错误页、保存选择器）跟随 FileViewer 主题
- [ ] PDF「编辑底栏印章/图片」依赖 `assets/t1.png`、`t1.jpg`，夸克侧缺失，当前不可达
- [ ] 文档查看仅 arm64-v8a（split so 只有 arm64），其它 ABI 已不产出
- [ ] 融合后应用不再提供桌面启动图标，只能由「打开方式 / 分享」唤起（与 FileViewer 一致）
- [ ] 文档入口目前只接 `VIEW`；`SEND`（分享单个文件）打开文档需要在 `OfficeActivity` 里补 `EXTRA_STREAM` 分支
- [ ] minSdk 为 23（为完整脱糖库），`core-pip` 被强制放开；API 23~25 上画中画不可用，其余功能未在老设备验证
- [ ] 仍需真机验证：docx/xlsx/pptx 打开、PDF 打开、另存为、打印
