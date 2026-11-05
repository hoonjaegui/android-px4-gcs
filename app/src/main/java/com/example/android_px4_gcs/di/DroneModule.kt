package com.example.android_px4_gcs.di

import com.example.android_px4_gcs.BuildConfig
import com.example.android_px4_gcs.data.drone.DefaultDroneRepository
import com.example.android_px4_gcs.domain.drone.DroneRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** 앱 프로세스 수명 동안 유지되는 스코프. Drone 연결은 화면 회전/재생성과 무관하게 유지된다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** MAVSDK 접속 URL. 값은 app/build.gradle.kts의 gcs.connectionUrl 참고. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MavsdkConnectionUrl

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @MavsdkConnectionUrl
    fun provideMavsdkConnectionUrl(): String = BuildConfig.MAVSDK_CONNECTION_URL
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DroneModule {
    @Binds
    abstract fun bindDroneRepository(impl: DefaultDroneRepository): DroneRepository
}
