package io.github.hoonjaegui.px4gcs.feature.mission.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hoonjaegui.px4gcs.core.domain.drone.ObserveVehiclePositionUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.AddWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.ClearMissionPlanUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.MoveWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.ObserveMissionPlanUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.RemoveWaypointUseCase
import io.github.hoonjaegui.px4gcs.core.domain.mission.UpdateWaypointAltitudeUseCase
import io.github.hoonjaegui.px4gcs.core.model.drone.VehiclePosition
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditRejection
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionEditResult
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionPlan
import io.github.hoonjaegui.px4gcs.core.model.mission.MissionValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MissionPlannerUiState(
    val plan: MissionPlan = MissionPlan(),
    /** 현재 plan 전체의 문제. 비어 있으면 유효. */
    val errors: List<MissionValidationError> = listOf(MissionValidationError.Empty),
    /** 마지막 편집이 거부된 이유. 다음 편집이 성공하면 사라진다. */
    val rejection: EditRejection? = null,
    /** 지도에 표시할 기체 위치. 한 번도 신뢰할 수 있는 위치를 못 받았으면 null. */
    val vehicle: VehiclePosition? = null,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/** 편집 요청을 plan에 반영하지 않은 이유. */
sealed interface EditRejection {
    /** 숫자로 읽을 수 없는 입력 (예: 고도 칸에 "abc"). 화면 입력 문제라 ViewModel이 판정한다. */
    data class NotANumber(val input: String) : EditRejection

    /** domain 규칙에 따른 거부. */
    data class Rejected(val reason: MissionEditRejection) : EditRejection
}

/**
 * 편집 규칙은 전부 UseCase에 있다. ViewModel은 입력(문자열) 해석과 화면 상태(거부 사유 표시)만 맡는다.
 */
@HiltViewModel
class MissionPlannerViewModel @Inject constructor(
    observeMissionPlan: ObserveMissionPlanUseCase,
    observeVehiclePosition: ObserveVehiclePositionUseCase,
    private val addWaypoint: AddWaypointUseCase,
    private val removeWaypoint: RemoveWaypointUseCase,
    private val moveWaypoint: MoveWaypointUseCase,
    private val updateWaypointAltitude: UpdateWaypointAltitudeUseCase,
    private val clearMissionPlan: ClearMissionPlanUseCase,
) : ViewModel() {

    private val rejection = MutableStateFlow<EditRejection?>(null)

    val uiState: StateFlow<MissionPlannerUiState> =
        combine(observeMissionPlan(), rejection, observeVehiclePosition()) { validated, rejection, vehicle ->
            MissionPlannerUiState(validated.plan, validated.errors, rejection, vehicle)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionPlannerUiState())

    /** 지도 탭. */
    fun onAddWaypoint(latitudeDeg: Double, longitudeDeg: Double) = show(addWaypoint(latitudeDeg, longitudeDeg))

    fun onRemoveWaypoint(index: Int) {
        removeWaypoint(index)
        rejection.value = null
    }

    fun onMoveWaypoint(from: Int, to: Int) {
        moveWaypoint(from, to)
        rejection.value = null
    }

    /** 고도 입력 확정. [input]은 사용자가 친 문자열 그대로. */
    fun onAltitudeSubmitted(index: Int, input: String) {
        val altitude = input.trim().toDoubleOrNull()
        if (altitude == null) {
            rejection.value = EditRejection.NotANumber(input)
            return
        }
        show(updateWaypointAltitude(index, altitude))
    }

    fun onClear() {
        clearMissionPlan()
        rejection.value = null
    }

    private fun show(result: MissionEditResult) {
        rejection.value = when (result) {
            MissionEditResult.Accepted -> null
            is MissionEditResult.Rejected -> EditRejection.Rejected(result.reason)
        }
    }
}
