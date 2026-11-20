package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import javax.inject.Inject

/** 통신 두절 시 기체 행동(NAV_DLL_ACT)을 기체에 쓴다. 결과는 기체에서 다시 읽은 값 기준. */
class SetLinkLossActionUseCase @Inject constructor(
    private val repository: DroneRepository,
) {
    /**
     * @throws IllegalArgumentException [LinkLossAction.selectable] 밖의 값.
     *   TERMINATE(모터 정지 → 추락)·DISARM은 공중 사고로 직결되므로 GCS에서 설정하지 못하게 한다.
     *   지금까지는 화면이 선택지를 안 보여주는 것으로만 막았다 — 규칙을 domain에 둔다.
     */
    suspend operator fun invoke(action: LinkLossAction): CommandResult {
        require(action in LinkLossAction.selectable) { "$action cannot be set from the GCS" }
        return repository.setLinkLossAction(action)
    }
}
