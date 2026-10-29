package com.re.docviewer.office;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.re.docviewer.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 把夸克 ucoffice 的白色系界面改成「轻阅」主题色（浅色 / 深色都跟随）。
 * <p>
 * office 的顶栏 / 底栏 / 弹窗全部由代码 new 出来（split 里只有 4 个 layout），
 * 颜色写死在构造方法里（{@code OfficeBottomItem.initView()} 的 #FFFAFAFA、
 * {@code AbsOfficeDialog.initRootView()} 的白色圆角卡片、确认按钮的 #FF2828FF 等），
 * 资源层面改不到，所以在这里运行时重设：
 * <ul>
 *     <li>状态栏 / 导航栏：主色 {@code doc_primary}，并清掉浅色图标标记；</li>
 *     <li>顶栏 / 底栏：{@code doc_chrome}，文字 / 图标 {@code doc_on_chrome}；</li>
 *     <li>弹窗：{@code doc_dialog} 卡片 + 青绿强调按钮。</li>
 * </ul>
 * 底栏要等文档解析完成（{@code openFileFinish}）才 addView 到 {@code OfficeFrameLayout}，
 * 因此除首轮遍历外还挂一层 {@code OnHierarchyChangeListener} 补刷后加的视图。
 * 弹窗不是 Activity 生命周期的一环，只能轮询 {@code OfficeDialogHelper.sCurrentDialog}；
 * 底栏选中态的颜色是 {@code select()} 里写死的 #FF2828FF，故顺带按同一节奏补刷。
 */
public final class OfficeThemeColors {

    private static final String TAG = "OfficeThemeColors";

    private static final String CLASS_TOP_BAR = "com.uc.office.widget.OfficeTopBar";
    private static final String CLASS_BOTTOM_BAR = "com.uc.office.widget.OfficeBottomBar";
    private static final String CLASS_BOTTOM_ITEM = "com.uc.office.widget.OfficeBottomItem";
    private static final String CLASS_UPLOAD_ITEM = "com.uc.office.widget.UploadCloudBottomItem";
    private static final String PACKAGE_PREFIX = "com.uc.office.widget.";

    private static final String CLASS_LOADING_DIALOG = "com.uc.office.widget.dialog.OfficeLoadingDialog";
    private static final String CLASS_DIALOG_HEADER = "com.uc.office.widget.dialog.OfficeDialogCommonHeaderView";
    private static final String CLASS_DIALOG_HELPER = "com.uc.office.widget.dialog.OfficeDialogHelper";
    private static final String CLASS_ABS_DIALOG = "com.uc.office.widget.dialog.AbsOfficeDialog";

    /** split 代码里写死、需要替换的颜色。 */
    private static final int OLD_CHROME_TEXT = 0xFF222222;
    private static final int OLD_ACCENT_TEXT = 0xFF2828FF;
    private static final int OLD_DIALOG_TILE = 0xFFF4F4F4;
    private static final int OLD_SECONDARY_TEXT = 0xFF666666;
    private static final int OLD_WHITE = 0xFFFFFFFF;

    private static final long POLL_MS = 100L;
    /** 每 3 次轮询刷一次底栏选中态（≈300ms）。 */
    private static final int REFRESH_EVERY = 3;

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
    private static int sPollCount;
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

    // ---------------------------------------------------------------- 主题取值

    private static int color(Context context, int resId) {
        return context.getResources().getColor(resId, context.getTheme());
    }

