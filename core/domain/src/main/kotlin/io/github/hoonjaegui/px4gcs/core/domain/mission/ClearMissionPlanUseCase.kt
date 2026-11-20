package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import javax.inject.Inject

class ClearMissionPlanUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke() = repository.save(MissionPlan())
}
