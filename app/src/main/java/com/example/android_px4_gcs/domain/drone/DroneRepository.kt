package com.example.android_px4_gcs.domain.drone

import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.core.model.LinkLossAction
import kotlinx.coroutines.flow.StateFlow

interface DroneRepository {
    val droneState: StateFlow<DroneState>

    suspend fun arm(): CommandResult

    suspend fun takeoff(): CommandResult

    suspend fun land(): CommandResult

    suspend fun returnToLaunch(): CommandResult

    /** 통신 두절 시 기체 행동(NAV_DLL_ACT)을 기체에 쓰고, 다시 읽어 실제 반영 여부를 확인한다. */
    suspend fun setLinkLossAction(action: LinkLossAction): CommandResult
}
