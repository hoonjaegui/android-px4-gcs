package io.github.hoonjaegui.px4gcs.core.mission

import io.github.hoonjaegui.px4gcs.core.mission.MissionValidationError.Empty
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidationError.InvalidWaypoint
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidationError.TooCloseToPrevious
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidator.MAX_ALTITUDE_M
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidator.MIN_ALTITUDE_M
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem.ALTITUDE_TOO_HIGH
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem.ALTITUDE_TOO_LOW
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem.INVALID_ALTITUDE
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem.INVALID_LATITUDE
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem.INVALID_LONGITUDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionValidatorTest {

    private fun wp(lat: Double = 47.3977, lon: Double = 8.5455, alt: Double = 10.0) = Waypoint(lat, lon, alt)

    private fun problems(waypoint: Waypoint) = MissionValidator.validate(waypoint)

    // --- Waypoint 단독 ---

    @Test
    fun `normal waypoint is valid`() {
        assertTrue(problems(wp()).isEmpty())
    }

    @Test
    fun `altitude boundaries are inclusive`() {
        assertTrue(problems(wp(alt = MIN_ALTITUDE_M)).isEmpty())
        assertTrue(problems(wp(alt = MAX_ALTITUDE_M)).isEmpty())
        assertEquals(listOf(ALTITUDE_TOO_LOW), problems(wp(alt = MIN_ALTITUDE_M - 0.01)))
        assertEquals(listOf(ALTITUDE_TOO_HIGH), problems(wp(alt = MAX_ALTITUDE_M + 0.01)))
    }

    @Test
    fun `negative altitude is too low`() {
        // NED의 z(-30 = 위로 30m)를 그대로 넣는 실수를 잡는다
        assertEquals(listOf(ALTITUDE_TOO_LOW), problems(wp(alt = -30.0)))
    }

    @Test
    fun `latitude and longitude boundaries`() {
        assertTrue(problems(wp(lat = 90.0, lon = 180.0)).isEmpty())
        assertTrue(problems(wp(lat = -90.0, lon = -180.0)).isEmpty())
        assertEquals(listOf(INVALID_LATITUDE), problems(wp(lat = 90.000001)))
        assertEquals(listOf(INVALID_LONGITUDE), problems(wp(lon = -180.000001)))
    }

    @Test
    fun `degE7 integer passed as degrees is rejected`() {
        // MAVLink 정수(473977430)를 변환 없이 넣는 실수
        assertEquals(listOf(INVALID_LATITUDE, INVALID_LONGITUDE), problems(wp(lat = 473977430.0, lon = 85455930.0)))
    }

    @Test
    fun `NaN and infinity are rejected`() {
        assertEquals(
            listOf(INVALID_LATITUDE, INVALID_LONGITUDE, INVALID_ALTITUDE),
            problems(wp(lat = Double.NaN, lon = Double.POSITIVE_INFINITY, alt = Double.NaN)),
        )
    }

    // --- MissionPlan ---

    @Test
    fun `empty plan is invalid`() {
        assertEquals(listOf(Empty), MissionValidator.validate(MissionPlan()))
    }

    @Test
    fun `valid plan has no errors`() {
        val plan = MissionPlan(listOf(wp(), wp(lat = 47.3980), wp(lat = 47.3983)))
        assertTrue(MissionValidator.validate(plan).isEmpty())
    }

    @Test
    fun `all errors are collected with their index`() {
        val plan = MissionPlan(listOf(wp(alt = 1.0), wp(lat = 47.3980), wp(lat = 47.3983, alt = 500.0)))
        assertEquals(
            listOf(InvalidWaypoint(0, ALTITUDE_TOO_LOW), InvalidWaypoint(2, ALTITUDE_TOO_HIGH)),
            MissionValidator.validate(plan),
        )
    }

    @Test
    fun `consecutive waypoints closer than 1m are rejected`() {
        // 위도 0.000005도 ≈ 0.56m
        val plan = MissionPlan(listOf(wp(), wp(lat = 47.397705)))
        val errors = MissionValidator.validate(plan)
        assertEquals(1, errors.size)
        assertEquals(1, (errors[0] as TooCloseToPrevious).index)
    }

    @Test
    fun `same position at different altitude is still too close`() {
        // 수직 이동만 하는 Waypoint는 이번 범위에서는 실수로 본다
        val plan = MissionPlan(listOf(wp(alt = 10.0), wp(alt = 30.0)))
        assertTrue(MissionValidator.validate(plan).single() is TooCloseToPrevious)
    }

    @Test
    fun `distance is not checked against an invalid coordinate`() {
        val plan = MissionPlan(listOf(wp(lat = Double.NaN), wp()))
        assertEquals(listOf(InvalidWaypoint(0, INVALID_LATITUDE)), MissionValidator.validate(plan))
    }

    @Test
    fun `distance of about 1 degree latitude is 111km`() {
        val d = MissionValidator.distanceM(wp(lat = 47.0), wp(lat = 48.0))
        assertEquals(111_195.0, d, 10.0)
    }
}
