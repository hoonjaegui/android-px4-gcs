package com.example.android_px4_gcs.data.drone

import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.domain.drone.DroneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** MAVSDK Telemetry 스트림들을 도메인 모델 DroneState 하나로 합친다. */
class DefaultDroneRepository(
    private val dataSource: MavsdkDroneDataSource,
    scope: CoroutineScope,
) : DroneRepository {

    override val droneState: StateFlow<DroneState> =
        dataSource.isConnected
            .map { connected -> DroneState(connected = connected) }
            .stateIn(scope, SharingStarted.Eagerly, DroneState())

    init {
        dataSource.start()
    }
}
