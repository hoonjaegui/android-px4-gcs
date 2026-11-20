package io.github.hoonjaegui.px4gcs.core.data.mission.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.hoonjaegui.px4gcs.core.data.mission.InMemoryMissionPlanRepository
import io.github.hoonjaegui.px4gcs.core.domain.mission.MissionPlanRepository

/** domain의 interface ← 이 모듈의 구현. 앱은 이 모듈을 의존성에 넣기만 하면 된다. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MissionDataModule {
    @Binds
    abstract fun bindsMissionPlanRepository(impl: InMemoryMissionPlanRepository): MissionPlanRepository
}
