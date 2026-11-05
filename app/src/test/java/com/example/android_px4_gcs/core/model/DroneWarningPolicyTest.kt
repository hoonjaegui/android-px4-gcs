package com.example.android_px4_gcs.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneWarningPolicyTest {

    private val connected = DroneState(connection = ConnectionStatus.CONNECTED)

    private fun at(elapsedMs: Long) = Timestamp(elapsedMs = elapsedMs, epochMs = elapsedMs)

    @Test
    fun `searching is not a connection loss`() {
        val warnings = DroneWarningPolicy.evaluate(DroneState(connection = ConnectionStatus.SEARCHING), nowElapsedMs = 0)
        assertFalse(warnings.connectionLost)
    }

    @Test
    fun `lost after connected is a connection loss`() {
        val warnings = DroneWarningPolicy.evaluate(DroneState(connection = ConnectionStatus.LOST), nowElapsedMs = 0)
        assertTrue(warnings.connectionLost)
    }

    @Test
    fun `field becomes stale only after threshold`() {
        val state = connected.copy(receivedAt = mapOf(TelemetryField.POSITION to at(1_000L)))

        val fresh = DroneWarningPolicy.evaluate(state, nowElapsedMs = 1_000L + DroneWarningPolicy.STALE_AFTER_MS)
        val stale = DroneWarningPolicy.evaluate(state, nowElapsedMs = 1_001L + DroneWarningPolicy.STALE_AFTER_MS)

        assertTrue(fresh.staleFields.isEmpty())
        assertEquals(setOf(TelemetryField.POSITION), stale.staleFields)
        assertTrue(stale.telemetryStale)
    }

    @Test
    fun `never received field is not stale`() {
        val warnings = DroneWarningPolicy.evaluate(connected, nowElapsedMs = 1_000_000L)
        assertTrue(warnings.staleFields.isEmpty())
    }

    @Test
    fun `stale data while disconnected is reported as connection loss not telemetry loss`() {
        val state = DroneState(connection = ConnectionStatus.LOST, receivedAt = mapOf(TelemetryField.POSITION to at(0L)))
        val warnings = DroneWarningPolicy.evaluate(state, nowElapsedMs = 10_000L)

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

    @Test
    fun `position freezes when gps is lost but altitude keeps updating`() {
        val beforeLoss = connected.copy(gpsAvailable = true)
            .withPosition(latitude = 47.0, longitude = 8.0, altitude = 10.0, at = at(1_000L))
        val afterLoss = beforeLoss.copy(gpsAvailable = false)
            .withPosition(latitude = 47.1, longitude = 8.1, altitude = 12.0, at = at(2_000L)) // EKF 추정치

        assertEquals(47.0, afterLoss.latitude!!, 0.0)
        assertEquals(8.0, afterLoss.longitude!!, 0.0)
        assertEquals(at(1_000L), afterLoss.receivedAt[TelemetryField.POSITION]) // 마지막 GPS 위치 시각 유지
        assertEquals(12.0, afterLoss.altitude!!, 0.0) // 고도는 기압계 기반이라 계속 갱신
        assertEquals(at(2_000L), afterLoss.receivedAt[TelemetryField.ALTITUDE])
    }

    @Test
    fun `position freezes when ekf distrusts global position even with gps fix`() {
        val state = connected.copy(gpsAvailable = true, globalPositionOk = false)
            .withPosition(latitude = 47.1, longitude = 8.1, altitude = 0.0, at = at(0L))
        assertEquals(null, state.latitude)
        assertTrue(DroneWarningPolicy.evaluate(state, 0).positionUnreliable)
    }

    @Test
    fun `frozen position is not reported as telemetry loss`() {
        val state = connected.copy(gpsAvailable = false, receivedAt = mapOf(TelemetryField.POSITION to at(0L)))
        val warnings = DroneWarningPolicy.evaluate(state, nowElapsedMs = 10_000L)
        assertTrue(warnings.gpsUnavailable)
        assertFalse(warnings.telemetryStale) // GPS SIGNAL LOST 하나로 충분
    }
}
