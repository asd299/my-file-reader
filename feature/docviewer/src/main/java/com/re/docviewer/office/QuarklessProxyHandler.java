package com.re.docviewer.office;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.lifecycle.Lifecycle;

import com.ucpro.office.module.IOfficeActionCallback;
import com.ucpro.office.module.IOfficeProxyHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * 离线壳的宿主回调实现，替代夸克主 APK 里带业务的 IOfficeProxyHandler。
 * 分享 / 用其他应用打开 / 另存为 / 转 PDF 都落到本机真实可用的实现，其余在线能力直接关闭。
 */
public class QuarklessProxyHandler implements IOfficeProxyHandler {

    private static final String TAG = "QuarklessProxy";
    /** com.uc.office.constant.EventConstant.APP_COUNT_PAGES_ID */
    private static final int KEY_PAGE_COUNT = 536870922;
    /** com.uc.office.constant.EventConstant.APP_PAGE_TO_PRINT_IMAGE */
    private static final int KEY_PAGE_IMAGE = 536870944;
    /** 与源码 PDFExportProcessor.MAX_PRINT_PAGE_COUNTS 一致 */
    private static final int MAX_EXPORT_PAGES = 50;
    /** 与 QuarkOfficeActivity.INTENT_PICK_IMAGE 一致，编辑态插图的选择结果靠这个 requestCode 回传 */
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String DOWNLOAD_DIR = "QuarkDocViewer";

    private static volatile Context sContext;
    private static volatile WeakReference<Activity> sActivity;

    private final Handler mMain = new Handler(Looper.getMainLooper());

    public static void setContext(Context context) {
        sContext = context;
    }

    @Override
    public void onInitOfficeActivity(Activity activity, Lifecycle lifecycle, IOfficeActionCallback callback) {
        Log.i(TAG, "onInitOfficeActivity");
        sActivity = new WeakReference<>(activity);
        sContext = activity.getApplicationContext();
    }

    @Override
    public void checkSendToPCDetail() {
    }

    @Override
    public void checkSendToPCFailure() {
    }

    @Override
    public void convertPdf(String str) {
        exportPdf(str);
    }

    @Override
    public String getQuarkDir() {
        Context context = sContext;
        if (context != null) {
            return context.getCacheDir().getAbsolutePath();
        }
        return "/storage/emulated/0/Quark/Download";
    }

    @Override
    public boolean isEnableUCOfficeMultiProcess() {
        // 离线壳只有一个进程
        return false;
    }

    @Override
    public boolean isOfficeActive() {
        return true;
    }

    @Override
    public boolean needCollectImprovementOpinion() {
        return false;
    }

    @Override
    public void onFileOpen(String str) {
        Log.i(TAG, "onFileOpen " + str);
    }

    @Override
    public void openCollectOpinionPage() {
    }

    @Override
    public void openFeedback(String str) {
        toast("离线版没有反馈通道");
    }

    @Override
    public void openFileToOtherApp(String str, String str2) {
        sendFile(str, true);
    }

    @Override
    public void openImage(String str) {
        sendFile(str, true);
    }

    @Override
    public void openImageChooser() {
        // 编辑态插图：交给系统图片选择器。requestCode 必须是 QuarkOfficeActivity.INTENT_PICK_IMAGE(1)，
        // 它会把结果通过 onActivityResult 转成 LiveDataBus 的 EDITOR_PICK_IMAGE 事件。
        Activity activity = activity();
        if (activity == null) {
            return;
        }
        try {
            activity.startActivityForResult(Intent.createChooser(
                    new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "选择图片"),
                    PICK_IMAGE_REQUEST);
        } catch (Throwable t) {
            toast("没有可用的图片选择器");
        }
    }

    @Override
    public void openPdf(String str) {
        sendFile(str, true);
    }

    @Override
    public void openPrivacyLicence() {
    }

    @Override
    public void saveAs(String str) {
        exportCopy(str);
    }

    @Override
    public void sendFileToOtherApp(String str) {
        sendFile(str, false);
    }

    @Override
    public boolean sendToPC(String str, int i, int i2, int i3) {
        // 离线壳没有“发送到电脑”能力，底部栏该入口已隐藏
        return false;
    }

    @Override
    public void showToast(String str) {
        showToast(str, 0);
    }

