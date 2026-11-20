package io.github.hoonjaegui.px4gcs.core.domain.mission

import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.testing.FakeMissionPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 거부가 없는 편집(삭제·순서 변경·전체 삭제)과 plan 관찰. */
class MissionEditUseCasesTest {

    private val a = Waypoint(47.3977, 8.5455, 10.0)
    private val b = Waypoint(47.3980, 8.5460, 20.0)
    private val c = Waypoint(47.3983, 8.5465, 30.0)
    private val repository = FakeMissionPlanRepository(MissionPlan(listOf(a, b, c)))

    @Test
    fun `remove move and clear`() {
        MoveWaypointUseCase(repository)(from = 2, to = 0)
        assertEquals(listOf(c, a, b), repository.plan.value.waypoints)

        RemoveWaypointUseCase(repository)(1)
        assertEquals(listOf(c, b), repository.plan.value.waypoints)

        ClearMissionPlanUseCase(repository)()
        assertTrue(repository.plan.value.isEmpty())
    }

    @Test
    fun `observed plan comes with validation result`() = runTest {
        val observe = ObserveMissionPlanUseCase(repository)
        assertTrue(observe().first().isValid)

        ClearMissionPlanUseCase(repository)()
        val empty = observe().first()
        assertFalse(empty.isValid)
        assertEquals(listOf(MissionValidationError.Empty), empty.errors)
    }

    @Test
    fun `reorder that makes neighbours too close is allowed but reported`() = runTest {
        // a와 aNear는 0.5m. 사이에 b가 있어서 처음엔 유효
        val aNear = a.copy(latitudeDeg = a.latitudeDeg + 0.0000045)
        val repository = FakeMissionPlanRepository(MissionPlan(listOf(a, b, aNear)))

        MoveWaypointUseCase(repository)(from = 2, to = 1)

        assertEquals(listOf(a, aNear, b), repository.plan.value.waypoints)
        val error = ObserveMissionPlanUseCase(repository)().first().errors.single()
        assertEquals(1, (error as MissionValidationError.TooCloseToPrevious).index)
    }
}
