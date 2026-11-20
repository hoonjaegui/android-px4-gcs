plugins {
    // 메모리 저장만 하므로 Android API가 필요 없다 → JVM 모듈
    alias(libs.plugins.px4gcs.jvm.library)
    alias(libs.plugins.px4gcs.hilt)
}

dependencies {
    implementation(projects.core.domain)

    testImplementation(projects.core.testing)
}
