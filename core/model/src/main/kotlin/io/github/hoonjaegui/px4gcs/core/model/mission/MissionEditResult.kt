package io.github.hoonjaegui.px4gcs.core.model.mission

/** Waypoint 추가·고도 변경처럼 거부될 수 있는 편집의 결과. */
sealed interface MissionEditResult {
    data object Accepted : MissionEditResult

    /** plan은 바뀌지 않았다. */
    data class Rejected(val reason: MissionEditRejection) : MissionEditResult
}

sealed interface MissionEditRejection {
    data class InvalidWaypoint(val problems: List<WaypointProblem>) : MissionEditRejection

    /** 바로 앞 Waypoint와 [distanceM]m밖에 안 떨어짐 (지도 더블 탭 등). */
    data class TooCloseToPrevious(val distanceM: Double) : MissionEditRejection
}
