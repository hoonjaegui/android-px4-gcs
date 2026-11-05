package com.example.android_px4_gcs.data.drone

import android.os.SystemClock
import android.util.Log
import com.example.android_px4_gcs.core.model.CommandFailureReason
import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.ConnectionStatus
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.core.model.LinkLossAction
import com.example.android_px4_gcs.core.model.LinkLossPolicy
import com.example.android_px4_gcs.core.model.MessageSeverity
import com.example.android_px4_gcs.core.model.VehicleMessage
import com.example.android_px4_gcs.core.model.TelemetryField
import com.example.android_px4_gcs.core.model.Timestamp
import com.example.android_px4_gcs.di.ApplicationScope
import com.example.android_px4_gcs.domain.drone.DroneRepository
import io.mavsdk.action.Action
import io.mavsdk.param.Param
import io.mavsdk.telemetry.Telemetry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
@OptIn(ExperimentalCoroutinesApi::class)
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

    /** Telemetry 스트림이 아닌, Repository가 직접 만든 상태 변경 (예: 파라미터를 쓴 뒤 다시 읽은 값). */
    private val localUpdates = MutableSharedFlow<Update>(extraBufferCapacity = 16)

    override val droneState: StateFlow<DroneState> =
        merge(
            localUpdates,
            dataSource.isConnected.map { connected -> Update { it.withConnection(connected) } },
            dataSource.batteryPercent.toTelemetryUpdate(TelemetryField.BATTERY) { copy(batteryPercent = it.roundToInt()) },
            dataSource.gpsInfo.toTelemetryUpdate(TelemetryField.GPS) { copy(gpsAvailable = it.fixType.hasPositionFix()) },
            dataSource.position.map { p ->
                // 고도는 홈(이륙 지점) 기준. absoluteAltitudeM은 해발(AMSL) 고도.
                Update { it.withPosition(p.latitudeDeg, p.longitudeDeg, p.relativeAltitudeM.toDouble(), now()) }
            },
            dataSource.globalPositionOk.toStateUpdate { copy(globalPositionOk = it) },
            dataSource.statusText.map { msg ->
                Update { it.withMessage(VehicleMessage(msg.type.toSeverity(), msg.text, now())) }
            },
            // 연결될 때마다 다시 읽는다 (재연결 사이에 설정이 바뀌었을 수 있다).
            dataSource.isConnected.distinctUntilChanged().filter { it }
                .mapLatest { readLinkLossPolicy() }
                .filterNotNull()
                .toStateUpdate { copy(linkLossPolicy = it) },
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

    override suspend fun setLinkLossAction(action: LinkLossAction): CommandResult {
        val value = action.navDllAct
            ?: return CommandResult.Failure(CommandFailureReason.UNKNOWN, "cannot write $action")
        if (!droneState.value.isConnected) {
            return CommandResult.Failure(CommandFailureReason.NOT_CONNECTED, "not connected")
        }

        val writeFailure: CommandResult.Failure? = try {
            dataSource.setParamInt("NAV_DLL_ACT", value)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Param.ParamException) {
            Log.w(TAG, "set NAV_DLL_ACT=$value failed: ${e.code} ${e.description}")
            CommandResult.Failure(e.code.toFailureReason(), "${e.code}: ${e.description}")
        } catch (e: Exception) {
            CommandResult.Failure(CommandFailureReason.UNKNOWN, e.message ?: e.javaClass.simpleName)
        }

        // 쓰기 성공/실패와 무관하게 기체에서 다시 읽는다. 쓰기가 TIMEOUT이어도 실제로는 반영됐을 수 있다.
        val readBack = readLinkLossPolicy()
        if (readBack != null) localUpdates.emit(Update { it.copy(linkLossPolicy = readBack) })

        return when {
            readBack?.action == action -> CommandResult.Success
            writeFailure != null -> writeFailure
            readBack == null -> CommandResult.Failure(CommandFailureReason.TIMEOUT, "written, but could not read back to verify")
            else -> CommandResult.Failure(CommandFailureReason.REJECTED, "vehicle reports ${readBack.action}")
        }
    }

    private fun Param.ParamResult.Result.toFailureReason(): CommandFailureReason = when (this) {
        Param.ParamResult.Result.NO_SYSTEM,
        Param.ParamResult.Result.CONNECTION_ERROR -> CommandFailureReason.NOT_CONNECTED
        Param.ParamResult.Result.TIMEOUT -> CommandFailureReason.TIMEOUT
        Param.ParamResult.Result.UNKNOWN -> CommandFailureReason.UNKNOWN
        else -> CommandFailureReason.REJECTED // 범위 초과, 읽기 전용, 타입 불일치 등
    }

    private suspend fun readLinkLossPolicy(): LinkLossPolicy? = try {
        LinkLossPolicy(
            action = LinkLossAction.fromNavDllAct(dataSource.getParamInt("NAV_DLL_ACT")),
            timeoutSec = dataSource.getParamInt("COM_DL_LOSS_T"),
        ).also { Log.i(TAG, "link loss policy: $it") }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "failed to read link loss params", e)
        null
    }

    /** MAVSDK 예외를 도메인 결과로 변환하고, 실패면 PX4가 보낸 사유 메시지를 붙인다. */
    private suspend fun runCommand(name: String, block: suspend () -> Unit): CommandResult {
        val sentAt = SystemClock.elapsedRealtime()
        val result = executeCommand(name, block)
        if (result !is CommandResult.Failure || result.reason == CommandFailureReason.NOT_CONNECTED) return result
        return result.copy(vehicleMessage = awaitVehicleMessageSince(sentAt))
    }

    /**
     * 명령을 보낸 뒤 PX4가 보낸 WARNING 이상 메시지. STATUSTEXT와 COMMAND_ACK는 별개 메시지라
     * 순서가 보장되지 않으므로, 이미 와 있으면 바로 쓰고 아니면 잠깐 기다린다.
     */
    private suspend fun awaitVehicleMessageSince(sentAtElapsedMs: Long): String? =
        withTimeoutOrNull(VEHICLE_MESSAGE_WAIT_MS) {
            droneState.mapNotNull { state ->
                state.messages.firstOrNull { it.at.elapsedMs >= sentAtElapsedMs && it.severity != MessageSeverity.INFO }?.text
            }.first()
        }

    private suspend fun executeCommand(name: String, block: suspend () -> Unit): CommandResult =
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

    private fun Telemetry.StatusTextType.toSeverity(): MessageSeverity = when (this) {
        Telemetry.StatusTextType.DEBUG,
        Telemetry.StatusTextType.INFO,
        Telemetry.StatusTextType.NOTICE -> MessageSeverity.INFO
        Telemetry.StatusTextType.WARNING -> MessageSeverity.WARNING
        else -> MessageSeverity.ERROR
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
        const val VEHICLE_MESSAGE_WAIT_MS = 500L
    }
}
