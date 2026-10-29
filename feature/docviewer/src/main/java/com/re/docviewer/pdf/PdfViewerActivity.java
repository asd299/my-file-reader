package com.re.docviewer.pdf;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.re.docviewer.DocViewerBootstrap;
import com.re.docviewer.ErrorPanel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * PDF 容器页。不做自绘 UI，直接实例化 pdfviewer split 源码里的
 * com.uc.pdf.PDFViewer（内部 inflate split 自己的 activity_pdfviewer、PDFTopBar、PDFBottomBar），
 * 宿主能力通过源码约定的 IPDFActionHandler 回调补齐。
 */
public class PdfViewerActivity extends Activity implements LifecycleOwner {

    private static final String TAG = "PdfViewerHost";
    public static final String EXTRA_PATH = "pdf_path";
    public static final String EXTRA_TITLE = "pdf_title";
    private static final int REQ_STORAGE = 1003;

    private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
    private Object mViewer;
    private Object mActionHandler;
    private Object mPreviousActionHandler;
    private boolean mStarted;
    private boolean mResumed;

    /**
     * 每次取 Resources 都确保 split 路径已挂上：系统在部分场景（配置变化、createConfigurationContext）
     * 会给出新的 ResourcesImpl，只在启动时补一次并不保险。
     */
    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        DocViewerBootstrap.installSplitResources(resources);
        return resources;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // API 29 以下不会回调 onActivityPreCreated，这里补挂一次 split 资源
        DocViewerBootstrap.installSplitResources(getResources());

