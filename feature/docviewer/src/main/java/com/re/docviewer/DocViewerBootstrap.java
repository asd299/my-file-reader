package com.re.docviewer;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import me.weishu.reflection.Reflection;

/**
 * 离线文档查看器的引导逻辑（融合前是壳工程的 Application）：
 * 1. 解除隐藏 API 限制（源码 QuarkOfficeActivity.attachBaseContext 也调用 Reflection.unseal）；
 * 2. 把 assets 里的核心包（ucoffice / pdfviewer）解到私有目录；
 * 3. 与 Qigsaw 的 SplitCompatResourcesLoader 一致：对目标 Resources 的 AssetManager
 *    调 addAssetPath，把两个 split 的资源挂进去（源码 SplitInstallHelper.loadResources 走这条链路）；
 * 4. 把核心包 dex 注入 App ClassLoader，系统才能实例化 split 里的 Activity / View。
 * <p>
 * 由宿主 Application 的 attachBaseContext / onCreate / getResources / getAssets 调用。
 */
public final class DocViewerBootstrap {

    private static final String TAG = "DocViewerBootstrap";
    /** ucoffice split 的应用主题 id，运行期设置给 com.uc.office.* Activity。 */
    public static final int OFFICE_THEME_RES = 0x70080001;
    /** split 包内容版本：资产变更时手动递增，用于让已解压的旧副本失效。 */
    private static final String SPLIT_ASSET_VERSION = "2";
    static final String OFFICE_PKG = "com.quark.browser.ucoffice";
    static final String PDF_PKG = "com.quark.browser.pdfviewer";

    private static final Map<AssetManager, Boolean> sInstalled = new WeakHashMap<>();

    private static File sOfficeMaster;
    private static File sPdfMaster;
    private static String sBootError;
    private static boolean sDexInjected;

    private DocViewerBootstrap() {
    }

    /** 在 Application.attachBaseContext 之后调用。 */
    public static void init(Context base) {
        try {
            Reflection.unseal(base);
            extractSplits(base);
            installSplitResources(base.getResources());
            injectDex(base);
            Log.i(TAG, "bootstrap ok, injected=" + sDexInjected);
        } catch (Throwable t) {
            sBootError = Log.getStackTraceString(t);
            Log.e(TAG, "bootstrap failed", t);
        }
    }

    public static String getBootError() {
        return sBootError;
    }

    public static boolean isDexInjected() {
        return sDexInjected;
    }

    /**
     * API 29+ 的 onActivityPreCreated：setTheme 必须赶在 onCreate 之前，
     * 因此主题切换只在这里做。
     */
    public static void onActivityPreCreated(Activity activity) {
        installForContext(activity);
        String name = activity.getClass().getName();
        if (name.startsWith("com.uc.office.")) {
            try {
                activity.setTheme(OFFICE_THEME_RES);
            } catch (Throwable t) {
                Log.e(TAG, "setTheme failed", t);
            }
        }
    }

    /**
     * onActivityCreated 兜底（API 29 以下没有 preCreated 回调）：
     * split 控件常拿 application context 造 View，必须把 Activity / baseContext /
     * Application 三份 Resources 都补挂一遍。
     */
    public static void onActivityCreated(Activity activity) {
        String name = activity.getClass().getName();
        if (name.startsWith("com.uc.office.") || name.startsWith("com.re.docviewer.")) {
            installForContext(activity);
        }
    }

