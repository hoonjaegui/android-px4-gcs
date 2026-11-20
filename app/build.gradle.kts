plugins {
    alias(libs.plugins.px4gcs.android.application)
    alias(libs.plugins.px4gcs.android.application.compose)
    alias(libs.plugins.px4gcs.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs"

    defaultConfig {
        applicationId = "io.github.hoonjaegui.px4gcs"
        versionCode = 1
        versionName = "1.0"

        // MAVSDK connection URL. Default: emulator (host UDP 14540 redirected into the guest).
        // USB phone via tools/mavlink_udp_tcp_bridge.py: -Pgcs.connectionUrl=tcpout://127.0.0.1:5760
        val connectionUrl = providers.gradleProperty("gcs.connectionUrl")
            .getOrElse("udpin://0.0.0.0:14540")
        buildConfigField("String", "MAVSDK_CONNECTION_URL", "\"$connectionUrl\"")

        // mavsdk-server AAR ships ~37MB native binary per ABI; keep only what we run on
        // (arm64 emulator / device, x86_64 emulator).
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // 화면 (feature): app은 api(NavKey)와 impl(EntryProvider)만 조립한다
    implementation(projects.feature.dashboard.api)
    implementation(projects.feature.dashboard.impl)
    implementation(projects.feature.mission.api)
    implementation(projects.feature.mission.impl)

    // 구현 (data): Hilt 바인딩을 앱 그래프에 포함시키기 위해 필요
    implementation(projects.core.data.drone)
    implementation(projects.core.data.mission)

    implementation(projects.core.common) // Clock (AndroidClock 제공)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation3.ui)

    testImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
