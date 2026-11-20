package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneCommand
import javax.inject.Inject

/**
 * ARM / TAKEOFF / LAND / RTL. 네 명령 모두 "보내고 ACK 결과를 받는다"로 같아서 하나로 묶었다.
 * Success는 PX4가 수락했다는 뜻이지 상태가 바뀌었다는 뜻이 아니다 — 상태는 Telemetry로 확인한다.
 */
class ExecuteDroneCommandUseCase @Inject constructor(
    private val repository: DroneRepository,
) {
    suspend operator fun invoke(command: DroneCommand): CommandResult = when (command) {
        DroneCommand.ARM -> repository.arm()
        DroneCommand.TAKEOFF -> repository.takeoff()
        DroneCommand.LAND -> repository.land()
        DroneCommand.RTL -> repository.returnToLaunch()
    }
}
