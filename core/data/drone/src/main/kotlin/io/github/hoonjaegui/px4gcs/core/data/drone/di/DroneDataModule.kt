package io.github.hoonjaegui.px4gcs.core.data.drone.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.hoonjaegui.px4gcs.core.data.drone.DefaultDroneRepository
import io.github.hoonjaegui.px4gcs.core.domain.drone.DroneRepository

/** domain의 interface ← 이 모듈의 MAVSDK 구현. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DroneDataModule {
    @Binds
    abstract fun bindsDroneRepository(impl: DefaultDroneRepository): DroneRepository
}
