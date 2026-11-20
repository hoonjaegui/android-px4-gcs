package io.github.hoonjaegui.px4gcs.core.common.time

/**
 * 시간 출처. Android의 `SystemClock`을 직접 부르면 순수 Kotlin 모듈에서 쓸 수 없고 테스트에서 시간을 조작할 수 없다.
 * 구현은 앱이 주입한다(Android: `SystemClock.elapsedRealtime` / `System.currentTimeMillis`).
 */
interface Clock {
    /** 단조 증가(monotonic) 시각 ms. 경과 시간 계산용. 시스템 시간 변경에 영향 없음. */
    fun elapsedRealtimeMs(): Long

    /** 벽시계 ms (epoch). "17:36:45" 같은 표시용. */
    fun epochMs(): Long
}
