plugins {
    alias(libs.plugins.px4gcs.android.library)
    alias(libs.plugins.px4gcs.android.library.compose)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs.core.navigation"
}

dependencies {
    // NavKey, NavBackStack은 feature api 모듈이 그대로 쓰므로 api
    api(libs.androidx.navigation3.runtime)
    // 화면(NavEntry)마다 ViewModelStore를 따로 준다 → 탭을 닫으면 그 화면의 ViewModel도 정리
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
