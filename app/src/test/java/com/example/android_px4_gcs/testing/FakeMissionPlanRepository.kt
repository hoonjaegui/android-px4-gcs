package com.example.android_px4_gcs.testing

import com.example.android_px4_gcs.core.mission.MissionPlan
import com.example.android_px4_gcs.domain.mission.MissionPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 테스트용 Repository. 실제 구현과 달리
 * - 시작 plan을 마음대로 정할 수 있고 (Given)
 * - save가 몇 번, 무엇으로 불렸는지 기록한다 (Then: "거부된 편집은 저장되지 않았다")
 */
class FakeMissionPlanRepository(initial: MissionPlan = MissionPlan()) : MissionPlanRepository {
    private val _plan = MutableStateFlow(initial)
    override val plan: StateFlow<MissionPlan> = _plan

    val savedPlans = mutableListOf<MissionPlan>()

    override fun save(plan: MissionPlan) {
        savedPlans += plan
        _plan.value = plan
    }
}
