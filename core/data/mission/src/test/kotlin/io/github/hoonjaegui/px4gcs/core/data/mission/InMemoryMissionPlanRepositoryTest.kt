package io.github.hoonjaegui.px4gcs.core.data.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryMissionPlanRepositoryTest {

    @Test
    fun `starts empty and keeps the saved plan`() {
        val repository = InMemoryMissionPlanRepository()
        assertTrue(repository.plan.value.isEmpty())

        val plan = MissionPlan(listOf(Waypoint(47.3977, 8.5455, 10.0)))
        repository.save(plan)

        assertEquals(plan, repository.plan.value)
    }
}
