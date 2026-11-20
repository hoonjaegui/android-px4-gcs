package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.common.time.Clock
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneStatus
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * 기체 상태 + 경고. Telemetry가 "안 오는 것"은 이벤트가 없어서 Flow만으로는 감지할 수 없으므로
 * [TICK_MS]마다 시계를 흘려 "마지막 수신 후 얼마나 지났나"를 다시 판정한다.
 */
class ObserveDroneStatusUseCase @Inject constructor(
    private val repository: DroneRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<DroneStatus> =
        combine(repository.droneState, ticker()) { drone, now ->
            DroneStatus(drone = drone, warnings = DroneWarningPolicy.evaluate(drone, now), nowElapsedMs = now)
        }

    private fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(clock.elapsedRealtimeMs())
            delay(TICK_MS)
        }
    }

    companion object {
        const val TICK_MS = 500L
    }
}
