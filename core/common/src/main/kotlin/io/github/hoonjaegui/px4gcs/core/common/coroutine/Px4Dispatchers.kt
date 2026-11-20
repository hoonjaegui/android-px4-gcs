package io.github.hoonjaegui.px4gcs.core.common.coroutine

import javax.inject.Qualifier

/**
 * 주입받는 CoroutineDispatcher를 구분한다. `Dispatchers.IO`를 코드에 직접 쓰지 않고 주입받으면
 * 테스트에서 TestDispatcher로 바꿀 수 있다. (NIA `NiaDispatchers`와 같은 방식)
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: Px4Dispatchers)

enum class Px4Dispatchers {
    Default,
    IO,
}