    @Override
    public void showToast(final String str, final int i) {
        mMain.post(new Runnable() {
            @Override
            public void run() {
                Context context = sContext;
                if (context != null) {
                    Toast.makeText(context, str, i == 1 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void updateStatBizParams(Map<String, String> map) {
    }

    @Override
    public void uploadCrashTrace(String str, Throwable th) {
        Log.e(TAG, "uploadCrashTrace " + str, th);
    }

    @Override
    public void utStatClickEvent(String str, String str2, String str3, Map<String, String> map) {
    }

    @Override
    public void utStatCustomEvent(String str, String str2, String str3, Map<String, String> map) {
    }

    @Override
    public void utStatExposureEvent(String str, String str2, String str3, Map<String, String> map) {
    }

    @Override
    public void utStatOpenFileResult(boolean z, long j, long j2, String str, boolean z2) {
        Log.i(TAG, "utStatOpenFileResult officeLoad=" + j + " docLoad=" + j2);
    }

    @Override
    public void utStatSendToPCDialogClick(Map<String, String> map) {
    }

    @Override
    public void visitFileOnPC() {
    }

    // ------------------------------------------------------------------ 实际能力

    /** 分享或“用其他应用打开”当前文件，走 FileProvider 授权。 */
    private void sendFile(String path, boolean view) {
        final Activity activity = activity();
        final File file = path == null ? null : new File(path);
        if (activity == null || file == null || !file.isFile()) {
            toast("文件不存在，无法打开");
            return;
        }
        mMain.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String mime = mimeOf(file.getName());
                    Uri uri = FileProvider.getUriForFile(activity,
                            activity.getPackageName() + ".fileprovider", file);
                    Intent target = new Intent(view ? Intent.ACTION_VIEW : Intent.ACTION_SEND);
                    target.setType(mime);
                    if (view) {
                        target.setDataAndType(uri, mime);
                    } else {
                        target.putExtra(Intent.EXTRA_STREAM, uri);
                    }
                    target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(Intent.createChooser(target,
                            view ? "用其他应用打开" : "分享"));
                } catch (Throwable t) {
                    Log.e(TAG, "send file failed", t);
                    toast("没有可用的应用： " + t.getMessage());
                }
            }
        });
    }

    /** 另存为：交给系统文件选择器，由用户决定保存位置。 */
    private void exportCopy(final String path) {
        final Activity activity = activity();
        final File source = path == null ? null : new File(path);
        if (activity == null || source == null || !source.isFile()) {
            toast("文件不存在，无法保存");
            return;
        }
        launchSave(activity, source, source.getName(), mimeOf(source.getName()), false);
    }

    /** 转 PDF：沿用源码打印导出的思路，逐页取位图写成本地 PDF。 */
    private void exportPdf(final String path) {
        final Activity activity = activity();
        if (activity == null) {
            return;
        }
        toast("正在导出 PDF…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                File temp = new File(activity.getCacheDir(), "office-export-" + System.currentTimeMillis() + ".pdf");
                PdfDocument pdf = new PdfDocument();
                // 临时 PDF 交给保存界面后不能再删；只有没交接出去（导出失败）时才清理
                boolean handedOff = false;
                try {
                    Object control = currentControl();
                    if (control == null) {
                        throw new IllegalStateException("文档尚未就绪");
                    }
                    Method getActionValue = control.getClass().getMethod("getActionValue", int.class, Object.class);
                    Object countValue = getActionValue.invoke(control, KEY_PAGE_COUNT, null);
                    int pages = countValue instanceof Integer ? (Integer) countValue : 0;
                    if (pages <= 0) {
                        throw new IllegalStateException("文档仍在加载，请稍后再试");
                    }
                    Paint paint = new Paint();
                    int exported = 0;
                    for (int page = 1; page <= Math.min(pages, MAX_EXPORT_PAGES); page++) {
                        Bitmap bitmap = (Bitmap) getActionValue.invoke(control, KEY_PAGE_IMAGE, page);
                        if (bitmap == null) {
                            continue;
                        }
                        // 与源码一致：只有取到位图的页才计入 PDF 页码
                        exported++;
                        PdfDocument.Page pdfPage = pdf.startPage(new PdfDocument.PageInfo.Builder(
                                bitmap.getWidth(), bitmap.getHeight(), exported).create());
                        pdfPage.getCanvas().drawBitmap(bitmap, 0f, 0f, paint);
                        pdf.finishPage(pdfPage);
                        // 等价于源码 PDFExportProcessor.safeRecycleBitmap
                        try {
                            bitmap.recycle();
                        } catch (Throwable ignored) {
                        }
                    }
                    if (exported == 0) {
                        throw new IllegalStateException("没有可导出的页面");
                    }
                    try (FileOutputStream out = new FileOutputStream(temp)) {
                        pdf.writeTo(out);
                    }
                    // 转换结果交给系统保存界面，由用户选择保存位置
                    launchSave(activity, temp, pdfFileName(path), "application/pdf", true);
                    handedOff = true;
                } catch (Throwable t) {
                    Log.e(TAG, "exportPdf failed", t);
                    toast("导出 PDF 失败：" + t.getMessage());
                } finally {
                    try {
                        pdf.close();
                    } catch (Throwable ignored) {
                    }
                    if (!handedOff) {
                        //noinspection ResultOfMethodCallIgnored
                        temp.delete();
                    }
                }
            }
        }, "office-export-pdf").start();
    }

