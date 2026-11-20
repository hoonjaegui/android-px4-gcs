/*
 * Adapted from Now in Android (https://github.com/android/nowinandroid, commit 2982e63)
 * core/navigation — Copyright 2025 The Android Open Source Project, Apache License 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.hoonjaegui.px4gcs.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * 탭(최상위 화면)마다 자기 back stack을 갖는 구조.
 * - [topLevelStack]: 방문한 탭 순서. 예) [Dashboard, Mission] — 뒤로 가기 시 Mission → Dashboard
 * - [subStacks]: 탭별 화면 스택. 지금은 탭마다 화면 하나지만, 상세 화면이 생기면 여기에 쌓인다.
 *
 * Navigation 3에서는 back stack이 그냥 관찰 가능한 리스트다. 리스트를 바꾸면 화면이 바뀐다.
 */
@Composable
fun rememberNavigationState(
    startKey: NavKey,
    topLevelKeys: Set<NavKey>,
): NavigationState {
    val topLevelStack = rememberNavBackStack(startKey)
    val subStacks = topLevelKeys.associateWith { key -> rememberNavBackStack(key) }
    return remember(startKey, topLevelKeys) {
        NavigationState(startKey = startKey, topLevelStack = topLevelStack, subStacks = subStacks)
    }
}

class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val topLevelKeys: Set<NavKey>
        get() = subStacks.keys

    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelKey] ?: error("Sub stack for $currentTopLevelKey does not exist")

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }

    val isAtTopLevel: Boolean by derivedStateOf { currentSubStack.size == 1 }
}

/**
 * NavDisplay에 넘길 화면 목록. 각 화면에
 * - rememberSaveable 상태 보관(SaveableStateHolder)
 * - 화면 전용 ViewModelStore (hiltViewModel()이 여기에 ViewModel을 만든다)
 * 를 붙인다(decorator).
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = subStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }
    return topLevelStack
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
