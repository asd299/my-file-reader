package com.uc.office.widget.dialog;

import android.view.View;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 运行时解析到 ucoffice split 里的真实 IOfficeDialog。
 */
public interface IOfficeDialog {
    IOfficeDialog addNewRow();

    IOfficeDialog addNoButton(String str);

    IOfficeDialog addView(View view);
}
