plugins {
    alias(libs.plugins.px4gcs.jvm.library)
}

/**
 * 테스트 공용 도구 (Fake 저장소, TestClock, MainDispatcherRule). 각 모듈은 testImplementation으로만 쓴다.
 * 순수 Kotlin이라 JVM 모듈(domain)과 Android 모듈(feature, data) 테스트 모두에서 쓸 수 있다.
 */
dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.test)
    api(libs.junit)
}
