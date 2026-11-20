package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditRejection
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditResult
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import javax.inject.Inject

/**
 * 맨 뒤에 Waypoint 추가. 고도는 마지막 Waypoint 것을 이어받는다(없으면 [DEFAULT_ALTITUDE_M], QGC와 같은 방식).
 * 새 Waypoint가 잘못됐거나 바로 앞 점과 너무 가까우면 거부하고 plan을 그대로 둔다.
 */
class AddWaypointUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke(latitudeDeg: Double, longitudeDeg: Double): MissionEditResult {
        val plan = repository.plan.value
        val altitude = plan.waypoints.lastOrNull()?.relativeAltitudeM ?: DEFAULT_ALTITUDE_M
        val waypoint = Waypoint(latitudeDeg, longitudeDeg, altitude)

        val problems = MissionValidator.validate(waypoint)
        if (problems.isNotEmpty()) return MissionEditResult.Rejected(MissionEditRejection.InvalidWaypoint(problems))

        val candidate = plan.add(waypoint)
        val tooClose = MissionValidator.validate(candidate)
            .filterIsInstance<MissionValidationError.TooCloseToPrevious>()
            .firstOrNull { it.index == candidate.size - 1 }
        if (tooClose != null) {
            return MissionEditResult.Rejected(MissionEditRejection.TooCloseToPrevious(tooClose.distanceM))
        }

        repository.save(candidate)
        return MissionEditResult.Accepted
    }

    companion object {
        const val DEFAULT_ALTITUDE_M = 10.0
    }
}
