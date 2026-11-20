package io.github.hoonjaegui.px4gcs.feature.mission.impl

import io.github.hoonjaegui.px4gcs.core.domain.drone.ObserveVehiclePositionUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.AddWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.ClearMissionPlanUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.MoveWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.ObserveMissionPlanUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.RemoveWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.UpdateWaypointAltitudeUseCase
import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.VehiclePosition
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditRejection
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.model.mission.WaypointProblem
import io.github.hoonjaegui.px4gcs.core.testing.FakeDroneRepository
import io.github.hoonjaegui.px4gcs.core.testing.FakeMissionPlanRepository
import io.github.hoonjaegui.px4gcs.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 편집 규칙 자체는 `:core:domain`의 UseCase 테스트가 검증한다.
 * 여기서는 ViewModel의 몫만 본다: 입력 문자열 해석, 거부 사유 표시/해제, UseCase 결과가 화면 상태로 이어지는지.
 */
class MissionPlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val a = Waypoint(47.3977, 8.5455, 10.0)
    private val b = Waypoint(47.3980, 8.5460, 20.0)
    private val c = Waypoint(47.3983, 8.5465, 30.0)

    private fun viewModel(
        repository: FakeMissionPlanRepository,
        drone: FakeDroneRepository = FakeDroneRepository(),
    ) = MissionPlannerViewModel(
        observeMissionPlan = ObserveMissionPlanUseCase(repository),
        observeVehiclePosition = ObserveVehiclePositionUseCase(drone),
        addWaypoint = AddWaypointUseCase(repository),
        removeWaypoint = RemoveWaypointUseCase(repository),
        moveWaypoint = MoveWaypointUseCase(repository),
        updateWaypointAltitude = UpdateWaypointAltitudeUseCase(repository),
        clearMissionPlan = ClearMissionPlanUseCase(repository),
    )

    /**
     * uiState는 WhileSubscribed라 누군가 collect해야 값이 흐른다(화면이 구독하는 것과 같은 상황).
     * backgroundScope는 기본적으로 launch를 줄만 세우고 바로 실행하지 않는다 → Unconfined로 즉시 구독 시작.
     */
    private fun TestScope.subscribe(viewModel: MissionPlannerViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun TestScope.setUp(
        initial: MissionPlan = MissionPlan(),
        drone: FakeDroneRepository = FakeDroneRepository(),
    ): Pair<MissionPlannerViewModel, FakeMissionPlanRepository> {
        val repository = FakeMissionPlanRepository(initial)
        return viewModel(repository, drone).also { subscribe(it) } to repository
    }

    @Test
    fun `initial state is empty and invalid`() = runTest {
        val (viewModel, _) = setUp()
        assertEquals(listOf(MissionValidationError.Empty), viewModel.uiState.value.errors)
        assertFalse(viewModel.uiState.value.isValid)
    }

    @Test
    fun `added waypoint and edits appear in ui state`() = runTest {
        val (viewModel, _) = setUp(MissionPlan(listOf(a, b, c)))

        viewModel.onMoveWaypoint(from = 2, to = 0)
        viewModel.onRemoveWaypoint(1)
        viewModel.onAltitudeSubmitted(0, " 15 ")

        assertEquals(listOf(c.copy(relativeAltitudeM = 15.0), b), viewModel.uiState.value.plan.waypoints)
        assertTrue(viewModel.uiState.value.isValid)
    }

    @Test
    fun `domain rejection is shown and plan unchanged`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "500")

        assertEquals(
            EditRejection.Rejected(MissionEditRejection.InvalidWaypoint(listOf(WaypointProblem.ALTITUDE_TOO_HIGH))),
            viewModel.uiState.value.rejection,
        )
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `non numeric altitude is rejected by the view model`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "abc")

        assertEquals(EditRejection.NotANumber("abc"), viewModel.uiState.value.rejection)
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `successful edit clears previous rejection`() = runTest {
        val (viewModel, _) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "-5")
        viewModel.onAddWaypoint(47.3980, 8.5460)

        assertNull(viewModel.uiState.value.rejection)
        assertEquals(2, viewModel.uiState.value.plan.size)
    }

    @Test
    fun `remove clears previous rejection`() = runTest {
        val (viewModel, _) = setUp(MissionPlan(listOf(a, b)))

        viewModel.onAltitudeSubmitted(0, "abc")
        viewModel.onRemoveWaypoint(1)

        assertNull(viewModel.uiState.value.rejection)
    }

    @Test
    fun `vehicle position is passed to ui state`() = runTest {
        val drone = FakeDroneRepository(
            DroneState(connection = ConnectionStatus.CONNECTED, latitude = 47.3977, longitude = 8.5455),
        )
        val (viewModel, _) = setUp(drone = drone)
        assertEquals(VehiclePosition(47.3977, 8.5455, current = true), viewModel.uiState.value.vehicle)

        drone.droneState.value = drone.droneState.value.copy(connection = ConnectionStatus.LOST)
        assertEquals(VehiclePosition(47.3977, 8.5455, current = false), viewModel.uiState.value.vehicle)
    }

    @Test
    fun `plan survives a new ViewModel`() = runTest {
        // 화면을 나갔다 들어오면 ViewModel은 새로 만들어지지만 Repository는 Singleton이라 그대로
        val repository = FakeMissionPlanRepository()
        viewModel(repository).onAddWaypoint(47.3977, 8.5455)

        val next = viewModel(repository)
        subscribe(next)

        assertEquals(1, next.uiState.value.plan.size)
    }
}
