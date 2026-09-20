package com.huanchengfly.tieba.post.ui.page.thread

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentNavigationStateTest {
    @Test fun longFinalFloorDoesNotRequireListBottom() {
        val feedback = CommentNavigationEndFeedback()
        assertTrue(feedback.onArrived(CommentNavDirection.NEXT, 3, listOf(1, 2, 3), false, false, true))
        assertFalse(feedback.onArrived(CommentNavDirection.NEXT, 3, listOf(1, 2, 3), false, true, true))
    }

    @Test fun shortFloorsCanReachBottomBeforeTargetingLastFloor() {
        val feedback = CommentNavigationEndFeedback()
        assertFalse(feedback.onArrived(CommentNavDirection.NEXT, 2, listOf(1, 2, 3), false, true, false))
        assertTrue(feedback.onArrived(CommentNavDirection.NEXT, 2, listOf(1, 2, 3), false, true, true))
    }

    @Test fun previousAndUnfinishedPagesDoNotVibrateAndReturningCanVibrateAgain() {
        val feedback = CommentNavigationEndFeedback()
        assertFalse(feedback.onArrived(CommentNavDirection.NEXT, 3, listOf(1, 2, 3), true, true, true))
        assertTrue(feedback.onArrived(CommentNavDirection.NEXT, 3, listOf(1, 2, 3), false, false, true))
        assertFalse(feedback.onArrived(CommentNavDirection.PREV, 2, listOf(1, 2, 3), false, true, true))
        assertTrue(feedback.onArrived(CommentNavDirection.NEXT, 3, listOf(1, 2, 3), false, false, true))
    }

    @Test fun interruptedScrollNeverSignalsArrivalAndClearsBusyState() = runTest {
        val nav = CommentNavigationScrollState()
        var arrived = false
        var cancelled = false
        nav.launch(this, { cancelled = true }, { arrived = true }) { throw CancellationException() }
        runCurrent()
        assertFalse(arrived)
        assertTrue(cancelled)
        assertFalse(nav.isScrolling)
    }

    @Test fun replacingScrollOnlyCompletesLatestNavigation() = runTest {
        val nav = CommentNavigationScrollState()
        val arrivals = mutableListOf<Int>()
        nav.launch(this, {}, { arrivals += 1 }) { delay(100) }
        runCurrent()
        nav.launch(this, {}, { arrivals += 2 }) { delay(200) }
        runCurrent()
        assertTrue(nav.isScrolling)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(2), arrivals)
        assertFalse(nav.isScrolling)
    }
}
