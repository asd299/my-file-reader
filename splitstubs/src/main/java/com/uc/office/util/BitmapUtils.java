package com.uc.office.util;

import android.graphics.drawable.Drawable;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 运行时解析到 ucoffice split 里的真实 BitmapUtils。
 */
public final class BitmapUtils {

    private BitmapUtils() {
    }

    public static Drawable getCornerBackground(int color, int radius) {
        return null;
    }
}
