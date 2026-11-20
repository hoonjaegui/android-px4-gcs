package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.ValidatedMissionPlan
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMissionPlanUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke(): Flow<ValidatedMissionPlan> =
        repository.plan.map { ValidatedMissionPlan(it, MissionValidator.validate(it)) }
}
