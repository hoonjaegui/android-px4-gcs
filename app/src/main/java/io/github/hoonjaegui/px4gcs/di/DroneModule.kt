package io.github.hoonjaegui.px4gcs.di

import android.os.SystemClock
import io.github.hoonjaegui.px4gcs.BuildConfig
import io.github.hoonjaegui.px4gcs.core.common.time.Clock
import io.github.hoonjaegui.px4gcs.data.drone.DefaultDroneRepository
import io.github.hoonjaegui.px4gcs.data.mission.InMemoryMissionPlanRepository
import io.github.hoonjaegui.px4gcs.core.domain.drone.DroneRepository
import io.github.hoonjaegui.px4gcs.core.domain.mission.MissionPlanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

/** MAVSDK 접속 URL. 값은 app/build.gradle.kts의 gcs.connectionUrl 참고. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MavsdkConnectionUrl

/** Android 시계. 순수 Kotlin 모듈은 [Clock] interface만 안다. */
object AndroidClock : Clock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    override fun epochMs(): Long = System.currentTimeMillis()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @MavsdkConnectionUrl
    fun provideMavsdkConnectionUrl(): String = BuildConfig.MAVSDK_CONNECTION_URL

    @Provides
    fun provideClock(): Clock = AndroidClock
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DroneModule {
    @Binds
    abstract fun bindDroneRepository(impl: DefaultDroneRepository): DroneRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MissionModule {
    @Binds
    abstract fun bindMissionPlanRepository(impl: InMemoryMissionPlanRepository): MissionPlanRepository
}
