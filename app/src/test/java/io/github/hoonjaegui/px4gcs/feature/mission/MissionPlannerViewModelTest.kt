package io.github.hoonjaegui.px4gcs.feature.mission

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.model.mission.WaypointProblem
import io.github.hoonjaegui.px4gcs.testing.FakeDroneRepository
import io.github.hoonjaegui.px4gcs.testing.FakeMissionPlanRepository
import io.github.hoonjaegui.px4gcs.testing.MainDispatcherRule
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

class MissionPlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val a = Waypoint(47.3977, 8.5455, 10.0)
    private val b = Waypoint(47.3980, 8.5460, 20.0)
    private val c = Waypoint(47.3983, 8.5465, 30.0)

    /**
     * uiState는 WhileSubscribed라 누군가 collect해야 값이 흐른다(화면이 구독하는 것과 같은 상황).
     * backgroundScope는 기본적으로 launch를 줄만 세우고 바로 실행하지 않는다 → Unconfined로 즉시 구독 시작.
     * backgroundScope의 collect는 테스트가 끝나면 자동으로 취소된다.
     */
    private fun TestScope.subscribe(viewModel: MissionPlannerViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun TestScope.setUp(initial: MissionPlan = MissionPlan()): Pair<MissionPlannerViewModel, FakeMissionPlanRepository> {
        val repository = FakeMissionPlanRepository(initial)
        val viewModel = MissionPlannerViewModel(repository, FakeDroneRepository())
        subscribe(viewModel)
        return viewModel to repository
    }

    @Test
    fun `initial state is empty and invalid`() = runTest {
        val (viewModel, _) = setUp()
        assertEquals(listOf(MissionValidationError.Empty), viewModel.uiState.value.errors)
        assertFalse(viewModel.uiState.value.isValid)
    }

    @Test
    fun `first waypoint uses default altitude, next inherits last altitude`() = runTest {
        val (viewModel, repository) = setUp()

        viewModel.onAddWaypoint(47.3977, 8.5455)
        viewModel.onAltitudeSubmitted(0, "25")
        viewModel.onAddWaypoint(47.3980, 8.5460)

        val waypoints = repository.plan.value.waypoints
        assertEquals(listOf(25.0, 25.0), waypoints.map { it.relativeAltitudeM })
        assertTrue(viewModel.uiState.value.isValid)
    }

    @Test
    fun `three waypoints can be added, removed and reordered`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a, b, c)))

        viewModel.onMoveWaypoint(from = 2, to = 0)
        assertEquals(listOf(c, a, b), repository.plan.value.waypoints)

        viewModel.onRemoveWaypoint(1)
        assertEquals(listOf(c, b), repository.plan.value.waypoints)
        assertEquals(listOf(c, b), viewModel.uiState.value.plan.waypoints)
    }

    @Test
    fun `invalid coordinate is rejected and not saved`() = runTest {
        val (viewModel, repository) = setUp()

        viewModel.onAddWaypoint(473977430.0, 8.5455)

        assertTrue(repository.savedPlans.isEmpty())
        assertEquals(
            EditRejection.InvalidWaypoint(listOf(WaypointProblem.INVALID_LATITUDE)),
            viewModel.uiState.value.rejection,
        )
    }

    @Test
    fun `tap right next to previous waypoint is rejected`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a)))

        viewModel.onAddWaypoint(a.latitudeDeg + 0.000001, a.longitudeDeg) // ≈ 0.1m

        assertEquals(listOf(a), repository.plan.value.waypoints)
        assertTrue(viewModel.uiState.value.rejection is EditRejection.TooCloseToPrevious)
    }

    @Test
    fun `out of range altitude is rejected and old value kept`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "500")

        assertEquals(10.0, repository.plan.value.waypoints[0].relativeAltitudeM, 0.0)
        assertEquals(
            EditRejection.InvalidWaypoint(listOf(WaypointProblem.ALTITUDE_TOO_HIGH)),
            viewModel.uiState.value.rejection,
        )
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `non numeric altitude is rejected`() = runTest {
        val (viewModel, repository) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "abc")

        assertEquals(EditRejection.NotANumber("abc"), viewModel.uiState.value.rejection)
        assertTrue(repository.savedPlans.isEmpty())
    }

    @Test
    fun `successful edit clears previous rejection`() = runTest {
        val (viewModel, _) = setUp(MissionPlan(listOf(a)))

        viewModel.onAltitudeSubmitted(0, "-5")
        viewModel.onAltitudeSubmitted(0, " 15 ")

        assertNull(viewModel.uiState.value.rejection)
        assertEquals(15.0, viewModel.uiState.value.plan.waypoints[0].relativeAltitudeM, 0.0)
    }

    @Test
    fun `reorder that makes neighbours too close is allowed but reported`() = runTest {
        // a와 aNear는 0.5m. 사이에 b가 있어서 처음엔 유효
        val aNear = a.copy(latitudeDeg = a.latitudeDeg + 0.0000045)
        val (viewModel, repository) = setUp(MissionPlan(listOf(a, b, aNear)))
        assertTrue(viewModel.uiState.value.isValid)

        viewModel.onMoveWaypoint(from = 2, to = 1)

        assertEquals(listOf(a, aNear, b), repository.plan.value.waypoints)
        assertNull(viewModel.uiState.value.rejection)
        val error = viewModel.uiState.value.errors.single()
        assertEquals(1, (error as MissionValidationError.TooCloseToPrevious).index)
    }

    @Test
    fun `plan survives a new ViewModel`() = runTest {
        // 화면을 나갔다 들어오면 ViewModel은 새로 만들어지지만 Repository는 Singleton이라 그대로
        val repository = FakeMissionPlanRepository()
        MissionPlannerViewModel(repository, FakeDroneRepository()).onAddWaypoint(47.3977, 8.5455)

        val next = MissionPlannerViewModel(repository, FakeDroneRepository())
        subscribe(next)

        assertEquals(1, next.uiState.value.plan.size)
    }

    // --- 기체 위치 (지도 마커) ---

    private fun TestScope.setUpWithDrone(drone: FakeDroneRepository): MissionPlannerViewModel =
        MissionPlannerViewModel(FakeMissionPlanRepository(), drone).also { subscribe(it) }

    @Test
    fun `no vehicle marker before any position`() = runTest {
        val viewModel = setUpWithDrone(FakeDroneRepository(DroneState(connection = ConnectionStatus.CONNECTED)))
        assertNull(viewModel.uiState.value.vehicle)
    }

    @Test
    fun `connected with trusted position is current`() = runTest {
        val drone = FakeDroneRepository(
            DroneState(connection = ConnectionStatus.CONNECTED, latitude = 47.3977, longitude = 8.5455),
        )
        val viewModel = setUpWithDrone(drone)
        assertEquals(VehicleMarker(47.3977, 8.5455, current = true), viewModel.uiState.value.vehicle)
    }

    @Test
    fun `link lost keeps last position but not current`() = runTest {
        val drone = FakeDroneRepository(
            DroneState(connection = ConnectionStatus.CONNECTED, latitude = 47.3977, longitude = 8.5455),
        )
        val viewModel = setUpWithDrone(drone)

        drone.droneState.value = drone.droneState.value.copy(connection = ConnectionStatus.LOST)

        assertEquals(VehicleMarker(47.3977, 8.5455, current = false), viewModel.uiState.value.vehicle)
    }

    @Test
    fun `gps lost while connected is not current`() = runTest {
        val drone = FakeDroneRepository(
            DroneState(
                connection = ConnectionStatus.CONNECTED,
                latitude = 47.3977,
                longitude = 8.5455,
                gpsAvailable = false,
            ),
        )
        assertFalse(setUpWithDrone(drone).uiState.value.vehicle!!.current)
    }
}
