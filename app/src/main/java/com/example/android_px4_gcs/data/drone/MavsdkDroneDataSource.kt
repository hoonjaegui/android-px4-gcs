package com.example.android_px4_gcs.data.drone

import android.util.Log
import com.example.android_px4_gcs.di.ApplicationScope
import com.example.android_px4_gcs.di.MavsdkConnectionUrl
import io.mavsdk.System
import io.mavsdk.mavsdkserver.MavsdkServer
import io.mavsdk.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MAVSDK를 직접 다루는 유일한 클래스.
 *
 * 구조: 앱 프로세스 안에서 mavsdk_server(C++ 네이티브)가 MAVLink UDP를 받고,
 * MAVSDK Java(io.mavsdk.System)는 로컬 gRPC로 mavsdk_server와 통신한다.
 *
 *   PX4 SITL --MAVLink/UDP--> mavsdk_server --gRPC--> io.mavsdk.System --Rx--> Flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MavsdkDroneDataSource @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    @MavsdkConnectionUrl private val systemAddress: String,
) {
    private val server = MavsdkServer()
    private val _system = MutableStateFlow<System?>(null)

    /** null = 아직 Vehicle을 발견하지 못함 (Discovery 중). */
    val system: StateFlow<System?> = _system.asStateFlow()

    fun start() {
        scope.launch(Dispatchers.IO) {
            Log.i(TAG, "mavsdk_server starting, listening on $systemAddress (vehicle discovery...)")
            // run()은 첫 Vehicle의 HEARTBEAT를 받을 때까지 블로킹된다 = Vehicle Discovery.
            val grpcPort = server.run(systemAddress)
            Log.i(TAG, "vehicle discovered, mavsdk_server gRPC port=$grpcPort")
            _system.value = System(GRPC_HOST, grpcPort)
        }
    }

    /** HEARTBEAT 기반 연결 상태. Discovery 전에는 false. */
    val isConnected: Flow<Boolean> = system.flatMapLatest { sys ->
        if (sys == null) flowOf(false)
        else sys.core.connectionState.asFlow().map { it.isConnected }
    }.safe("connectionState")

    /** 배터리 잔량 (0~100%). */
    val batteryPercent: Flow<Float> =
        telemetry("battery") { it.telemetry.battery.asFlow().map { b -> b.remainingPercent } }

    /** GPS 수신 상태 (fix type, 위성 수). */
    val gpsInfo: Flow<Telemetry.GpsInfo> =
        telemetry("gpsInfo") { it.telemetry.gpsInfo.asFlow() }

    /** 추정 위치 (위도/경도/고도). PX4 EKF가 GPS 등을 융합한 결과. */
    val position: Flow<Telemetry.Position> =
        telemetry("position") { it.telemetry.position.asFlow() }

    /** 기수 방향 (0~360°, 북=0, 시계방향). */
    val headingDeg: Flow<Double> =
        telemetry("heading") { it.telemetry.heading.asFlow().map { h -> h.headingDeg } }

    /** ARM 여부 (HEARTBEAT.base_mode의 SAFETY_ARMED 비트). */
    val armed: Flow<Boolean> =
        telemetry("armed") { it.telemetry.armed.asFlow() }

    /** 비행 모드 (HEARTBEAT.custom_mode를 MAVSDK가 해석한 값). */
    val flightMode: Flow<Telemetry.FlightMode> =
        telemetry("flightMode") { it.telemetry.flightMode.asFlow() }

    /**
     * ARM 명령 (MAV_CMD_COMPONENT_ARM_DISARM). PX4의 COMMAND_ACK를 받을 때까지 suspend.
     * 거부되면 [io.mavsdk.action.Action.ActionException]을 던진다.
     */
    suspend fun arm() = requireSystem().action.arm().await()

    private fun requireSystem(): System =
        system.value ?: throw VehicleNotDiscoveredException()

    class VehicleNotDiscoveredException : IllegalStateException("vehicle not discovered yet")

    /** system 발견 후에만 구독하는 Telemetry 스트림 헬퍼. */
    private fun <T> telemetry(name: String, select: (System) -> Flow<T>): Flow<T> =
        system.flatMapLatest { sys -> if (sys == null) emptyFlow() else select(sys) }.safe(name)

    private fun <T> Flow<T>.safe(name: String): Flow<T> =
        catch { e -> Log.w(TAG, "$name stream failed", e) }

    companion object {
        private const val TAG = "MavsdkDataSource"
        private const val GRPC_HOST = "127.0.0.1"
    }
}
