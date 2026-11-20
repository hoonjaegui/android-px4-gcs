package io.github.hoonjaegui.px4gcs.core.data.drone.di

import javax.inject.Qualifier

/**
 * MAVSDK 접속 URL (예: `udpin://0.0.0.0:14540`, `tcpout://127.0.0.1:5760`).
 * 값은 빌드 설정에 따라 다르므로 앱이 제공한다(app/build.gradle.kts의 `gcs.connectionUrl`).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MavsdkConnectionUrl
