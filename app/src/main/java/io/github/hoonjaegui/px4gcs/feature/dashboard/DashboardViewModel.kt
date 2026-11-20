package io.github.hoonjaegui.px4gcs.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneCommand
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneWarningState
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import io.github.hoonjaegui.px4gcs.core.domain.drone.ExecuteDroneCommandUseCase
import io.github.hoonjaegui.px4gcs.core.domain.drone.ObserveDroneStatusUseCase
import io.github.hoonjaegui.px4gcs.core.domain.drone.SetLinkLossActionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val drone: DroneState = DroneState(),
    val warnings: DroneWarningState = DroneWarningState(),
    val commandStates: Map<DroneCommand, CommandState> = emptyMap(),
    /** 통신 두절 설정 쓰기 진행 상태. */
    val linkLossSetting: LinkLossSettingState = LinkLossSettingState(),
    /** "N초 전" 계산용 현재 시각 (monotonic ms). */
    val nowElapsedMs: Long = 0L,
)

data class LinkLossSettingState(
    /** 지금 쓰고 있거나 마지막으로 쓰려 했던 값. */
    val requested: LinkLossAction? = null,
    val state: CommandState = CommandState.Idle,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDroneStatus: ObserveDroneStatusUseCase,
    private val executeDroneCommand: ExecuteDroneCommandUseCase,
    private val setLinkLossAction: SetLinkLossActionUseCase,
) : ViewModel() {

    private val commandStates = MutableStateFlow<Map<DroneCommand, CommandState>>(emptyMap())
    private val linkLossSetting = MutableStateFlow(LinkLossSettingState())

    val uiState: StateFlow<DashboardUiState> =
        combine(observeDroneStatus(), commandStates, linkLossSetting) { status, commands, setting ->
            DashboardUiState(
                drone = status.drone,
                warnings = status.warnings,
                commandStates = commands,
                linkLossSetting = setting,
                nowElapsedMs = status.nowElapsedMs,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onCommand(command: DroneCommand) {
        if (commandStates.value[command] == CommandState.Running) return // 중복 전송 방지
        setState(command, CommandState.Running)
        viewModelScope.launch {
            setState(command, executeDroneCommand(command).toCommandState())
        }
    }

    fun onLinkLossActionSelected(action: LinkLossAction) {
        if (linkLossSetting.value.state == CommandState.Running) return
        linkLossSetting.value = LinkLossSettingState(action, CommandState.Running)
        viewModelScope.launch {
            linkLossSetting.value = LinkLossSettingState(action, setLinkLossAction(action).toCommandState())
        }
    }

    private fun setState(command: DroneCommand, state: CommandState) =
        commandStates.update { it + (command to state) }

    private fun CommandResult.toCommandState(): CommandState = when (this) {
        CommandResult.Success -> CommandState.Success
        is CommandResult.Failure -> CommandState.Failed(reason, detail, vehicleMessage)
    }
}
