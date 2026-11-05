package com.example.android_px4_gcs.feature.dashboard

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.CommandState
import com.example.android_px4_gcs.core.model.DroneCommand
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.core.model.DroneWarningPolicy
import com.example.android_px4_gcs.core.model.DroneWarningState
import com.example.android_px4_gcs.core.model.LinkLossAction
import com.example.android_px4_gcs.domain.drone.DroneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val drone: DroneState = DroneState(),
    val warnings: DroneWarningState = DroneWarningState(),
    val commandStates: Map<DroneCommand, CommandState> = emptyMap(),
    /** 통신 두절 설정 쓰기 진행 상태. */
    val linkLossSetting: LinkLossSettingState = LinkLossSettingState(),
    /** "N초 전" 계산용 현재 시각 (SystemClock.elapsedRealtime ms). */
    val nowElapsedMs: Long = 0L,
)

data class LinkLossSettingState(
    /** 지금 쓰고 있거나 마지막으로 쓰려 했던 값. */
    val requested: LinkLossAction? = null,
    val state: CommandState = CommandState.Idle,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DroneRepository,
) : ViewModel() {

    private val commandStates = MutableStateFlow<Map<DroneCommand, CommandState>>(emptyMap())
    private val linkLossSetting = MutableStateFlow(LinkLossSettingState())

    /**
     * Telemetry가 "안 오는 것"은 이벤트가 발생하지 않으므로 Flow만으로는 감지할 수 없다.
     * 시계를 따로 흘려서 "마지막 수신 후 얼마나 지났나"를 주기적으로 다시 계산한다.
     */
    private val clock = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(CLOCK_TICK_MS)
        }
    }

    val uiState: StateFlow<DashboardUiState> =
        combine(repository.droneState, commandStates, linkLossSetting, clock) { drone, commands, setting, now ->
            DashboardUiState(
                drone = drone,
                warnings = DroneWarningPolicy.evaluate(drone, now),
                commandStates = commands,
                linkLossSetting = setting,
                nowElapsedMs = now,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onCommand(command: DroneCommand) {
        if (commandStates.value[command] == CommandState.Running) return // 중복 전송 방지
        setState(command, CommandState.Running)
        viewModelScope.launch {
            val result = when (command) {
                DroneCommand.ARM -> repository.arm()
                DroneCommand.TAKEOFF -> repository.takeoff()
                DroneCommand.LAND -> repository.land()
                DroneCommand.RTL -> repository.returnToLaunch()
            }
            setState(
                command,
                when (result) {
                    CommandResult.Success -> CommandState.Success
                    is CommandResult.Failure -> CommandState.Failed(result.reason, result.detail, result.vehicleMessage)
                },
            )
        }
    }

    fun onLinkLossActionSelected(action: LinkLossAction) {
        if (linkLossSetting.value.state == CommandState.Running) return
        linkLossSetting.value = LinkLossSettingState(action, CommandState.Running)
        viewModelScope.launch {
            val state = when (val result = repository.setLinkLossAction(action)) {
                CommandResult.Success -> CommandState.Success
                is CommandResult.Failure -> CommandState.Failed(result.reason, result.detail, result.vehicleMessage)
            }
            linkLossSetting.value = LinkLossSettingState(action, state)
        }
    }

    private fun setState(command: DroneCommand, state: CommandState) =
        commandStates.update { it + (command to state) }

    private companion object {
        const val CLOCK_TICK_MS = 500L
    }
}