    /** 一次把 Activity / Application / baseContext 三份 Resources 都补挂上 split 资源。 */
    public static void installForContext(Context context) {
        if (context == null) {
            return;
        }
        if (context instanceof ContextWrapper) {
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base != null) {
                installSplitResources(base.getResources());
            }
        }
        installSplitResources(context.getResources());
        Context app = context.getApplicationContext();
        if (app != null && app != context) {
            installSplitResources(app.getResources());
        }
    }

    /**
     * 等价于 Quark 的 SplitInstallHelper.loadResources -> Qigsaw n.c.f：
     * 对目标 Resources 的 AssetManager 追加两个 split 包路径。
     */
    public static boolean installSplitResources(Resources resources) {
        if (resources == null || (sOfficeMaster == null && sPdfMaster == null)) {
            return false;
        }
        AssetManager assets = resources.getAssets();
        synchronized (sInstalled) {
            if (Boolean.TRUE.equals(sInstalled.get(assets))) {
                return true;
            }
            try {
                Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
                addAssetPath.setAccessible(true);
                int office = sOfficeMaster == null ? 0
                        : (Integer) addAssetPath.invoke(assets, sOfficeMaster.getAbsolutePath());
                int pdf = sPdfMaster == null ? 0
                        : (Integer) addAssetPath.invoke(assets, sPdfMaster.getAbsolutePath());
                // 探针直接查崩溃点用到的 id（loading / activity_pdfviewer），比只看 cookie 可靠：
                // 同一路径重复 addAssetPath 会返回 0，但包其实已经在表里了。
                int officeProbe = resources.getIdentifier("loading", "string", OFFICE_PKG);
                int pdfProbe = resources.getIdentifier("activity_pdfviewer", "layout", PDF_PKG);
                Log.i(TAG, "install resources@0x" + Integer.toHexString(System.identityHashCode(resources))
                        + " office=" + office + " pdf=" + pdf
                        + " probeLoading=" + Integer.toHexString(officeProbe)
                        + " probePdfLayout=" + Integer.toHexString(pdfProbe));
                boolean officeOk = sOfficeMaster == null || office != 0 || officeProbe != 0;
                boolean pdfOk = sPdfMaster == null || pdf != 0 || pdfProbe != 0;
                if (officeOk && pdfOk) {
                    sInstalled.put(assets, Boolean.TRUE);
                    return true;
                }
                Log.w(TAG, "split resources not added on this Resources");
            } catch (Throwable t) {
                Log.e(TAG, "install split resources failed", t);
            }
        }
        return false;
    }

    private static void extractSplits(Context base) throws Exception {
        File officeDir = new File(base.getFilesDir(), "ucoffice");
        if (!officeDir.exists() && !officeDir.mkdirs()) {
            throw new IllegalStateException("无法创建 ucoffice 目录");
        }
        sOfficeMaster = extractAsset(base, "ucoffice/ucoffice-master.apk",
                new File(officeDir, "ucoffice-master.apk"));

        File pdfDir = new File(base.getFilesDir(), "pdfviewer");
        if (!pdfDir.exists() && !pdfDir.mkdirs()) {
            throw new IllegalStateException("无法创建 pdfviewer 目录");
        }
        // pdfviewer 的原生库已在 jniLibs 里，远程包只需核心包
        sPdfMaster = extractAsset(base, "pdfviewer/pdfviewer-master.apk",
                new File(pdfDir, "pdfviewer-master.apk"));
    }

    private static File extractAsset(Context base, String assetName, File target) throws Exception {
        // 用版本标记判断是否需要重新解压：patch 过的 split 包体积可能与旧副本相同，
        // 只比 length 会把旧副本当成最新的。
        File marker = new File(target.getAbsolutePath() + ".ver");
        String expected = SPLIT_ASSET_VERSION + ":" + assetName;
        if (target.exists() && marker.isFile()) {
            try (InputStream in = new FileInputStream(marker)) {
                byte[] tag = new byte[(int) marker.length()];
                int read = 0;
                int n;
                while (read < tag.length && (n = in.read(tag, read, tag.length - read)) > 0) {
                    read += n;
                }
                if (read > 0 && expected.equals(new String(tag, 0, read, "UTF-8"))) {
                    return target;
                }
            } catch (Throwable ignored) {
            }
        }
        try (InputStream in = base.getAssets().open(assetName);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
        try (FileOutputStream out = new FileOutputStream(marker)) {
            out.write(expected.getBytes("UTF-8"));
        }
        return target;
    }

    /** 把 split 核心包 dex 追加到 App ClassLoader，供系统实例化 split 里的 Activity。 */
    private static void injectDex(Context base) throws Exception {
        ClassLoader loader = base.getClassLoader();
        Field pathListField = Class.forName("dalvik.system.BaseDexClassLoader")
                .getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(loader);

        Method addDexPath;
        Object[] extra;
        try {
            addDexPath = pathList.getClass().getDeclaredMethod("addDexPath", String.class, boolean.class);
            extra = new Object[]{Boolean.TRUE};
        } catch (NoSuchMethodException e) {
            try {
                addDexPath = pathList.getClass().getDeclaredMethod("addDexPath", String.class, File.class);
                // 26/27 上第二参是 dex 优化输出目录，传 null 会 NPE
                extra = new Object[]{base.getCodeCacheDir()};
            } catch (NoSuchMethodException e2) {
                addDexPath = pathList.getClass().getDeclaredMethod("addDexPath", String.class);
                extra = new Object[0];
            }
        }
        addDexPath.setAccessible(true);

        for (File apk : new File[]{sOfficeMaster, sPdfMaster}) {
            if (apk == null) {
                continue;
            }
            Object[] args = new Object[1 + extra.length];
            args[0] = apk.getAbsolutePath();
            System.arraycopy(extra, 0, args, 1, extra.length);
            addDexPath.invoke(pathList, args);
        }
        Class.forName("com.uc.office.ui.QuarkOfficeActivity", false, loader);
        Class.forName("com.uc.pdf.PDFViewer", false, loader);
        sDexInjected = true;
        Log.i(TAG, "dex injected via " + addDexPath);
    }
}
