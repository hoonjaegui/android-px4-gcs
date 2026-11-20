plugins {
    alias(libs.plugins.px4gcs.android.application)
    alias(libs.plugins.px4gcs.android.application.compose)
    alias(libs.plugins.px4gcs.hilt)
}

android {
    namespace = "io.github.hoonjaegui.px4gcs"

    defaultConfig {
        applicationId = "io.github.hoonjaegui.px4gcs"
        versionCode = 1
        versionName = "1.0"

        // mavsdk-server AAR ships ~37MB native binary per ABI; keep only what we run on
        // (arm64 emulator / device, x86_64 emulator).
        // MAVSDK connection URL. Default: emulator (host UDP 14540 redirected into the guest).
        // USB phone via tools/mavlink_udp_tcp_bridge.py: -Pgcs.connectionUrl=tcpout://127.0.0.1:5760
        val connectionUrl = providers.gradleProperty("gcs.connectionUrl")
            .getOrElse("udpin://0.0.0.0:14540")
        buildConfigField("String", "MAVSDK_CONNECTION_URL", "\"$connectionUrl\"")

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
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.data.mission)
    implementation(projects.core.model)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.mavsdk)
    implementation(libs.mavsdk.server)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.maplibre.android.sdk)
    testImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}