package com.example.android_px4_gcs.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_px4_gcs.core.model.DroneState

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.droneState.collectAsStateWithLifecycle()
    DashboardScreen(state, modifier)
}

@Composable
fun DashboardScreen(state: DroneState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PX4 GCS", style = MaterialTheme.typography.headlineMedium)
        TelemetryItem(
            label = "Connection",
            value = if (state.connected) "CONNECTED" else "DISCONNECTED",
            valueColor = if (state.connected) Color(0xFF2E7D32) else Color(0xFFC62828),
        )
        TelemetryItem("Battery", state.batteryPercent?.let { "$it%" } ?: NO_DATA)
        TelemetryItem(
            label = "GPS",
            value = if (state.gpsAvailable) "3D FIX" else "NO FIX",
            valueColor = if (state.gpsAvailable) Color.Unspecified else Color(0xFFC62828),
        )
        TelemetryItem("Altitude", state.altitude?.let { "%.1fm".format(it) } ?: NO_DATA)
        TelemetryItem("Heading", state.heading?.let { "%.0f°".format(it) } ?: NO_DATA)
        TelemetryItem("Latitude", state.latitude?.let { "%.7f".format(it) } ?: NO_DATA)
        TelemetryItem("Longitude", state.longitude?.let { "%.7f".format(it) } ?: NO_DATA)
    }
}

private const val NO_DATA = "--"

@Composable
private fun TelemetryItem(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    DashboardScreen(DroneState(connected = true))
}
