plugins {
    alias(libs.plugins.px4gcs.jvm.library)
}

dependencies {
    // UseCase가 공개 타입으로 model(DroneState 등)·common(Clock)·Flow를 쓰므로 api
    api(projects.core.model)
    api(projects.core.common)
    api(libs.kotlinx.coroutines.core)
    // @Inject만 필요하다. domain은 Hilt(DI 프레임워크)를 모른다.
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}
