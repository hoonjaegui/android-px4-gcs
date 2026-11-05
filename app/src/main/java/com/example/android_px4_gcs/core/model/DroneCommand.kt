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
    /**
     * @param detail MAVSDK 결과 코드 (예: "COMMAND_DENIED: Command Denied")
     * @param vehicleMessage 거부 무렵 PX4가 보낸 STATUSTEXT. 실제 원인이 여기 있다.
     */
    data class Failure(
        val reason: CommandFailureReason,
        val detail: String,
        val vehicleMessage: String? = null,
    ) : CommandResult
}

/** 실패의 "의미"에 따른 분류. 운용자가 다음에 무엇을 해야 하는지가 다르다. */
enum class CommandFailureReason {
    /** 기체에 닿지 않음 → 기체 상태는 그대로. */
    NOT_CONNECTED,
    /** 기체가 받고 거부함 (COMMAND_DENIED 등) → 기체 상태는 그대로. */
    REJECTED,
    /** 이전 명령 처리 중. */
    BUSY,
    /** 응답(ACK)을 못 받음 → 명령이 전달·실행됐는지 알 수 없다. Telemetry로 확인해야 한다. */
    TIMEOUT,
    UNKNOWN,
}

/** UI에 보여줄 명령 진행 상태. */
sealed interface CommandState {
    data object Idle : CommandState
    data object Running : CommandState
    data object Success : CommandState
    data class Failed(val reason: CommandFailureReason, val detail: String, val vehicleMessage: String? = null) : CommandState
}
