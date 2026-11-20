package io.github.hoonjaegui.px4gcs.di

import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.hoonjaegui.px4gcs.BuildConfig
import io.github.hoonjaegui.px4gcs.core.common.time.Clock
import io.github.hoonjaegui.px4gcs.core.data.drone.di.MavsdkConnectionUrl

/** Android 시계. 순수 Kotlin 모듈은 [Clock] interface만 안다. */
object AndroidClock : Clock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    override fun epochMs(): Long = System.currentTimeMillis()
}

/**
 * 앱만 알 수 있는 값(빌드 설정, Android 플랫폼)을 제공한다.
 * Repository 바인딩은 각 data 모듈(`core:data:*`)의 Hilt 모듈이 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** 값은 app/build.gradle.kts의 `gcs.connectionUrl`. */
    @Provides
    @MavsdkConnectionUrl
    fun provideMavsdkConnectionUrl(): String = BuildConfig.MAVSDK_CONNECTION_URL

    @Provides
    fun provideClock(): Clock = AndroidClock
}
