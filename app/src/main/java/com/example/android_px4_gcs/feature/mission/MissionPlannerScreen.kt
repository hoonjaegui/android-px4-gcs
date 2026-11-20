package com.example.android_px4_gcs.feature.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MissionPlannerRoute(
    modifier: Modifier = Modifier,
    viewModel: MissionPlannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MissionPlannerScreen(uiState, modifier)
}

/** A4-1: Navigation 확인용 뼈대. 지도와 Waypoint 편집은 A4-2. */
@Composable
fun MissionPlannerScreen(
    uiState: MissionPlannerUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Mission Planner", style = MaterialTheme.typography.headlineSmall)
        Text("Waypoints: ${uiState.plan.size}")
        Text(if (uiState.isValid) "Plan valid" else "Problems: ${uiState.errors.size}")
    }
}
