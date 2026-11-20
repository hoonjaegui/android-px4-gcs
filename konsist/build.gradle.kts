import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.px4gcs.jvm.library)
}

/**
 * 아키텍처 규칙 테스트. 코드는 없고 테스트만 있다.
 * Konsist가 프로젝트 전체 .kt 파일을 읽어서 "ViewModel은 UseCase만 받는다" 같은 규칙을 검사한다.
 * 컴파일러(모듈 의존성)로 막을 수 없는 규칙을 리뷰 대신 테스트가 잡게 한다.
 */

// Konsist는 JVM 17 이상이 필요하다. 이 모듈은 앱에 들어가지 않는 테스트 전용이라 여기만 17로 올린다.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.konsist)
}

// Konsist는 다른 모듈의 소스를 읽는다. 그 파일들을 입력으로 선언하지 않으면 Gradle이 "변경 없음"으로 보고
// 테스트를 건너뛴다(규칙 위반이 생겨도 통과). 프로젝트의 모든 .kt를 입력으로 등록한다.
tasks.test {
    inputs.files(
        fileTree(rootDir) {
            include("**/*.kt")
            exclude("**/build/**", "build-logic/**", ".gradle/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
}
