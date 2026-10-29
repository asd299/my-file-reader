package com.google.android.play.core.splitinstall;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;

import com.re.docviewer.DocViewerBootstrap;

import java.io.File;

/**
 * 宿主桩：夸克主包里该类的 loadResources 走 Qigsaw 的 SplitCompatResourcesLoader，
 * 把已安装 split 的 apk 路径 addAssetPath 到传入 Resources 的 AssetManager 上。
 * 这里同样实现，只是 split 集合固定为壳工程的 ucoffice 核心包 + 远程包。
 * 方法签名与夸克 classes12 中的真实类保持一致，避免 split 内调用方 NoSuchMethodError。
 */
public final class SplitInstallHelper {

    private SplitInstallHelper() {
    }

    public static void loadResources(Activity activity, Resources resources) {
        DocViewerBootstrap.installSplitResources(resources);
    }

    public static void loadResources(Service service) {
        DocViewerBootstrap.installSplitResources(service.getBaseContext().getResources());
    }

    public static void loadResources(BroadcastReceiver receiver, Context context) {
        DocViewerBootstrap.installSplitResources(context.getResources());
    }

    public static void updateAppInfo(Context context) {
    }

    /** 与夸克 classes12 里的实现一致：先按库名加载，失败再试 nativeLibraryDir 下的绝对路径。 */
    public static void loadLibrary(Context context, String name) {
        try {
            System.loadLibrary(name);
        } catch (UnsatisfiedLinkError e) {
            String path = context.getApplicationInfo().nativeLibraryDir + "/"
                    + System.mapLibraryName(name);
            if (!new File(path).exists()) {
                throw e;
            }
            System.load(path);
        }
    }
}
