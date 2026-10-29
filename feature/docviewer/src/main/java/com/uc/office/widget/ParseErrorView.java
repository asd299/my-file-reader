package com.uc.office.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uc.office.constant.SSConstant;
import com.uc.office.sdk.R;
import com.uc.office.util.BitmapUtils;
import com.uc.office.util.DisplayUtils;

/**
 * 宿主覆盖类：与 ucoffice split 中同名类 layout 一致，仅删除“问题反馈”入口。
 *
 * 原实现在错误页底部无条件追加一个“问题反馈”TextView（R.string.feedback），点击回调
 * OfficeFrameLayout.ID_PARSE_ERROR_FEEDBACK -> mListener.handleFeedback()，离线壳没有反馈页。
 * 这里仍然创建 mFeedbackView 并保留 getFeedbackView()（OfficeFrameLayout 会调用
 * getFeedbackView().setOnClickListener(this)，返回 null 会 NPE），但不把它加入布局，界面不可见。
 *
 * setErrorCore 里 OfficeService.getService 泛型擦除后返回 AbstractManager，桩中未声明该方法，
 * 因此改为反射调用真实 split 的 OfficeService / ErrorManager。
 */
public class ParseErrorView extends FrameLayout {

    private Button mActionView;
    private TextView mCodeView;
    private TextView mFeedbackView;
    private TextView mTipView;

    public ParseErrorView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBackgroundColor(-1);
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.drawable.uoffice_file_parse_error);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(
                DisplayUtils.dp2px(120.0f), DisplayUtils.dp2px(120.0f)));

        TextView textView = new TextView(getContext());
        this.mTipView = textView;
        textView.setText(R.string.file_parse_fail);
        this.mTipView.setTextColor(SSConstant.HEADER_GRIDLINE_COLOR);
        this.mTipView.setTextSize(1, 12.0f);
        linearLayout.addView(this.mTipView, new LinearLayout.LayoutParams(-2, -2));

        TextView textView2 = new TextView(getContext());
        this.mCodeView = textView2;
        textView2.setVisibility(8);
        this.mCodeView.setTextColor(SSConstant.HEADER_GRIDLINE_COLOR);
        this.mCodeView.setTextSize(1, 12.0f);
        linearLayout.addView(this.mCodeView, new LinearLayout.LayoutParams(-2, -2));

        Button button = new Button(getContext());
        this.mActionView = button;
        button.setId(OfficeFrameLayout.ID_PARSE_ERROR_OPEN);
        this.mActionView.setText(R.string.open_on_third_app);
        this.mActionView.setTextColor(-1);
        this.mActionView.setTypeface(Typeface.DEFAULT_BOLD);
        this.mActionView.setTextSize(1, 14.0f);
        this.mActionView.setBackground(BitmapUtils.getCornerBackground(-14145281, 12));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                DisplayUtils.dp2px(120.0f), DisplayUtils.dp2px(40.0f));
        layoutParams.topMargin = DisplayUtils.dp2px(17.0f);
        linearLayout.addView(this.mActionView, layoutParams);

        // split 原实现此处无条件 addView 一个“问题反馈”入口，离线壳删除
        this.mFeedbackView = new TextView(getContext());

        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(-2, -2);
        layoutParams3.gravity = 17;
        layoutParams3.bottomMargin = DisplayUtils.dp2px(60.0f);
        addView(linearLayout, layoutParams3);
    }

    public Button getActionView() {
        return this.mActionView;
    }

    public void setErrorCore(int i, int i2) {
        if (i2 != 0) {
            String errorMessage = getErrorMessage(i, i2);
            if (!TextUtils.isEmpty(errorMessage)) {
                this.mTipView.setText(errorMessage);
            }
            this.mCodeView.setVisibility(0);
            this.mCodeView.setText(getContext().getString(R.string.error_code) + i2);
        }
    }

    public TextView getFeedbackView() {
        return this.mFeedbackView;
    }

    private static String getErrorMessage(int i, int i2) {
        try {
            Class<?> serviceClass = Class.forName("com.uc.office.service.OfficeService");
            Object manager = serviceClass
                    .getMethod("getService", Class.class)
                    .invoke(null, Class.forName("com.uc.office.system.ErrorManager"));
            if (manager == null) {
                return null;
            }
            return (String) manager.getClass()
                    .getMethod("getErrorMessage", int.class, Integer.class)
                    .invoke(manager, i, Integer.valueOf(i2));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
