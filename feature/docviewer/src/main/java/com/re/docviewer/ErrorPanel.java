package com.re.docviewer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 出错时展示可复制的堆栈信息，避免只能靠 adb 排查。 */
public final class ErrorPanel {

    private ErrorPanel() {
    }

    public static LinearLayout show(Activity activity, String message) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setGravity(Gravity.CENTER);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                activity.getResources().getDisplayMetrics());

        TextView text = new TextView(activity);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(13);
        text.setPadding(padding, padding, padding, padding);
        text.setText(message);

        // 堆栈通常很长，必须放进 ScrollView，否则下面的复制按钮会被挤出屏幕
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(text, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button copy = new Button(activity);
        copy.setText("复制错误信息");
        copy.setOnClickListener(v -> {
            ClipboardManager manager = (ClipboardManager) activity.getSystemService(
                    Activity.CLIPBOARD_SERVICE);
            if (manager == null) {
                return;
            }
            manager.setPrimaryClip(ClipData.newPlainText("error", text.getText()));
            Toast.makeText(activity, "已复制", Toast.LENGTH_SHORT).show();
        });
        root.addView(copy, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }
}
