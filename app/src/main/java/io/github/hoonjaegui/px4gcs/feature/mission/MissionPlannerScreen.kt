package io.github.hoonjaegui.px4gcs.feature.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidationError
import io.github.hoonjaegui.px4gcs.core.mission.MissionValidator
import io.github.hoonjaegui.px4gcs.core.mission.Waypoint
import io.github.hoonjaegui.px4gcs.core.mission.WaypointProblem
import java.util.Locale

@Composable
fun MissionPlannerRoute(
    modifier: Modifier = Modifier,
    viewModel: MissionPlannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MissionPlannerScreen(
        uiState = uiState,
        actions = MissionPlannerActions(
            onAdd = viewModel::onAddWaypoint,
            onRemove = viewModel::onRemoveWaypoint,
            onMove = viewModel::onMoveWaypoint,
            onAltitudeSubmitted = viewModel::onAltitudeSubmitted,
            onClear = viewModel::onClear,
        ),
        modifier = modifier,
    )
}

class MissionPlannerActions(
    val onAdd: (latitudeDeg: Double, longitudeDeg: Double) -> Unit,
    val onRemove: (index: Int) -> Unit,
    val onMove: (from: Int, to: Int) -> Unit,
    val onAltitudeSubmitted: (index: Int, input: String) -> Unit,
    val onClear: () -> Unit,
)

/** 위: 지도(탭 = Waypoint 추가). 아래: 거부 사유, plan 문제, Waypoint 목록. */
@Composable
fun MissionPlannerScreen(
    uiState: MissionPlannerUiState,
    actions: MissionPlannerActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MissionMap(
                waypoints = uiState.plan.waypoints,
                vehicle = uiState.vehicle,
                onMapClick = actions.onAdd,
                modifier = Modifier.fillMaxSize(),
            )
            // OSM 데이터 사용 조건: 출처를 보이게 표시
            Text(
                text = "© OpenFreeMap © OpenStreetMap",
                fontSize = 10.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        HorizontalDivider()
        MissionPanel(uiState, actions, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun MissionPanel(
    uiState: MissionPlannerUiState,
    actions: MissionPlannerActions,
    modifier: Modifier = Modifier,
) {
    val waypoints = uiState.plan.waypoints
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Waypoints ${waypoints.size} · " + if (uiState.isValid) "Plan valid" else "Not ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onClear, enabled = waypoints.isNotEmpty()) { Text("Clear") }
            }
            Text(
                text = "Tap the map to add. Altitude is relative to home (${MissionValidator.MIN_ALTITUDE_M.fmt()}–${MissionValidator.MAX_ALTITUDE_M.fmt()} m).",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        uiState.rejection?.let { rejection ->
            item { Notice("Rejected: ${rejection.message()}", MaterialTheme.colorScheme.errorContainer) }
        }

        items(uiState.errors) { error ->
            Notice(error.message(), MaterialTheme.colorScheme.secondaryContainer)
        }

        itemsIndexed(waypoints) { index, waypoint ->
            WaypointRow(
                index = index,
                waypoint = waypoint,
                isLast = index == waypoints.lastIndex,
                rejection = uiState.rejection,
                actions = actions,
            )
        }
    }
}

@Composable
private fun WaypointRow(
    index: Int,
    waypoint: Waypoint,
    isLast: Boolean,
    rejection: EditRejection?,
    actions: MissionPlannerActions,
) {
    val focusManager = LocalFocusManager.current
    // 입력 칸은 "기체에 보낼 실제 plan 값"을 보여 준다. 입력이 거부되면(rejection 변경) 실제 값으로 되돌린다.
    var altitudeText by remember(index, waypoint.relativeAltitudeM, rejection) {
        mutableStateOf(waypoint.relativeAltitudeM.fmt())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("#${index + 1}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = String.format(Locale.US, "%.6f\n%.6f", waypoint.latitudeDeg, waypoint.longitudeDeg),
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
        }
        OutlinedTextField(
            value = altitudeText,
            onValueChange = { altitudeText = it },
            singleLine = true,
            suffix = { Text("m") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                actions.onAltitudeSubmitted(index, altitudeText)
                focusManager.clearFocus()
            }),
            modifier = Modifier.width(96.dp),
        )
        IconButton(onClick = { actions.onMove(index, index - 1) }, enabled = index > 0) { Text("▲") }
        IconButton(onClick = { actions.onMove(index, index + 1) }, enabled = !isLast) { Text("▼") }
        IconButton(onClick = { actions.onRemove(index) }) { Text("✕", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun Notice(text: String, color: Color) {
    Surface(color = color, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
    }
}

private fun Double.fmt(): String =
    if (this % 1.0 == 0.0) String.format(Locale.US, "%.0f", this) else String.format(Locale.US, "%.1f", this)

private fun WaypointProblem.message(): String = when (this) {
    WaypointProblem.INVALID_LATITUDE -> "latitude must be between -90 and 90"
    WaypointProblem.INVALID_LONGITUDE -> "longitude must be between -180 and 180"
    WaypointProblem.INVALID_ALTITUDE -> "altitude is not a valid number"
    WaypointProblem.ALTITUDE_TOO_LOW -> "altitude must be at least ${MissionValidator.MIN_ALTITUDE_M.fmt()} m"
    WaypointProblem.ALTITUDE_TOO_HIGH -> "altitude must be at most ${MissionValidator.MAX_ALTITUDE_M.fmt()} m"
}

private fun EditRejection.message(): String = when (this) {
    is EditRejection.NotANumber -> "\"$input\" is not a number"
    is EditRejection.InvalidWaypoint -> problems.joinToString(", ") { it.message() }
    is EditRejection.TooCloseToPrevious ->
        "only ${String.format(Locale.US, "%.1f", distanceM)} m from the previous waypoint " +
            "(min ${MissionValidator.MIN_SEGMENT_M.fmt()} m)"
}

private fun MissionValidationError.message(): String = when (this) {
    MissionValidationError.Empty -> "Add at least one waypoint."
    is MissionValidationError.InvalidWaypoint -> "#${index + 1}: ${problem.message()}"
    is MissionValidationError.TooCloseToPrevious ->
        "#${index + 1} is only ${String.format(Locale.US, "%.1f", distanceM)} m from #$index"
}
