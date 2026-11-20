package io.github.hoonjaegui.px4gcs.core.model.mission

/** Waypoint 하나만 보고 알 수 있는 문제. */
enum class WaypointProblem {
    /** NaN/Infinity 또는 -90~90 밖. */
    INVALID_LATITUDE,

    /** NaN/Infinity 또는 -180~180 밖. */
    INVALID_LONGITUDE,

    /** NaN/Infinity. */
    INVALID_ALTITUDE,
    ALTITUDE_TOO_LOW,
    ALTITUDE_TOO_HIGH,
}

sealed interface MissionValidationError {
    /** Waypoint가 하나도 없음. */
    data object Empty : MissionValidationError

    data class InvalidWaypoint(val index: Int, val problem: WaypointProblem) : MissionValidationError

    /** [index]번이 바로 앞 Waypoint와 [distanceM]m밖에 안 떨어져 있음 (지도 더블 탭 등). */
    data class TooCloseToPrevious(val index: Int, val distanceM: Double) : MissionValidationError
}
