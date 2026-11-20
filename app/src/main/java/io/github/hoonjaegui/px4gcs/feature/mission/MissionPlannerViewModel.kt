package io.github.hoonjaegui.px4gcs.feature.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.domain.mission.MissionValidator
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.model.mission.WaypointProblem
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.domain.drone.DroneRepository
import io.github.hoonjaegui.px4gcs.domain.mission.MissionPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MissionPlannerUiState(
    val plan: MissionPlan = MissionPlan(),
    /** 현재 plan 전체의 문제. 비어 있으면 유효. */
    val errors: List<MissionValidationError> = listOf(MissionValidationError.Empty),
    /** 마지막 편집이 거부된 이유. 다음 편집이 성공하면 사라진다. */
    val rejection: EditRejection? = null,
    /** 지도에 표시할 기체 위치. 한 번도 신뢰할 수 있는 위치를 못 받았으면 null. */
    val vehicle: VehicleMarker? = null,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * @property current false = 마지막으로 알던 위치(연결 끊김 또는 GPS/EKF 불신). 지도에서 현재 위치처럼 보이면 안 된다.
 */
data class VehicleMarker(val latitudeDeg: Double, val longitudeDeg: Double, val current: Boolean)

internal fun DroneState.toVehicleMarker(): VehicleMarker? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return VehicleMarker(lat, lon, current = isConnected && positionTrusted)
}

/** 편집 요청을 plan에 반영하지 않은 이유. */
sealed interface EditRejection {
    /** 숫자로 읽을 수 없는 입력 (예: 고도 칸에 "abc"). */
    data class NotANumber(val input: String) : EditRejection

    data class InvalidWaypoint(val problems: List<WaypointProblem>) : EditRejection

    data class TooCloseToPrevious(val distanceM: Double) : EditRejection
}

/**
 * Mission 편집 규칙:
 * - Waypoint를 **새로 만들거나 고도를 바꿀 때** 그 Waypoint가 잘못됐으면 거부하고 plan을 그대로 둔다.
 * - 삭제/순서 변경은 거부하지 않는다. 그 결과 생긴 문제(예: 순서를 바꿨더니 붙은 두 점이 1m 이내)는
 *   [MissionPlannerUiState.errors]로 보여 준다. 운용자가 고치는 중간 상태를 막으면 편집이 불가능해지므로.
 */
@HiltViewModel
class MissionPlannerViewModel @Inject constructor(
    private val repository: MissionPlanRepository,
    droneRepository: DroneRepository,
) : ViewModel() {

    private val rejection = MutableStateFlow<EditRejection?>(null)

    private val vehicle = droneRepository.droneState.map { it.toVehicleMarker() }.distinctUntilChanged()

    val uiState: StateFlow<MissionPlannerUiState> =
        combine(repository.plan, rejection, vehicle) { plan, rejection, vehicle ->
            MissionPlannerUiState(plan, MissionValidator.validate(plan), rejection, vehicle)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionPlannerUiState())

    private val plan: MissionPlan get() = repository.plan.value

    /** 지도 탭. 고도는 마지막 Waypoint 것을 이어받는다(없으면 [DEFAULT_ALTITUDE_M]). QGC와 같은 방식. */
    fun onAddWaypoint(latitudeDeg: Double, longitudeDeg: Double) {
        val altitude = plan.waypoints.lastOrNull()?.relativeAltitudeM ?: DEFAULT_ALTITUDE_M
        val waypoint = Waypoint(latitudeDeg, longitudeDeg, altitude)

        val problems = MissionValidator.validate(waypoint)
        if (problems.isNotEmpty()) return reject(EditRejection.InvalidWaypoint(problems))

        val candidate = plan.add(waypoint)
        val tooClose = MissionValidator.validate(candidate)
            .filterIsInstance<MissionValidationError.TooCloseToPrevious>()
            .firstOrNull { it.index == candidate.size - 1 }
        if (tooClose != null) return reject(EditRejection.TooCloseToPrevious(tooClose.distanceM))

        accept(candidate)
    }

    fun onRemoveWaypoint(index: Int) = accept(plan.remove(index))

    fun onMoveWaypoint(from: Int, to: Int) = accept(plan.move(from, to))

    /** 고도 입력 확정. [input]은 사용자가 친 문자열 그대로. */
    fun onAltitudeSubmitted(index: Int, input: String) {
        val altitude = input.trim().toDoubleOrNull() ?: return reject(EditRejection.NotANumber(input))
        val candidate = plan.updateAltitude(index, altitude)
        val problems = MissionValidator.validate(candidate.waypoints[index])
        if (problems.isNotEmpty()) return reject(EditRejection.InvalidWaypoint(problems))
        accept(candidate)
    }

    fun onClear() = accept(MissionPlan())

    private fun accept(newPlan: MissionPlan) {
        rejection.value = null
        repository.save(newPlan)
    }

    private fun reject(reason: EditRejection) {
        rejection.value = reason
    }

    companion object {
        const val DEFAULT_ALTITUDE_M = 10.0
    }
}
