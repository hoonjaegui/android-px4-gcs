package com.example.android_px4_gcs.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.CommandState
import com.example.android_px4_gcs.core.model.DroneCommand
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.domain.drone.DroneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DroneRepository,
) : ViewModel() {
    /** 기체가 "지금 어떤 상태인가" (Telemetry). */
    val droneState: StateFlow<DroneState> = repository.droneState

    private val _commandStates = MutableStateFlow<Map<DroneCommand, CommandState>>(emptyMap())

    /** 내가 보낸 명령이 "어떻게 처리됐는가" (명령별). 없으면 Idle. */
    val commandStates: StateFlow<Map<DroneCommand, CommandState>> = _commandStates.asStateFlow()

    fun onCommand(command: DroneCommand) {
        if (_commandStates.value[command] == CommandState.Running) return // 중복 전송 방지
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
                    is CommandResult.Failure -> CommandState.Failed(result.reason)
                },
            )
        }
    }

    private fun setState(command: DroneCommand, state: CommandState) =
        _commandStates.update { it + (command to state) }
}
