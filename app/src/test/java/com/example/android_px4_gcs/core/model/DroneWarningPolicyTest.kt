package com.example.android_px4_gcs.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneWarningPolicyTest {

    private val connected = DroneState(connection = ConnectionStatus.CONNECTED)

    @Test
    fun `searching is not a connection loss`() {
        val warnings = DroneWarningPolicy.evaluate(DroneState(connection = ConnectionStatus.SEARCHING), nowMs = 0)
        assertFalse(warnings.connectionLost)
    }

    @Test
    fun `lost after connected is a connection loss`() {
        val warnings = DroneWarningPolicy.evaluate(DroneState(connection = ConnectionStatus.LOST), nowMs = 0)
        assertTrue(warnings.connectionLost)
    }

    @Test
    fun `field becomes stale only after threshold`() {
        val state = connected.copy(receivedAt = mapOf(TelemetryField.POSITION to 1_000L))

        val fresh = DroneWarningPolicy.evaluate(state, nowMs = 1_000L + DroneWarningPolicy.STALE_AFTER_MS)
        val stale = DroneWarningPolicy.evaluate(state, nowMs = 1_001L + DroneWarningPolicy.STALE_AFTER_MS)

        assertTrue(fresh.staleFields.isEmpty())
        assertEquals(setOf(TelemetryField.POSITION), stale.staleFields)
        assertTrue(stale.telemetryStale)
    }

    @Test
    fun `never received field is not stale`() {
        val warnings = DroneWarningPolicy.evaluate(connected, nowMs = 1_000_000L)
        assertTrue(warnings.staleFields.isEmpty())
    }

    @Test
    fun `stale data while disconnected is reported as connection loss not telemetry loss`() {
        val state = DroneState(connection = ConnectionStatus.LOST, receivedAt = mapOf(TelemetryField.POSITION to 0L))
        val warnings = DroneWarningPolicy.evaluate(state, nowMs = 10_000L)

        assertTrue(warnings.connectionLost)
        assertEquals(setOf(TelemetryField.POSITION), warnings.staleFields) // 값은 여전히 과거 값으로 표시
        assertFalse(warnings.telemetryStale) // 배너는 CONNECTION LOST 하나로
    }

    @Test
    fun `gps unknown is not gps lost`() {
        assertFalse(DroneWarningPolicy.evaluate(connected.copy(gpsAvailable = null), 0).gpsUnavailable)
        assertTrue(DroneWarningPolicy.evaluate(connected.copy(gpsAvailable = false), 0).gpsUnavailable)
    }

    @Test
    fun `low battery below threshold`() {
        fun low(percent: Int?) = DroneWarningPolicy.evaluate(connected.copy(batteryPercent = percent), 0).lowBattery
        assertFalse(low(null))
        assertFalse(low(DroneWarningPolicy.LOW_BATTERY_PERCENT))
        assertTrue(low(DroneWarningPolicy.LOW_BATTERY_PERCENT - 1))
    }
}
