package io.github.hoonjaegui.px4gcs.navigation

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import io.github.hoonjaegui.px4gcs.R
import io.github.hoonjaegui.px4gcs.feature.dashboard.api.DashboardNavKey
import io.github.hoonjaegui.px4gcs.feature.mission.api.MissionPlannerNavKey

/** 하단 탭에 노출하는 최상위 화면. 순서 = 탭 순서. */
enum class TopLevelDestination(
    val key: NavKey,
    val label: String,
    @DrawableRes val icon: Int,
) {
    DASHBOARD(DashboardNavKey, "Dashboard", R.drawable.ic_tab_dashboard),
    MISSION(MissionPlannerNavKey, "Mission", R.drawable.ic_tab_mission),
}
