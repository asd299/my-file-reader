package com.uc.office.widget;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 运行时解析到 ucoffice split 里的真实 OfficeFrameLayout。
 * 两个 id 由 View.generateViewId() 在类初始化时赋值，不能内联，必须 getstatic 读取。
 */
public final class OfficeFrameLayout {

    public static int ID_PARSE_ERROR_OPEN;
    public static int ID_PARSE_ERROR_FEEDBACK;

    private OfficeFrameLayout() {
    }
}
