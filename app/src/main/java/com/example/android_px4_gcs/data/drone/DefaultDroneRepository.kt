package com.example.android_px4_gcs.data.drone

import android.os.SystemClock
import android.util.Log
import com.example.android_px4_gcs.core.model.CommandFailureReason
import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.ConnectionStatus
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.core.model.TelemetryField
import com.example.android_px4_gcs.core.model.Timestamp
import com.example.android_px4_gcs.di.ApplicationScope
import com.example.android_px4_gcs.domain.drone.DroneRepository
import io.mavsdk.action.Action
import io.mavsdk.telemetry.Telemetry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * MAVSDK Telemetry 스트림들을 도메인 모델 DroneState 하나로 합친다.
 *
 * 각 스트림은 서로 다른 주기로 들어오므로, 스트림마다 "DroneState를 어떻게 갱신할지"(Update)로
 * 변환한 뒤 merge → scan으로 누적한다. 스트림이 늘어도 이 구조는 그대로다.
 */
@Singleton
class DefaultDroneRepository @Inject constructor(
    private val dataSource: MavsdkDroneDataSource,
    @ApplicationScope scope: CoroutineScope,
) : DroneRepository {

    private fun interface Update : (DroneState) -> DroneState

    /** Telemetry 값 갱신 + 해당 필드의 수신 시각 기록. */
    private fun <T> Flow<T>.toTelemetryUpdate(
        field: TelemetryField,
        apply: DroneState.(T) -> DroneState,
    ): Flow<Update> = map { value ->
        Update { it.apply(value).copy(receivedAt = it.receivedAt + (field to now())) }
    }

    /** 상태 값(armed, flight mode, health). 신선도는 connection이 대표하므로 시각은 기록하지 않는다. */
    private fun <T> Flow<T>.toStateUpdate(apply: DroneState.(T) -> DroneState): Flow<Update> =
        map { value -> Update { it.apply(value) } }

    private fun now() = Timestamp(elapsedMs = SystemClock.elapsedRealtime(), epochMs = System.currentTimeMillis())

    override val droneState: StateFlow<DroneState> =
        merge(
            dataSource.isConnected.map { connected -> Update { it.withConnection(connected) } },
            dataSource.batteryPercent.toTelemetryUpdate(TelemetryField.BATTERY) { copy(batteryPercent = it.roundToInt()) },
            dataSource.gpsInfo.toTelemetryUpdate(TelemetryField.GPS) { copy(gpsAvailable = it.fixType.hasPositionFix()) },
            dataSource.position.map { p ->
                // 고도는 홈(이륙 지점) 기준. absoluteAltitudeM은 해발(AMSL) 고도.
                Update { it.withPosition(p.latitudeDeg, p.longitudeDeg, p.relativeAltitudeM.toDouble(), now()) }
            },
            dataSource.globalPositionOk.toStateUpdate { copy(globalPositionOk = it) },
            dataSource.headingDeg.toTelemetryUpdate(TelemetryField.HEADING) { copy(heading = it) },
            dataSource.armed.toStateUpdate { copy(armed = it) },
            dataSource.flightMode.toStateUpdate { copy(flightMode = it.toDisplayName()) },
        )
            .scan(DroneState()) { state, update -> update(state) }
            .stateIn(scope, SharingStarted.Eagerly, DroneState())

    /** 한 번이라도 연결된 뒤 끊기면 LOST(+끊긴 시각). 처음부터 못 찾은 건 SEARCHING으로 남는다. */
    private fun DroneState.withConnection(connected: Boolean): DroneState = when {
        connected -> copy(connection = ConnectionStatus.CONNECTED, connectionLostAt = null)
        connection == ConnectionStatus.CONNECTED -> copy(connection = ConnectionStatus.LOST, connectionLostAt = now())
        else -> this // SEARCHING은 SEARCHING, LOST는 처음 끊긴 시각 유지
    }

    override suspend fun arm(): CommandResult = runCommand("arm") { dataSource.arm() }

    override suspend fun takeoff(): CommandResult = runCommand("takeoff") { dataSource.takeoff() }

    override suspend fun land(): CommandResult = runCommand("land") { dataSource.land() }

    override suspend fun returnToLaunch(): CommandResult = runCommand("rtl") { dataSource.returnToLaunch() }

    /** MAVSDK 예외를 도메인 결과로 변환. ViewModel은 MAVSDK 타입을 모른다. */
    private suspend fun runCommand(name: String, block: suspend () -> Unit): CommandResult =
        try {
            block()
            Log.i(TAG, "$name accepted")
            CommandResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Action.ActionException) {
            Log.w(TAG, "$name failed: ${e.code} ${e.description}")
            CommandResult.Failure(e.code.toFailureReason(), "${e.code}: ${e.description}")
        } catch (e: MavsdkDroneDataSource.VehicleNotDiscoveredException) {
            CommandResult.Failure(CommandFailureReason.NOT_CONNECTED, "vehicle not discovered")
        } catch (e: Exception) {
            Log.w(TAG, "$name failed", e)
            CommandResult.Failure(CommandFailureReason.UNKNOWN, e.message ?: e.javaClass.simpleName)
        }

    private fun Action.ActionResult.Result.toFailureReason(): CommandFailureReason = when (this) {
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

    /** 3D fix 이상이어야 위치로 쓸 수 있다. */
    private fun Telemetry.FixType.hasPositionFix(): Boolean =
        this >= Telemetry.FixType.FIX_3D

    /** MAVSDK enum을 UI 표시용 문자열로. (UI가 MAVSDK 타입을 몰라도 되게) */
    private fun Telemetry.FlightMode.toDisplayName(): String = when (this) {
        Telemetry.FlightMode.POSCTL -> "POSITION"
        Telemetry.FlightMode.ALTCTL -> "ALTITUDE"
        Telemetry.FlightMode.RETURN_TO_LAUNCH -> "RETURN"
        Telemetry.FlightMode.FOLLOW_ME -> "FOLLOW ME"
        else -> name
    }

    init {
        dataSource.start()
    }

    private companion object {
        const val TAG = "DroneRepository"
    }
}
