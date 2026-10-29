package com.ucpro.office.module;

import android.app.Activity;
import androidx.lifecycle.Lifecycle;

import java.util.Map;

/**
 * 宿主桩：与夸克主 APK 中同名接口的二进制签名保持一致。
 * ucoffice 的 UCOfficeProxyHandler 会加载实现类并回调这些方法。
 */
public interface IOfficeProxyHandler {
    void checkSendToPCDetail();

    void checkSendToPCFailure();

    void convertPdf(String str);

    String getQuarkDir();

    boolean isEnableUCOfficeMultiProcess();

    boolean isOfficeActive();

    boolean needCollectImprovementOpinion();

    void onFileOpen(String str);

    void onInitOfficeActivity(Activity activity, Lifecycle lifecycle, IOfficeActionCallback callback);

    void openCollectOpinionPage();

    void openFeedback(String str);

    void openFileToOtherApp(String str, String str2);

    void openImage(String str);

    void openImageChooser();

    void openPdf(String str);

    void openPrivacyLicence();

    void saveAs(String str);

    void sendFileToOtherApp(String str);

    boolean sendToPC(String str, int i, int i2, int i3);

    void showToast(String str);

    void showToast(String str, int i);

    void updateStatBizParams(Map<String, String> map);

    void uploadCrashTrace(String str, Throwable th);

    void utStatClickEvent(String str, String str2, String str3, Map<String, String> map);

    void utStatCustomEvent(String str, String str2, String str3, Map<String, String> map);

    void utStatExposureEvent(String str, String str2, String str3, Map<String, String> map);

    void utStatOpenFileResult(boolean z, long j, long j2, String str, boolean z2);

    void utStatSendToPCDialogClick(Map<String, String> map);

    void visitFileOnPC();
}
