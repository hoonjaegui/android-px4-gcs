package io.github.hoonjaegui.px4gcs.core.domain.mission

import javax.inject.Inject

/**
 * 삭제는 거부하지 않는다. 결과로 생긴 문제(예: 양옆 점이 붙음)는 [ObserveMissionPlanUseCase]의 errors로 드러난다.
 * 고치는 중간 상태를 막으면 운용자가 편집을 할 수 없기 때문이다. [MoveWaypointUseCase]도 같다.
 */
class RemoveWaypointUseCase @Inject constructor(
    private val repository: MissionPlanRepository,
) {
    operator fun invoke(index: Int) = repository.save(repository.plan.value.remove(index))
}
