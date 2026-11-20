package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditRejection
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditResult
import javax.inject.Inject

/** 고도 변경. 바뀐 Waypoint가 잘못되면 거부하고 기존 값을 유지한다. */
class UpdateWaypointAltitudeUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke(index: Int, relativeAltitudeM: Double): MissionEditResult {
        val candidate = repository.plan.value.updateAltitude(index, relativeAltitudeM)
        val problems = MissionValidator.validate(candidate.waypoints[index])
        if (problems.isNotEmpty()) return MissionEditResult.Rejected(MissionEditRejection.InvalidWaypoint(problems))
        repository.save(candidate)
        return MissionEditResult.Accepted
    }
}
