package io.github.hoonjaegui.px4gcs.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.hoonjaegui.px4gcs.core.navigation.NavigationState
import io.github.hoonjaegui.px4gcs.core.navigation.Navigator
import io.github.hoonjaegui.px4gcs.core.navigation.rememberNavigationState
import io.github.hoonjaegui.px4gcs.core.navigation.toEntries
import io.github.hoonjaegui.px4gcs.feature.dashboard.DashboardRoute
import io.github.hoonjaegui.px4gcs.feature.mission.MissionPlannerRoute

/**
 * 앱 전체 화면 구성: 하단 탭 + NavDisplay (Navigation 3, NIA 방식).
 *
 * 각 화면(Route)은 Navigator를 받지 않는다 — 이동이 필요하면 콜백으로 받고 여기서 navigate한다.
 * ViewModel은 Route 안의 hiltViewModel()이 그 화면(NavEntry) 전용 ViewModelStore에 만든다.
 * 뒤로 가기: Mission 탭 → Dashboard, Dashboard(시작 화면) → 앱 종료.
 */
@Composable
fun GcsNavGraph() {
    val navigationState = rememberNavigationState(
        startKey = DashboardNavKey,
        topLevelKeys = TopLevelDestination.entries.map { it.key }.toSet(),
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = entryProvider {
        /** 기체 상태 / 명령 / 장애 표시 (Phase 1). */
        entry<DashboardNavKey> { DashboardRoute() }

        /** Mission 편집. 업로드/실행은 Week 3. */
        entry<MissionPlannerNavKey> { MissionPlannerRoute() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { GcsBottomBar(navigationState, navigator) },
    ) { innerPadding ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun GcsBottomBar(state: NavigationState, navigator: Navigator) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = state.currentTopLevelKey == destination.key,
                onClick = { navigator.navigate(destination.key) },
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}
