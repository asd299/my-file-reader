package com.re.docviewer.office;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 让用户选择保存位置：ACTION_CREATE_DOCUMENT 交给系统文件选择器，选好后把来源文件复制过去。
 * 系统没有 DocumentsUI（极少数 ROM）时退回“下载/QuarkDocViewer”。
 * <p>
 * 之所以要单独做一个 Activity：QuarkOfficeActivity 是 split 里的类，壳工程无法接管它的
 * onActivityResult，而 SAF 必须靠结果回调拿到目标 Uri。
 */
public class SaveFileActivity extends Activity {

    private static final String TAG = "SaveFileActivity";
    public static final String EXTRA_SOURCE = "save_source";
    public static final String EXTRA_NAME = "save_name";
    public static final String EXTRA_MIME = "save_mime";
    /** 复制完成后是否删掉来源文件（转 PDF 的临时产物需要） */
    public static final String EXTRA_DELETE_SOURCE = "save_delete_source";
    private static final int REQUEST_CREATE_DOCUMENT = 1001;

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private File mSource;
    private String mName;
    private String mMime;
    private boolean mDeleteSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String path = intent == null ? null : intent.getStringExtra(EXTRA_SOURCE);
        mSource = path == null ? null : new File(path);
        mName = intent == null ? null : intent.getStringExtra(EXTRA_NAME);
        mMime = intent == null ? null : intent.getStringExtra(EXTRA_MIME);
        mDeleteSource = intent != null && intent.getBooleanExtra(EXTRA_DELETE_SOURCE, false);
        if (mSource == null || !mSource.isFile()) {
            toast("文件不存在，无法保存");
            finish();
            return;
        }
        if (TextUtils.isEmpty(mName)) {
            mName = mSource.getName();
        }
        if (TextUtils.isEmpty(mMime)) {
            mMime = "application/octet-stream";
        }
        try {
            startActivityForResult(buildCreateIntent(), REQUEST_CREATE_DOCUMENT);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "no document picker, fallback to downloads", e);
            mMain.post(this::fallbackToDownloads);
        }
    }

    private Intent buildCreateIntent() {
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType(mMime);
        create.putExtra(Intent.EXTRA_TITLE, mName);
        return create;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_DOCUMENT) {
            return;
        }
        Uri target = data == null ? null : data.getData();
        if (resultCode != RESULT_OK || target == null) {
            toast("已取消保存");
            finishAndCleanup();
            return;
        }
        new Thread(() -> {
            try {
                writeTo(target);
                mMain.post(() -> {
                    toast("已保存到 " + describe(target));
                    finishAndCleanup();
                });
            } catch (Throwable t) {
                Log.e(TAG, "save failed", t);
                mMain.post(() -> {
                    toast("保存失败：" + t.getMessage());
                    finishAndCleanup();
                });
            }
        }, "save-file").start();
    }

    private void writeTo(Uri target) throws Exception {
        try (InputStream in = new FileInputStream(mSource);
             OutputStream out = getContentResolver().openOutputStream(target)) {
            if (out == null) {
                throw new IOException("无法写入所选位置");
            }
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }

    private void fallbackToDownloads() {
        new Thread(() -> {
            try {
                String where = QuarklessProxyHandler.saveToDownloads(this, mSource, mName, mMime);
                mMain.post(() -> {
                    toast("已保存到 " + where);
                    finishAndCleanup();
                });
            } catch (Throwable t) {
                Log.e(TAG, "fallback save failed", t);
                mMain.post(() -> {
                    toast("保存失败：" + t.getMessage());
                    finishAndCleanup();
                });
            }
        }, "save-file-fallback").start();
    }

    /** 目标 Uri 在部分第三方选择器上查不到名字，退回显示 Uri。 */
    private String describe(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String display = cursor.getString(index);
                    if (!TextUtils.isEmpty(display)) {
                        return display;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "query display name failed", t);
        }
        return uri.toString();
    }

    private void finishAndCleanup() {
        if (mDeleteSource && mSource != null) {
            //noinspection ResultOfMethodCallIgnored
            mSource.delete();
        }
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
