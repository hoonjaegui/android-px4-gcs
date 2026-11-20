plugins {
    alias(libs.plugins.px4gcs.android.library)
    alias(libs.plugins.px4gcs.hilt)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs.core.data.drone"
}

/**
 * MAVSDK를 아는 유일한 모듈. mavsdk_server 네이티브 바이너리와 그것이 필요로 하는
 * libc++_shared.so(src/main/jniLibs, AAR에 빠져 있어 NDK r27.3 것을 번들)도 여기에 둔다.
 */
dependencies {
    implementation(projects.core.domain)
    implementation(libs.mavsdk)
    implementation(libs.mavsdk.server)
    implementation(libs.kotlinx.coroutines.rx2)

    testImplementation(projects.core.testing)
}
