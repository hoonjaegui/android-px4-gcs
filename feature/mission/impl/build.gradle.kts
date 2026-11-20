plugins {
    alias(libs.plugins.px4gcs.android.feature.impl)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs.feature.mission.impl"
}

dependencies {
    implementation(projects.feature.mission.api)
    implementation(projects.core.domain)
    // 지도는 Mission 화면에서만 쓴다
    implementation(libs.maplibre.android.sdk)
}
