package io.github.hoonjaegui.px4gcs.core.data.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.CommandFailureReason
import io.github.hoonjaegui.px4gcs.core.model.drone.FlightMode
import io.github.hoonjaegui.px4gcs.core.model.drone.MessageSeverity
import io.mavsdk.action.Action
import io.mavsdk.param.Param
import io.mavsdk.telemetry.Telemetry

/*
 * MAVSDK 타입 → 도메인 타입 변환. MAVSDK 타입은 이 모듈 밖으로 나가지 않는다.
 */

internal fun Param.ParamResult.Result.toFailureReason(): CommandFailureReason = when (this) {
    Param.ParamResult.Result.NO_SYSTEM,
    Param.ParamResult.Result.CONNECTION_ERROR -> CommandFailureReason.NOT_CONNECTED
    Param.ParamResult.Result.TIMEOUT -> CommandFailureReason.TIMEOUT
    Param.ParamResult.Result.UNKNOWN -> CommandFailureReason.UNKNOWN
    else -> CommandFailureReason.REJECTED // 범위 초과, 읽기 전용, 타입 불일치 등
}

internal fun Action.ActionResult.Result.toFailureReason(): CommandFailureReason = when (this) {
    Action.ActionResult.Result.NO_SYSTEM,
    Action.ActionResult.Result.CONNECTION_ERROR -> CommandFailureReason.NOT_CONNECTED
    Action.ActionResult.Result.COMMAND_DENIED,
    Action.ActionResult.Result.COMMAND_DENIED_LANDED_STATE_UNKNOWN,
    Action.ActionResult.Result.COMMAND_DENIED_NOT_LANDED,
    Action.ActionResult.Result.UNSUPPORTED,
    Action.ActionResult.Result.FAILED -> CommandFailureReason.REJECTED
    Action.ActionResult.Result.BUSY -> CommandFailureReason.BUSY
    Action.ActionResult.Result.TIMEOUT -> CommandFailureReason.TIMEOUT
    else -> CommandFailureReason.UNKNOWN
}

internal fun Telemetry.StatusTextType.toSeverity(): MessageSeverity = when (this) {
    Telemetry.StatusTextType.DEBUG,
    Telemetry.StatusTextType.INFO,
    Telemetry.StatusTextType.NOTICE -> MessageSeverity.INFO
    Telemetry.StatusTextType.WARNING -> MessageSeverity.WARNING
    else -> MessageSeverity.ERROR
}

/** 3D fix 이상이어야 위치로 쓸 수 있다. */
internal fun Telemetry.FixType.hasPositionFix(): Boolean =
    this >= Telemetry.FixType.FIX_3D

/** MAVSDK 비행 모드 → 도메인 [FlightMode]. 화면 문구는 UI가 정한다. */
internal fun Telemetry.FlightMode.toFlightMode(): FlightMode = when (this) {
    Telemetry.FlightMode.READY -> FlightMode.READY
    Telemetry.FlightMode.TAKEOFF -> FlightMode.TAKEOFF
    Telemetry.FlightMode.HOLD -> FlightMode.HOLD
    Telemetry.FlightMode.MISSION -> FlightMode.MISSION
    Telemetry.FlightMode.RETURN_TO_LAUNCH -> FlightMode.RETURN
    Telemetry.FlightMode.LAND -> FlightMode.LAND
    Telemetry.FlightMode.OFFBOARD -> FlightMode.OFFBOARD
    Telemetry.FlightMode.FOLLOW_ME -> FlightMode.FOLLOW_ME
    Telemetry.FlightMode.MANUAL -> FlightMode.MANUAL
    Telemetry.FlightMode.ALTCTL -> FlightMode.ALTITUDE
    Telemetry.FlightMode.POSCTL -> FlightMode.POSITION
    Telemetry.FlightMode.ACRO -> FlightMode.ACRO
    Telemetry.FlightMode.STABILIZED -> FlightMode.STABILIZED
    Telemetry.FlightMode.RATTITUDE -> FlightMode.RATTITUDE
    Telemetry.FlightMode.UNKNOWN -> FlightMode.UNKNOWN
}
