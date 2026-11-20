package io.github.hoonjaegui.px4gcs.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.hoonjaegui.px4gcs.core.common.coroutine.ApplicationScope
import io.github.hoonjaegui.px4gcs.core.common.coroutine.Dispatcher
import io.github.hoonjaegui.px4gcs.core.common.coroutine.Px4Dispatchers
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    /** 하나가 실패해도 나머지는 계속되도록 SupervisorJob. 기존 `CoroutineScope(SupervisorJob())`와 같이 Default에서 돈다. */
    @Provides
    @Singleton
    @ApplicationScope
    fun providesApplicationScope(
        @Dispatcher(Px4Dispatchers.Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
