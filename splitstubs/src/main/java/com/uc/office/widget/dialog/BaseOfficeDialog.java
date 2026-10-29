package com.uc.office.widget.dialog;

import android.content.Context;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 运行时解析到 ucoffice split 里的真实 BaseOfficeDialog（extends AbsOfficeDialog）。
 */
public class BaseOfficeDialog extends android.app.Dialog {

    protected Context mContext;

    public BaseOfficeDialog(Context context) {
        super(context);
    }

    protected boolean cancelable() {
        return true;
    }

    protected boolean canceledOnTouchOutside() {
        return true;
    }

    public IOfficeDialog addNewRow() {
        return null;
    }

    public IOfficeDialog addNoButton(String str) {
        return null;
    }
}
