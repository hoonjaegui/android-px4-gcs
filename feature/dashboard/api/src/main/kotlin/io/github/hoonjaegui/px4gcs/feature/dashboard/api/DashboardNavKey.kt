package io.github.hoonjaegui.px4gcs.feature.dashboard.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Dashboard 화면으로 가는 키. @Serializable: back stack 저장/복원용. */
@Serializable
data object DashboardNavKey : NavKey
