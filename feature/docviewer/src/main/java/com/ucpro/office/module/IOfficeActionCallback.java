package com.ucpro.office.module;

/** 宿主桩：与夸克主 APK 中同名接口签名一致。 */
public interface IOfficeActionCallback {
    void onConvertPdfFailure();

    void onConvertPdfSuccess();

    void onSelectedImageSucceed(String str);

    void onSendToPCFailed();

    void onSendToPCProgress(long j, long j2);

    void onSendToPCSucceed();
}
