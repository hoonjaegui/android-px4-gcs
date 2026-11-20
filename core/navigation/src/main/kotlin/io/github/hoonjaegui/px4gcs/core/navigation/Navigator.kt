/*
 * Adapted from Now in Android (https://github.com/android/nowinandroid, commit 2982e63)
 * core/navigation — Copyright 2025 The Android Open Source Project, Apache License 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.hoonjaegui.px4gcs.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 화면 이동 규칙. 화면(Composable)은 Navigator를 받지 않고 콜백만 받는다 — 이동은 app/EntryProvider가 한다.
 */
class Navigator(val state: NavigationState) {

    /**
     * - 지금 탭을 다시 누름 → 그 탭의 첫 화면으로
     * - 다른 탭 → 그 탭으로 (시작 탭이면 탭 기록을 비움)
     * - 그 외 → 현재 탭 스택에 쌓음 (이미 있으면 맨 위로)
     */
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack()
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    /** 시작 탭의 첫 화면에서는 호출되지 않는다 — NavDisplay가 시스템(앱 종료)에 넘긴다. */
    fun goBack() {
        when {
            !state.isAtTopLevel -> state.currentSubStack.removeLastOrNull()
            state.currentTopLevelKey == state.startKey -> error("You cannot go back from the start route")
            else -> state.topLevelStack.removeLastOrNull()
        }
    }

    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            remove(key)
            add(key)
        }
    }

    private fun goToTopLevel(key: NavKey) {
        state.topLevelStack.apply {
            if (key == state.startKey) clear() else remove(key)
            add(key)
        }
    }

    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
