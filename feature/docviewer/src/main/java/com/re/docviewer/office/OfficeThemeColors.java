package com.re.docviewer.office;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 把夸克 ucoffice 的白色系界面改成「轻阅」主题色。
 * <p>
 * office 的顶栏 / 底栏 / 弹窗全部由代码 new 出来（split 里只有 4 个 layout），
 * 颜色写死在构造方法里（{@code OfficeBottomItem.initView()} 的 #FFFAFAFA、
 * {@code AbsOfficeDialog.initRootView()} 的白色圆角卡片、确认按钮的 #FF2828FF 等），
 * 资源层面改不到，所以在这里运行时重设：
 * <ul>
 *     <li>状态栏 / 导航栏：主色 {@link #COLOR_PRIMARY}，并清掉浅色图标标记（白字）；</li>
 *     <li>顶栏 / 底栏：浅青底 {@link #COLOR_BAR_BACKGROUND}；</li>
 *     <li>弹窗：浅青卡片 + 青绿强调按钮（{@link #COLOR_DIALOG_SURFACE}）。</li>
 * </ul>
 * 底栏要等文档解析完成（{@code openFileFinish}）才 addView 到 {@code OfficeFrameLayout}，
 * 因此除首轮遍历外还挂一层 {@code OnHierarchyChangeListener} 补刷后加的视图。
 * 弹窗不是 Activity 的生命周期一环，只能轮询 {@code OfficeDialogHelper.sCurrentDialog}。
 */
public final class OfficeThemeColors {

    private static final String TAG = "OfficeThemeColors";

    /** 主色（与 core:ui 的 md_theme_light_primary 一致）。 */
    public static final int COLOR_PRIMARY = 0xFF006A68;
    /** 顶栏 / 底栏底色。 */
    public static final int COLOR_BAR_BACKGROUND = 0xFFE6F4F3;
    /** 弹窗卡片底色。 */
    public static final int COLOR_DIALOG_SURFACE = 0xFFF2F8F7;
    /** 弹窗里菜单图标的小方块底色。 */
    public static final int COLOR_DIALOG_TILE = 0xFFDFF0EE;
    /** 弹窗确认按钮底色。 */
    public static final int COLOR_DIALOG_ACCENT = 0xFFCFE8E4;
    /** 弹窗取消按钮底色。 */
    public static final int COLOR_DIALOG_NEUTRAL = 0xFFE9F4F2;

    /** split 代码里写死、需要替换的颜色。 */
    private static final int OLD_TILE = 0xFFF4F4F4;
    private static final int OLD_ACCENT_TEXT = 0xFF2828FF;

    private static final String CLASS_LOADING_DIALOG = "com.uc.office.widget.dialog.OfficeLoadingDialog";
    private static final String CLASS_DIALOG_HEADER = "com.uc.office.widget.dialog.OfficeDialogCommonHeaderView";
    private static final String CLASS_DIALOG_HELPER = "com.uc.office.widget.dialog.OfficeDialogHelper";
    private static final String CLASS_ABS_DIALOG = "com.uc.office.widget.dialog.AbsOfficeDialog";
    /** 弹窗出现后最多晚这么久才换色（没有 Dialog 级别的回调可挂）。 */
    private static final long DIALOG_POLL_MS = 100L;

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

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<Dialog> sTintedDialogs =
            Collections.newSetFromMap(new WeakHashMap<Dialog, Boolean>());
    private static Activity sWatchedActivity;
    private static Field sCurrentDialogField;
    private static boolean sDialogFieldResolved;
    private static Field sDialogRootField;
    private static boolean sDialogRootResolved;

    private OfficeThemeColors() {
    }

    public static void apply(Activity activity) {
        if (activity == null) {
            return;
        }
        styleSystemBars(activity);
        tintOfficeViews(activity);
        watchDialogs(activity);
    }

    private static void tintOfficeViews(Activity activity) {
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

    // ---------------------------------------------------------------- 弹窗

    /**
     * 弹窗不是 Activity 生命周期的一部分，split 也没有对外暴露回调，只能轮询
     * {@code OfficeDialogHelper.sCurrentDialog}（全类共用同一个“当前弹窗”）。
     */
    private static void watchDialogs(Activity activity) {
        sWatchedActivity = activity;
        MAIN_HANDLER.removeCallbacks(DIALOG_POLLER);
        MAIN_HANDLER.postDelayed(DIALOG_POLLER, DIALOG_POLL_MS);
    }

    private static final Runnable DIALOG_POLLER = new Runnable() {
        @Override
        public void run() {
            Activity activity = sWatchedActivity;
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                sWatchedActivity = null;
                return;
            }
            try {
                tintCurrentDialog();
            } catch (Throwable t) {
                Log.w(TAG, "tint dialog failed", t);
            }
            MAIN_HANDLER.postDelayed(this, DIALOG_POLL_MS);
        }
    };

    private static void tintCurrentDialog() {
        Dialog dialog = currentDialog();
        if (dialog == null || !dialog.isShowing() || sTintedDialogs.contains(dialog)) {
            return;
        }
        // 加载浮层自带深色卡片（OfficeLoadingDialog 直接 setContentView），不需要换色
        if (CLASS_LOADING_DIALOG.equals(dialog.getClass().getName())) {
            sTintedDialogs.add(dialog);
            return;
        }
        View root = dialogRoot(dialog);
        if (root == null || root.getWindowToken() == null) {
            return;
        }
        sTintedDialogs.add(dialog);
        root.setBackground(roundRect(24, COLOR_DIALOG_SURFACE));
        tintDialogTree(root);
    }

    private static Dialog currentDialog() {
        if (!sDialogFieldResolved) {
            sDialogFieldResolved = true;
            try {
                Class<?> helper = Class.forName(CLASS_DIALOG_HELPER, false,
                        OfficeThemeColors.class.getClassLoader());
                sCurrentDialogField = helper.getDeclaredField("sCurrentDialog");
                sCurrentDialogField.setAccessible(true);
            } catch (Throwable t) {
                Log.w(TAG, "OfficeDialogHelper not found", t);
            }
        }
        if (sCurrentDialogField == null) {
            return null;
        }
        try {
            Object dialog = sCurrentDialogField.get(null);
            return dialog instanceof Dialog ? (Dialog) dialog : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** {@code AbsOfficeDialog.mRoot}：白色圆角卡片本身。 */
    private static View dialogRoot(Dialog dialog) {
        if (!sDialogRootResolved) {
            sDialogRootResolved = true;
            try {
                Class<?> abs = Class.forName(CLASS_ABS_DIALOG, false,
                        OfficeThemeColors.class.getClassLoader());
                sDialogRootField = abs.getDeclaredField("mRoot");
                sDialogRootField.setAccessible(true);
            } catch (Throwable t) {
                Log.w(TAG, "AbsOfficeDialog not found", t);
            }
        }
        if (sDialogRootField == null) {
            return null;
        }
        try {
            Object root = sDialogRootField.get(dialog);
            return root instanceof View ? (View) root : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void tintDialogTree(View view) {
        String name = view.getClass().getName();
        if (view instanceof Button) {
            tintDialogButton((Button) view);
            return;
        }
        if (CLASS_DIALOG_HEADER.equals(name)) {
            // 头部原本是一块白色底衬，跟卡片同色才不会有接缝
            view.setBackgroundColor(COLOR_DIALOG_SURFACE);
            return;
        }
        Drawable background = view.getBackground();
        if (background instanceof ShapeDrawable) {
            Paint paint = ((ShapeDrawable) background).getPaint();
            if (paint.getColor() == OLD_TILE) {
                // 菜单项图标下方的小方块
                paint.setColor(COLOR_DIALOG_TILE);
                view.invalidate();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintDialogTree(group.getChildAt(i));
            }
        }
    }

    /** 确认按钮原来是 #FF2828FF 字 + 淡蓝底，其余按钮是 #FF222222 字 + 灰底。 */
    private static void tintDialogButton(Button button) {
        boolean accent = button.getCurrentTextColor() == OLD_ACCENT_TEXT;
        if (accent) {
            button.setTextColor(COLOR_PRIMARY);
        }
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed},
                roundRect(12, accent ? 0x99CFE8E4 : 0x99E9F4F2));
        background.addState(new int[0],
                roundRect(12, accent ? COLOR_DIALOG_ACCENT : COLOR_DIALOG_NEUTRAL));
        button.setBackground(background);
    }

    private static GradientDrawable roundRect(int radiusDp, int color) {
        float density = android.content.res.Resources.getSystem()
                .getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(radiusDp * density);
        drawable.setColor(color);
        return drawable;
    }
}
