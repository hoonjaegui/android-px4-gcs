package io.github.hoonjaegui.px4gcs.feature.mission.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.hoonjaegui.px4gcs.feature.mission.api.MissionPlannerNavKey
import io.github.hoonjaegui.px4gcs.feature.mission.impl.MissionPlannerRoute

/** [MissionPlannerNavKey] → 화면. 업로드/실행은 Week 3. */
fun EntryProviderScope<NavKey>.missionPlannerEntry() {
    entry<MissionPlannerNavKey> { MissionPlannerRoute() }
}
