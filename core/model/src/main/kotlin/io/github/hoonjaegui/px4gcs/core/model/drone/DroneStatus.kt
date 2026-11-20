package io.github.hoonjaegui.px4gcs.core.model.drone

/**
 * 화면이 한 번에 받는 기체 상태: Telemetry 값 + 그 값으로 판정한 경고 + 판정 시각.
 * [nowElapsedMs]는 "N초 전" 표시에 쓴다(값이 안 와도 시간은 흐른다).
 */
data class DroneStatus(
    val drone: DroneState = DroneState(),
    val warnings: DroneWarningState = DroneWarningState(),
    val nowElapsedMs: Long = 0L,
)
