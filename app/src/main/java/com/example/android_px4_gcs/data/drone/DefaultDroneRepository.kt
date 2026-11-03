package com.example.android_px4_gcs.data.drone

import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.domain.drone.DroneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/**
 * MAVSDK Telemetry 스트림들을 도메인 모델 DroneState 하나로 합친다.
 *
 * 각 스트림은 서로 다른 주기로 들어오므로, 스트림마다 "DroneState를 어떻게 갱신할지"(Update)로
 * 변환한 뒤 merge → scan으로 누적한다. 스트림이 늘어도 이 구조는 그대로다.
 */
class DefaultDroneRepository(
    private val dataSource: MavsdkDroneDataSource,
    scope: CoroutineScope,
) : DroneRepository {

    private fun interface Update : (DroneState) -> DroneState

    /** Telemetry 값 갱신 + 수신 시각 기록. */
    private fun <T> Flow<T>.toTelemetryUpdate(apply: DroneState.(T) -> DroneState): Flow<Update> =
        map { value -> Update { it.apply(value).copy(lastTelemetryTime = System.currentTimeMillis()) } }

    override val droneState: StateFlow<DroneState> =
        merge(
            dataSource.isConnected.map { connected -> Update { it.copy(connected = connected) } },
            dataSource.batteryPercent.toTelemetryUpdate { copy(batteryPercent = it.roundToInt()) },
        )
            .scan(DroneState()) { state, update -> update(state) }
            .stateIn(scope, SharingStarted.Eagerly, DroneState())

    init {
        dataSource.start()
    }
}
