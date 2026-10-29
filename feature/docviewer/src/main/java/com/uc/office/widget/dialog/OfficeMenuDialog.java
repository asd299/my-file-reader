package com.uc.office.widget.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.uc.office.sdk.R;
import com.uc.office.util.DisplayUtils;

/**
 * 宿主覆盖类：与 ucoffice split 中同名类逐行一致，仅删除“问题反馈”入口。
 *
 * 原实现在 Provider.QUARK 时追加一个“问题反馈”图标位，点击回调
 * QuarkOfficeActivity$5$1#handleFeedback -> proxyHandler.openFeedback，离线壳里没有反馈页，
 * 点了没有任何反应。壳 dex 先于 split dex 被 PathClassLoader 命中，因此这里的版本生效；
 * split 里的 IOfficeDialog / BaseOfficeDialog / DisplayUtils 等仍用真实实现。
 *
 * 保留 OnActionItemClickListener#handleFeedback 声明：QuarkOfficeActivity 的匿名实现仍要实现它。
 */
public class OfficeMenuDialog extends BaseOfficeDialog {

    private int mApplicationType;
    private View mFileDetailItemView;
    private OnActionItemClickListener mOnActionItemClickListener;
    private View mOpenFileByOtherAppItemView;
    private View mOtherOpenItemView;
    private View mPrinterItemView;
    private View mSaveAsItemView;
    private View mShareItemView;

    public interface OnActionItemClickListener {
        void handleFeedback();

        void handleFileDetail();

        void handleOtherOpen();

        void handlePrinter();

        void handleSaveAs();

        void handleSearch();

        void handleShare();
    }

    @Override
    protected boolean cancelable() {
        return true;
    }

    @Override
    protected boolean canceledOnTouchOutside() {
        return true;
    }

    public OfficeMenuDialog(Context context, int i) {
        super(context);
        this.mApplicationType = i;
        init();
    }

    public void setOnActionItemClickListener(OnActionItemClickListener onActionItemClickListener) {
        this.mOnActionItemClickListener = onActionItemClickListener;
    }

    private void init() {
        LinearLayout linearLayout = new LinearLayout(this.mContext);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(DisplayUtils.dp2px(20.0f), DisplayUtils.dp2px(30.0f), DisplayUtils.dp2px(20.0f), 0);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2);
        layoutParams.weight = 1.0f;
        int iDp2px = DisplayUtils.dp2px(2.0f);
        layoutParams.rightMargin = iDp2px;
        layoutParams.leftMargin = iDp2px;
        layoutParams.gravity = 17;
        View shareItemView = crateItemView(this.mContext.getString(R.string.share),
                ContextCompat.getDrawable(getContext(), R.drawable.uoffice_menu_share));
        this.mShareItemView = shareItemView;
        shareItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mOnActionItemClickListener != null) {
                    mOnActionItemClickListener.handleShare();
                }
            }
        });
        linearLayout.addView(this.mShareItemView, layoutParams);
        if (5 != this.mApplicationType) {
            View searchItemView = crateItemView(this.mContext.getString(R.string.search),
                    ContextCompat.getDrawable(getContext(), R.drawable.uoffice_search));
            this.mOpenFileByOtherAppItemView = searchItemView;
            searchItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                    if (mOnActionItemClickListener != null) {
                        mOnActionItemClickListener.handleSearch();
                    }
                }
            });
            linearLayout.addView(this.mOpenFileByOtherAppItemView, layoutParams);
        }
        View otherOpenItemView = crateItemView(this.mContext.getString(R.string.open_on_third_app),
                ContextCompat.getDrawable(getContext(), R.drawable.uoffice_android_app));
        this.mOtherOpenItemView = otherOpenItemView;
        otherOpenItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mOnActionItemClickListener != null) {
                    mOnActionItemClickListener.handleOtherOpen();
                }
            }
        });
        linearLayout.addView(this.mOtherOpenItemView, layoutParams);
        // split 原实现此处按 Provider.QUARK 追加“问题反馈”位，离线壳删除
        View fileDetailItemView = crateItemView(this.mContext.getString(R.string.detail),
                ContextCompat.getDrawable(getContext(), R.drawable.uoffice_detail_info));
        this.mFileDetailItemView = fileDetailItemView;
        fileDetailItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mOnActionItemClickListener != null) {
                    mOnActionItemClickListener.handleFileDetail();
                }
            }
        });
        linearLayout.addView(this.mFileDetailItemView, layoutParams);
        addNewRow().addView(linearLayout);
        // split 原实现是 R.string.dialog_no_btn，桩中未收录该 id，用同义字面量
        addNewRow().addNoButton("取消");
    }

    private View crateItemView(String str, Drawable drawable) {
        LinearLayout linearLayout = new LinearLayout(this.mContext);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        FrameLayout frameLayout = new FrameLayout(this.mContext);
        frameLayout.setBackground(DisplayUtils.getRoundRectShapeDrawable(
                DisplayUtils.dp2px(12.0f), color(com.re.docviewer.R.color.doc_dialog_tile)));
        ImageView imageView = new ImageView(this.mContext);
        imageView.setImageDrawable(drawable);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                DisplayUtils.dp2px(24.0f), DisplayUtils.dp2px(24.0f));
        layoutParams.gravity = 17;
        frameLayout.addView(imageView, layoutParams);
        linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(
                DisplayUtils.dp2px(44.0f), DisplayUtils.dp2px(44.0f)));
        TextView textView = new TextView(this.mContext);
        textView.setTextSize(0, DisplayUtils.dp2px(10.0f));
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(color(com.re.docviewer.R.color.doc_on_dialog));
        textView.setGravity(17);
        textView.setSingleLine();
        textView.setText(str);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = DisplayUtils.dp2px(8.0f);
        linearLayout.addView(textView, layoutParams2);
        return linearLayout;
    }

    private int color(int resId) {
        return this.mContext.getResources().getColor(resId, this.mContext.getTheme());
    }

    public void setShareItemEnable(boolean z) {
        this.mShareItemView.setVisibility(z ? 0 : 8);
    }
}
