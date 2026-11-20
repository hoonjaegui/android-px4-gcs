package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandFailureReason
import io.github.hoonjaegui.px4gcs.core.model.drone.CommandResult
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneCommand
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import io.github.hoonjaegui.px4gcs.core.testing.FakeDroneRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneCommandUseCasesTest {

    private val drone = FakeDroneRepository()

    @Test
    fun `each command calls the matching repository action`() = runTest {
        val execute = ExecuteDroneCommandUseCase(drone)
        DroneCommand.entries.forEach { execute(it) }
        assertEquals(listOf("arm", "takeoff", "land", "returnToLaunch"), drone.calls)
    }

    @Test
    fun `command failure is passed through unchanged`() = runTest {
        val failure = CommandResult.Failure(CommandFailureReason.REJECTED, "COMMAND_DENIED", "Arming denied")
        drone.commandResult = failure
        assertEquals(failure, ExecuteDroneCommandUseCase(drone)(DroneCommand.ARM))
    }

    @Test
    fun `selectable link loss actions are written`() = runTest {
        val set = SetLinkLossActionUseCase(drone)
        LinkLossAction.selectable.forEach { set(it) }
        assertEquals(LinkLossAction.selectable.map { "setLinkLossAction($it)" }, drone.calls)
    }

    @Test
    fun `dangerous link loss actions are refused before reaching the vehicle`() = runTest {
        val set = SetLinkLossActionUseCase(drone)
        listOf(LinkLossAction.TERMINATE, LinkLossAction.DISARM, LinkLossAction.UNKNOWN).forEach { action ->
            val thrown = runCatching { set(action) }.exceptionOrNull()
            assertTrue("$action", thrown is IllegalArgumentException)
        }
        assertTrue(drone.calls.isEmpty())
    }
}
