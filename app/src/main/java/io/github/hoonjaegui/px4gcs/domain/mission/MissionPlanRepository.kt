package io.github.hoonjaegui.px4gcs.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import kotlinx.coroutines.flow.StateFlow

/**
 * 편집 중인 MissionPlan의 보관소. 화면(ViewModel)보다 오래 산다 → 화면을 나갔다 와도 plan이 남고,
 * Week 3에서 업로드할 때도 같은 plan을 읽는다.
 *
 * ViewModel은 이 interface만 안다 → 테스트에서 Fake로 바꿔 끼울 수 있다.
 */
interface MissionPlanRepository {
    val plan: StateFlow<MissionPlan>

    /** 검증은 하지 않는다. 받은 plan을 그대로 현재 plan으로 둔다. */
    fun save(plan: MissionPlan)
}
