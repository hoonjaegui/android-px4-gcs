package com.example.android_px4_gcs.feature.dashboard

import androidx.lifecycle.ViewModel
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.domain.drone.DroneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DashboardViewModel @Inject constructor(repository: DroneRepository) : ViewModel() {
    val droneState: StateFlow<DroneState> = repository.droneState
}