        if (DocViewerBootstrap.getBootError() != null) {
            setContentView(ErrorPanel.show(this, "pdfviewer 引擎初始化失败：\n\n" + DocViewerBootstrap.getBootError()));
            return;
        }
        if (!DocViewerBootstrap.isDexInjected()) {
            setContentView(ErrorPanel.show(this, "pdfviewer dex 注入失败，无法启动查看器"));
            return;
        }
        // 与融合前的入口页一致：API<=29 先补存储权限再打开
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        startPrepare();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) {
            startPrepare();
        }
    }

    private void startPrepare() {
        final Uri uri = getIntent() == null ? null : getIntent().getData();
        final String extraPath = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_PATH);
        new Thread(() -> {
            try {
                File file = !TextUtils.isEmpty(extraPath) ? new File(extraPath) : copyToCache(uri);
                if (file == null || !file.isFile()) {
                    throw new IOException("无法读取所选文件");
                }
                final File target = file;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    showViewer(target);
                });
            } catch (Throwable t) {
                Log.e(TAG, "prepare file failed", t);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    setContentView(ErrorPanel.show(PdfViewerActivity.this,
                            "读取文件失败：\n\n" + Log.getStackTraceString(t)));
                });
            }
        }, "pdf-prepare").start();
    }

    private void showViewer(File file) {
        try {
            ClassLoader loader = getClassLoader();
            Class<?> viewerClass = Class.forName("com.uc.pdf.PDFViewer", true, loader);
            Class<?> kIface = Class.forName("com.uc.pdf.PDFViewer$k", false, loader);
            Class<?> lIface = Class.forName("com.uc.pdf.PDFViewer$l", false, loader);
            Class<?> webIface = Class.forName("com.uc.pdf.IPDFWebEmbedProxy", false, loader);
            Class<?> handlerIface = Class.forName("com.uc.pdf.IPDFActionHandler", false, loader);

            Object actionHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{handlerIface}, new ActionHandler());
            // c0.q(IPDFActionHandler)：源码里这一步由 PDFViewer 读 intent 的 PDF_ACTION_HANDLER_CLASS 完成，
            // 这里直接写入静态配置，避免壳里再拼一个 split 类名。
            // 该配置是进程级静态值：同时存在多个 PDF 页时必须先记下旧值，销毁时还原，
            // 否则上层实例销毁会把仍在使用的下层实例的回调清成 null（分享/打印等直接失效）。
            Class<?> config = Class.forName("com.uc.pdf.c0", true, loader);
            mPreviousActionHandler = config.getMethod("c").invoke(null);
            config.getMethod("q", handlerIface).invoke(null, actionHandler);
            mActionHandler = actionHandler;

            Object kCallback = Proxy.newProxyInstance(loader,
                    new Class<?>[]{kIface}, new DocCallback());

            Intent viewerIntent = buildViewerIntent(file);
            Constructor<?> ctor = viewerClass.getConstructor(Activity.class, Intent.class,
                    Lifecycle.class, kIface, lIface, webIface);
            mViewer = ctor.newInstance(this, viewerIntent, mLifecycle, kCallback, null, null);
            View root = (View) viewerClass.getMethod("X1").invoke(mViewer);
            setContentView(root);
            // 拷贝文件期间可能已经走过 onResume，这里按当前 Activity 的真实状态补齐生命周期
            syncLifecycle();
            DocViewerBootstrap.installSplitResources(getApplicationContext().getResources());
            probeSplitDrawable();
            Log.i(TAG, "PDFViewer created for " + file.getAbsolutePath());
        } catch (Throwable t) {
            Log.e(TAG, "create PDFViewer failed", t);
            if (!isFinishing() && !isDestroyed()) {
                setContentView(ErrorPanel.show(this, "启动 PDF 查看器失败：\n\n" + Log.getStackTraceString(t)));
            }
        }
    }

    /** 与主 APK 里打开本地 PDF 时使用的 intent 约定一致。 */
    private Intent buildViewerIntent(File file) {
        String name = file.getName();
        if (getIntent() != null && !TextUtils.isEmpty(getIntent().getStringExtra(EXTRA_TITLE))) {
            name = getIntent().getStringExtra(EXTRA_TITLE);
        }
        Intent intent = new Intent();
        // 源码用 type==application/pdf 跳过 API<=29 的 WRITE_EXTERNAL_STORAGE 判断；
        // 不设置 data，解析仍走 PDF_FILE_PATH 的本地分支。
        intent.setType("application/pdf");
        intent.putExtra("PDF_FILE_PATH", file.getAbsolutePath());
        intent.putExtra("pdf_file_name", name);
        intent.putExtra("ENTRY", "other");
        // 容器样式 1：源码据此隐藏“转 Word / PDF 工具 / 发送到电脑（上传）”这些依赖云端的入口，
        // 同时启用底部“分享”、隐藏顶部重复的分享入口。
        intent.putExtra("PDF_CONTAINER_STYLE", 1);
        intent.putExtra("PDF_CONTENT_PROTECT_MODE", false);
        intent.putExtra("PDF_OUTSIDE_PROVIDE_PASSWORD", false);
        intent.putExtra("office_is_copy_file", false);
        intent.putExtra("PDF_DISABLE_RECOVER_LAST_READ_RECORD", true);
        return intent;
    }

    /** 诊断：确认 split 资源在 Activity / Application / 弹窗主题包装后的 Context 上都能解析。 */
    private void probeSplitDrawable() {
        final int drawableId = 0x7d050043; // drawable/pdfviewer_readmode_normal
        probeDrawable("activity", getResources(), drawableId);
        probeDrawable("application", getApplicationContext().getResources(), drawableId);
        int styleId = 0;
        try {
            styleId = getResources().getIdentifier("pdf_dialog_theme", "style",
                    "com.quark.browser.pdfviewer");
            ContextThemeWrapper wrapper = new ContextThemeWrapper(this, styleId);
            probeDrawable("dialog-wrapper", wrapper.getResources(), drawableId);
        } catch (Throwable t) {
            Log.e(TAG, "probe dialog wrapper failed style=0x" + Integer.toHexString(styleId), t);
        }
    }

    private void probeDrawable(String where, Resources resources, int id) {
        try {
            resources.getDrawable(id, null);
            Log.i(TAG, "probe drawable ok[" + where + "] density="
                    + resources.getDisplayMetrics().densityDpi);
        } catch (Throwable t) {
            Log.e(TAG, "probe drawable FAILED[" + where + "] density="
                    + resources.getDisplayMetrics().densityDpi
                    + " id=0x" + Integer.toHexString(id), t);
        }
    }

    /** 源码 PDFViewer.l 的回调：文档加载状态。 */
    private final class DocCallback implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "finishContainer":
                    // 回调可能来自工作线程，finish 必须回主线程
                    runOnUiThread(PdfViewerActivity.this::finish);
                    return null;
                case "onDocLoaded":
                case "onDocParseDone":
                    Log.i(TAG, method.getName() + " " + (args != null && args.length > 0 ? args[0] : ""));
                    return null;
                case "onDocLoadFail":
                    toast(String.valueOf(args != null && args.length > 0 ? args[0] : "文档打开失败"));
                    return null;
                case "toString":
                    return "PdfDocCallback";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    /** 源码 IPDFActionHandler 的本地实现：能本机做的做掉，在线能力返回失败。 */
    private final class ActionHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getExportPDFImageDir":
                    return ensureDir("pdf-export").getAbsolutePath();
                case "getSaveToFileDir": {
                    String name = defensiveString(args, 0);
                    if (TextUtils.isEmpty(name)) {
                        name = "document.pdf";
                    }
                    return new File(ensureDir("pdf-save"), name).getAbsolutePath();
                }
                case "getOnlineFileLength":
                    return 0L;
                case "handleDownFileBlockSync":
                    return null;
                case "handleSendFileToOtherApp":
                    share(defensiveString(args, 1),
                            args != null && args.length > 2 ? args[2] : null);
                    return null;
                case "handleEditPDF":
                    // 源码 PDFActionHandler 里“编辑”就是交给外部应用打开编辑，不是内嵌编辑器
                    editWithOtherApp(defensiveString(args, 1),
                            args != null && args.length > 2 ? args[2] : null);
                    return null;
                case "handleConvertFile":
                    return null;
                case "handleUploadFile":
                    return null;
                case "handleClickOpenByPC":
                    return null;
                case "loadDocumentPasswordByOutside":
                    return null;
                case "onExportPDFImage":
                    toast("已导出：" + defensiveString(args, 0));
                    return null;
                case "onReaderCreated":
                case "onReaderDestroy":
                    return null;
                case "uploadCrashTrace":
                    Log.e(TAG, "reader crash: " + defensiveString(args, 1),
                            args != null && args.length > 1 && args[1] instanceof Throwable
                                    ? (Throwable) args[1] : null);
                    return null;
                case "toString":
                    return "QuarklessPdfActionHandler";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    /** 分享当前 PDF，走 FileProvider，失败时按源码约定回调错误文案。 */
    private void share(String path, Object errorCallback) {
        runOnUiThread(() -> doShare(path, errorCallback));
    }

    private void doShare(String path, Object errorCallback) {
        try {
            File file = new File(path);
            if (!file.isFile()) {
                notifyShareError(errorCallback, "文件不存在");
                return;
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "分享"));
        } catch (Throwable t) {
            Log.e(TAG, "share failed", t);
            notifyShareError(errorCallback, "没有可用的应用");
        }
    }

    /** 与源码 handleEditPDF 一致：把 PDF 交给外部应用编辑，没有编辑器就退化为查看。 */
    private void editWithOtherApp(String path, Object errorCallback) {
        runOnUiThread(() -> {
            File file = new File(path);
            if (!file.isFile()) {
                notifyShareError(errorCallback, "文件不存在");
                return;
            }
            Uri uri;
            try {
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            } catch (Throwable t) {
                Log.e(TAG, "edit uri failed", t);
                notifyShareError(errorCallback, "无法授权文件");
                return;
            }
            Intent edit = new Intent(Intent.ACTION_EDIT)
                    .setDataAndType(uri, "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // 用直接 startActivity（有多个应用时系统自己弹选择器）：createChooser 没有匹配项时
            // 系统仍会显示空选择器，无法据此判断“没有编辑器”，拿不到异常也就无法退化到查看。
            try {
                startActivity(edit);
            } catch (ActivityNotFoundException e) {
                Intent view = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/pdf")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(view);
                } catch (ActivityNotFoundException e2) {
                    notifyShareError(errorCallback, "没有可用的应用");
                }
            }
        });
    }

    private void notifyShareError(Object callback, String message) {
        if (callback == null) {
            toast(message);
            return;
        }
        try {
            Class<?> iface = Class.forName("com.uc.pdf.IPDFActionHandler$c", false, getClassLoader());
            iface.getMethod("a", String.class).invoke(callback, message);
        } catch (Throwable t) {
            Log.w(TAG, "notify share error failed", t);
            toast(message);
        }
    }

    private static String defensiveString(Object[] args, int index) {
        return args != null && args.length > index && args[index] != null
                ? String.valueOf(args[index]) : "";
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return (char) 0;
        }
        return null;
    }

    private File ensureDir(String name) {
        File dir = new File(getCacheDir(), name);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private File copyToCache(Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("没有文件 Uri");
        }
        String name = queryDisplayName(uri);
        if (TextUtils.isEmpty(name)) {
            name = "document.pdf";
        }
        File dir = ensureDir("pdf-open");
        File out = new File(dir, name.replace('/', '_'));
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(out)) {
            if (in == null) {
                throw new IOException("无法打开输入流");
            }
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                os.write(buffer, 0, n);
            }
        }
        return out;
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "query display name failed", t);
        }
        return null;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 与源码 PDFViewerWrapper.handleBackKey 一致：先让 PDFViewer 处理返回
     * （关闭复选弹窗/页面工具/缩略图面板等），它自己不再处理时才交给宿主。
     */
    @Override
    public void onBackPressed() {
        Object viewer = mViewer;
        if (viewer != null) {
            try {
                boolean failed = (Boolean) viewer.getClass().getMethod("y2").invoke(viewer);
                if (!failed) {
                    viewer.getClass().getMethod("a").invoke(viewer);
                    return;
                }
            } catch (Throwable t) {
                Log.w(TAG, "PDFViewer back handling failed", t);
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // singleTop 复用时必须换成新 Uri 重新初始化，否则会继续显示上一篇文档
        Uri current = getIntent() == null ? null : getIntent().getData();
        Uri next = intent == null ? null : intent.getData();
        setIntent(intent);
        if (!java.util.Objects.equals(current, next) && !isFinishing() && !isDestroyed()) {
            recreate();
        }
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycle;
    }

    /** PDFViewer 是 LifecycleObserver，状态必须与 Activity 同步，否则不会开始排版/渲染。 */
    private void syncLifecycle() {
        if (mViewer == null) {
            return;
        }
        if (mResumed) {
            mLifecycle.setCurrentState(Lifecycle.State.RESUMED);
        } else if (mStarted) {
            mLifecycle.setCurrentState(Lifecycle.State.STARTED);
        } else {
            mLifecycle.setCurrentState(Lifecycle.State.CREATED);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;
        syncLifecycle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        syncLifecycle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        syncLifecycle();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mStarted = false;
        syncLifecycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLifecycle.setCurrentState(Lifecycle.State.DESTROYED);
        mViewer = null;
        clearActionHandler();
    }

    /**
     * 回调对象是 PDFViewer 的静态配置。销毁时只在“当前指向本实例”时才还原，
     * 避免把后创建/上层仍存活的实例回调清掉。
     */
    private void clearActionHandler() {
        Object handler = mActionHandler;
        Object previous = mPreviousActionHandler;
        mActionHandler = null;
        mPreviousActionHandler = null;
        if (handler == null) {
            return;
        }
        try {
            ClassLoader loader = getClassLoader();
            Class<?> config = Class.forName("com.uc.pdf.c0", false, loader);
            if (config.getMethod("c").invoke(null) != handler) {
                return;
            }
            Class<?> handlerIface = Class.forName("com.uc.pdf.IPDFActionHandler", false, loader);
            config.getMethod("q", handlerIface).invoke(null, previous);
        } catch (Throwable t) {
            Log.w(TAG, "clear action handler failed", t);
        }
    }
}
