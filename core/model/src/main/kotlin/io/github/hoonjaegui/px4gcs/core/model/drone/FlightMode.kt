package io.github.hoonjaegui.px4gcs.core.model.drone

/**
 * PX4 비행 모드. MAVSDK `Telemetry.FlightMode`와 1:1이지만 도메인은 MAVSDK 타입을 모른다.
 * 화면에 보일 문구는 UI가 정한다.
 */
enum class FlightMode {
    UNKNOWN,
    READY,
    TAKEOFF,
    HOLD,
    MISSION,
    RETURN,
    LAND,
    OFFBOARD,
    FOLLOW_ME,
    MANUAL,
    ALTITUDE,
    POSITION,
    ACRO,
    STABILIZED,
    RATTITUDE,
}
