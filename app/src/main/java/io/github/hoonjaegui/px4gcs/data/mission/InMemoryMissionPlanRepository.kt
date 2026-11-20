package io.github.hoonjaegui.px4gcs.data.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.domain.mission.MissionPlanRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 앱 프로세스가 살아 있는 동안만 유지한다. 파일/DB 저장은 필요해지면 이 클래스만 바꾼다. */
@Singleton
class InMemoryMissionPlanRepository @Inject constructor() : MissionPlanRepository {
    private val _plan = MutableStateFlow(MissionPlan())
    override val plan: StateFlow<MissionPlan> = _plan.asStateFlow()

    override fun save(plan: MissionPlan) {
        _plan.value = plan
    }
}
