package com.uc.office.sdk;

/**
 * 仅用于编译壳工程的宿主覆盖类，不会打进 APK。
 * 常量取自 ucoffice split（资源包 id 0x70）的 resources.arsc，编译期直接内联。
 */
public final class R {

    private R() {
    }

    public static final class string {
        public static final int share = 0x700700a1;
        public static final int search = 0x7007009c;
        public static final int open_on_third_app = 0x70070070;
        public static final int detail = 0x70070028;
        public static final int file_parse_fail = 0x70070056;
        public static final int error_code = 0x70070049;

        private string() {
        }
    }

    public static final class drawable {
        public static final int uoffice_android_app = 0x70040014;
        public static final int uoffice_detail_info = 0x70040019;
        public static final int uoffice_file_parse_error = 0x70040044;
        public static final int uoffice_menu_share = 0x7004004a;
        public static final int uoffice_search = 0x70040050;

        private drawable() {
        }
    }
}
