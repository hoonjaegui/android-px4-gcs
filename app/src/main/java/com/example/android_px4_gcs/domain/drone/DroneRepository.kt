package com.example.android_px4_gcs.domain.drone

import com.example.android_px4_gcs.core.model.CommandResult
import com.example.android_px4_gcs.core.model.DroneState
import kotlinx.coroutines.flow.StateFlow

interface DroneRepository {
    val droneState: StateFlow<DroneState>

    suspend fun arm(): CommandResult
}
