package com.example.android_px4_gcs.core.mission

/**
 * Mission의 한 지점. MAVSDK 타입을 전혀 모른다.
 *
 * - 좌표계: WGS84 (GPS와 PX4가 쓰는 지구 타원체). 위도 -90~90, 경도 -180~180, 단위는 도(degree).
 *   MAVLink는 정수 degE7(도 × 10^7)로 보내지만 도메인에서는 Double 도 단위로 다룬다.
 * - 고도: **홈(이륙 지점) 기준 상대고도**, 위쪽이 양수(m).
 *   해발(AMSL) 절대고도가 아니다. 홈이 해발 489m인 취리히 SITL에서 30m는 해발 519m를 뜻한다.
 *   PX4 내부 좌표(NED)의 z와도 다르다 — NED는 아래가 +라서 30m 위는 z = -30.
 *
 * 값의 유효성(범위, NaN)은 여기서 막지 않는다. 검사는 [MissionValidator] 한 곳에서 한다(A2).
 */
data class Waypoint(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val relativeAltitudeM: Double,
)

/**
 * 비행할 Waypoint의 순서 있는 목록.
 *
 * 불변 객체다. 편집 함수는 자기 자신을 바꾸지 않고 **새 MissionPlan**을 돌려준다.
 * → 이전 plan이 그대로 남으므로 "편집 전/후" 비교, 되돌리기, StateFlow 갱신이 단순해진다.
 *
 * index가 범위를 벗어나면 [IndexOutOfBoundsException]. 사용자의 잘못된 입력이 아니라
 * 호출하는 코드의 버그이므로 조용히 무시하지 않는다.
 */
data class MissionPlan(
    val waypoints: List<Waypoint> = emptyList(),
) {
    val size: Int get() = waypoints.size
    fun isEmpty(): Boolean = waypoints.isEmpty()

    /** 맨 뒤에 추가. */
    fun add(waypoint: Waypoint): MissionPlan = copy(waypoints = waypoints + waypoint)

    fun remove(index: Int): MissionPlan {
        checkIndex(index)
        return copy(waypoints = waypoints.filterIndexed { i, _ -> i != index })
    }

    /**
     * [from] 위치의 Waypoint를 빼서 [to] 위치에 넣는다. 나머지는 상대 순서를 유지한다.
     * 예: [A, B, C, D].move(0, 2) → [B, C, A, D]
     */
    fun move(from: Int, to: Int): MissionPlan {
        checkIndex(from)
        checkIndex(to)
        if (from == to) return this
        val list = waypoints.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(waypoints = list)
    }

    fun updateAltitude(index: Int, relativeAltitudeM: Double): MissionPlan {
        checkIndex(index)
        return copy(
            waypoints = waypoints.mapIndexed { i, wp ->
                if (i == index) wp.copy(relativeAltitudeM = relativeAltitudeM) else wp
            },
        )
    }

    private fun checkIndex(index: Int) {
        if (index !in waypoints.indices) {
            throw IndexOutOfBoundsException("index $index, size $size")
        }
    }
}
