plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.re.docviewer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    buildFeatures {
        buildConfig = false
    }

    // split 包的 so 由宿主 APK 解压到 lib 目录，libpdfsdk_jni.so 按 NEEDED 找 libpdfium.so
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // 覆盖 split 同名类时所需的编译期签名，运行时不打包
    compileOnly(project(":splitstubs"))

    // ucoffice / pdfviewer dex 运行时依赖的宿主库（split 内未打包）
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.documentfile)
    implementation(libs.commons.logging)
}
