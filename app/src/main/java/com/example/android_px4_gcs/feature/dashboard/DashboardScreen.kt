package com.example.android_px4_gcs.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_px4_gcs.core.model.CommandFailureReason
import com.example.android_px4_gcs.core.model.CommandState
import com.example.android_px4_gcs.core.model.ConnectionStatus
import com.example.android_px4_gcs.core.model.DroneCommand
import com.example.android_px4_gcs.core.model.DroneState
import com.example.android_px4_gcs.core.model.DroneWarningState
import com.example.android_px4_gcs.core.model.TelemetryField
import com.example.android_px4_gcs.core.model.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Danger = Color(0xFFC62828)
private val Warning = Color(0xFFE65100)
private val Ok = Color(0xFF2E7D32)
private val Stale = Color(0xFF9E9E9E)

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(uiState, viewModel::onCommand, modifier)
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onCommand: (DroneCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = uiState.drone
    val warnings = uiState.warnings
    /** "16:24:05 (8s ago)". 절대 시각은 기록/대조용, 경과 시간은 한눈에 판단용. */
    fun Timestamp.describe(): String =
        "${TIME_FORMAT.format(Date(epochMs))} (${(uiState.nowElapsedMs - elapsedMs) / 1000}s ago)"

    /** 오래된 필드면 "Last update ..." 문구, 신선하면 null. */
    fun lastUpdate(field: TelemetryField): String? =
        if (field in warnings.staleFields) state.receivedAt[field]?.let { "Last update ${it.describe()}" } else null

    // 연결이 끊기면 HEARTBEAT 기반 값(armed, flight mode)도 "마지막으로 알던 값"일 뿐이다.
    val heartbeatNote = state.connectionLostAt?.let { "Last known value — link lost ${it.describe()}" }

    // GPS/EKF가 불량이면 위경도는 갱신을 멈춘 "마지막 GPS 위치"다. 3초 기준과 무관하게 바로 과거 값으로 표시한다.
    val positionNote = when {
        state.latitude == null -> null
        !state.positionTrusted -> state.receivedAt[TelemetryField.POSITION]?.let { "Last GPS position ${it.describe()}" }
        else -> lastUpdate(TelemetryField.POSITION)
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("PX4 GCS", style = MaterialTheme.typography.headlineMedium)
        WarningBanners(warnings)

        TelemetryItem(
            label = "Connection",
            value = when (state.connection) {
                ConnectionStatus.SEARCHING -> "SEARCHING..."
                ConnectionStatus.CONNECTED -> "CONNECTED"
                ConnectionStatus.LOST -> "LOST"
            },
            valueColor = when (state.connection) {
                ConnectionStatus.SEARCHING -> Stale
                ConnectionStatus.CONNECTED -> Ok
                ConnectionStatus.LOST -> Danger
            },
        )
        TelemetryItem("Flight Mode", state.flightMode ?: NO_DATA, staleNote = heartbeatNote)
        TelemetryItem(
            label = "Armed",
            value = if (state.armed) "TRUE" else "FALSE",
            valueColor = if (state.armed) Warning else Color.Unspecified,
            staleNote = heartbeatNote,
        )
        TelemetryItem(
            label = "Battery",
            value = state.batteryPercent?.let { "$it%" } ?: NO_DATA,
            valueColor = if (warnings.lowBattery) Danger else Color.Unspecified,
            staleNote = lastUpdate(TelemetryField.BATTERY),
        )
        TelemetryItem(
            label = "GPS",
            value = when (state.gpsAvailable) {
                null -> NO_DATA
                true -> "3D FIX"
                false -> "NO FIX"
            },
            valueColor = if (warnings.gpsUnavailable) Danger else Color.Unspecified,
            staleNote = lastUpdate(TelemetryField.GPS),
        )
        TelemetryItem("Altitude", state.altitude?.let { "%.1fm".format(it) } ?: NO_DATA, staleNote = lastUpdate(TelemetryField.ALTITUDE))
        TelemetryItem("Heading", state.heading?.let { "%.0f°".format(it) } ?: NO_DATA, staleNote = lastUpdate(TelemetryField.HEADING))
        TelemetryItem(
            label = "Latitude / Longitude",
            value = if (state.latitude != null && state.longitude != null) {
                "%.7f, %.7f".format(state.latitude, state.longitude)
            } else {
                NO_DATA
            },
            staleNote = positionNote,
            // GPS가 끊긴 뒤 PX4가 보내는 추정 위경도(dead reckoning)는 쓰지 않는다.
            note = if (!state.positionTrusted && state.latitude != null) "Not updating — PX4 estimate ignored until GPS returns" else null,
        )

        HorizontalDivider()
        DroneCommand.entries.forEach { command ->
            CommandRow(
                command = command,
                state = uiState.commandStates[command] ?: CommandState.Idle,
                // 연결이 없으면 명령을 보내지 않는다. 보내도 기체에 닿지 않는다.
                enabled = state.isConnected,
                onCommand = onCommand,
            )
        }
    }
}

