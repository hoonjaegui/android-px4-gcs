package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossOutlook
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossPolicy
import io.github.hoonjaegui.px4gcs.core.model.drone.MessageSeverity
import io.github.hoonjaegui.px4gcs.core.model.drone.Timestamp
import io.github.hoonjaegui.px4gcs.core.model.drone.VehicleMessage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkLossOutlookTest {

    private val lostAt = Timestamp(elapsedMs = 10_000L, epochMs = 0L)
    private val lost = DroneState(connection = ConnectionStatus.LOST, connectionLostAt = lostAt, armed = true)

    @Test
    fun `no outlook while connected`() {
        assertNull(LinkLossOutlookPolicy.evaluate(DroneState(connection = ConnectionStatus.CONNECTED), nowElapsedMs = 99_000L))
    }

    @Test
    fun `unknown when params were never read`() {
        assertEquals(LinkLossOutlook.Unknown, LinkLossOutlookPolicy.evaluate(lost, nowElapsedMs = 12_000L))
    }

    @Test
    fun `px4 default does nothing on link loss`() {
        val state = lost.copy(linkLossPolicy = LinkLossPolicy(LinkLossAction.DISABLED, timeoutSec = 10))
        assertEquals(LinkLossOutlook.NoAction, LinkLossOutlookPolicy.evaluate(state, nowElapsedMs = 99_000L))
    }

    @Test
    fun `pending before timeout then triggered after`() {
        val state = lost.copy(linkLossPolicy = LinkLossPolicy(LinkLossAction.RETURN, timeoutSec = 10))

        assertEquals(LinkLossOutlook.Pending(LinkLossAction.RETURN, remainingSec = 7), LinkLossOutlookPolicy.evaluate(state, nowElapsedMs = 13_000L))
        assertEquals(LinkLossOutlook.Triggered(LinkLossAction.RETURN, sinceSec = 5), LinkLossOutlookPolicy.evaluate(state, nowElapsedMs = 25_000L))
    }

    @Test
    fun `nav dll act values follow px4 definition`() {
        assertEquals(LinkLossAction.DISABLED, LinkLossAction.fromNavDllAct(0))
        assertEquals(LinkLossAction.RETURN, LinkLossAction.fromNavDllAct(2))
        assertEquals(LinkLossAction.DISARM, LinkLossAction.fromNavDllAct(6))
        assertEquals(LinkLossAction.UNKNOWN, LinkLossAction.fromNavDllAct(4))
    }

    @Test
    fun `keeps only latest messages newest first`() {
        val state = (1..7).fold(DroneState()) { s, i ->
            s.withMessage(VehicleMessage(MessageSeverity.INFO, "m$i", Timestamp(i.toLong(), 0L)))
        }
        assertEquals(listOf("m7", "m6", "m5", "m4", "m3"), state.messages.map { it.text })
    }

    @Test
    fun `no failsafe expected when vehicle was disarmed`() {
        val state = lost.copy(armed = false, linkLossPolicy = LinkLossPolicy(LinkLossAction.RETURN, timeoutSec = 10))
        assertEquals(LinkLossOutlook.Disarmed, LinkLossOutlookPolicy.evaluate(state, nowElapsedMs = 25_000L))
    }
}
