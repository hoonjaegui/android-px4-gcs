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

class UpdateWaypointAltitudeUseCaseTest {

    private val repository = FakeMissionPlanRepository(MissionPlan(listOf(Waypoint(47.3977, 8.5455, 10.0))))
    private val update = UpdateWaypointAltitudeUseCase(repository)

    @Test
    fun `out of range altitude is rejected and old value kept`() {
        assertEquals(
            MissionEditResult.Rejected(MissionEditRejection.InvalidWaypoint(listOf(WaypointProblem.ALTITUDE_TOO_HIGH))),
            update(0, 500.0),
        )
        assertEquals(10.0, repository.plan.value.waypoints[0].relativeAltitudeM, 0.0)
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `valid altitude is saved`() {
        assertEquals(MissionEditResult.Accepted, update(0, 30.0))
        assertEquals(30.0, repository.plan.value.waypoints[0].relativeAltitudeM, 0.0)
    }
}
