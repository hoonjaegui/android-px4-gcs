package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditRejection
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditResult
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.model.mission.WaypointProblem
import io.github.hoonjaegui.px4gcs.core.testing.FakeMissionPlanRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddWaypointUseCaseTest {

    private val a = Waypoint(47.3977, 8.5455, 25.0)

    @Test
    fun `first waypoint uses default altitude`() {
        val repository = FakeMissionPlanRepository()
        assertEquals(MissionEditResult.Accepted, AddWaypointUseCase(repository)(47.3977, 8.5455))
        assertEquals(AddWaypointUseCase.DEFAULT_ALTITUDE_M, repository.plan.value.waypoints.single().relativeAltitudeM, 0.0)
    }

    @Test
    fun `next waypoint inherits last altitude`() {
        val repository = FakeMissionPlanRepository(MissionPlan(listOf(a)))
        AddWaypointUseCase(repository)(47.3980, 8.5460)
        assertEquals(listOf(25.0, 25.0), repository.plan.value.waypoints.map { it.relativeAltitudeM })
    }

    @Test
    fun `invalid coordinate is rejected and not saved`() {
        val repository = FakeMissionPlanRepository()
        val result = AddWaypointUseCase(repository)(473977430.0, 8.5455) // degE7를 그대로 넣는 실수
        assertEquals(
            MissionEditResult.Rejected(MissionEditRejection.InvalidWaypoint(listOf(WaypointProblem.INVALID_LATITUDE))),
            result,
        )
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `tap right next to previous waypoint is rejected and not saved`() {
        val repository = FakeMissionPlanRepository(MissionPlan(listOf(a)))
        val result = AddWaypointUseCase(repository)(a.latitudeDeg + 0.000001, a.longitudeDeg) // ≈ 0.1m
        val rejection = (result as MissionEditResult.Rejected).reason
        assertTrue(rejection is MissionEditRejection.TooCloseToPrevious)
        assertTrue(repository.savedPlans.isEmpty())
    }
}