    private static boolean isNight(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** 状态栏 / 导航栏：主色底 + 白色图标（清掉 SystemUI 的浅色图标标记）。 */
    private static void styleSystemBars(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        int primary = color(activity, R.color.doc_primary);
        boolean night = isNight(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(night ? color(activity, R.color.doc_chrome) : primary);
            window.setNavigationBarColor(night ? color(activity, R.color.doc_chrome) : primary);
        }
        View decor = window.getDecorView();
        int visibility = decor.getSystemUiVisibility();
        // 深青绿主色 / 深色 chrome 上都用浅色（白色）图标，清掉 SystemUI 的浅色图标标记
        visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(visibility);
    }

    // ---------------------------------------------------------------- 顶栏 / 底栏

    private static void tint(View view) {
        if (view == null) {
            return;
        }
        String name = view.getClass().getName();
        if (CLASS_TOP_BAR.equals(name)) {
            invokeSetBarBackgroundColor(view, color(view.getContext(), R.color.doc_chrome));
            tintBarText(view, false);
        } else if (CLASS_BOTTOM_BAR.equals(name) || CLASS_BOTTOM_ITEM.equals(name)
                || CLASS_UPLOAD_ITEM.equals(name)) {
            view.setBackgroundColor(color(view.getContext(), R.color.doc_chrome));
            tintBarText(view, false);
        }
    }

    /** 把 split 写死的深灰文字 / 深色图标换成当前 chrome 上的前景色。 */
    private static void tintBarText(View view, boolean selectedAware) {
        Context context = view.getContext();
        int onChrome = color(context, R.color.doc_on_chrome);
        int primary = color(context, R.color.doc_primary);
        boolean night = isNight(context);
        if (view instanceof TextView) {
            int current = ((TextView) view).getCurrentTextColor();
            if (selectedAware && (current == OLD_ACCENT_TEXT || current == primary)) {
                ((TextView) view).setTextColor(primary);
            } else if (current == OLD_CHROME_TEXT || current == OLD_ACCENT_TEXT) {
                ((TextView) view).setTextColor(onChrome);
            }
        } else if (view instanceof ImageView) {
            // 浅色底下图标本来就是深灰，只有深色模式需要反色
            if (night) {
                ((ImageView) view).setColorFilter(onChrome);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintBarText(group.getChildAt(i), selectedAware);
            }
        }
    }

    /** 底栏别处理：底栏项选中态由 {@code select()} 直接写死，无法在构造期拿到，只能周期补刷。 */
    private static void refreshBottomItems() {
        Activity activity = sWatchedActivity;
        if (activity == null) {
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        refreshBottomItems((ViewGroup) content);
    }

    private static void refreshBottomItems(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            String name = child.getClass().getName();
            if (CLASS_BOTTOM_ITEM.equals(name) || CLASS_UPLOAD_ITEM.equals(name)) {
                ViewGroup item = (ViewGroup) child;
                for (int j = 0; j < item.getChildCount(); j++) {
                    tintBarText(item.getChildAt(j), true);
                }
            } else if (child instanceof ViewGroup && name.startsWith(PACKAGE_PREFIX)) {
                refreshBottomItems((ViewGroup) child);
            }
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
        sPollCount = 0;
        MAIN_HANDLER.removeCallbacks(DIALOG_POLLER);
        MAIN_HANDLER.postDelayed(DIALOG_POLLER, POLL_MS);
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
                if (++sPollCount % REFRESH_EVERY == 0) {
                    refreshBottomItems();
                }
            } catch (Throwable t) {
                Log.w(TAG, "tint dialog failed", t);
            }
            MAIN_HANDLER.postDelayed(this, POLL_MS);
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
        Context context = dialog.getContext();
        sTintedDialogs.add(dialog);
        root.setBackground(roundRect(context, 24, color(context, R.color.doc_dialog)));
        tintDialogTree(root, context);
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

    private static void tintDialogTree(View view, Context context) {
        String name = view.getClass().getName();
        if (view instanceof Button) {
            tintDialogButton((Button) view, context);
            return;
        }
        if (CLASS_DIALOG_HEADER.equals(name)) {
            // 头部原本是一块白色底衬，跟卡片同色才不会有接缝
            view.setBackgroundColor(color(context, R.color.doc_dialog));
            return;
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int current = textView.getCurrentTextColor();
            if (current == OLD_ACCENT_TEXT) {
                textView.setTextColor(color(context, R.color.doc_primary));
            } else if (current == OLD_CHROME_TEXT) {
                textView.setTextColor(color(context, R.color.doc_on_dialog));
            } else if (current == OLD_SECONDARY_TEXT) {
                textView.setTextColor(color(context, R.color.doc_on_dialog_secondary));
            }
        }
        Drawable background = view.getBackground();
        if (background instanceof ShapeDrawable) {
            Paint paint = ((ShapeDrawable) background).getPaint();
            if (paint.getColor() == OLD_DIALOG_TILE) {
                // 菜单项图标下方的小方块
                paint.setColor(color(context, R.color.doc_dialog_tile));
                view.invalidate();
            }
        } else if (background instanceof android.graphics.drawable.ColorDrawable) {
            if (((android.graphics.drawable.ColorDrawable) background).getColor() == OLD_WHITE) {
                view.setBackgroundColor(color(context, R.color.doc_dialog));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintDialogTree(group.getChildAt(i), context);
            }
        }
    }

    /** 确认按钮原来是 #FF2828FF 字 + 淡蓝底，其余按钮是 #FF222222 字 + 灰底。 */
    private static void tintDialogButton(Button button, Context context) {
        boolean accent = button.getCurrentTextColor() == OLD_ACCENT_TEXT
                || button.getCurrentTextColor() == color(context, R.color.doc_primary);
        if (accent) {
            button.setTextColor(color(context, R.color.doc_primary));
        } else {
            button.setTextColor(color(context, R.color.doc_on_dialog));
        }
        int accentBg = color(context, R.color.doc_primary_container);
        int neutralBg = color(context, R.color.doc_dialog_neutral);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed},
                roundRect(context, 12, withAlpha(accent ? accentBg : neutralBg, 0x66)));
        background.addState(new int[0],
                roundRect(context, 12, accent ? accentBg : neutralBg));
        button.setBackground(background);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static GradientDrawable roundRect(Context context, int radiusDp, int color) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(radiusDp * density);
        drawable.setColor(color);
        return drawable;
    }
}
