package io.github.hoonjaegui.px4gcs.core.domain.mission

import javax.inject.Inject

/** 순서 변경. 거부하지 않는다 — [RemoveWaypointUseCase] 참고. */
class MoveWaypointUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke(from: Int, to: Int) = repository.save(repository.plan.value.move(from, to))
}
