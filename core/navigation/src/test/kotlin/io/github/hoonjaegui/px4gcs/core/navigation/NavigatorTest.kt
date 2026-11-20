package io.github.hoonjaegui.px4gcs.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private object DashboardKey : NavKey
private object MissionKey : NavKey
private object DetailKey : NavKey

/** 탭 2개(Dashboard 시작, Mission) 기준. NIA NavigatorTest를 이 앱 상황으로 줄였다. */
class NavigatorTest {

    private val state = NavigationState(
        startKey = DashboardKey,
        topLevelStack = NavBackStack(DashboardKey),
        subStacks = listOf(DashboardKey, MissionKey).associateWith { NavBackStack(it) },
    )
    private val navigator = Navigator(state)

    @Test
    fun `starts on dashboard`() {
        assertEquals(DashboardKey, state.currentKey)
    }

    @Test
    fun `tab switch then back returns to dashboard`() {
        navigator.navigate(MissionKey)
        assertEquals(MissionKey, state.currentTopLevelKey)
        assertEquals(listOf<NavKey>(DashboardKey, MissionKey), state.topLevelStack.toList())

        navigator.goBack()
        assertEquals(DashboardKey, state.currentTopLevelKey)
    }

    @Test
    fun `switching tabs does not pile up history`() {
        repeat(3) {
            navigator.navigate(MissionKey)
            navigator.navigate(DashboardKey)
        }
        assertEquals(listOf<NavKey>(DashboardKey), state.topLevelStack.toList())
    }

    @Test
    fun `detail screen stacks inside current tab and reselecting the tab clears it`() {
        navigator.navigate(MissionKey)
        navigator.navigate(DetailKey)
        assertEquals(listOf<NavKey>(MissionKey, DetailKey), state.currentSubStack.toList())

        navigator.navigate(MissionKey)
        assertEquals(listOf<NavKey>(MissionKey), state.currentSubStack.toList())
    }

    @Test
    fun `back from start screen is not allowed`() {
        assertThrows(IllegalStateException::class.java) { navigator.goBack() }
    }
}
