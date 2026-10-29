package com.uc.office.util;

import android.graphics.drawable.ShapeDrawable;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 运行时解析到 ucoffice split 里的真实 DisplayUtils。
 */
public final class DisplayUtils {

    private DisplayUtils() {
    }

    public static int dp2px(float f) {
        return 0;
    }

    public static ShapeDrawable getRoundRectShapeDrawable(int radius, int color) {
        return null;
    }
}
