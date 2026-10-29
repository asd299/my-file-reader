import java.util.Properties

plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 只用于编译壳工程：提供 split 同名类的签名与真实资源 id，运行时解析到 ucoffice split，
// 因此由 docviewer 以 compileOnly 依赖，不会打进 APK。
val localProps = rootProject.file("local.properties")
val sdkDir: String = if (localProps.exists()) {
    val props = Properties()
    localProps.inputStream().use { props.load(it) }
    props.getProperty("sdk.dir")
} else {
    null
} ?: System.getenv("ANDROID_HOME")

// Android 36 起平台目录带小版本号（android-37.0），不能直接拼 android-37
val compileSdk = libs.versions.android.compileSdk.get()
val androidJar = file("$sdkDir/platforms")
    .listFiles()
    ?.filter { it.isDirectory && it.name.startsWith("android-$compileSdk") }
    ?.sortedBy { it.name }
    ?.firstOrNull { File(it, "android.jar").isFile }
    ?.let { File(it, "android.jar") }
    ?: error("找不到 Android $compileSdk 的 android.jar，请检查 sdk.dir/ANDROID_HOME")

dependencies {
    compileOnly(files(androidJar))
}
