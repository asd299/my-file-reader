package com.re.docviewer.pdf;

import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * 修复 PDF 顶栏控件重叠。
 *
 * pdfviewer split 的 pdf_common_top_bar.xml 是在夸克应用内编译的，app: 命名空间下的
 * layout_constraint* 保留了夸克自己的资源 id（0x7f03xxxx）。融合后宿主资源表里没有这些 id，
 * ConstraintLayout 读不到任何约束，返回键 / 标题 / 分享 / 菜单全部堆在左上角，返回键压住标题。
 *
 * 这里按 split XML 的原始语义在运行时就地重建 LayoutParams（数值与 XML 完全一致）：
 * 返回键 32dp、start 16dp；标题宽 0dp、左右各 98dp；分享 32dp、位于菜单左侧 8dp；
 * 菜单 32dp、end 12dp。四个控件都垂直居中。
 *
 * 顶栏不只一个：正文页和“页面缩略图”（PageManagerView）都各建了一个 PDFTopBar，
 * 后者是懒加载的，因此除了启动时扫一遍，还要在根容器新增子视图时补一次。
 */
final class PdfTopBarLayoutFix {

    // 取自 split 资源表 com.quark.browser.pdfviewer，与 pdf_common_top_bar.xml 中的 android:id 一致
    private static final int ID_TOP_BAR_BACK = 0x7d060017; // iv_topbar_common_back
    private static final int ID_TOP_BAR_TITLE = 0x7d06003f; // tv_topbar_common_title
    private static final int ID_TOP_BAR_SHARE = 0x7d060019; // iv_topbar_common_share
    private static final int ID_TOP_BAR_MENU = 0x7d060018; // iv_topbar_common_menu

    private PdfTopBarLayoutFix() {
    }

    static void apply(View root) {
        if (root == null) {
            return;
        }
        fixTree(root);
        if (root instanceof ViewGroup) {
            ((ViewGroup) root).setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    fixTree(child);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            });
        }
    }

    private static void fixTree(View root) {
        View back = root.findViewById(ID_TOP_BAR_BACK);
        if (back == null) {
            return;
        }
        View title = root.findViewById(ID_TOP_BAR_TITLE);
        View share = root.findViewById(ID_TOP_BAR_SHARE);
        View menu = root.findViewById(ID_TOP_BAR_MENU);
        ViewGroup parent = back.getParent() instanceof ViewGroup
                ? (ViewGroup) back.getParent() : null;
        if (title == null || share == null || menu == null
                || !(parent instanceof ConstraintLayout)) {
            return;
        }
        float density = parent.getResources().getDisplayMetrics().density;
        int icon = dp(density, 32);

        ConstraintLayout.LayoutParams backParams = new ConstraintLayout.LayoutParams(icon, icon);
        backParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        backParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        backParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        backParams.setMarginStart(dp(density, 16));
        back.setLayoutParams(backParams);

        ConstraintLayout.LayoutParams titleParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        titleParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.setMarginStart(dp(density, 98));
        titleParams.setMarginEnd(dp(density, 98));
        title.setLayoutParams(titleParams);

        ConstraintLayout.LayoutParams shareParams = new ConstraintLayout.LayoutParams(icon, icon);
        shareParams.endToStart = menu.getId();
        shareParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        shareParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        shareParams.setMarginEnd(dp(density, 8));
        share.setLayoutParams(shareParams);

        ConstraintLayout.LayoutParams menuParams = new ConstraintLayout.LayoutParams(icon, icon);
        menuParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        menuParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        menuParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        menuParams.setMarginEnd(dp(density, 12));
        menu.setLayoutParams(menuParams);
    }

    private static int dp(float density, int value) {
        return (int) (value * density + 0.5f);
    }
}
