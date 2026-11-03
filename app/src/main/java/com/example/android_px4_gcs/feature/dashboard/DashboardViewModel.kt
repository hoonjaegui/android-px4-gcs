package com.example.android_px4_gcs.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.android_px4_gcs.GcsApplication
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.domain.drone.DroneRepository
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel(repository: DroneRepository) : ViewModel() {
    val droneState: StateFlow<DroneState> = repository.droneState

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GcsApplication
                DashboardViewModel(app.droneRepository)
            }
        }
    }
}
