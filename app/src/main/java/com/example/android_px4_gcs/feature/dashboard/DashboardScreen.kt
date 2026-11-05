package com.example.android_px4_gcs.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_px4_gcs.core.model.CommandState
import com.example.android_px4_gcs.core.model.DroneCommand
import com.example.android_px4_gcs.core.model.DroneState

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.droneState.collectAsStateWithLifecycle()
    val commandStates by viewModel.commandStates.collectAsStateWithLifecycle()
    DashboardScreen(state, commandStates, viewModel::onCommand, modifier)
}

@Composable
fun DashboardScreen(
    state: DroneState,
    commandStates: Map<DroneCommand, CommandState>,
    onCommand: (DroneCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("PX4 GCS", style = MaterialTheme.typography.headlineMedium)
        TelemetryItem(
            label = "Connection",
            value = if (state.connected) "CONNECTED" else "DISCONNECTED",
            valueColor = if (state.connected) Color(0xFF2E7D32) else Color(0xFFC62828),
        )
        TelemetryItem("Flight Mode", state.flightMode ?: NO_DATA)
        TelemetryItem(
            label = "Armed",
            value = if (state.armed) "TRUE" else "FALSE",
            valueColor = if (state.armed) Color(0xFFE65100) else Color.Unspecified,
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

        HorizontalDivider()
        DroneCommand.entries.forEach { command ->
            CommandRow(command, commandStates[command] ?: CommandState.Idle, onCommand)
        }
    }
}

@Composable
private fun CommandRow(command: DroneCommand, state: CommandState, onCommand: (DroneCommand) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { onCommand(command) }, enabled = state != CommandState.Running) {
            Text(command.name)
        }
        when (state) {
            CommandState.Idle -> Unit
            CommandState.Running -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            CommandState.Success -> Text("ACCEPTED", color = Color(0xFF2E7D32))
            is CommandState.Failed -> Text(state.reason, color = Color(0xFFC62828))
        }
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
    DashboardScreen(DroneState(connected = true), mapOf(DroneCommand.ARM to CommandState.Success), {})
}
