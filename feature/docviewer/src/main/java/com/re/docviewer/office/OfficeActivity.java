package com.re.docviewer.office;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.re.docviewer.DocViewerBootstrap;
import com.re.docviewer.R;
import com.re.docviewer.pdf.PdfViewerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件落地页：把 SAF Uri 拷到私有目录，然后把 intent 原样交给 ucoffice 源码里的
 * OfficeSdkModuleImpl#openActivity + OfficeStartUp#startup 逻辑，由真实的
 * com.uc.office.ui.QuarkOfficeActivity 接管显示。
 */
public class OfficeActivity extends Activity {

    private static final String TAG = "OfficeLauncher";
    public static final String EXTRA_PATH = "office_path";
    public static final String EXTRA_TITLE = "office_title";
    private static final int REQ_STORAGE = 1002;

    private TextView mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // API 29 以下不会回调 onActivityPreCreated，这里补挂一次 split 资源
        DocViewerBootstrap.installSplitResources(getResources());
        // PDF 走 pdfium 的专属容器页，其它 Office 文档走 ucoffice
        if (routePdfIfNeeded()) {
            return;
        }
        buildUi();

        if (DocViewerBootstrap.getBootError() != null) {
            mStatus.setText("ucoffice 引擎初始化失败：\n\n" + DocViewerBootstrap.getBootError());
            return;
        }
        if (!DocViewerBootstrap.isDexInjected()) {
            mStatus.setText("ucoffice dex 注入失败，无法启动查看器");
            return;
        }
        // QuarkOfficeActivity 在 API<=29 上以 WRITE_EXTERNAL_STORAGE 为门槛，
        // SystemUtil.isScopedStorage() 只在 SDK_INT>29 才返回 true，先在宿主补授权。
        if (needStoragePermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        prepareOffice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) {
            prepareOffice();
        }
    }

    /** 以 FileViewer 主题色填充背景，而不是写死白色。 */
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(themeBackgroundColor());
        root.setGravity(Gravity.CENTER);
        mStatus = new TextView(this);
        mStatus.setGravity(Gravity.CENTER);
        mStatus.setTextSize(14);
        mStatus.setTextColor(color(R.color.doc_on_dialog));
        mStatus.setPadding(dp(16), dp(16), dp(16), dp(16));
        mStatus.setText("正在准备文档…");
        // 失败时这里会显示完整堆栈，必须可滚动
        ScrollView scroll = new ScrollView(this);
        scroll.addView(mStatus, new ScrollView.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Button copy = new Button(this);
        copy.setText("复制错误信息");
        copy.setTextColor(color(R.color.doc_on_primary));
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setCornerRadius(dp(20));
        copyBg.setColor(color(R.color.doc_primary));
        copy.setBackground(copyBg);
        copy.setOnClickListener(v -> copyError());
        root.addView(copy, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private int themeBackgroundColor() {
        TypedArray array = obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
        try {
            return array.getColor(0, Color.TRANSPARENT);
        } finally {
            array.recycle();
        }
    }

    /** ACTION_VIEW 进来的 PDF 转发给 PdfViewerActivity，其余类型返回 false 继续走 ucoffice。 */
    private boolean routePdfIfNeeded() {
        Intent intent = getIntent();
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction()) || intent.getData() == null) {
            return false;
        }
        Uri uri = intent.getData();
        String name = intent.getStringExtra(EXTRA_TITLE);
        if (TextUtils.isEmpty(name)) {
            name = queryDisplayName(uri);
        }
        String mime = null;
        try {
            mime = getContentResolver().getType(uri);
        } catch (Throwable ignored) {
        }
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf") && !"application/pdf".equals(mime)) {
            return false;
        }
        Intent pdf = new Intent(this, PdfViewerActivity.class);
        pdf.setData(uri);
        pdf.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (name != null) {
            pdf.putExtra(PdfViewerActivity.EXTRA_TITLE, name);
        }
        startActivity(pdf);
        finish();
        return true;
    }

    private boolean needStoragePermission() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void prepareOffice() {
        final Uri uri = getIntent() == null ? null : getIntent().getData();
        final String extraPath = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_PATH);
        new Thread(() -> {
            try {
                File file = !TextUtils.isEmpty(extraPath) ? new File(extraPath) : copyToCache(uri);
                if (file == null || !file.isFile()) {
                    throw new IOException("无法读取所选文件");
                }
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        launchViewer(file, uri);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "prepare file failed", t);
                runOnUiThread(() -> mStatus.setText("读取文件失败：\n\n" + Log.getStackTraceString(t)));
            }
        }, "office-prepare").start();
    }

    private void launchViewer(File file, Uri uri) {
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), "com.uc.office.ui.QuarkOfficeActivity");
            intent.putExtra("office_file_path", file.getAbsolutePath());
            intent.putExtra("filePath", file.getAbsolutePath());
            if (uri != null) {
                intent.setData(uri);
                intent.putExtra("fileUri", uri.toString());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            // UCOfficeProxyHandler 读取的宿主回调类
            intent.putExtra("office_proxy_handler_class", QuarklessProxyHandler.class.getName());
            intent.putExtra("proxyClass", "com.uc.office.impl.UCOfficeProxyHandler");
            intent.putExtra("startOfficeTime", SystemClock.elapsedRealtime());
            intent.putExtra("entry", "other");
            intent.putExtra("handledOfficeInstall", false);
            intent.putExtra("useFreeConvertTips", false);
            intent.putExtra("alreadyUpload", false);
            intent.putExtra("topBarShareEnable", true);
            // 打印走源码 BaseOfficeProxyHandler 的本地 PDF 导出 + PrintManager，离线可用
            intent.putExtra("bottomBarPrinterEnable", true);
            // 底部“发送到电脑”依赖云端，离线不可用，直接隐藏
            intent.putExtra("bottomBarUploadEnable", false);
            intent.putExtra("bottomBarShareEnable", false);
            intent.putExtra("bottomBarConfigClass", (String) null);

            Class<?> startup = Class.forName("com.uc.office.startup.OfficeStartUp");
            startup.getMethod("enableMultiProcess", boolean.class).invoke(null, false);
            startup.getMethod("startup", android.content.Context.class, Intent.class)
                    .invoke(null, this, intent);
            Log.i(TAG, "started QuarkOfficeActivity for " + file.getAbsolutePath());
            finish();
        } catch (Throwable t) {
            Log.e(TAG, "launch viewer failed", t);
            mStatus.setText("启动查看器失败：\n\n" + Log.getStackTraceString(t));
        }
    }

    private void copyError() {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null) {
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText("error", mStatus.getText()));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    private File copyToCache(Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("没有文件 Uri");
        }
        String name = queryDisplayName(uri);
        if (TextUtils.isEmpty(name)) {
            name = "document.bin";
        }
        File dir = new File(getCacheDir(), "office-open");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建缓存目录");
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(int resId) {
        return getResources().getColor(resId, getTheme());
    }
}
