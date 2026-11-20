package com.example.android_px4_gcs.core.mission

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

/**
 * MissionPlan의 유효성 규칙을 한 곳에 모은다. 순수 함수.
 *
 * 첫 번째 문제에서 멈추지 않고 **모든 문제를 모아서** 돌려준다 → UI가 Waypoint별로 표시할 수 있다.
 * 빈 목록 = 유효.
 */
object MissionValidator {
    /** 지면 충돌 여유. */
    const val MIN_ALTITUDE_M = 2.0

    /** 한국 드론 비행 제한(150m)에 여유를 둔 값. */
    const val MAX_ALTITUDE_M = 120.0

    /** 이보다 가까운 연속 Waypoint는 실수로 본다. */
    const val MIN_SEGMENT_M = 1.0

    /** Waypoint를 plan에 넣기 전에 검사할 때 쓴다. 빈 목록 = 유효. */
    fun validate(waypoint: Waypoint): List<WaypointProblem> = buildList {
        val lat = waypoint.latitudeDeg
        val lon = waypoint.longitudeDeg
        val alt = waypoint.relativeAltitudeM

        // NaN은 모든 비교가 false라 범위 검사만으로도 걸리지만, 의도를 드러내려고 isFinite를 명시한다.
        if (!lat.isFinite() || lat !in -90.0..90.0) add(WaypointProblem.INVALID_LATITUDE)
        if (!lon.isFinite() || lon !in -180.0..180.0) add(WaypointProblem.INVALID_LONGITUDE)
        when {
            !alt.isFinite() -> add(WaypointProblem.INVALID_ALTITUDE)
            alt < MIN_ALTITUDE_M -> add(WaypointProblem.ALTITUDE_TOO_LOW)
            alt > MAX_ALTITUDE_M -> add(WaypointProblem.ALTITUDE_TOO_HIGH)
        }
    }

    fun validate(plan: MissionPlan): List<MissionValidationError> = buildList {
        if (plan.isEmpty()) {
            add(MissionValidationError.Empty)
            return@buildList
        }
        plan.waypoints.forEachIndexed { index, waypoint ->
            val problems = validate(waypoint)
            problems.forEach { add(MissionValidationError.InvalidWaypoint(index, it)) }

            // 좌표가 잘못된 Waypoint와의 거리는 의미가 없으므로 둘 다 좌표가 정상일 때만 잰다.
            if (index > 0 && problems.none { it.isCoordinateProblem() }) {
                val previous = plan.waypoints[index - 1]
                if (validate(previous).none { it.isCoordinateProblem() }) {
                    val distance = distanceM(previous, waypoint)
                    if (distance < MIN_SEGMENT_M) add(MissionValidationError.TooCloseToPrevious(index, distance))
                }
            }
        }
    }

    private fun WaypointProblem.isCoordinateProblem() =
        this == WaypointProblem.INVALID_LATITUDE || this == WaypointProblem.INVALID_LONGITUDE

    /**
     * 두 Waypoint 사이의 수평 거리(m). WGS84 타원체 대신 구(평균 반지름)로 근사한 haversine 공식.
     * Waypoint 간격(수 m ~ 수 km)에서 오차는 0.5% 이하라 "너무 가까운가" 판정에는 충분하다.
     */
    fun distanceM(from: Waypoint, to: Waypoint): Double {
        val lat1 = Math.toRadians(from.latitudeDeg)
        val lat2 = Math.toRadians(to.latitudeDeg)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitudeDeg - from.longitudeDeg)
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    private const val EARTH_RADIUS_M = 6_371_008.8
}
