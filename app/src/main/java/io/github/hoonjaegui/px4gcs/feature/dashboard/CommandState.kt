package io.github.hoonjaegui.px4gcs.feature.dashboard

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandFailureReason

/** UI에 보여줄 명령 진행 상태. */
sealed interface CommandState {
    data object Idle : CommandState
    data object Running : CommandState
    data object Success : CommandState
    data class Failed(val reason: CommandFailureReason, val detail: String, val vehicleMessage: String? = null) : CommandState
}
