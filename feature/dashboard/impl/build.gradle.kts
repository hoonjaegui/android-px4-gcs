plugins {
    alias(libs.plugins.px4gcs.android.feature.impl)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs.feature.dashboard.impl"
}

dependencies {
    implementation(projects.feature.dashboard.api)
    implementation(projects.core.domain)
}
