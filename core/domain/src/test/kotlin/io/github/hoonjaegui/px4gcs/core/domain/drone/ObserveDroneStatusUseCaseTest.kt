package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.TelemetryField
import io.github.hoonjaegui.px4gcs.core.model.drone.Timestamp
import io.github.hoonjaegui.px4gcs.core.testing.FakeDroneRepository
import io.github.hoonjaegui.px4gcs.core.testing.TestClock
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveDroneStatusUseCaseTest {

    @Test
    fun `field becomes stale while nothing new arrives`() = runTest {
        // 시계 = 가상 시간. 수신 시각 0에 POSITION을 받고 그 뒤로 아무것도 안 온다.
        val clock = TestClock { testScheduler.currentTime }
        val drone = FakeDroneRepository(
            DroneState(
                connection = ConnectionStatus.CONNECTED,
                receivedAt = mapOf(TelemetryField.POSITION to Timestamp(elapsedMs = 0, epochMs = 0)),
            ),
        )
        var latest = DroneStatus()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveDroneStatusUseCase(drone, clock)().collect { latest = it }
        }

        assertTrue(latest.warnings.staleFields.isEmpty())

        // 상태는 그대로인데 시간만 흐름 → ticker가 다시 판정해 stale이 된다
        advanceTimeBy(DroneWarningPolicy.STALE_AFTER_MS + ObserveDroneStatusUseCase.TICK_MS + 1)

        assertEquals(setOf(TelemetryField.POSITION), latest.warnings.staleFields)
        assertTrue(latest.nowElapsedMs > DroneWarningPolicy.STALE_AFTER_MS)
    }
}
