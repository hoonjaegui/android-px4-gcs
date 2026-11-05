package com.example.android_px4_gcs.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkLossOutlookTest {

    private val lostAt = Timestamp(elapsedMs = 10_000L, epochMs = 0L)
    private val lost = DroneState(connection = ConnectionStatus.LOST, connectionLostAt = lostAt)

    @Test
    fun `no outlook while connected`() {
        assertNull(LinkLossOutlook.of(DroneState(connection = ConnectionStatus.CONNECTED), nowElapsedMs = 99_000L))
    }

    @Test
    fun `unknown when params were never read`() {
        assertEquals(LinkLossOutlook.Unknown, LinkLossOutlook.of(lost, nowElapsedMs = 12_000L))
    }

    @Test
    fun `px4 default does nothing on link loss`() {
        val state = lost.copy(linkLossPolicy = LinkLossPolicy(LinkLossAction.DISABLED, timeoutSec = 10))
        assertEquals(LinkLossOutlook.NoAction, LinkLossOutlook.of(state, nowElapsedMs = 99_000L))
    }

    @Test
    fun `pending before timeout then triggered after`() {
        val state = lost.copy(linkLossPolicy = LinkLossPolicy(LinkLossAction.RETURN, timeoutSec = 10))

        assertEquals(LinkLossOutlook.Pending(LinkLossAction.RETURN, remainingSec = 7), LinkLossOutlook.of(state, nowElapsedMs = 13_000L))
        assertEquals(LinkLossOutlook.Triggered(LinkLossAction.RETURN, sinceSec = 5), LinkLossOutlook.of(state, nowElapsedMs = 25_000L))
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
}
