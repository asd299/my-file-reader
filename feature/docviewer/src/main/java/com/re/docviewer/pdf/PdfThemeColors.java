package com.re.docviewer.pdf;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.re.docviewer.R;

/**
 * PDF 查看器的顶栏 / 底栏 / 页码条都是 split 里写死的白色背景 + #FF222222 文字，
 * 且只在 XML 里出现过一次，没法用主题覆盖，只能运行时逐层换色。
 * <p>
 * 页面内容区域（{@code fl_pdf_view_content}）只铺底色，不碰里面的绘制视图，
 * 白色页面本身保持原样。
 */
final class PdfThemeColors {

    /** pdfviewer split 的资源 id，见 aapt2 dump resources。 */
    private static final int ID_TOP_BAR = 0x7d060030;
    private static final int ID_BOTTOM_BAR = 0x7d06002d;
    private static final int ID_PAGE_SLIDER = 0x7d060029;
    private static final int ID_ERROR_VIEW = 0x7d06002e;
    private static final int ID_LOADING_VIEW = 0x7d06002f;
    private static final int ID_CONTENT = 0x7d060008;

    private static final int OLD_TEXT = 0xFF222222;
    private static final int OLD_ACCENT = 0xFF2828FF;
    private static final int OLD_WHITE = 0xFFFFFFFF;

    private PdfThemeColors() {
    }

    static void apply(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        styleSystemBars(activity);
        View decor = activity.getWindow().getDecorView();
        View content = decor.findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            apply(activity, ((ViewGroup) content).getChildAt(0));
        }
    }

    static void apply(Activity activity, View root) {
        if (root == null) {
            return;
        }
        Context context = root.getContext();
        int chrome = color(context, R.color.doc_chrome);
        int surface = color(context, R.color.doc_surface);

        View content = root.findViewById(ID_CONTENT);
        if (content != null) {
            content.setBackgroundColor(surface);
        }
        tint(root.findViewById(ID_TOP_BAR), chrome, true);
        tint(root.findViewById(ID_BOTTOM_BAR), chrome, true);
        tint(root.findViewById(ID_PAGE_SLIDER), chrome, true);
        tint(root.findViewById(ID_ERROR_VIEW), surface, false);
        tint(root.findViewById(ID_LOADING_VIEW), surface, false);
    }

    private static void tint(View view, int background, boolean chrome) {
        if (view == null) {
            return;
        }
        tintTree(view, background, chrome);
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setOnHierarchyChangeListener(
                    new ViewGroup.OnHierarchyChangeListener() {
                        @Override
                        public void onChildViewAdded(View parent, View child) {
                            tintTree(child, background, chrome);
                        }

                        @Override
                        public void onChildViewRemoved(View parent, View child) {
                        }
                    });
        }
    }

    private static void tintTree(View view, int background, boolean chrome) {
        Context context = view.getContext();
        Drawable bg = view.getBackground();
        if (bg instanceof ColorDrawable) {
            if (((ColorDrawable) bg).getColor() == OLD_WHITE) {
                view.setBackgroundColor(background);
            }
        } else if (bg instanceof ShapeDrawable) {
            Paint paint = ((ShapeDrawable) bg).getPaint();
            if (paint.getColor() == OLD_WHITE) {
                paint.setColor(background);
                view.invalidate();
            }
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int current = textView.getCurrentTextColor();
            if (current == OLD_TEXT) {
                textView.setTextColor(color(context, chrome
                        ? R.color.doc_on_chrome : R.color.doc_on_dialog));
            } else if (current == OLD_ACCENT) {
                textView.setTextColor(color(context, R.color.doc_primary));
            }
        } else if (view instanceof ImageView) {
            // 浅色 chrome 上图标本来就是深灰，只有深色模式需要反色
            if (chrome && isNight(context)) {
                ((ImageView) view).setColorFilter(color(context, R.color.doc_on_chrome));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintTree(group.getChildAt(i), background, chrome);
            }
        }
    }

    private static void styleSystemBars(Activity activity) {
        Window window = activity.getWindow();
        int chrome = color(activity, R.color.doc_chrome);
        int primary = color(activity, R.color.doc_primary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(isNight(activity) ? chrome : primary);
            window.setNavigationBarColor(isNight(activity) ? chrome : primary);
        }
        View decor = window.getDecorView();
        int visibility = decor.getSystemUiVisibility();
        visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(visibility);
    }

    private static int color(Context context, int resId) {
        return context.getResources().getColor(resId, context.getTheme());
    }

    private static boolean isNight(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
