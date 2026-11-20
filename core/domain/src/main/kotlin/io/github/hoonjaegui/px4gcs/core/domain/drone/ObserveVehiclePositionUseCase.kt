package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.VehiclePosition
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 지도용 기체 위치. 연결이 끊겼거나 GPS/EKF를 못 믿으면 current=false (Dashboard의 stale 회색과 같은 규칙). */
class ObserveVehiclePositionUseCase @Inject constructor(
    private val repository: DroneRepository,
) {
    operator fun invoke(): Flow<VehiclePosition?> =
        repository.droneState.map { it.toVehiclePosition() }.distinctUntilChanged()

    private fun DroneState.toVehiclePosition(): VehiclePosition? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return VehiclePosition(lat, lon, current = isConnected && positionTrusted)
    }
}
