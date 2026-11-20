package io.github.hoonjaegui.px4gcs.core.common.coroutine

import javax.inject.Qualifier

/**
 * 앱 프로세스 수명 동안 유지되는 CoroutineScope.
 * Drone 연결·Telemetry 수집처럼 화면(ViewModel)이 사라져도 계속돼야 하는 작업에 쓴다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