@Composable
private fun WarningBanners(warnings: DroneWarningState) {
    val messages = buildList {
        if (warnings.connectionLost) add("CONNECTION LOST")
        if (warnings.telemetryStale) add("TELEMETRY LOST")
        if (warnings.gpsUnavailable) add("GPS SIGNAL LOST")
        if (warnings.positionUnreliable) add("POSITION UNRELIABLE")
        if (warnings.lowBattery) add("LOW BATTERY")
    }
    messages.forEach { message ->
        Text(
            text = "⚠ $message",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .background(Danger, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private const val NO_DATA = "--"
private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    /** 과거 값이면 그 설명 ("Last update 16:24:05 (8s ago)"). null이면 신선함. */
    staleNote: String? = null,
    note: String? = null,
) {
    val isStale = staleNote != null
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = if (isStale) Stale else valueColor)
        staleNote?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Warning) }
        note?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Danger) }
    }
}

@Composable
private fun CommandRow(
    command: DroneCommand,
    state: CommandState,
    enabled: Boolean,
    onCommand: (DroneCommand) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { onCommand(command) }, enabled = enabled && state != CommandState.Running) {
            Text(command.name)
        }
        when (state) {
            CommandState.Idle -> Unit
            CommandState.Running -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            CommandState.Success -> Text("ACCEPTED", color = Ok)
            is CommandState.Failed -> Column {
                Text(state.reason.headline(), color = Danger, fontWeight = FontWeight.Bold)
                Text(state.reason.guidance(), style = MaterialTheme.typography.labelMedium)
                Text(state.detail, style = MaterialTheme.typography.labelSmall, color = Stale)
            }
        }
    }
}

private fun CommandFailureReason.headline(): String = when (this) {
    CommandFailureReason.NOT_CONNECTED -> "NOT CONNECTED"
    CommandFailureReason.REJECTED -> "REJECTED"
    CommandFailureReason.BUSY -> "BUSY"
    CommandFailureReason.TIMEOUT -> "NO RESPONSE"
    CommandFailureReason.UNKNOWN -> "FAILED"
}

/** 운용자가 다음에 무엇을 해야 하는가. */
private fun CommandFailureReason.guidance(): String = when (this) {
    CommandFailureReason.NOT_CONNECTED -> "Not sent. Vehicle state unchanged."
    CommandFailureReason.REJECTED -> "Vehicle refused. State unchanged."
    CommandFailureReason.BUSY -> "Previous command in progress."
    CommandFailureReason.TIMEOUT -> "May or may not have executed. Check telemetry."
    CommandFailureReason.UNKNOWN -> "Check telemetry."
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    DashboardScreen(
        DashboardUiState(
            drone = DroneState(
                connection = ConnectionStatus.LOST,
                connectionLostAt = Timestamp(elapsedMs = 2_000L, epochMs = 0L),
                altitude = 32.0,
                batteryPercent = 15,
                gpsAvailable = false,
                latitude = 47.3977,
                longitude = 8.5456,
                receivedAt = mapOf(TelemetryField.POSITION to Timestamp(0L, 0L), TelemetryField.BATTERY to Timestamp(0L, 0L)),
            ),
            warnings = DroneWarningState(
                connectionLost = true,
                staleFields = setOf(TelemetryField.POSITION, TelemetryField.BATTERY),
                gpsUnavailable = true,
                lowBattery = true,
            ),
            commandStates = mapOf(DroneCommand.ARM to CommandState.Failed(CommandFailureReason.TIMEOUT, "TIMEOUT: Timeout")),
            nowElapsedMs = 8_000L,
        ),
        onCommand = {},
    )
}
