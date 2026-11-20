package io.github.hoonjaegui.px4gcs.navigation

import androidx.annotation.DrawableRes
import io.github.hoonjaegui.px4gcs.R

/** 화면(목적지)과 route 문자열. Grider_Rider `compose/navigation/Screen.kt`와 같은 방식. */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")

    data object MissionPlanner : Screen("mission_planner")
}

/** 하단 탭에 노출하는 최상위 화면. 순서 = 탭 순서. */
enum class TopLevelDestination(
    val screen: Screen,
    val label: String,
    @DrawableRes val icon: Int,
) {
    DASHBOARD(Screen.Dashboard, "Dashboard", R.drawable.ic_tab_dashboard),
    MISSION(Screen.MissionPlanner, "Mission", R.drawable.ic_tab_mission),
}
