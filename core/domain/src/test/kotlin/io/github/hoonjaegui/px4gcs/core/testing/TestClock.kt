package io.github.hoonjaegui.px4gcs.core.testing

import io.github.hoonjaegui.px4gcs.core.common.time.Clock

/**
 * 테스트가 정하는 시계. [elapsedMs]를 람다로 받으므로 runTest의 가상 시간(`testScheduler.currentTime`)에 묶을 수 있다:
 * `TestClock { startMs + testScheduler.currentTime }` → `advanceTimeBy(3_000)` 하면 시계도 3초 흐른다.
 */
class TestClock(
    private val epochOffsetMs: Long = 1_700_000_000_000L,
    private val elapsedMs: () -> Long,
) : Clock {
    override fun elapsedRealtimeMs(): Long = elapsedMs()
    override fun epochMs(): Long = epochOffsetMs + elapsedMs()
}
