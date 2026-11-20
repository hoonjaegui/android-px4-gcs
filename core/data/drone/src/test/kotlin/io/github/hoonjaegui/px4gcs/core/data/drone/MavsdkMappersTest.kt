package io.github.hoonjaegui.px4gcs.core.data.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandFailureReason
import io.github.hoonjaegui.px4gcs.core.model.drone.FlightMode
import io.github.hoonjaegui.px4gcs.core.model.drone.MessageSeverity
import io.mavsdk.action.Action
import io.mavsdk.telemetry.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MavsdkMappersTest {

    @Test
    fun `every MAVSDK flight mode maps to a distinct domain mode`() {
        val mapped = Telemetry.FlightMode.values().map { it.toFlightMode() }
        assertEquals("1:1 매핑이어야 한다", Telemetry.FlightMode.values().size, mapped.toSet().size)
        assertEquals(FlightMode.entries.toSet(), mapped.toSet())
    }

    @Test
    fun `renamed flight modes`() {
        assertEquals(FlightMode.RETURN, Telemetry.FlightMode.RETURN_TO_LAUNCH.toFlightMode())
        assertEquals(FlightMode.POSITION, Telemetry.FlightMode.POSCTL.toFlightMode())
        assertEquals(FlightMode.ALTITUDE, Telemetry.FlightMode.ALTCTL.toFlightMode())
    }

    @Test
    fun `action results map to what the operator should do next`() {
        assertEquals(CommandFailureReason.REJECTED, Action.ActionResult.Result.COMMAND_DENIED.toFailureReason())
        assertEquals(CommandFailureReason.TIMEOUT, Action.ActionResult.Result.TIMEOUT.toFailureReason())
        assertEquals(CommandFailureReason.NOT_CONNECTED, Action.ActionResult.Result.NO_SYSTEM.toFailureReason())
        assertEquals(CommandFailureReason.BUSY, Action.ActionResult.Result.BUSY.toFailureReason())
    }

    @Test
    fun `status text severity`() {
        assertEquals(MessageSeverity.INFO, Telemetry.StatusTextType.INFO.toSeverity())
        assertEquals(MessageSeverity.WARNING, Telemetry.StatusTextType.WARNING.toSeverity())
        assertEquals(MessageSeverity.ERROR, Telemetry.StatusTextType.CRITICAL.toSeverity())
    }

    @Test
    fun `position needs at least a 3D fix`() {
        assertFalse(Telemetry.FixType.FIX_2D.hasPositionFix())
        assertTrue(Telemetry.FixType.FIX_3D.hasPositionFix())
        assertTrue(Telemetry.FixType.RTK_FIXED.hasPositionFix())
    }
}
