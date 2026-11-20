plugins {
    alias(libs.plugins.px4gcs.android.library)
    alias(libs.plugins.px4gcs.android.library.compose)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs.core.designsystem"
}

/** 테마·색·공통 UI 요소. Compose material3는 api로 내보내서 feature가 따로 선언하지 않아도 된다(NIA 방식). */
dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
}
