package io.github.hoonjaegui.px4gcs.testing

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import io.github.hoonjaegui.px4gcs.domain.drone.DroneRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** 테스트에서 기체 상태를 직접 밀어 넣는다. 명령은 전부 성공으로 응답. */
class FakeDroneRepository(initial: DroneState = DroneState()) : DroneRepository {
    override val droneState = MutableStateFlow(initial)

    override suspend fun arm() = CommandResult.Success
    override suspend fun takeoff() = CommandResult.Success
    override suspend fun land() = CommandResult.Success
    override suspend fun returnToLaunch() = CommandResult.Success
    override suspend fun setLinkLossAction(action: LinkLossAction) = CommandResult.Success
}
