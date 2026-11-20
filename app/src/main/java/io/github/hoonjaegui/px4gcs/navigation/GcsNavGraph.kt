package io.github.hoonjaegui.px4gcs.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.hoonjaegui.px4gcs.feature.dashboard.DashboardRoute
import io.github.hoonjaegui.px4gcs.feature.mission.MissionPlannerRoute

/**
 * 앱 전체 화면 구성. Scaffold의 하단 탭 + NavHost.
 *
 * 각 화면(Route)은 navController를 받지 않는다 — 이동이 필요하면 콜백(onXxx)으로 받고 여기서 navigate한다.
 * (Grider_Rider NavGraph와 같은 방식. 화면이 Navigation을 몰라야 Preview/테스트가 쉽다.)
 * ViewModel은 각 Route 안의 hiltViewModel()이 그 화면의 NavBackStackEntry에 묶어 만든다.
 */
@Composable
fun GcsNavGraph(navController: NavHostController = rememberNavController()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { GcsBottomBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            /** 기체 상태 / 명령 / 장애 표시 (Phase 1). */
            composable(route = Screen.Dashboard.route) {
                DashboardRoute()
            }

            /** Mission 편집. 업로드/실행은 Week 3. */
            composable(route = Screen.MissionPlanner.route) {
                MissionPlannerRoute()
            }
        }
    }
}

@Composable
private fun GcsBottomBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination) },
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

/**
 * 탭 이동의 표준 패턴.
 * - popUpTo(시작 화면): 탭을 오가도 back stack이 쌓이지 않는다 → 뒤로 가기는 항상 Dashboard로, 거기서 한 번 더면 종료
 * - saveState / restoreState: 떠난 탭의 상태(ViewModel, 스크롤 위치 등)를 저장했다가 돌아오면 복원
 * - launchSingleTop: 같은 탭을 다시 눌러도 화면이 중복 생성되지 않음
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
