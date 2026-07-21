package com.nilpo.contenttracker.ui

import androidx.navigation3.runtime.NavKey
import com.nilpo.contenttracker.ui.home.MediaSection
import com.nilpo.contenttracker.ui.navigation.AppRoute
import com.nilpo.contenttracker.ui.navigation.goBack
import com.nilpo.contenttracker.ui.navigation.push
import com.nilpo.contenttracker.ui.navigation.selectHome
import com.nilpo.contenttracker.ui.navigation.selectSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailNavigationHistoryTest {
    @Test
    fun backPopsTheExactNavigationPath() {
        val stack = mutableListOf<NavKey>(AppRoute.Home)
        stack.push(AppRoute.Section(MediaSection.Books))
        stack.push(AppRoute.CollectionDetail(9L, MediaSection.Books))
        stack.push(AppRoute.MediaDetail(101L))
        stack.push(AppRoute.MediaDetail(202L))

        assertTrue(stack.goBack())
        assertEquals(AppRoute.MediaDetail(101L), stack.last())
        assertTrue(stack.goBack())
        assertEquals(AppRoute.CollectionDetail(9L, MediaSection.Books), stack.last())
        assertTrue(stack.goBack())
        assertEquals(AppRoute.Section(MediaSection.Books), stack.last())
    }

    @Test
    fun bottomNavigationSelectsARootWithoutLeavingDrillDownHistory() {
        val stack = mutableListOf<NavKey>(
            AppRoute.Home,
            AppRoute.Section(MediaSection.Anime),
            AppRoute.MediaDetail(101L),
        )

        stack.selectSection(MediaSection.Games)
        assertEquals(
            listOf(AppRoute.Home, AppRoute.Section(MediaSection.Games)),
            stack,
        )

        stack.selectHome()
        assertEquals(listOf(AppRoute.Home), stack)
        assertFalse(stack.goBack())
    }

    @Test
    fun selectingTheActiveSectionPopsDrillDownButKeepsItsRootEntry() {
        val sectionRoute = AppRoute.Section(MediaSection.Anime)
        val stack = mutableListOf<NavKey>(
            AppRoute.Home,
            sectionRoute,
            AppRoute.MediaDetail(101L),
        )

        stack.selectSection(MediaSection.Anime)

        assertEquals(listOf(AppRoute.Home, sectionRoute), stack)
    }

    @Test
    fun timelineAndDetailReturnToTheHomeDrillInPath() {
        val stack = mutableListOf<NavKey>(AppRoute.Home)
        stack.push(AppRoute.Timeline)
        stack.push(AppRoute.MediaDetail(101L))

        assertTrue(stack.goBack())
        assertEquals(AppRoute.Timeline, stack.last())
        assertEquals(AppRoute.Home, stack.first())
    }
}
