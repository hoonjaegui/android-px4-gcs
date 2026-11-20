package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.VehiclePosition
import io.github.hoonjaegui.px4gcs.core.testing.FakeDroneRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveVehiclePositionUseCaseTest {

    private val connectedAtHome = DroneState(connection = ConnectionStatus.CONNECTED, latitude = 47.3977, longitude = 8.5455)

    private suspend fun positionOf(state: DroneState) = ObserveVehiclePositionUseCase(FakeDroneRepository(state))().first()

    @Test
    fun `no position before any position`() = runTest {
        assertNull(positionOf(DroneState(connection = ConnectionStatus.CONNECTED)))
    }

    @Test
    fun `connected with trusted position is current`() = runTest {
        assertEquals(VehiclePosition(47.3977, 8.5455, current = true), positionOf(connectedAtHome))
    }

    @Test
    fun `link lost keeps last position but not current`() = runTest {
        assertEquals(
            VehiclePosition(47.3977, 8.5455, current = false),
            positionOf(connectedAtHome.copy(connection = ConnectionStatus.LOST)),
        )
    }

    @Test
    fun `gps lost while connected is not current`() = runTest {
        assertFalse(positionOf(connectedAtHome.copy(gpsAvailable = false))!!.current)
    }

    @Test
    fun `ekf distrust while connected is not current`() = runTest {
        assertFalse(positionOf(connectedAtHome.copy(globalPositionOk = false))!!.current)
    }
}
