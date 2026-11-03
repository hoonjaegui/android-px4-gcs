package com.example.android_px4_gcs

import android.app.Application
import com.example.android_px4_gcs.data.drone.DefaultDroneRepository
import com.example.android_px4_gcs.data.drone.MavsdkDroneDataSource
import com.example.android_px4_gcs.domain.drone.DroneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * 수동 DI 컨테이너. Drone 연결은 화면 회전/재생성과 무관하게 앱 프로세스 동안 유지한다.
 */
class GcsApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob())

    val droneRepository: DroneRepository by lazy {
        DefaultDroneRepository(MavsdkDroneDataSource(appScope), appScope)
    }
}
