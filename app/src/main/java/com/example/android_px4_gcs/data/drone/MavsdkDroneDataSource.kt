package com.example.android_px4_gcs.data.drone

import android.util.Log
import io.mavsdk.System
import io.mavsdk.mavsdkserver.MavsdkServer
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

/**
 * MAVSDK를 직접 다루는 유일한 클래스.
 *
 * 구조: 앱 프로세스 안에서 mavsdk_server(C++ 네이티브)가 MAVLink UDP를 받고,
 * MAVSDK Java(io.mavsdk.System)는 로컬 gRPC로 mavsdk_server와 통신한다.
 *
 *   PX4 SITL --MAVLink/UDP--> mavsdk_server --gRPC--> io.mavsdk.System --Rx--> Flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MavsdkDroneDataSource(
    private val scope: CoroutineScope,
    private val systemAddress: String = DEFAULT_SYSTEM_ADDRESS,
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

    /** system 발견 후에만 구독하는 Telemetry 스트림 헬퍼. */
    private fun <T> telemetry(name: String, select: (System) -> Flow<T>): Flow<T> =
        system.flatMapLatest { sys -> if (sys == null) emptyFlow() else select(sys) }.safe(name)

    private fun <T> Flow<T>.safe(name: String): Flow<T> =
        catch { e -> Log.w(TAG, "$name stream failed", e) }

    companion object {
        private const val TAG = "MavsdkDataSource"
        private const val GRPC_HOST = "127.0.0.1"

        /** PX4 SITL의 Offboard/API 링크(remote port 14540)를 받는 UDP 서버. */
        const val DEFAULT_SYSTEM_ADDRESS = "udpin://0.0.0.0:14540"
    }
}
