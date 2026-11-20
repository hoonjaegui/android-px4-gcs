package io.github.hoonjaegui.px4gcs.core.model.mission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionPlanTest {

    // 취리히 SITL 홈 근처
    private val a = Waypoint(47.3977, 8.5455, 10.0)
    private val b = Waypoint(47.3980, 8.5460, 20.0)
    private val c = Waypoint(47.3983, 8.5465, 30.0)
    private val d = Waypoint(47.3986, 8.5470, 40.0)

    private val abcd = MissionPlan(listOf(a, b, c, d))

    @Test
    fun `add appends in order`() {
        val plan = MissionPlan().add(a).add(b).add(c)
        assertEquals(listOf(a, b, c), plan.waypoints)
    }

    @Test
    fun `remove middle keeps order of the rest`() {
        assertEquals(listOf(a, c, d), abcd.remove(1).waypoints)
    }

    @Test
    fun `remove last leaves empty plan`() {
        assertTrue(MissionPlan(listOf(a)).remove(0).isEmpty())
    }

    @Test
    fun `move forward`() {
        assertEquals(listOf(b, c, a, d), abcd.move(from = 0, to = 2).waypoints)
    }

    @Test
    fun `move backward`() {
        assertEquals(listOf(d, a, b, c), abcd.move(from = 3, to = 0).waypoints)
    }

    @Test
    fun `move to same index returns same plan`() {
        assertSame(abcd, abcd.move(from = 1, to = 1))
    }

    @Test
    fun `updateAltitude changes only that waypoint`() {
        val plan = abcd.updateAltitude(2, 55.0)
        assertEquals(listOf(a, b, c.copy(relativeAltitudeM = 55.0), d), plan.waypoints)
    }

    @Test
    fun `editing does not change the original plan`() {
        abcd.add(a)
        abcd.remove(0)
        abcd.move(0, 3)
        abcd.updateAltitude(0, 99.0)
        assertEquals(listOf(a, b, c, d), abcd.waypoints)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `remove out of range throws`() {
        abcd.remove(4)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `move out of range throws`() {
        abcd.move(0, 4)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `updateAltitude on empty plan throws`() {
        MissionPlan().updateAltitude(0, 10.0)
    }
}