    /**
     * 取当前文档的 IControl。QuarkOfficeActivity 没有 getControl() 方法，
     * 源码把 MainControl 注册在 OfficeService 单例里，这里按同样路径取。
     */
    private static Object currentControl() {
        try {
            Class<?> service = Class.forName("com.uc.office.service.OfficeService");
            Object instance = service.getMethod("instance").invoke(null);
            return service.getMethod("getControl").invoke(instance);
        } catch (Throwable t) {
            Log.e(TAG, "get IControl failed", t);
            return null;
        }
    }

    /**
     * 打开系统保存界面，由用户选择保存位置（ACTION_CREATE_DOCUMENT）。
     * 导出/复制都在工作线程里完成，启动 Activity 必须切回主线程。
     */
    private void launchSave(final Activity activity, final File source, final String name,
                            final String mime, final boolean deleteSource) {
        mMain.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(activity, SaveFileActivity.class);
                    intent.putExtra(SaveFileActivity.EXTRA_SOURCE, source.getAbsolutePath());
                    intent.putExtra(SaveFileActivity.EXTRA_NAME, name);
                    intent.putExtra(SaveFileActivity.EXTRA_MIME, mime);
                    intent.putExtra(SaveFileActivity.EXTRA_DELETE_SOURCE, deleteSource);
                    activity.startActivity(intent);
                } catch (Throwable t) {
                    Log.e(TAG, "launch save failed", t);
                    toast("无法打开保存界面：" + t.getMessage());
                }
            }
        });
    }

    /** 导出文件名：优先沿用原文件名，path 为空时给个时间戳兜底。 */
    private static String pdfFileName(String path) {
        String base = path == null ? "" : stripExtension(new File(path).getName());
        if (base.isEmpty()) {
            base = "office_export_" + System.currentTimeMillis();
        }
        return base + ".pdf";
    }

    /**
     * 写到系统下载目录；Android 10 以下没有 MediaStore.Downloads，退回应用外部目录。
     * 系统没有文件选择器时，SaveFileActivity 用这条路径兜底。
     */
    static String saveToDownloads(Activity activity, File source, String name, String mime) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + DOWNLOAD_DIR);
            Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IllegalStateException("无法在下载目录创建文件");
            }
            try (OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    throw new IllegalStateException("无法写入下载目录");
                }
                copy(source, out);
            }
            return "下载/" + DOWNLOAD_DIR + "/" + name;
        }
        File external = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (external == null) {
            // 外部存储不可用（未挂载/已卸载）时 getExternalFilesDir 返回 null
            throw new IllegalStateException("外部存储不可用");
        }
        File dir = new File(external, DOWNLOAD_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建保存目录");
        }
        File target = new File(dir, name);
        try (OutputStream out = new FileOutputStream(target)) {
            copy(source, out);
        }
        return target.getAbsolutePath();
    }

    private static void copy(File source, OutputStream out) throws Exception {
        try (InputStream in = new FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String mimeOf(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        }
        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        // openImage 会把文档里点开的图片交给外部看图应用，缺了图片类型会退化成
        // application/octet-stream，很多看图应用因此不响应
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    private void toast(String message) {
        showToast(message, 0);
    }

    private static Activity activity() {
        WeakReference<Activity> ref = sActivity;
        Activity activity = ref == null ? null : ref.get();
        return activity == null || activity.isFinishing() ? null : activity;
    }
}
