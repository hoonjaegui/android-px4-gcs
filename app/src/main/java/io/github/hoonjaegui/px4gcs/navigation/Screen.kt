package io.github.hoonjaegui.px4gcs.navigation

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import io.github.hoonjaegui.px4gcs.R
import kotlinx.serialization.Serializable

/*
 * Navigation 3의 목적지는 문자열 route가 아니라 타입(NavKey)이다.
 * @Serializable: 화면 회전·프로세스 재시작 때 back stack을 저장/복원하기 위해 필요.
 * (11단계에서 각 feature의 api 모듈로 옮긴다)
 */
@Serializable
data object DashboardNavKey : NavKey

@Serializable
data object MissionPlannerNavKey : NavKey

/** 하단 탭에 노출하는 최상위 화면. 순서 = 탭 순서. */
enum class TopLevelDestination(
    val key: NavKey,
    val label: String,
    @DrawableRes val icon: Int,
) {
    DASHBOARD(DashboardNavKey, "Dashboard", R.drawable.ic_tab_dashboard),
    MISSION(MissionPlannerNavKey, "Mission", R.drawable.ic_tab_mission),
}
