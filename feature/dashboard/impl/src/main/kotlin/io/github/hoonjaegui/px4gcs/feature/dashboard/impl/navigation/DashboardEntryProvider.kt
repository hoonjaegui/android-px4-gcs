package io.github.hoonjaegui.px4gcs.feature.dashboard.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.hoonjaegui.px4gcs.feature.dashboard.api.DashboardNavKey
import io.github.hoonjaegui.px4gcs.feature.dashboard.impl.DashboardRoute

/** [DashboardNavKey] → 화면. app의 entryProvider에서 호출한다 (NIA `forYouEntry`와 같은 방식). */
fun EntryProviderScope<NavKey>.dashboardEntry() {
    entry<DashboardNavKey> { DashboardRoute() }
}
