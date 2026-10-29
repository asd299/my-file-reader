package com.re.docviewer.office;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * 把夸克 ucoffice 的白色系界面改成「轻阅」主题色。
 * <p>
 * office 的顶栏 / 底栏全部由代码 new 出来（split 里只有 4 个 layout），
 * 颜色写在 {@code OfficeBottomItem.initView()} 这类方法里（#FFFAFAFA），
 * 资源层面改不到，所以在这里运行时重设：
 * <ul>
 *     <li>状态栏 / 导航栏：主色 {@link #COLOR_PRIMARY}，并清掉浅色图标标记（白字）；</li>
 *     <li>顶栏 / 底栏：浅青底 {@link #COLOR_BAR_BACKGROUND}。</li>
 * </ul>
 * 底栏要等文档解析完成（{@code openFileFinish}）才 addView 到 {@code OfficeFrameLayout}，
 * 因此除首轮遍历外还挂一层 {@code OnHierarchyChangeListener} 补刷后加的视图。
 */
public final class OfficeThemeColors {

    private static final String TAG = "OfficeThemeColors";

    /** 主色（与 core:ui 的 md_theme_light_primary 一致）。 */
    public static final int COLOR_PRIMARY = 0xFF006A68;
    /** 顶栏 / 底栏底色。 */
    public static final int COLOR_BAR_BACKGROUND = 0xFFE6F4F3;

    private static final String CLASS_TOP_BAR = "com.uc.office.widget.OfficeTopBar";
    private static final String CLASS_BOTTOM_BAR = "com.uc.office.widget.OfficeBottomBar";
    private static final String CLASS_BOTTOM_ITEM = "com.uc.office.widget.OfficeBottomItem";
    private static final String CLASS_UPLOAD_ITEM = "com.uc.office.widget.UploadCloudBottomItem";
    private static final String PACKAGE_PREFIX = "com.uc.office.widget.";

    /** 只对 split 自己的容器挂监听，避免污染文档视图内部的 ViewGroup。 */
    private static final ViewGroup.OnHierarchyChangeListener LISTENER =
            new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    tint(child);
                    watch(child);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            };

    private static Method sSetBarBackgroundColor;
    private static boolean sMethodResolved;

    private OfficeThemeColors() {
    }

    public static void apply(Activity activity) {
        if (activity == null) {
            return;
        }
        styleSystemBars(activity);
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) content;
        for (int i = 0; i < group.getChildCount(); i++) {
            tint(group.getChildAt(i));
            watch(group.getChildAt(i));
        }
    }

    /** 状态栏 / 导航栏：主色底 + 白色图标（清掉 SystemUI 的浅色图标标记）。 */
    private static void styleSystemBars(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(COLOR_PRIMARY);
            window.setNavigationBarColor(COLOR_PRIMARY);
        }
        View decor = window.getDecorView();
        int visibility = decor.getSystemUiVisibility();
        // SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(visibility);
    }

    private static void tint(View view) {
        if (view == null) {
            return;
        }
        String name = view.getClass().getName();
        if (CLASS_TOP_BAR.equals(name)) {
            invokeSetBarBackgroundColor(view, COLOR_BAR_BACKGROUND);
        } else if (CLASS_BOTTOM_BAR.equals(name)
                || CLASS_BOTTOM_ITEM.equals(name)
                || CLASS_UPLOAD_ITEM.equals(name)) {
            view.setBackgroundColor(COLOR_BAR_BACKGROUND);
        }
    }

    /** 对 split 的 ViewGroup 递归挂监听，覆盖文档加载后才加进来的顶栏 / 底栏。 */
    private static void watch(View view) {
        if (!(view instanceof ViewGroup)) {
            return;
        }
        String name = view.getClass().getName();
        if (!name.startsWith(PACKAGE_PREFIX)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        group.setOnHierarchyChangeListener(LISTENER);
        for (int i = 0; i < group.getChildCount(); i++) {
            tint(group.getChildAt(i));
            watch(group.getChildAt(i));
        }
    }

    private static void invokeSetBarBackgroundColor(View topBar, int color) {
        if (!sMethodResolved) {
            sMethodResolved = true;
            try {
                sSetBarBackgroundColor = topBar.getClass()
                        .getMethod("setBarBackgroundColor", int.class);
            } catch (Throwable t) {
                Log.w(TAG, "setBarBackgroundColor not found", t);
            }
        }
        if (sSetBarBackgroundColor == null) {
            topBar.setBackgroundColor(color);
            return;
        }
        try {
            sSetBarBackgroundColor.invoke(topBar, color);
        } catch (Throwable t) {
            Log.w(TAG, "setBarBackgroundColor failed", t);
        }
    }
}
