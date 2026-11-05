package com.example.android_px4_gcs.core.model

/** GCS가 보낼 수 있는 명령. */
enum class DroneCommand {
    ARM,
    TAKEOFF,
    LAND,
    RTL,
}

/**
 * 명령 한 번의 결과 (Repository → ViewModel).
 *
 * Success = PX4가 명령을 "수락"했다(COMMAND_ACK ACCEPTED)는 뜻이지,
 * 기체 상태가 바뀌었다는 뜻은 아니다. 실제 상태 변화는 Telemetry(DroneState)로 확인한다.
 */
sealed interface CommandResult {
    data object Success : CommandResult
    data class Failure(val reason: String) : CommandResult
}

/** UI에 보여줄 명령 진행 상태. */
sealed interface CommandState {
    data object Idle : CommandState
    data object Running : CommandState
    data object Success : CommandState
    data class Failed(val reason: String) : CommandState
}
