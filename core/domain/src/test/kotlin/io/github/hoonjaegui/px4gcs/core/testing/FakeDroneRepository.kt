package io.github.hoonjaegui.px4gcs.core.testing

import io.github.hoonjaegui.px4gcs.core.domain.drone.DroneRepository
import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import kotlinx.coroutines.flow.MutableStateFlow

/** 테스트에서 기체 상태를 직접 밀어 넣는다. 명령은 [commandResult]로 응답하고 호출을 [calls]에 기록한다. */
class FakeDroneRepository(initial: DroneState = DroneState()) : DroneRepository {
    override val droneState = MutableStateFlow(initial)

    var commandResult: CommandResult = CommandResult.Success
    val calls = mutableListOf<String>()

    override suspend fun arm() = record("arm")
    override suspend fun takeoff() = record("takeoff")
    override suspend fun land() = record("land")
    override suspend fun returnToLaunch() = record("returnToLaunch")
    override suspend fun setLinkLossAction(action: LinkLossAction) = record("setLinkLossAction($action)")

    private fun record(call: String): CommandResult {
        calls += call
        return commandResult
    }
}
