plugins {
    alias(libs.plugins.px4gcs.jvm.library)
    alias(libs.plugins.px4gcs.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
