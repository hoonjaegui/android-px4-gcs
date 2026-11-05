package com.example.android_px4_gcs.core.model

/** PX4가 보낸 사람이 읽는 메시지 (MAVLink STATUSTEXT). 예: "Arming denied: Resolve system health failures first". */
data class VehicleMessage(
    val severity: MessageSeverity,
    val text: String,
    val at: Timestamp,
)

enum class MessageSeverity { INFO, WARNING, ERROR }

/**
 * GCS 연결이 끊겼을 때 PX4가 스스로 할 행동 (파라미터 NAV_DLL_ACT, COM_DL_LOSS_T).
 * PX4 기본값은 DISABLED — 끊겨도 현재 모드를 그대로 유지한다.
 */
data class LinkLossPolicy(
    val action: LinkLossAction,
    val timeoutSec: Int,
)

enum class LinkLossAction {
    DISABLED, HOLD, RETURN, LAND, TERMINATE, DISARM, UNKNOWN;

    companion object {
        /** PX4 commander_params.yaml의 NAV_DLL_ACT 값. */
        fun fromNavDllAct(value: Int): LinkLossAction = when (value) {
            0 -> DISABLED
            1 -> HOLD
            2 -> RETURN
            3 -> LAND
            5 -> TERMINATE
            6 -> DISARM
            else -> UNKNOWN
        }
    }
}

/** 연결이 끊긴 지금, 기체가 무엇을 하고 있을 것으로 예상되는가. */
sealed interface LinkLossOutlook {
    /** 기체 설정을 못 읽음. */
    data object Unknown : LinkLossOutlook

    /** NAV_DLL_ACT=0. 기체는 끊기기 전 모드를 계속 수행한다. */
    data object NoAction : LinkLossOutlook

    /** 아직 timeout 전. [remainingSec] 후 [action] 예정. */
    data class Pending(val action: LinkLossAction, val remainingSec: Long) : LinkLossOutlook

    /** timeout이 지남. 기체는 [action]에 들어갔을 것이다 (확인은 재연결 후에만 가능). */
    data class Triggered(val action: LinkLossAction, val sinceSec: Long) : LinkLossOutlook

    companion object {
        /** 연결 상태가 LOST가 아니면 null. */
        fun of(state: DroneState, nowElapsedMs: Long): LinkLossOutlook? {
            val lostAt = state.connectionLostAt ?: return null
            if (state.connection != ConnectionStatus.LOST) return null
            val policy = state.linkLossPolicy ?: return Unknown
            if (policy.action == LinkLossAction.DISABLED) return NoAction
            // GCS의 LOST 판정(3초)과 PX4의 판정 시점은 정확히 같지 않다. 대략적인 예상치다.
            val lostForSec = (nowElapsedMs - lostAt.elapsedMs) / 1000
            val remaining = policy.timeoutSec - lostForSec
            return if (remaining > 0) Pending(policy.action, remaining) else Triggered(policy.action, -remaining)
        }
    }
}
