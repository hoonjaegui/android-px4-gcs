package io.github.hoonjaegui.px4gcs.core.model.mission

/** plan + 그 plan 전체의 문제 목록. [errors]가 비어 있으면 유효. */
data class ValidatedMissionPlan(
    val plan: MissionPlan = MissionPlan(),
    val errors: List<MissionValidationError> = listOf(MissionValidationError.Empty),
) {
    val isValid: Boolean get() = errors.isEmpty()
}
